-- Tikitta 더미 공연 데이터 생성 스크립트 (최종본)
-- API 호출 흐름을 분석하여 최종 DB 상태를 재현합니다.

-- ---------------------------------
-- 변수 설정
-- ---------------------------------
SET @oauth_id = 20;
SET @location_id = 2;

-- ---------------------------------
-- 1. 매니저 ID 조회
-- ---------------------------------
SELECT id INTO @manager_id FROM manager WHERE oauth_id = @oauth_id;

-- ---------------------------------
-- 2. 'shows' 테이블에 최종 공연 정보 삽입
-- ---------------------------------
INSERT INTO shows (
    manager_id,
    location_id,
    show_title,
    show_poster_picture,
    show_detail_text,
    show_book_start,
    show_status,
    show_bank_master,
    show_bank,
    show_bank_number,
    show_review_url,
    show_sale_method,
    seat_count
) VALUES (
    @manager_id,
    @location_id,
    '더미 공연: 한여름 밤의 꿈',
    'https://example.com/poster.jpg',
    '이것은 더미 데이터로 생성된 공연입니다.',
    '2025-06-01 14:00:00',
    'PUBLISHED',
    '김해찬',
    'TikkitaBank', -- 실제 DB의 Enum 값
    '123-456-789012',
    'https://example.com/review',
    'SEATED', -- 'seatType'에 해당
    250
);

-- 방금 삽입된 공연의 ID를 변수에 저장
SET @show_id = LAST_INSERT_ID();

-- ---------------------------------
-- 3. 'message' 테이블에 메시지 정보 삽입
-- ---------------------------------
INSERT INTO message (
    show_id,
    payment_guide,
    booking_confirmation,
    show_guide,
    review_request
) VALUES (
    @show_id,
    '결제는 이렇게 하시면 됩니다.',
    '예매가 확정되었습니다.',
    '공연 관람 시 유의사항입니다.',
    '리뷰를 남겨주세요!'
);

-- ---------------------------------
-- 4. 'show_time' 테이블에 공연 시간 정보 삽입
-- ---------------------------------
INSERT INTO show_time (show_id, show_start, show_end, booking_end_at, remain_seat_count)
VALUES (@show_id, '2025-07-01 19:30:00', '2025-07-01 21:30:00', '2025-07-01 18:30:00', 250);

-- 방금 삽입된 공연 시간의 ID를 변수에 저장
SET @show_time_id = LAST_INSERT_ID();

-- ---------------------------------
-- 5. 'ticket_option' 테이블에 티켓 정보 삽입
-- ---------------------------------
INSERT INTO ticket_option (show_id, name, price, description)
VALUES
    (@show_id, 'R석', 88000, 'R석입니다.'),
    (@show_id, 'S석', 66000, 'S석입니다.');

-- ---------------------------------
-- 6. 'show_seat' 테이블에 판매 좌석 정보 삽입
-- 'location_id'가 2인 공연장의 모든 좌석을 가져와 이 공연의 판매 가능 좌석으로 등록합니다.
-- ---------------------------------
INSERT INTO show_seat (seat_id, show_time_id, is_available, is_good_seat)
SELECT id, @show_time_id, TRUE, FALSE
FROM seat
WHERE location_id = @location_id;

SELECT '더미 데이터 생성이 완료되었습니다. show_id: ', @show_id;
