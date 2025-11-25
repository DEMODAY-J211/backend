package com.tikitta.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.tikitta.backend.domain.DomainEnums;
import com.tikitta.backend.domain.KakaoOauth;
import com.tikitta.backend.domain.Manager;
import com.tikitta.backend.exception.CustomApplicationException;
import com.tikitta.backend.exception.ErrorCode;
import com.tikitta.backend.repository.ManagerRepository;
import com.tikitta.backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    // 공연장 이미지 업로드
    // ----------------------------
    public String uploadLocationImage(MultipartFile file, Long locationId) {
        String key = String.format("location/%d/image.jpg", locationId);
        return uploadToS3(file, key);
    }

    // ----------------------------
    // 매니저 프로필 이미지 업로드
    // ----------------------------
    public String uploadManagerProfileImage(MultipartFile file) {
        KakaoOauth user = authUtil.getCurrentUser();
        Long kakaoId = user.getId();

        String key = String.format("kakao/%d/profile.jpg", kakaoId);
        return uploadToS3(file, key);
    }

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
        // 서비스 레벨에서 직접 인증/인가 처리
        Manager manager = getCurrentManager();
        if (manager == null) {
            throw new AccessDeniedException("포스터를 업로드하려면 매니저로 로그인해야 합니다.");
        }
        Long managerId = manager.getId();

        String key = String.format("manager/%d/shows/%d/poster.jpg", managerId, showId);
        return uploadToS3(file, key);
    }

    // ----------------------------
    // 공연 상세 이미지 업로드
    // ----------------------------
    public List<String> uploadShowDetailImages(List<MultipartFile> files, Long showId) {
        // 서비스 레벨에서 직접 인증/인가 처리
        Manager manager = getCurrentManager();
        if (manager == null) {
            throw new AccessDeniedException("상세 이미지를 업로드하려면 매니저로 로그인해야 합니다.");
        }
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
            // amazonS3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead); // ACL 설정 제거

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
        try {
            KakaoOauth user = authUtil.getCurrentUser();
            if (user.getRole() != DomainEnums.Role.MANAGER) {
                return null;
            }
            return managerRepository.findByKakaoOauth(user)
                    .orElse(null);
        } catch (Exception e) {
            // 로그인하지 않은 사용자의 경우 예외가 발생할 수 있음
            return null;
        }
    }

    // ----------------------------
    // URL에서 S3 Key 추출
    // ----------------------------
    private String extractKeyFromUrl(String s3Url) {
        try {
            URL url = new URL(s3Url);
            String path = url.getPath(); // 예: /manager/1/shows/poster.jpg

            // 맨 앞의 '/' 제거
            String key = path.startsWith("/") ? path.substring(1) : path;

            // URL 인코딩된 문자열(한글, 공백 등) 디코딩
            return URLDecoder.decode(key, StandardCharsets.UTF_8.name());

        } catch (Exception e) {
            log.error("S3 Key 추출 실패: {}", s3Url, e);
            throw new CustomApplicationException(ErrorCode.IO_EXCEPTION_UPLOAD_FILE); // 혹은 적절한 에러 처리
        }
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

    public String uploadShowQrCode(Long managerId, Long showId, String originalFilename, byte[] bytes, String contentType) {
        validateFile(originalFilename);

        try {
            // 파라미터로 받은 managerId를 사용
            managerRepository.findById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 ID의 매니저를 찾을 수 없습니다: " + managerId));

            String saveName = UUID.randomUUID() + "_" + originalFilename;

            // S3 저장 경로
            String key = String.format("manager/%d/shows/%d/qrcode/%s",
                    managerId, showId, saveName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            metadata.setContentType(contentType);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

            amazonS3.putObject(bucketName, key, inputStream, metadata);
            // amazonS3.setObjectAcl(bucketName, key, CannedAccessControlList.PublicRead); // ACL 설정 제거

            return amazonS3.getUrl(bucketName, key).toString();

        } catch (Exception e) {
            log.error("QR 코드 업로드 실패: {}", e.getMessage());
            throw new CustomApplicationException(ErrorCode.IO_EXCEPTION_UPLOAD_FILE);
        }
    }

}