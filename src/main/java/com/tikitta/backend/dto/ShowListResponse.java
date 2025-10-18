package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Manager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
