package com.tikitta.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeService {

    public final ImageService imageService;

    /**
     * @param content QR코드에 담길 내용 (ReservationItem ID)
     * @return 생성된 QR코드 이미지의 상대 URL (예: /qrcodes/123.png)
     * @throws Exception QR코드 생성 및 저장 실패 시
     */
    public String createAndUploadQrCode(Long showId, String content) throws Exception {


            // 1. QR BitMatrix 생성
            BitMatrix matrix = new MultiFormatWriter().encode(
                    content, BarcodeFormat.QR_CODE, 250, 250);

            // 2. 메모리에서 PNG로 변환
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            byte[] bytes = baos.toByteArray();

            // 3. 파일 이름
            String fileName = "qrcodes/QR_" + content + ".png";

            // 4. ImageService 재사용해서 S3 업로드
            return imageService.uploadShowQrCode(showId, fileName, bytes, "image/png");
    }
}
