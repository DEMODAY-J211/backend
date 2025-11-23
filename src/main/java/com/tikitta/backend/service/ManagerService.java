package com.tikitta.backend.service;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Location;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.LocationLikeResponse;
import com.tikitta.backend.dto.ManagerInfoResponse;
import com.tikitta.backend.dto.ManagerUpdateRequest;
import com.tikitta.backend.repository.LocationRepository;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final LocationRepository locationRepository;
    private final AuthUtil authUtil;
    private final ImageService imageService;

    public List<LocationLikeResponse> getLikedLocations() {
        KakaoOauth currentUser = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(currentUser)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));

        return manager.getLikedLocations().stream()
                .map(location -> new LocationLikeResponse(location.getId(), location.getName(), location.getType()))
                .collect(Collectors.toList());
    }

    @Transactional
    public LocationLikeResponse likeVenue(Long locationId) {
        KakaoOauth currentUser = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(currentUser)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 공연장을 찾을 수 없습니다: " + locationId));

        if (manager.getLikedLocations().contains(location)) {
            manager.getLikedLocations().remove(location);
        } else {
            manager.getLikedLocations().add(location);
        }
        managerRepository.save(manager);

        return new LocationLikeResponse(location.getId(), location.getName(), location.getType());
    }


    public ManagerInfoResponse getMyOrganizationInfo(Authentication authentication) {
        KakaoOauth oauth = (KakaoOauth) authentication.getPrincipal();

        Manager manager = managerRepository.findByKakaoOauth(oauth)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "매니저 정보가 존재하지 않습니다."));

        return new ManagerInfoResponse(
                manager.getPictureUrl(),
                manager.getName(),
                manager.getIntroduction(),
                manager.getDescription(),
                manager.getUrls()
        );
    }

    @Transactional
    public ManagerInfoResponse updateMyOrganizationInfo(Authentication authentication,
                                                        ManagerUpdateRequest request) {

        KakaoOauth oauth = (KakaoOauth) authentication.getPrincipal();

        Manager manager = managerRepository.findByKakaoOauth(oauth)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "매니저 정보가 존재하지 않습니다.")
                );

        // PATCH: null이 아닌 필드만 업데이트
        if (request.getManagerPicture() != null) {
            String oldUrl = manager.getPictureUrl();
            if (oldUrl != null && !oldUrl.isBlank()) {
                // 기존 S3 이미지 삭제
                imageService.delete(oldUrl);
            }
            // 새 URL 저장
            manager.setPictureUrl(request.getManagerPicture());
        }

        if (request.getManagerPicture() != null) {
            manager.setPictureUrl(request.getManagerPicture());
        }
        if (request.getManagerName() != null) {
            manager.setName(request.getManagerName());
        }
        if (request.getManagerIntro() != null) {
            manager.setIntroduction(request.getManagerIntro());
        }
        if (request.getManagerText() != null) {
            manager.setDescription(request.getManagerText());
        }

        managerRepository.save(manager);

        return new ManagerInfoResponse(
                manager.getPictureUrl(),
                manager.getName(),
                manager.getIntroduction(),
                manager.getDescription(),
                manager.getUrls()
        );
    }
}