package com.tikitta.backend.service;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Location;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.LocationViewResponse;
import com.tikitta.backend.repository.LocationRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;

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
