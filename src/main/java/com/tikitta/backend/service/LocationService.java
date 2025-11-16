package com.tikitta.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tikitta.backend.domain.*;
import com.tikitta.backend.dto.LocationViewResponse;
import com.tikitta.backend.dto.venue.SeatData;
import com.tikitta.backend.dto.venue.VenueRegisterRequest;
import com.tikitta.backend.dto.venue.VenueSeatmapRequest;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    @Transactional
    public void registerLocation(VenueRegisterRequest request, MultipartFile locationPicture) {
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
    }

    @Transactional
    public void registerSeatmap(VenueSeatmapRequest request) {
        Location location = locationRepository.findByName(request.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + request.getLocation()));

        try {
            // 1. 무대(-1) 좌표 추출
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

            // 2. seat_map과 무대 좌표를 JSON 문자열로 변환
            String seatMapJson = objectMapper.writeValueAsString(request.getSeatMap());
            String stageCoordinatesJson = objectMapper.writeValueAsString(stageCoordinates);

            // 3. LocationMap 엔티티 생성 및 저장
            LocationMap locationMap = LocationMap.builder()
                    .location(location)
                    .layoutWidth(request.getLayoutWidth())
                    .layoutHeight(request.getLayoutHeight())
                    .seatMapData(seatMapJson)
                    .stageCoordinates(stageCoordinatesJson) // 추출한 무대 좌표 저장
                    .build();
            locationMapRepository.save(locationMap);

            // 4. Seat 엔티티 생성 및 저장
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