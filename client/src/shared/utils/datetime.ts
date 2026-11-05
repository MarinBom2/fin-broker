const KST_DATE = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** 서버의 UTC ISO 문자열을 한국 날짜로 (서버 BaseEntity 는 Instant) */
export function formatKstDate(iso: string | null | undefined) {
  return iso ? KST_DATE.format(new Date(iso)) : '-'
}
