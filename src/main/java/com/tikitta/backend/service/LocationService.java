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
import com.tikitta.backend.repository.ShowSeatRepository;
import com.tikitta.backend.util.AuthUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final ShowSeatRepository showSeatRepository;
    private final AuthUtil authUtil;
    private final ImageService imageService; // ImageService 주입
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public SeatDeleteResponse createShowSeatsFromMap(Long locationId, SeatDeleteRequest request) {
        try {
            // 1. 원본 LocationMap 및 Location 조회
            LocationMap locationMap = locationMapRepository.findByLocationId(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("Seat map not found for locationId: " + locationId));
            Location location = locationMap.getLocation();

            // 2. 해당 Location의 모든 Seat를 좌표 기반 Map으로 변환
            List<Seat> allSeatsInLocation = seatRepository.findByLocation(location);
            Map<String, Seat> seatMapByCoords = allSeatsInLocation.stream()
                .collect(Collectors.toMap(s -> s.getSeatRow() + "," + s.getSeatColumn(), Function.identity()));

            // 3. 원본 좌석맵에서 좌표 Set 생성
            List<List<Object>> originalSeatMap = objectMapper.readValue(locationMap.getSeatMapData(), new TypeReference<>() {});
            Set<String> originalSeatCoords = new HashSet<>();
            for (int i = 0; i < originalSeatMap.size(); i++) {
                for (int j = 0; j < originalSeatMap.get(i).size(); j++) {
                    if (originalSeatMap.get(i).get(j) instanceof String) {
                        originalSeatCoords.add(i + "," + j);
                    }
                }
            }

            // 4. 요청으로 들어온 새로운 좌석맵 처리 및 ShowSeat 생성
            List<List<Object>> newSeatMap = request.getSeatMap();
            Set<String> newSeatCoords = new HashSet<>();
            List<ShowSeat> showSeatsToCreate = new ArrayList<>();

            for (int i = 0; i < newSeatMap.size(); i++) {
                for (int j = 0; j < newSeatMap.get(i).size(); j++) {
                    if (newSeatMap.get(i).get(j) instanceof String) {
                        String currentCoords = i + "," + j;
                        newSeatCoords.add(currentCoords);
                        
                        Seat seat = seatMapByCoords.get(currentCoords);
                        if (seat != null) {
                            ShowSeat showSeat = ShowSeat.builder()
                                .seat(seat)
                                .showTime(null)
                                .isAvailable(true)
                                .isGoodSeat(false)
                                .build();
                            showSeatsToCreate.add(showSeat);
                        }
                    }
                }
            }
            showSeatRepository.saveAll(showSeatsToCreate);

            // 5. 변경된(삭제된) 좌석 수 계산
            Set<String> removedCoords = new HashSet<>(originalSeatCoords);
            removedCoords.removeAll(newSeatCoords);
            long updatedCount = removedCoords.size();

            // 6. 최종 응답 생성
            return SeatDeleteResponse.builder()
                    .updatedSeatCount(updatedCount)
                    .totalAvailableSeats((long) showSeatsToCreate.size())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to process seat map update", e);
        }
    }

    @Transactional
    public SeatVipResponse updateVipSeats(Long locationId, SeatVipRequest request) {
        // 1. locationId로 showTime이 null인 ShowSeat들을 조회하고, 좌표를 키로 하는 맵으로 변환
        List<ShowSeat> draftShowSeats = showSeatRepository.findDraftShowSeatsByLocationId(locationId);
        Map<String, ShowSeat> showSeatMapByCoords = draftShowSeats.stream()
                .collect(Collectors.toMap(ss -> ss.getSeat().getSeatRow() + "," + ss.getSeat().getSeatColumn(), Function.identity()));

        // 2. 요청으로 받은 vip_seat_map 순회
        List<List<Object>> vipSeatMap = request.getSeatMap();
        long updatedCount = 0;
        
        // 3. 모든 좌석의 isGoodSeat를 false로 초기화
        for (ShowSeat showSeat : draftShowSeats) {
            showSeat.setIsGoodSeat(false);
        }

        // 4. VIP로 지정된 좌석만 isGoodSeat를 true로 설정
        for (int i = 0; i < vipSeatMap.size(); i++) {
            for (int j = 0; j < vipSeatMap.get(i).size(); j++) {
                Object cell = vipSeatMap.get(i).get(j);
                if (cell instanceof Integer && (Integer) cell == 1) {
                    String coords = i + "," + j;
                    ShowSeat showSeat = showSeatMapByCoords.get(coords);
                    if (showSeat != null) {
                        if (showSeat.getIsGoodSeat() != null && !showSeat.getIsGoodSeat()) {
                             updatedCount++;
                        }
                        showSeat.setIsGoodSeat(true);
                    }
                }
            }
        }
        
        showSeatRepository.saveAll(draftShowSeats);

        return SeatVipResponse.builder()
                .updatedSeatCount(updatedCount)
                .totalAvailableSeats((long) draftShowSeats.size())
                .build();
    }
    
    @Transactional
    public Location registerLocation(VenueRegisterRequest request, MultipartFile locationPicture) {
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

        // 1. 이미지 URL 없이 Location 정보 먼저 저장
        Location location = Location.builder()
                .name(request.getLocationName())
                .address(request.getLocationAddress())
                .addressDetail(request.getLocationAddressDetail())
                .totalSeats(totalSeats)
                .floor(floor)
                .type(type)
                .build();
        locationRepository.saveAndFlush(location); // ID 생성을 위해 즉시 DB 반영

        // 2. S3에 이미지 업로드
        String pictureUrl = imageService.uploadLocationImage(locationPicture, location.getId());

        // 3. 이미지 URL을 Location에 업데이트
        location.setSeatPictureUrl(pictureUrl);
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

        List<ShowSeat> draftShowSeats = showSeatRepository.findDraftShowSeatsByLocationId(locationId);

        try {
            // 임시 저장된 ShowSeat이 없으면 기존 로직대로 LocationMap의 데이터를 반환
            if (draftShowSeats.isEmpty()) {
                List<List<Object>> seatMap = objectMapper.readValue(locationMap.getSeatMapData(), new TypeReference<>() {});
                return VenueSeatmapResponse.builder()
                        .location(locationMap.getLocation().getName())
                        .layoutWidth(locationMap.getLayoutWidth())
                        .layoutHeight(locationMap.getLayoutHeight())
                        .seatMap(seatMap)
                        .build();
            }

            // 임시 저장된 ShowSeat이 있으면 이를 기반으로 seat_map을 재구성
            int height = locationMap.getLayoutHeight();
            int width = locationMap.getLayoutWidth();
            List<List<Object>> newSeatMap = new ArrayList<>();
            for (int i = 0; i < height; i++) {
                newSeatMap.add(new ArrayList<>(Collections.nCopies(width, 0)));
            }

            // 무대(-1) 배치
            List<List<Integer>> stageCoords = objectMapper.readValue(locationMap.getStageCoordinates(), new TypeReference<>() {});
            for (List<Integer> coord : stageCoords) {
                newSeatMap.get(coord.get(0)).set(coord.get(1), -1);
            }

            // 좌석(seatNumber) 배치
            for (ShowSeat showSeat : draftShowSeats) {
                Seat seat = showSeat.getSeat();
                newSeatMap.get(seat.getSeatRow()).set(seat.getSeatColumn(), seat.getSeatNumber());
            }

            return VenueSeatmapResponse.builder()
                    .location(locationMap.getLocation().getName())
                    .layoutWidth(width)
                    .layoutHeight(height)
                    .seatMap(newSeatMap)
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