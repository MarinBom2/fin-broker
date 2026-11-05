package com.marinbom2.finbroker.warning.domain;

/**
 * 규칙이 검사할 대상. 글·답변·붙여 넣은 문자 모두 이 형태로 들어온다.
 *
 * @param text      검사할 문장 전체 (제목과 본문을 이어 붙인 것)
 * @param companyId 글이 가리키는 업체. 없으면 null
 */
public record WarningTarget(String text, Long companyId) {

    public WarningTarget {
        text = text == null ? "" : text;
    }

    public static WarningTarget of(String title, String body, Long companyId) {
        String joined = (title == null ? "" : title) + "\n" + (body == null ? "" : body);
        return new WarningTarget(joined, companyId);
    }
}
