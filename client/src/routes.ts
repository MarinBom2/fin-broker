/** 화면 ID(SCR-xx)별 경로. 화면 목록은 md/domain-design.md §9.1 */
export const ROUTES = {
  SCR01_HOME: '/',
  /** SCR-07 로그인·가입 (?next=, ?mode=signup) */
  SCR07_LOGIN: '/login',
} as const

export const loginPath = (next: string) => `/login?next=${encodeURIComponent(next)}`

/** 로그인 뒤 돌아갈 경로. 같은 사이트 경로만 받는다 */
export function safeNext(next: string | null) {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : ROUTES.SCR01_HOME
}
