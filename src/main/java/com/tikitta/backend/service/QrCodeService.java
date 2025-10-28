package com.tikitta.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class QrCodeService {

    private static final String QR_CODE_DIRECTORY = "src/main/resources/static/qrcodes/";

    /**
     * 내용을 받아 QR코드를 생성하고, 로컬 파일 시스템에 저장 후 해당 URL을 반환합니다.
     * @param content QR코드에 담길 내용 (ReservationItem ID)
     * @return 생성된 QR코드 이미지의 상대 URL (예: /qrcodes/123.png)
     * @throws Exception QR코드 생성 및 저장 실패 시
     */
    public String createAndUploadQrCode(String content) throws Exception {
        // QR 코드 파일명은 ReservationItem ID로 설정
        String fileName = content + ".png";
        Path directoryPath = Paths.get(QR_CODE_DIRECTORY);
        Path filePath = directoryPath.resolve(fileName);

        // 디렉토리가 없으면 생성
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            log.info("QR 코드 저장 디렉토리 생성: {}", directoryPath.toAbsolutePath());
        }

        // QR 코드 생성
        BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 250, 250);

        // 파일로 저장
        MatrixToImageWriter.writeToFile(matrix, "PNG", filePath.toFile());
        log.info("QR 코드 저장 완료: {}", filePath.toAbsolutePath());

        // 정적 리소스 접근을 위한 상대 URL 반환
        return "/qrcodes/" + fileName;
    }
}
