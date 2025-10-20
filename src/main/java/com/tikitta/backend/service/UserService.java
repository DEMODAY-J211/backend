package com.tikitta.backend.service;

import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.dto.ManagerOrgResponse;
import com.tikitta.backend.dto.ShowDetailResponse;
import com.tikitta.backend.dto.ShowItemDto;
import com.tikitta.backend.dto.ShowListResponse;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.repository.ShowsRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final ManagerRepository managerRepository;
    private final ShowsRepository showsRepository;

    public ShowListResponse getUserMainPage(Long managerId){
// 1. managerId로 Manager 엔티티 조회
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        // 2. Manager 엔티티로 연관된 Shows 리스트 조회 (1단계에서 추가한 메소드 사용)
        List<Shows> showsList = showsRepository.findByManager(manager);

        // 3. List<Shows>를 List<ShowItemResponse>로 변환
        List<ShowItemDto> showItemList = showsList.stream()
                .map(ShowItemDto::new) // ◀ DTO 생성자 호출
                .collect(Collectors.toList());

        // 4. ShowListResponse DTO로 조립하여 반환
        return new ShowListResponse(manager, showItemList);
    }

    public ShowDetailResponse getShowDetail(Long showId){
        Shows show = showsRepository.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공연입니다. ID: " + showId));
        return new ShowDetailResponse(show);
    }

    public ManagerOrgResponse getManagerOrg(Long managerId){
        // 1. managerId로 Manager 엔티티 조회
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매니저입니다. ID: " + managerId));

        // 2. DTO로 변환하여 반환
        return new ManagerOrgResponse(manager);
    }
}
