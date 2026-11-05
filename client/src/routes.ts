/** 화면 ID(SCR-xx)별 경로. 화면 목록은 md/domain-design.md §9.1 */
export const ROUTES = {
  SCR01_HOME: '/',
  /** SCR-02 커뮤니티 목록 (?postType=, ?loanType=, ?page=) */
  SCR02_POSTS: '/posts',
  /** SCR-04 글쓰기 (?companyId= 로 업체를 미리 고를 수 있다) */
  SCR04_POST_NEW: '/posts/new',
  /** SCR-03 글 상세 + 답변 */
  SCR03_POST: '/posts/:postId',
  /** SCR-05 업체 조회 (?keyword=, ?page=) */
  SCR05_COMPANIES: '/companies',
  SCR05_COMPANY: '/companies/:companyId',
  /** SCR-06 피해 예방 안내 + 받은 문자 검사 */
  SCR06_SAFETY: '/safety',
  /** SCR-07 로그인·가입 (?next=, ?mode=signup) */
  SCR07_LOGIN: '/login',
} as const

export const postPath = (postId: number) => `/posts/${postId}`
export const companyPath = (companyId: number) => `/companies/${companyId}`
export const loginPath = (next: string) => `/login?next=${encodeURIComponent(next)}`

/** 로그인 뒤 돌아갈 경로. 같은 사이트 경로만 받는다 */
export function safeNext(next: string | null) {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : ROUTES.SCR01_HOME
}

/** ?page= 값. 없거나 1보다 작으면 1 */
export function pageParam(search: URLSearchParams) {
  const value = Number(search.get('page'))
  return Number.isInteger(value) && value > 0 ? value : 1
}
