package com.tikitta.backend.util;

import com.google.gson.Gson;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class SmsUtil {

    @Value("${gabia.sms.id}")
    private String smsId;

    @Value("${gabia.sms.api-key}")
    private String apiKey;

    @Value("${gabia.sms.callback}")
    private String callbackNumber;

    private static final String AUTH_API_URL = "https://sms.gabia.com/oauth/token";
    private static final String SMS_SEND_URL = "https://sms.gabia.com/api/send/sms";
    private static final String LMS_SEND_URL = "https://sms.gabia.com/api/send/lms"; // LMS URL 추가

    private String accessToken;
    private LocalDateTime tokenExpiryTime;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    /**
     * SMS(단문)를 발송하는 메소드
     */
    public Map<String, String> sendSms(String phoneNumber, String message) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("phone", phoneNumber)
                .addFormDataPart("callback", callbackNumber)
                .addFormDataPart("message", message)
                .addFormDataPart("refkey", "TIKITTA" + System.currentTimeMillis())
                .build();

        return sendMessage(SMS_SEND_URL, requestBody);
    }

    /**
     * LMS(장문)를 발송하는 메소드
     */
    public Map<String, String> sendLms(String phoneNumber, String subject, String message) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("phone", phoneNumber)
                .addFormDataPart("callback", callbackNumber)
                .addFormDataPart("subject", subject)
                .addFormDataPart("message", message)
                .addFormDataPart("refkey", "TIKITTA" + System.currentTimeMillis())
                .build();

        return sendMessage(LMS_SEND_URL, requestBody);
    }

    /**
     * 실제 메시지를 발송하는 공통 내부 메소드
     */
    private Map<String, String> sendMessage(String apiUrl, RequestBody requestBody) throws IOException {
        // 1. 토큰 유효성 검사 및 갱신
        if (accessToken == null || tokenExpiryTime == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            refreshAccessToken();
        }

        // 2. API 호출
        String authValue = Base64.getEncoder().encodeToString((smsId + ":" + accessToken).getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Authorization", "Basic " + authValue)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("메시지 발송 API 호출 실패: " + response);
            }
            String responseBody = Objects.requireNonNull(response.body()).string();
            return gson.fromJson(responseBody, HashMap.class);
        }
    }

    /**
     * Gabia API Access Token을 발급/갱신하는 메소드
     */
    private void refreshAccessToken() throws IOException {
        String authValue = Base64.getEncoder().encodeToString((smsId + ":" + apiKey).getBytes(StandardCharsets.UTF_8));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(AUTH_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Basic " + authValue)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Gabia 인증 토큰 발급 실패: " + response);
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            Map<String, String> result = gson.fromJson(responseBody, HashMap.class);

            if (result.containsKey("access_token")) {
                this.accessToken = result.get("access_token");
                long expiresIn = 3600L;
                try {
                    expiresIn = Long.parseLong(result.get("expires_in"));
                } catch (Exception e) {
                    System.err.println("Warning: 'expires_in' 파싱 실패. 기본값 3600초를 사용합니다.");
                }
                this.tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60);
            } else {
                throw new IOException("Gabia 인증 토큰 파싱 실패: 'access_token'이 없습니다. 응답: " + responseBody);
            }
        }
    }
}