const KST_DATE = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

/** 서버의 UTC ISO 문자열을 한국 날짜로 (서버 BaseEntity 는 Instant) */
export function formatKstDate(iso: string | null | undefined) {
  return iso ? KST_DATE.format(new Date(iso)) : '-'
}

/** 방금 · N분 전 · N시간 전 · N일 전, 일주일이 넘으면 날짜 */
export function formatRelative(iso: string, now: number = Date.now()) {
  const diff = now - new Date(iso).getTime()
  if (diff < MINUTE) return '방금'
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)}분 전`
  if (diff < DAY) return `${Math.floor(diff / HOUR)}시간 전`
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)}일 전`
  return formatKstDate(iso)
}
