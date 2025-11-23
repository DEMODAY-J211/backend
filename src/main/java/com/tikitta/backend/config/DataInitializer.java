package com.tikitta.backend.config;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final LocationRepository locationRepository;
    private final SeatRepository seatRepository;
    private final ShowsRepository showsRepository;
    private final MessageRepository messageRepository;
    private final ShowTimeRepository showTimeRepository;
    private final TicketOptionRepository ticketOptionRepository;
    private final ShowSeatRepository showSeatRepository;

    // 예매 관련 레포지토리는 현재 사용하지 않으므로 주석 처리하거나 남겨둬도 됩니다.
    // private final ReservationRepository reservationRepository;
    // private final ReservationItemRepository reservationItemRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        String testManagerEmail = "manager_test@kakao.com";

        // 중복 실행 방지
        if (kakaoOauthRepository.findByEmail(testManagerEmail).isPresent()) {
            System.out.println("--- 테스트 데이터가 이미 존재합니다. ---");
            return;
        }

        System.out.println("--- 테스트 데이터 생성을 시작합니다. ---");

        // ==========================================
        // 1. 매니저 및 유저 계정 생성
        // ==========================================
        KakaoOauth managerOauth = KakaoOauth.builder()
                .email(testManagerEmail)
                .name("티키타매니저")
                .role(DomainEnums.Role.MANAGER)
                .createdAt(LocalDateTime.now())
                .visitedPath(DomainEnums.VisitedPath.ETC)
                .build();
        kakaoOauthRepository.save(managerOauth);

        KakaoOauth userOauth = KakaoOauth.builder()
                .email("user_test@kakao.com")
                .name("티키타유저")
                .role(DomainEnums.Role.USER)
                .createdAt(LocalDateTime.now())
                .visitedPath(DomainEnums.VisitedPath.INSTAGRAM)
                .build();
        kakaoOauthRepository.save(userOauth);

        Manager manager = Manager.builder()
                .kakaoOauth(managerOauth)
                .name("티키타 엔터테인먼트")
                .introduction("공연 문화를 선도하는 티키타입니다.")
                .description("상세 소개글입니다.")
                .urls(List.of("https://instagram.com/tikitta", "https://youtube.com/tikitta"))
                .build();
        managerRepository.save(manager);

        // ==========================================
        // 2. 공연장 생성 (2개: 좌석형 / 스탠딩형)
        // ==========================================

        // 2-1. 좌석형 공연장 (소극장)
        Location seatedLocation = Location.builder()
                .name("대학로 드림아트센터 (좌석형)")
                .address("서울시 종로구 동숭동 1-1")
                .addressDetail("2관")
                .totalSeats(4) // 테스트용 4석
                .floor(1)
                .type(DomainEnums.LocationType.SEATED)
                .build();
        locationRepository.save(seatedLocation);

        // 좌석 데이터 생성 (A1, A2, B1, B2)
        List<Seat> seats = new ArrayList<>();
        for (String row : List.of("A", "B")) {
            for (int col = 1; col <= 2; col++) {
                seats.add(Seat.builder()
                        .location(seatedLocation)
                        .floor(1)
                        .section(row)           // A, B
                        .seatRow(row.equals("A") ? 1 : 2)
                        .seatColumn(col)        // 1, 2
                        .seatNumber(row + col)  // A1, A2, ...
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        // 2-2. 스탠딩형 공연장 (라이브홀)
        Location standingLocation = Location.builder()
                .name("홍대 롤링홀 (스탠딩형)")
                .address("서울시 마포구 서교동 100-1")
                .addressDetail("지하 1층")
                .totalSeats(200) // 수용 인원
                .floor(1)
                .type(DomainEnums.LocationType.STANDING)
                .build();
        locationRepository.save(standingLocation);
        // 스탠딩은 물리적 Seat 데이터를 생성하지 않음 (보통 ShowTime에 수량으로 관리)


        // ==========================================
        // 3. 공연 생성 (2개: 좌석제 / 스탠딩)
        // ==========================================

        // 3-1. 좌석제 공연
        Shows seatedShow = Shows.builder()
                .manager(manager)
                .location(seatedLocation)
                .title("2024 어쿠스틱 라이브 [겨울]")
                .posterUrl("http://example.com/poster1.jpg")
                .bookingStartAt(LocalDateTime.now().minusDays(7)) // 예매 시작됨
                .bankName(DomainEnums.Bank.TOSS)
                .bankAccountNumber("1000-0000-0000")
                .bankDepositorName("티키타엔터")
                .saleMethod(DomainEnums.SaleMethod.Select_by_User) // 유저가 좌석 선택
                .status(DomainEnums.ShowStatus.PUBLISHED)
                .build();
        showsRepository.save(seatedShow);

        // 메시지 생성
        messageRepository.save(Message.builder()
                .show(seatedShow)
                .paymentGuide("입금 확인까지 30분 소요됩니다.")
                .bookingConfirmation("예매가 확정되었습니다.")
                .bookingCustom("공연 시작 10분 전 입장 부탁드립니다.")
                .qrGuide("QR코드를 스태프에게 제시해주세요.")
                .build());

        // 3-2. 스탠딩 공연
        Shows standingShow = Shows.builder()
                .manager(manager)
                .location(standingLocation)
                .title("락 페스티벌 프리뷰")
                .posterUrl("http://example.com/poster2.jpg")
                .bookingStartAt(LocalDateTime.now().minusDays(3))
                .bankName(DomainEnums.Bank.KAKAO)
                .bankAccountNumber("3333-00-0000")
                .bankDepositorName("티키타엔터")
                .saleMethod(DomainEnums.SaleMethod.STANDING) // 스탠딩 예매
                .status(DomainEnums.ShowStatus.PUBLISHED)
                .build();
        showsRepository.save(standingShow);

        messageRepository.save(Message.builder()
                .show(standingShow)
                .paymentGuide("입금 순서대로 입장 번호가 부여됩니다.")
                .bookingConfirmation("예매 확정! 뛰어놀 준비 되셨나요?")
                .bookingCustom("물품 보관소가 협소합니다.")
                .qrGuide("입장 팔찌 교환처에서 QR을 보여주세요.")
                .build());


        // ==========================================
        // 4. 회차(ShowTime) 생성 (각 공연당 2개씩)
        // ==========================================

        // 4-1. 좌석제 공연 회차
        ShowTime st1_time1 = ShowTime.builder()
                .show(seatedShow)
                .startAt(LocalDateTime.now().plusDays(10).withHour(14).withMinute(0)) // 10일 뒤 14시
                .endAt(LocalDateTime.now().plusDays(10).withHour(16).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(9))
                .remainSeatCount(4L) // 전체 4석
                .build();

        ShowTime st1_time2 = ShowTime.builder()
                .show(seatedShow)
                .startAt(LocalDateTime.now().plusDays(10).withHour(19).withMinute(0)) // 10일 뒤 19시
                .endAt(LocalDateTime.now().plusDays(10).withHour(21).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(9))
                .remainSeatCount(4L)
                .build();
        showTimeRepository.saveAll(List.of(st1_time1, st1_time2));

        // 4-2. 스탠딩 공연 회차 (입장 정원 설정)
        ShowTime st2_time1 = ShowTime.builder()
                .show(standingShow)
                .startAt(LocalDateTime.now().plusDays(20).withHour(18).withMinute(0))
                .endAt(LocalDateTime.now().plusDays(20).withHour(20).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(19))
                .totalStandingQuantity(150L) // 스탠딩 150명
                .remainSeatCount(150L)
                .build();

        ShowTime st2_time2 = ShowTime.builder()
                .show(standingShow)
                .startAt(LocalDateTime.now().plusDays(21).withHour(18).withMinute(0))
                .endAt(LocalDateTime.now().plusDays(21).withHour(20).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(20))
                .totalStandingQuantity(150L)
                .remainSeatCount(150L)
                .build();
        showTimeRepository.saveAll(List.of(st2_time1, st2_time2));


        // ==========================================
        // 5. 티켓 옵션 생성 (각 공연당 2개씩)
        // ==========================================

        // 좌석제 공연 옵션
        ticketOptionRepository.saveAll(List.of(
                TicketOption.builder().show(seatedShow).name("VIP석").price(88000).description("1열 중앙").build(),
                TicketOption.builder().show(seatedShow).name("R석").price(66000).description("2열").build()
        ));

        // 스탠딩 공연 옵션
        ticketOptionRepository.saveAll(List.of(
                TicketOption.builder().show(standingShow).name("얼리버드").price(30000).description("한정 수량 특가").build(),
                TicketOption.builder().show(standingShow).name("일반예매").price(55000).description("정가").build()
        ));


        // ==========================================
        // 6. ShowSeat 생성 (좌석제 공연에만 필요)
        // ==========================================
        // 공연장(Location)의 물리적 Seat들과 ShowTime을 1:1로 매핑해야 함

        List<ShowSeat> showSeats = new ArrayList<>();

        // 회차 1 (st1_time1)에 대한 좌석 매핑
        for (Seat seat : seats) {
            showSeats.add(ShowSeat.builder()
                    .showTime(st1_time1)
                    .seat(seat)
                    .isAvailable(true)
                    .isGoodSeat(seat.getSection().equals("A")) // A열은 좋은 좌석으로 설정 예시
                    .build());
        }

        // 회차 2 (st1_time2)에 대한 좌석 매핑
        for (Seat seat : seats) {
            showSeats.add(ShowSeat.builder()
                    .showTime(st1_time2)
                    .seat(seat)
                    .isAvailable(true)
                    .isGoodSeat(seat.getSection().equals("A"))
                    .build());
        }
        showSeatRepository.saveAll(showSeats);

        // 스탠딩 공연은 ShowSeat를 만들지 않음 (ShowTime의 quantity로 관리하거나, 필요 시 가상의 Seat를 만드는데 보통 안 만듦)

        System.out.println("--- 테스트 데이터 생성 완료! ---");
    }
}