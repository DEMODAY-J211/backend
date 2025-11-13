package com.tikitta.backend.service;

import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.dto.LocationLikeResponse;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;

    public List<LocationLikeResponse> getLikedLocations() {
        KakaoOauth currentUser = authUtil.getCurrentUser();
        Manager manager = managerRepository.findByKakaoOauth(currentUser)
                .orElseThrow(() -> new RuntimeException("매니저 정보를 찾을 수 없습니다."));

        return manager.getLikedLocations().stream()
                .map(location -> new LocationLikeResponse(location.getId(), location.getName(), location.getType()))
                .collect(Collectors.toList());
    }
}
