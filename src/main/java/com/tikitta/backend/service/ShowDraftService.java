package com.tikitta.backend.service;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.dto.ShowDraftCreateResponse;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.repository.ShowsRepository;
import com.tikitta.backend.util.AuthUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShowDraftService {

    private final ShowsRepository showsRepository;
    private final ManagerRepository managerRepository;
    private final AuthUtil authUtil;

    @Transactional
    public ShowDraftCreateResponse CreateShow(){
        KakaoOauth user=authUtil.getCurrentUser();

        Manager manager=managerRepository.findByKakaoOauth(user)
                .orElseThrow(()->new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));

        Shows show = Shows.builder()
                .manager(manager)
                .status(DomainEnums.ShowStatus.DRAFT)
                .build();

        Shows saved = showsRepository.save(show);

        return  ShowDraftCreateResponse.builder()
                .showId(saved.getId())
                .status(saved.getStatus().name())
                .build();
    }

}
