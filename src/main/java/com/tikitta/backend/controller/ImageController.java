package com.tikitta.backend.controller;

import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao/manager")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<List<String>>> uploadManagerImage(@RequestParam("image") MultipartFile image) {
        String imageUrl = imageService.uploadManagerProfileImage(image);
        return ResponseEntity.ok(new ApiResponse<>(200, "이미지 업로드 성공", List.of(imageUrl)));
    }
}
