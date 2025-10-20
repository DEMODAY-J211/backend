package com.tikitta.backend.config;

import com.tikitta.backend.domain.*;
import com.tikitta.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    // (Repository 주입 부분은 동일)
    private final KakaoOauthRepository kakaoOauthRepository;
    private final ManagerRepository managerRepository;
    private final LocationRepository locationRepository;
    private final SeatRepository seatRepository;
    private final ShowsRepository showsRepository;
    private final MessageRepository messageRepository;
    private final ShowTimeRepository showTimeRepository;
    private final TicketOptionRepository ticketOptionRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        String testManagerEmail = "test_manager@kakao.com";

        if (kakaoOauthRepository.findByEmail(testManagerEmail).isPresent()) {
            System.out.println("--- 테스트 데이터가 이미 존재합니다. ---");
            return;
        }

        System.out.println("--- 테스트 데이터 생성을 시작합니다. ---");

        // (Oauth 계정, Manager 생성 부분은 동일)
        KakaoOauth managerOauth = KakaoOauth.builder()
                .email(testManagerEmail)
                .name("테스트매니저")
                .role(DomainEnums.Role.MANAGER)
                .createdAt(LocalDateTime.now())
                .visitedPath(DomainEnums.VisitedPath.ETC)
                .build();
        kakaoOauthRepository.save(managerOauth);

        KakaoOauth userOauth = KakaoOauth.builder()
                .email("test_user@kakao.com")
                .name("테스트유저")
                .role(DomainEnums.Role.USER)
                .createdAt(LocalDateTime.now())
                .visitedPath(DomainEnums.VisitedPath.FRIEND)
                .build();
        kakaoOauthRepository.save(userOauth);

        Manager testManager = Manager.builder()
                .kakaoOauth(managerOauth)
                .name("테스트공연기획사")
                .introduction("테스트용 매니저 소개입니다.")
                .urls(List.of("http://test.com/instagram"))
                .build();
        managerRepository.save(testManager);

        // (Location 생성 부분은 동일)
        Location testLocation = Location.builder()
                .name("테스트 공연장 (좌석제)")
                .address("서울시 테스트구 테스트로 123")
                .totalSeats(100)
                .floor(1)
                .type(DomainEnums.LocationType.SEATED)
                .build();
        locationRepository.save(testLocation);

        // 7. 좌석 생성 (A1, A2)  <- 🚨🚨🚨 수정된 부분 🚨🚨🚨
        Seat seatA1 = Seat.builder()
                .location(testLocation)
                .floor(1) // ◀◀◀ 이 값이 추가되었습니다.
                .section("A")
                .seatRow("1")
                .seatCol("1")
                .seatNumber("A1")
                .build();
        Seat seatA2 = Seat.builder()
                .location(testLocation)
                .floor(1) // ◀◀◀ 이 값이 추가되었습니다.
                .section("A")
                .seatRow("1")
                .seatCol("2")
                .seatNumber("A2")
                .build();
        seatRepository.saveAll(List.of(seatA1, seatA2));

        // (Shows, Message, ShowTime, TicketOption, ShowSeat, Reservation, ReservationItem 생성 부분은 동일)
        Shows testShow = Shows.builder()
                .manager(testManager)
                .location(testLocation)
                .title("테스트 공연 1")
                .posterUrl("http://example.com/poster.jpg")
                .bookingStartAt(LocalDateTime.now().plusDays(1))
                .bankName(DomainEnums.Bank.KAKAO)
                .bankAccountNumber("123-456-789")
                .bankDepositorName("테스트매니저")
                .saleMethod(DomainEnums.SaleMethod.Select_by_User)
                .status(DomainEnums.ShowStatus.PUBLISHED)
                .build();
        showsRepository.save(testShow);

        Message testMessage = Message.builder()
                .show(testShow)
                .paymentGuide("입금 안내: 1시간 내 미입금 시 자동 취소됩니다.")
                .bookingConfirmation("예매 확정: 예매가 확정되었습니다.")
                .bookingCustom("커스텀 안내: 즐거운 관람 되세요.")
                .qrGuide("QR 안내: 입장 시 QR 코드를 보여주세요.")
                .build();
        messageRepository.save(testMessage);

        ShowTime testShowTime = ShowTime.builder()
                .show(testShow)
                .startAt(LocalDateTime.now().plusDays(10).withHour(19).withMinute(0))
                .endAt(LocalDateTime.now().plusDays(10).withHour(21).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(9))
                .build();
        showTimeRepository.save(testShowTime);

        TicketOption rSeat = TicketOption.builder()
                .show(testShow)
                .name("R석")
                .quantity(50)
                .price(50000)
                .build();
        ticketOptionRepository.save(rSeat);

        // 🚨 [변경] ShowSeat가 Show가 아닌 ShowTime에 연결
        ShowSeat showSeatA1 = ShowSeat.builder()
                .showTime(testShowTime) // ◀ show(testShow) -> showTime(testShowTime)
                .seat(seatA1)
                .isAvailable(true)
                .build();
        ShowSeat showSeatA2 = ShowSeat.builder()
                .showTime(testShowTime) // ◀ show(testShow) -> showTime(testShowTime)
                .seat(seatA2)
                .isAvailable(true)
                .build();
        showSeatRepository.saveAll(List.of(showSeatA1, showSeatA2));

        Reservation testReservation = Reservation.builder()
                .user(userOauth)
                .showTime(testShowTime)
                .quantity(2)
                .totalPrice(100000)
                .refundAccountNumber("987-654-321 (테스트유저)")
                .status(DomainEnums.ReservationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        reservationRepository.save(testReservation);

        // 🚨 [변경] ReservationItem이 Seat이 아닌 ShowSeat에 연결
        ReservationItem item1 = ReservationItem.builder()
                .reservation(testReservation)
                .showSeat(showSeatA1) // ◀ seat(seatA1) -> showSeat(showSeatA1)
                .build();
        ReservationItem item2 = ReservationItem.builder()
                .reservation(testReservation)
                .showSeat(showSeatA2) // ◀ seat(seatA2) -> showSeat(showSeatA2)
                .build();
        reservationItemRepository.saveAll(List.of(item1, item2));

        // 🚨 [추가] 예매된 좌석(A1, A2)을 Not Available로 변경
        showSeatA1.reserve();
        showSeatA2.reserve();

        System.out.println("--- 테스트 데이터 생성 완료! ---");
    }
}