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
    private static final String SEND_API_URL = "https://sms.gabia.com/api/send/sms";

    private String accessToken;
    private LocalDateTime tokenExpiryTime;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    /**
     * SMS를 발송하는 메인 메소드
     * @param phoneNumber 수신자 전화번호
     * @param message 보낼 메시지 내용
     * @return 발송 결과
     * @throws IOException API 호출 실패 시
     */
    public Map<String, String> sendSms(String phoneNumber, String message) throws IOException {
        // 1. 토큰이 없거나 만료되었으면 새로 발급
        if (accessToken == null || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            refreshAccessToken();
        }

        // 2. SMS 발송 API 호출
        String authValue = Base64.getEncoder().encodeToString((smsId + ":" + accessToken).getBytes(StandardCharsets.UTF_8));

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("phone", phoneNumber)
                .addFormDataPart("callback", callbackNumber)
                .addFormDataPart("message", message)
                .addFormDataPart("refkey", "TIKITTA" + System.currentTimeMillis()) // 고유한 refkey 생성
                .build();

        Request request = new Request.Builder()
                .url(SEND_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Basic " + authValue)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("SMS 발송 API 호출 실패: " + response);
            }
            String responseBody = Objects.requireNonNull(response.body()).string();
            return gson.fromJson(responseBody, HashMap.class);
        }
    }

    /**
     * Gabia API Access Token을 발급/갱신하는 메소드
     * @throws IOException API 호출 실패 시
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
                // 토큰 만료 시간을 현재 시간 + (만료 시간 - 60초)로 설정 (안전 마진)
                long expiresIn = Long.parseLong(result.getOrDefault("expires_in", "3600"));
                this.tokenExpiryTime = LocalDateTime.now().plusSeconds(expiresIn - 60);
            } else {
                throw new IOException("Gabia 인증 토큰 파싱 실패: 'access_token'이 없습니다.");
            }
        }
    }
}