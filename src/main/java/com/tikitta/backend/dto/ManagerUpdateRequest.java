package com.tikitta.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ManagerUpdateRequest {

    private String managerPicture;
    private String managerName;
    private String managerIntro;
    private String managerText;
    private ManagerUrl managerUrl;

    @Getter
    @NoArgsConstructor
    public static class ManagerUrl {
        private String instagram;
        private String youtube;
        private String facebook;
    }}