package com.tikitta.backend.service;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Location;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.LocationViewResponse;
import com.tikitta.backend.dto.venue.VenueRegisterRequest;
import com.tikitta.backend.repository.LocationRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final AuthUtil authUtil;

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

    private String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
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