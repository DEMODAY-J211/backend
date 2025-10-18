package com.tikitta.backend.dto;

import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.Shows;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ShowListResponse {
    private Long managerId;
    private String managerName;
    private List<ShowItemDto> showList;

    public ShowListResponse(Manager manager, List<ShowItemDto> showList) {
        this.managerId = manager.getId();
        this.managerName = manager.getName();
        this.showList = showList;
    }
}
