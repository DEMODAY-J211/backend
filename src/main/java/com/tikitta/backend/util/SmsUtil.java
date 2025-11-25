package com.tikitta.backend.util;

import com.google.gson.Gson;
import com.tikitta.backend.domain.Reservation;
import com.tikitta.backend.domain.Shows;
import com.tikitta.backend.domain.ShowTime;
import com.tikitta.backend.domain.KakaoOauth;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 메시지 템플릿의 변수를 실제 데이터로 치환하는 정적 유틸리티 메소드
     *
     * @param template    치환할 변수가 포함된 메시지 템플릿 (예: "{공연명} 예매가 완료되었습니다.")
     * @param reservation 예매 정보를 담고 있는 Reservation 객체
     * @return 변수가 모두 실제 값으로 치환된 최종 메시지 문자열
     */
    public static String formatMessage(String template, Reservation reservation) {
        if (template == null || reservation == null) {
            return "";
        }

        ShowTime showTime = reservation.getShowTime();
        Shows show = showTime.getShow();
        KakaoOauth user = reservation.getUser();

        // 금액 포맷터
        DecimalFormat decimalFormat = new DecimalFormat("#,###원");
        String formattedPrice = decimalFormat.format(reservation.getTotalPrice());

        // 날짜/시간 포맷터
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd (E) HH:mm");
        String formattedShowTime = showTime.getStartAt().format(formatter);

        return template
                .replace("{단체명}", show.getManager().getName())
                .replace("{공연명}", show.getTitle())
                .replace("{'0,000 원'}", formattedPrice)
                .replace("{예금주명}", show.getBankDepositorName() + " " + show.getBankName().name())
                .replace("{계좌번호}", show.getBankAccountNumber())
                .replace("{예매_매수}", String.valueOf(reservation.getQuantity()))
                .replace("{'000 님'}", user.getName())
                .replace("{공연일시}", formattedShowTime)
                .replace("{관람장소}", show.getLocation().getAddress())
                .replace("{team_name}", show.getManager().getName())
                .replace("{show_name}", show.getTitle())
                .replace("{amount}", formattedPrice)
                .replace("{account_holder}", show.getBankDepositorName() + " " + show.getBankName().name())
                .replace("{account_number}", show.getBankAccountNumber())
                .replace("{ticket_count}", String.valueOf(reservation.getQuantity()))
                .replace("{username}", user.getName())
                .replace("{show_date_time}", formattedShowTime)
                .replace("{venue}", show.getLocation().getAddress());
    }


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
