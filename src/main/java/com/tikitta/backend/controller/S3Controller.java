package com.tikitta.backend.controller;

import com.tikitta.backend.dto.ApiResponse;
import com.tikitta.backend.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class S3Controller {

    private final ImageService imageService;

    // 포스터 1개 업로드
    @PostMapping(
            value = "/shows/{showId}/poster",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> uploadPoster(
            @PathVariable Long showId,
            @RequestPart("poster") MultipartFile poster
    ) {
        String url = imageService.uploadShowPoster(poster, showId);

        return ResponseEntity.ok(
                new ApiResponse<>(200, "포스터 업로드 성공", url)
        );
    }

    // 상세 이미지 여러개 업로드
    @PostMapping(
            value = "/shows/{showId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable Long showId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        List<String> urls = imageService.uploadShowDetailImages(images,showId);

        return ResponseEntity.ok(
                new ApiResponse<>(200, "상세 이미지 업로드 성공", urls)
        );
    }

    // 단체 이미지 업로드
    @PostMapping(
            value="/manager/{managerId}/organizationImage",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<String>> uploadOrganizationImage(
            @PathVariable Long managerId,
            @RequestPart("organizationImage") MultipartFile organizationImage
    ){
        String url = imageService.uploadOrganizationImage(organizationImage);

        return ResponseEntity.ok(new ApiResponse<>(200, "단체 이미지 업로드 성공", url));
    }
}
