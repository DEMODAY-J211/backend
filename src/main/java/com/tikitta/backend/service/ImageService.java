package com.tikitta.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.tikitta.backend.config.AmazonS3Config;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.exception.CustomApplicationException;
import com.tikitta.backend.exception.ErrorCode;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AmazonS3 amazonS3;
    private final AuthUtil authUtil;
    private final ManagerRepository managerRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // ----------------------------
    // 단체 이미지 업로드
    // ----------------------------
    public String uploadOrganizationImage(MultipartFile file) {
        Manager manager = getCurrentManager();
        Long managerId = manager.getId();

        String key = String.format("manager/%d/organization.jpg", managerId);
        return uploadToS3(file, key);
    }

    // ----------------------------
    // 공연 포스터 업로드
    // ----------------------------
    public String uploadShowPoster(MultipartFile file, Long showId) {
        Manager manager = getCurrentManager();
        Long managerId = manager.getId();

        String key = String.format("manager/%d/shows/%d/poster.jpg", managerId, showId);
        return uploadToS3(file, key);
    }

    // ----------------------------
    // 공연 상세 이미지 업로드
    // ----------------------------
    public List<String> uploadShowDetailImages(List<MultipartFile> files, Long showId) {
        Manager manager = getCurrentManager();
        Long managerId = manager.getId();

        return files.stream()
                .map(file -> {
                    String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                    String key = String.format("manager/%d/shows/%d/%s", managerId, showId, uniqueName);
                    return uploadToS3(file, key);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------
    // S3 단일 파일 업로드
    // ----------------------------
    private String uploadToS3(MultipartFile file, String key) {
        validateFile(file.getOriginalFilename());

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(bucketName, key, file.getInputStream(), metadata);
            amazonS3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);

            return amazonS3.getUrl(bucketName, key).toString();

        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new CustomApplicationException(ErrorCode.IO_EXCEPTION_UPLOAD_FILE);
        }
    }

    // ----------------------------
    // S3 파일 삭제
    // ----------------------------
    public void delete(String s3Url) {
        String key = extractKeyFromUrl(s3Url);
        amazonS3.deleteObject(bucketName, key);
    }

    // ----------------------------
    // 현재 로그인한 매니저 조회
    // ----------------------------
    private Manager getCurrentManager() {
        KakaoOauth user = authUtil.getCurrentUser();
        return managerRepository.findByKakaoOauth(user)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 매니저 정보를 찾을 수 없습니다."));
    }

    // ----------------------------
    // URL에서 S3 Key 추출
    // ----------------------------
    private String extractKeyFromUrl(String s3Url) {
        return s3Url.substring(s3Url.indexOf(bucketName) + bucketName.length() + 1);
    }

    // ----------------------------
    // 파일 유효성 검사
    // ----------------------------
    private void validateFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new CustomApplicationException(ErrorCode.NOT_EXIST_FILE);
        }

        if (!filename.contains(".")) {
            throw new CustomApplicationException(ErrorCode.NOT_EXIST_FILE_EXTENSION);
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowed = List.of("jpg", "jpeg", "png", "gif");

        if (!allowed.contains(ext)) {
            throw new CustomApplicationException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    public String uploadShowQrCode(Long showId, String originalFilename, byte[] bytes, String contentType) {
        validateFile(originalFilename);

        try {
            // 로그인한 매니저 조회
            Manager manager = getCurrentManager();
            Long managerId = manager.getId();

            String saveName = UUID.randomUUID() + "_" + originalFilename;

            // S3 저장 경로
            String key = String.format("manager/%d/shows/%d/qrcode/%s",
                    managerId, showId, saveName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            amazonS3.putObject(bucketName, key, inputStream, metadata);
            amazonS3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead);

            return amazonS3.getUrl(bucketName, key).toString();

        } catch (Exception e) {
            log.error("QR 코드 업로드 실패: {}", e.getMessage());
            throw new CustomApplicationException(ErrorCode.IO_EXCEPTION_UPLOAD_FILE);
        }
    }

}
