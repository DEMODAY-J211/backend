package com.tikitta.backend.domain;


public final class DomainEnums {

    // private 생성자로 인스턴스화 방지
    private DomainEnums() {}

    /**
     * 사용자 역할 (관리자 / 일반 예매자)
     */
    public enum Role {
        MANAGER, USER,NO
    }

    /**
     * 성별
     */
    public enum Gender {
        MALE, FEMALE
    }

    /**
     * 가입 경로
     */
    public enum VisitedPath {
        INSTAGRAM, PORTAL, FRIEND, ETC
    }

    /**
     * 공연장 유형 (좌석제 / 스탠딩)
     */
    public enum LocationType {
        SEATED, STANDING
    }

    /**
     * 공연 저장 상태 (임시저장 / 발행)
     */
    public enum ShowStatus {
        DRAFT, PUBLISHED
    }

    /**
     * 좌석 판매 방법 (미지정 / 스케줄링)
     */
    public enum SaleMethod {// 순서대로 공연자맘대로(현장예매)/스케쥴링/스탠딩/예매자선택
        Event_Host, SCHEDULING, STANDING, Select_by_User
    }

    /**
     * 입금 계좌 은행
     */
    public enum Bank {
        WOORI, NH, KAKAO, SHINHAN, IBK, HANA, KB, EPOST, TOSS
    }

    /**
     * 예매 상태 (주문 단위)
     */
    public enum ReservationStatus {
        PENDING_PAYMENT,    // 입금대기
        CONFIRMED,          // 입금확정
        CANCEL_REQUESTED,   // 환불대기
        CANCELED            // 취소완료
    }
}