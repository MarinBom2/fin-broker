package com.marinbom2.finbroker.company.domain;

import com.marinbom2.finbroker.company.domain.exception.IllegalCompanyStateException;
import com.marinbom2.finbroker.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출 관련 등록 업체 (FR-CO-001·002, md/domain-design.md §5.2).
 * 등록 정보에서 온 사실만 담는다. 금리·한도·상품 소개 칸을 두지 않는다 (홍보 통로가 되지 않게, §12).
 */
@Getter
@Entity
@Table(name = "company", uniqueConstraints = @UniqueConstraint(
        name = "uk_company_type_registration_number", columnNames = {"company_type", "registration_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 등록 상호 */
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 20)
    private CompanyType companyType;

    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    /** 등록기관 (시·도, 금융위원회 등) */
    @Column(name = "registration_authority", nullable = false, length = 50)
    private String registrationAuthority;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    /** 마지막으로 대조한 운영자 회원 ID */
    @Column(name = "checked_by")
    private Long checkedBy;

    @Version
    private Long version;

    /** 운영자가 업체를 등록한다. 대조 전이라 UNVERIFIED 로 시작한다 */
    public static Company register(String name, CompanyType companyType, String registrationNumber,
                                   String registrationAuthority) {
        Company company = new Company();
        company.name = name.trim();
        company.companyType = companyType;
        company.registrationNumber = normalizeRegistrationNumber(registrationNumber);
        company.registrationAuthority = registrationAuthority.trim();
        company.registrationStatus = RegistrationStatus.UNVERIFIED;
        return company;
    }

    /** 등록·검색에서 같은 규칙으로 등록번호를 맞춘다 (공백 제거) */
    public static String normalizeRegistrationNumber(String raw) {
        return raw == null ? "" : raw.replaceAll("\\s+", "");
    }

    /**
     * 등록 정보와 대조한 결과를 남긴다 (§4.1 전이 표).
     *
     * @throws IllegalCompanyStateException 허용되지 않는 전이 (예: UNVERIFIED 로 되돌리기, 확인 전 업체를 만료)
     */
    public void recordCheck(RegistrationStatus result, Long adminId, Instant now) {
        if (!registrationStatus.canMoveTo(result)) {
            throw new IllegalCompanyStateException(id, registrationStatus, result);
        }
        this.registrationStatus = result;
        this.lastCheckedAt = now;
        this.checkedBy = adminId;
    }

    public boolean isVerified() {
        return registrationStatus == RegistrationStatus.VERIFIED;
    }

    /**
     * 등록 확인한 지 interval 이 지나 다시 대조해야 하는지 (등록 취소·만료를 놓치지 않으려는 것, md/domain-design.md §4.1).
     * 확인되지 않은 업체는 재확인 대상이 아니라 대조 대상이라 false.
     */
    public boolean needsRecheck(Duration interval, Instant now) {
        return isVerified() && lastCheckedAt != null && !lastCheckedAt.plus(interval).isAfter(now);
    }
}
