package com.marinbom2.finbroker.member.application;

import com.marinbom2.finbroker.exception.BusinessException;
import com.marinbom2.finbroker.member.domain.Member;
import com.marinbom2.finbroker.member.domain.MemberRepository;
import com.marinbom2.finbroker.member.domain.RoleType;
import com.marinbom2.finbroker.security.MemberSecurityProperties;
import com.marinbom2.finbroker.type.common.ErrorCode;
import com.marinbom2.finbroker.util.ValidationHelperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 가입·로그아웃·조회·권한 (FR-AU-001~003). 로그인은 시큐리티 필터가 한다 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberSecurityProperties securityProperties;

    /** FR-AU-001: 가입. 권한은 USER(금융소비자)로 시작한다 */
    @Transactional
    public Member signUp(String rawEmail, String rawPassword) {
        String email = Member.normalizeEmail(rawEmail);

        if (ValidationHelperUtils.isInValidPassword(rawPassword)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_DATA,
                    "비밀번호는 6~20자로, 영문 대소문자·숫자·특수문자를 조합해 주세요.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 가입된 이메일입니다.");
        }

        try {
            Member member = memberRepository.saveAndFlush(Member.signUp(email, passwordEncoder.encode(rawPassword)));
            log.info("[member] signup memberId={}", member.getId());
            return member;
        } catch (DataIntegrityViolationException e) {
            // 같은 이메일로 동시에 가입하면 위 확인을 둘 다 통과할 수 있다. 유니크 제약이 최종 판정
            throw new BusinessException(ErrorCode.ALREADY_EXIST, "이미 가입된 이메일입니다.");
        }
    }

    /** FR-AU-002: 이 계정으로 발급한 토큰을 전부 무효로 만든다 */
    @Transactional
    public void logout(Long memberId) {
        findMember(memberId).invalidateTokens();
        log.info("[member] logout memberId={}", memberId);
    }

    @Transactional(readOnly = true)
    public Member get(Long memberId) {
        return findMember(memberId);
    }

    /** FR-AU-003: 내 권한 바꾸기. security.role.self-change-allowed 가 켜진 환경(local)에서만 */
    @Transactional
    public Member changeOwnRole(Long memberId, RoleType roleType) {
        if (!securityProperties.role().selfChangeAllowed()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "이 환경에서는 권한을 직접 바꿀 수 없습니다.");
        }
        Member member = findMember(memberId);
        member.changeRole(roleType);
        log.warn("[member] self role change memberId={} role={}", memberId, roleType);
        return member;
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
    }
}
