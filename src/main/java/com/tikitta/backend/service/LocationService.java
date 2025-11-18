package com.tikitta.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.LocationViewResponse;
import com.tikitta.backend.dto.venue.*;
import com.tikitta.backend.repository.LocationMapRepository;
import com.tikitta.backend.repository.LocationRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.repository.SeatRepository;
import com.tikitta.backend.util.AuthUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final ManagerRepository managerRepository;
    private final SeatRepository seatRepository;
    private final LocationMapRepository locationMapRepository;
    private final AuthUtil authUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String uploadDir = "src/main/resources/static/images/";
    // ... (다른 메소드들은 그대로 유지)
    @Transactional
    public Location registerLocation(VenueRegisterRequest request, MultipartFile locationPicture) {
        String pictureUrl = saveFile(locationPicture);

        DomainEnums.LocationType type;
        Integer totalSeats;
        Integer floor;

        if (request.getLocationStandingCount() != null && request.getLocationStandingCount() > 0) {
            type = DomainEnums.LocationType.STANDING;
            totalSeats = request.getLocationStandingCount();
            floor = 0;
        } else {
            type = DomainEnums.LocationType.SEATED;
            totalSeats = request.getLocationSeatCount();
            floor = request.getLocationSeatFloor() != null ? request.getLocationSeatFloor() : 1;
        }

        Location location = Location.builder()
                .name(request.getLocationName())
                .address(request.getLocationAddress())
                .addressDetail(request.getLocationAddressDetail())
                .seatPictureUrl(pictureUrl)
                .totalSeats(totalSeats)
                .floor(floor)
                .type(type)
                .build();

        locationRepository.save(location);

        KakaoOauth currentUser = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(currentUser)
            .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));
        
        manager.getLikedLocations().add(location);
        managerRepository.save(manager);
        return location;
    }

    @Transactional
    public void registerSeatmap(VenueSeatmapRequest request) {
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + request.getLocationId()));

        try {
            List<List<Integer>> stageCoordinates = new ArrayList<>();
            List<List<Object>> seatMapList = request.getSeatMap();
            for (int i = 0; i < seatMapList.size(); i++) {
                for (int j = 0; j < seatMapList.get(i).size(); j++) {
                    Object cell = seatMapList.get(i).get(j);
                    if (cell instanceof Integer && (Integer) cell == -1) {
                        stageCoordinates.add(List.of(i, j));
                    }
                }
            }

            String seatMapJson = objectMapper.writeValueAsString(request.getSeatMap());
            String stageCoordinatesJson = objectMapper.writeValueAsString(stageCoordinates);

            LocationMap locationMap = LocationMap.builder()
                    .location(location)
                    .layoutWidth(request.getLayoutWidth())
                    .layoutHeight(request.getLayoutHeight())
                    .seatMapData(seatMapJson)
                    .stageCoordinates(stageCoordinatesJson)
                    .build();
            locationMapRepository.save(locationMap);

            List<Seat> seats = new ArrayList<>();
            if (request.getSeatData() != null) {
                for (SeatData data : request.getSeatData().values()) {
                    Seat seat = Seat.builder()
                            .location(location)
                            .floor(data.getSeatFloor())
                            .section(data.getSeatSection() != null ? data.getSeatSection() : "X")
                            .seatRow(data.getSeatRow())
                            .seatColumn(data.getSeatColumn())
                            .seatNumber(data.getSeatTable())
                            .build();
                    seats.add(seat);
                }
                seatRepository.saveAll(seats);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize seat map or stage coordinates", e);
        }
    }

    public VenueSeatmapResponse getSeatmap(Long locationId) {
        LocationMap locationMap = locationMapRepository.findByLocationId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Seat map not found for locationId: " + locationId));

        try {
            List<List<Object>> seatMap = objectMapper.readValue(locationMap.getSeatMapData(), new TypeReference<>() {});

            return VenueSeatmapResponse.builder()
                    .location(locationMap.getLocation().getName())
                    .layoutWidth(locationMap.getLayoutWidth())
                    .layoutHeight(locationMap.getLayoutHeight())
                    .seatMap(seatMap)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize seat map data", e);
        }
    }

    @Transactional
    public SeatDeleteResponse updateSeatmap(Long locationId, SeatDeleteRequest request) {
        LocationMap locationMap = locationMapRepository.findByLocationId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Seat map not found for locationId: " + locationId));

        try {
            List<List<Object>> originalSeatMap = objectMapper.readValue(locationMap.getSeatMapData(), new TypeReference<>() {});
            List<List<Object>> newSeatMap = request.getSeatMap();

            List<String> seatsToDelete = new ArrayList<>();
            Map<String, String> seatNumberUpdates = new HashMap<>(); // Key: old seat number, Value: new seat number
            long newTotalSeats = 0;

            int rows = Math.max(originalSeatMap.size(), newSeatMap.size());
            for (int i = 0; i < rows; i++) {
                List<Object> originalRow = i < originalSeatMap.size() ? originalSeatMap.get(i) : Collections.emptyList();
                List<Object> newRow = i < newSeatMap.size() ? newSeatMap.get(i) : Collections.emptyList();
                int cols = Math.max(originalRow.size(), newRow.size());

                for (int j = 0; j < cols; j++) {
                    Object originalCell = j < originalRow.size() ? originalRow.get(j) : 0;
                    Object newCell = j < newRow.size() ? newRow.get(j) : 0;

                    String originalSeatNum = (originalCell instanceof String) ? (String) originalCell : null;
                    String newSeatNum = (newCell instanceof String) ? (String) newCell : null;

                    if (originalSeatNum != null && newSeatNum == null) { // 좌석 -> 빈 공간 (삭제)
                        seatsToDelete.add(originalSeatNum);
                    } else if (originalSeatNum != null && !originalSeatNum.equals(newSeatNum)) { // 좌석 -> 다른 좌석 (업데이트)
                        seatNumberUpdates.put(originalSeatNum, newSeatNum);
                    }
                    
                    if (newSeatNum != null) {
                        newTotalSeats++;
                    }
                }
            }

            // DB 업데이트 수행
            if (!seatsToDelete.isEmpty()) {
                seatRepository.deleteByLocationIdAndSeatNumberIn(locationId, seatsToDelete);
            }

            if (!seatNumberUpdates.isEmpty()) {
                List<Seat> seatsToUpdate = seatRepository.findByLocationIdAndSeatNumberIn(locationId, new ArrayList<>(seatNumberUpdates.keySet()));
                Map<String, Seat> seatMapByOldNumber = seatsToUpdate.stream()
                        .collect(Collectors.toMap(Seat::getSeatNumber, Function.identity()));

                for (Map.Entry<String, String> entry : seatNumberUpdates.entrySet()) {
                    Seat seat = seatMapByOldNumber.get(entry.getKey());
                    if (seat != null) {
                        seat.setSeatNumber(entry.getValue());
                    }
                }
                seatRepository.saveAll(seatsToUpdate);
            }

            // LocationMap 업데이트
            String newSeatMapJson = objectMapper.writeValueAsString(newSeatMap);
            locationMap.setSeatMapData(newSeatMapJson);
            locationMapRepository.save(locationMap);

            long updatedCount = seatsToDelete.size() + seatNumberUpdates.size();

            return SeatDeleteResponse.builder()
                    .updatedSeatCount(updatedCount)
                    .totalAvailableSeats(newTotalSeats)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to process seat map update", e);
        }
    }

    @Transactional
    public SeatVipResponse updateVipSeats(Long locationId, SeatVipRequest request) {
        LocationMap locationMap = locationMapRepository.findByLocationId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Seat map not found for locationId: " + locationId));

        try {
            List<List<Object>> originalSeatMap = objectMapper.readValue(locationMap.getSeatMapData(), new TypeReference<>() {});
            List<List<Object>> vipMask = request.getSeatMap();

            List<String> seatsToUpdateToVip = new ArrayList<>();
            List<String> seatsToUpdateToNormal = new ArrayList<>();
            long totalAvailableSeats = 0;

            int rows = Math.min(originalSeatMap.size(), vipMask.size());
            for (int i = 0; i < rows; i++) {
                List<Object> originalRow = originalSeatMap.get(i);
                List<Object> maskRow = vipMask.get(i);
                int cols = Math.min(originalRow.size(), maskRow.size());

                for (int j = 0; j < cols; j++) {
                    Object originalCell = originalRow.get(j);
                    Object maskCell = maskRow.get(j);

                    if (originalCell instanceof String) {
                        totalAvailableSeats++;
                        String seatNumber = (String) originalCell;
                        if (maskCell instanceof Integer && (Integer) maskCell == 1) {
                            seatsToUpdateToVip.add(seatNumber);
                        } else {
                            seatsToUpdateToNormal.add(seatNumber);
                        }
                    }
                }
            }

            // DB 업데이트
            long updatedCount = 0;
            if (!seatsToUpdateToVip.isEmpty()) {
                List<Seat> seats = seatRepository.findByLocationIdAndSeatNumberIn(locationId, seatsToUpdateToVip);
                for (Seat seat : seats) {
                    if (!"VIP".equals(seat.getSection())) {
                        seat.setSection("VIP");
                        updatedCount++;
                    }
                }
                seatRepository.saveAll(seats);
            }

            if (!seatsToUpdateToNormal.isEmpty()) {
                List<Seat> seats = seatRepository.findByLocationIdAndSeatNumberIn(locationId, seatsToUpdateToNormal);
                for (Seat seat : seats) {
                    if (!"X".equals(seat.getSection())) {
                        seat.setSection("X");
                        updatedCount++;
                    }
                }
                seatRepository.saveAll(seats);
            }

            return SeatVipResponse.builder()
                    .updatedSeatCount(updatedCount)
                    .totalAvailableSeats(totalAvailableSeats)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to process VIP seat update", e);
        }
    }
    
    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.write(path, file.getBytes());
            return "/images/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public List<LocationViewResponse> getAllLocations() {
        KakaoOauth currentUser = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(currentUser)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));

        Set<Long> likedLocationIds = manager.getLikedLocations().stream()
                .map(Location::getId)
                .collect(Collectors.toSet());

        List<Location> allLocations = locationRepository.findAll();

        return allLocations.stream()
                .map(location -> LocationViewResponse.builder()
                        .locationId(location.getId())
                        .locationName(location.getName())
                        .locationAddress(location.getAddress())
                        .locationSeatPicture(location.getSeatPictureUrl())
                        .locationSeatTotalCount(location.getTotalSeats())
                        .locationFloor(location.getFloor())
                        .locationLike(likedLocationIds.contains(location.getId()))
                        .build())
                .sorted(Comparator.comparing(LocationViewResponse::isLocationLike).reversed()
                        .thenComparing(LocationViewResponse::getLocationId))
                .collect(Collectors.toList());
    }
}