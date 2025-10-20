package com.tikitta.backend.dto;

import com.tikitta.backend.domain.Manager;
import lombok.Getter;

import java.util.List;

@Getter
public class ManagerOrgResponse {

    private Long managerId;
    private String managerName;
    private String managerPicture;
    private String managerIntro;
    private String managerText;
    private List<String> managerUrl;

    public ManagerOrgResponse(Manager manager) {
        this.managerId = manager.getId();
        this.managerName = manager.getName();
        this.managerPicture = manager.getPictureUrl();
        this.managerIntro = manager.getIntroduction();
        this.managerText = manager.getDescription();
        this.managerUrl = manager.getUrls();
    }

}
