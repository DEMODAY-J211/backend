package com.tikitta.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.tikitta.backend.config.AmazonS3Config;
import com.tikitta.backend.exception.CustomApplicationException;
import com.tikitta.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public List<String> upload(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadImage)
                .toList();
    }

    public String upload(MultipartFile file) {
        validateFile(file.getOriginalFilename());
        return uploadToS3(file);
    }


    private String uploadImage(MultipartFile file) {
        validateFile(file.getOriginalFilename());
        return uploadToS3(file);
    }

    private void validateFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new CustomApplicationException(ErrorCode.NOT_EXIST_FILE);
        }

        if (!filename.contains(".")) {
            throw new CustomApplicationException(ErrorCode.NOT_EXIST_FILE_EXTENSION);
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1);
        List<String> allow = List.of("jpg", "jpeg", "png", "gif");

        if (!allow.contains(ext.toLowerCase())) {
            throw new CustomApplicationException(ErrorCode.INVALID_FILE_EXTENSION);
        }
    }

    private String uploadToS3(MultipartFile file) {
        try {
            String original = file.getOriginalFilename();
            String saveName = UUID.randomUUID() + "_" + original;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(
                    bucketName,
                    saveName,
                    file.getInputStream(),
                    metadata
            );

            return amazonS3.getUrl(bucketName, saveName).toString();

        } catch (Exception e) {
            throw new CustomApplicationException(ErrorCode.IO_EXCEPTION_UPLOAD_FILE);
        }
    }
}