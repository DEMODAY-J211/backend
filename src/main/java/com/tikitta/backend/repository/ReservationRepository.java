package com.tikitta.backend.repository;

import com.tikitta.backend.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation,Long> {
    List<Reservation> findByShowTimeAndStatusIn(ShowTime showTime, List<DomainEnums.ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.showTime st " +
            "JOIN FETCH st.show s " +
            "JOIN FETCH s.location l " +
            "LEFT JOIN FETCH r.reservationItems ri " +
            "LEFT JOIN FETCH ri.showSeat rss " +
            "LEFT JOIN FETCH rss.seat seat " +
            "WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

    // ▼▼▼ 수정된 메소드 ▼▼▼
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.ticketOption " + // 이제 이 JOIN이 정상적으로 동작합니다.
            "WHERE r.showTime = :showTime " +
            "ORDER BY r.createdAt DESC")
    List<Reservation> findByShowTimeWithDetailsOrderByCreatedAtDesc(@Param("showTime") ShowTime showTime);
    // ▲▲▲ 수정된 메소드 ▲▲▲

    /**
     * 특정 사용자의, 특정 매니저의 공연 중, 아직 시작하지 않은 회차의 예매 목록 조회
     */
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.showTime st " +
            "JOIN FETCH st.show s " +
            "JOIN FETCH r.ticketOption " +
            "WHERE r.user = :user AND s.manager = :manager AND st.startAt > :currentTime " + // ◀ 조건 추가
            "ORDER BY st.startAt ASC") // ◀ 다가올 순서로 정렬
    List<Reservation> findUpcomingReservationsByUserAndManager(
            @Param("user") KakaoOauth user,
            @Param("manager") Manager manager,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * 특정 사용자의, 특정 매니저의 공연 중, 이미 시작한 회차의 예매 목록 조회
     */
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.showTime st " +
            "JOIN FETCH st.show s " +
            "JOIN FETCH r.ticketOption " +
            "WHERE r.user = :user AND s.manager = :manager AND st.startAt <= :currentTime " + // ◀ 조건 추가
            "ORDER BY st.startAt DESC") // ◀ 최근 지난 순서로 정렬
    List<Reservation> findPastReservationsByUserAndManager(
            @Param("user") KakaoOauth user,
            @Param("manager") Manager manager,
            @Param("currentTime") LocalDateTime currentTime);
    @Query("SELECT r FROM Reservation r " +
            "JOIN FETCH r.user u " +
            "JOIN FETCH r.ticketOption " +
            "WHERE r.showTime = :showTime " +
            "AND (u.name LIKE %:keyword% " +
            "OR u.phone LIKE %:keyword% " +
            "OR FUNCTION('CONCAT', '', u.id) LIKE %:keyword%) " + // 숫자 ID를 문자열처럼 검색
            "ORDER BY r.createdAt DESC")
    List<Reservation> findByShowTimeAndKeywordWithDetails(
            @Param("showTime") ShowTime showTime,
            @Param("keyword") String keyword
    );

    List<Reservation> findByShowTime(ShowTime showTime);
    List<Reservation> findByShowTimeAndStatus(ShowTime showTime, DomainEnums.ReservationStatus status);

}
