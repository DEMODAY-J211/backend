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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HaechandeulDataInitializer implements CommandLineRunner {

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
        createTestDataForManager("haechandeul@naver.com", "전해찬", "해찬들 공연기획", "해찬들을 위한 테스트용 매니저입니다.", "HC");
        createTestDataForManager("cellano300@gmail.com", "이예나", "셀라노 공연기획", "셀라노를 위한 테스트용 매니저입니다.", "CL");
    }

    private void createTestDataForManager(String managerEmail, String userName, String managerName, String managerIntroduction, String reservationPrefix) {
        KakaoOauth managerOauth = kakaoOauthRepository.findByEmail(managerEmail)
                .orElseGet(() -> {
                    System.out.println("--- " + managerEmail + " 사용자가 존재하지 않아 새로 생성합니다. ---");
                    return kakaoOauthRepository.save(KakaoOauth.builder()
                            .email(managerEmail)
                            .name(userName)
                            .role(DomainEnums.Role.USER)
                            .createdAt(LocalDateTime.now())
                            .visitedPath(DomainEnums.VisitedPath.ETC)
                            .build());
                });

        if (managerOauth.getRole() != DomainEnums.Role.MANAGER) {
            System.out.println("--- " + managerEmail + " 사용자의 역할을 MANAGER로 변경합니다. ---");
            managerOauth.setRole(DomainEnums.Role.MANAGER);
            kakaoOauthRepository.save(managerOauth);
        }

        Manager manager;
        Optional<Manager> existingManagerOpt = managerRepository.findByKakaoOauth(managerOauth);

        if (existingManagerOpt.isPresent()) {
            manager = existingManagerOpt.get();
            if ("haechandeul@naver.com".equals(managerEmail)) {
                System.out.println("--- " + managerEmail + " 사용자가 이미 존재합니다. 새로운 공연을 생성합니다. ---");
            } else {
                System.out.println("--- " + managerEmail + " 사용자의 테스트 데이터가 이미 존재합니다. ---");
                return;
            }
        } else {
            System.out.println("--- " + managerEmail + " 사용자의 테스트 데이터 생성을 시작합니다. ---");
            manager = Manager.builder()
                    .kakaoOauth(managerOauth)
                    .name(managerName)
                    .introduction(managerIntroduction)
                    .urls(List.of("http://example.com"))
                    .build();
            managerRepository.save(manager);
        }

        Location location;
        List<Seat> seats = new ArrayList<>();

        if ("haechandeul@naver.com".equals(managerEmail)) {
            location = locationRepository.findById(18L)
                .orElseThrow(() -> new RuntimeException("Location with ID 18 not found"));
        } else {
            location = Location.builder()
                .name(userName + " 아트센터")
                .address("서울시 강남구 테헤란로 123")
                .totalSeats(150)
                .floor(2)
                .type(DomainEnums.LocationType.SEATED)
                .build();
            locationRepository.save(location);
            
            for (int i = 1; i <= 5; i++) {
                seats.add(Seat.builder().location(location).floor(1).section("A").seatRow(1).seatColumn(i).seatNumber("A-" + i).build());
            }
            seatRepository.saveAll(seats);
        }


        Shows newShow = Shows.builder()
                .manager(manager)
                .location(location)
                .title(userName + "의 첫번째 공연")
                .posterUrl("http://example.com/poster.jpg")
                .bookingStartAt(LocalDateTime.now().plusDays(2))
                .bankName(DomainEnums.Bank.HANA)
                .bankAccountNumber("111-222-333333")
                .bankDepositorName(userName)
                .saleMethod(DomainEnums.SaleMethod.Select_by_User)
                .status(DomainEnums.ShowStatus.PUBLISHED)
                .build();
        showsRepository.save(newShow);

        Message newMessage = Message.builder()
                .show(newShow)
                .paymentGuide("결제 안내: 2시간 내 미입금 시 자동 취소됩니다.")
                .bookingConfirmation("예매 확정: " + userName + "의 공연 예매가 확정되었습니다.")
                .bookingCustom("환영합니다! 즐거운 시간 되세요.")
                .qrGuide("QR 안내: 입장 시 본인 확인용 QR 코드를 준비해주세요.")
                .build();
        messageRepository.save(newMessage);

        ShowTime newShowTime1 = ShowTime.builder()
                .show(newShow)
                .startAt(LocalDateTime.now().plusDays(15).withHour(20).withMinute(0))
                .endAt(LocalDateTime.now().plusDays(15).withHour(22).withMinute(0))
                .bookingEndAt(LocalDateTime.now().plusDays(14))
                .remainSeatCount(150L)
                .build();
        showTimeRepository.save(newShowTime1);

        TicketOption vipSeat = TicketOption.builder().show(newShow).name("VIP석").price(150000).build();
        ticketOptionRepository.save(vipSeat);

        List<ShowSeat> showSeats = new ArrayList<>();
        for (Seat seat : seats) {
            showSeats.add(ShowSeat.builder().showTime(newShowTime1).seat(seat).isAvailable(true).build());
        }
        showSeatRepository.saveAll(showSeats);

        // 테스트 유저 5명 생성
        List<KakaoOauth> testUsers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            // 이메일 중복을 피하기 위해 고유한 이메일 생성
            String userEmail = "testuser" + reservationPrefix + i + "@example.com";
            if (kakaoOauthRepository.findByEmail(userEmail).isEmpty()) {
                testUsers.add(KakaoOauth.builder()
                        .email(userEmail)
                        .name("테스트유저" + i)
                        .role(DomainEnums.Role.USER)
                        .createdAt(LocalDateTime.now())
                        .visitedPath(DomainEnums.VisitedPath.ETC)
                        .build());
            }
        }
        kakaoOauthRepository.saveAll(testUsers);
        
        // 예매 데이터 생성
        for (int i = 0; i < Math.min(testUsers.size(), showSeats.size()); i++) {
            KakaoOauth user = testUsers.get(i);
            ShowSeat showSeat = showSeats.get(i);

            if (showSeat.isAvailable()) {
                Reservation reservation = Reservation.builder()
                        .reservationNumber(reservationPrefix + System.currentTimeMillis() + i)
                        .user(user)
                        .showTime(newShowTime1)
                        .ticketOption(vipSeat)
                        .quantity(1)
                        .totalPrice(vipSeat.getPrice())
                        .phone("010-1111-111" + i)
                        .refundAccountNumber("987-654-321" + i)
                        .status(DomainEnums.ReservationStatus.CONFIRMED)
                        .createdAt(LocalDateTime.now())
                        .build();
                reservationRepository.save(reservation);

                ReservationItem item = ReservationItem.builder()
                        .reservation(reservation)
                        .showSeat(showSeat)
                        .status(DomainEnums.ReservationStatus.CONFIRMED)
                        .build();
                reservationItemRepository.save(item);

                showSeat.reserve(); // 좌석 예매 처리
                showSeatRepository.save(showSeat);
            }
        }

        System.out.println("--- " + managerEmail + " 사용자의 테스트 데이터 생성 완료! ---");
    }
}