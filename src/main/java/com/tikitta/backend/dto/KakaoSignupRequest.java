package com.tikitta.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoSignupRequest {
    private Long kakaoOauthId;
    private String managerPicture;
    private String managerName;
    private String managerIntro;
    private String managerText;
    private List<String> managerUrl;
}
