package com.tikitta.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ManagerInfoResponse {
    private String managerPicture;
    private String managerName;
    private String managerIntro;
    private String managerText;
    private List<String> managerUrl;
}