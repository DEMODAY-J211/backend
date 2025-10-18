package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Manager;
import lombok.Data;

@Data
public class ManagerInfoDto {
    private String managerName;
    private String managerEmail;

    public ManagerInfoDto(Manager manager) {
        this.managerName = manager.getName();
        this.managerEmail = manager.getKakaoOauth().getEmail();
    }
}
