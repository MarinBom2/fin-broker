import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { postPath, ROUTES } from '../../routes'
import { companyApi } from '../../shared/api/company/companyService'
import type { Company } from '../../shared/api/company/companyService'
import type { CompanyRef, PostDetail } from '../../shared/api/community/communityService'
import { blockedWarnings, communityApi, LOAN_TYPES, POST_TYPES } from '../../shared/api/community/communityService'
import type { LoanTypeCode, PostTypeCode } from '../../shared/api/community/communityService'
import { warningApi } from '../../shared/api/warning/warningService'
import type { WarningCheckResult, WarningItem } from '../../shared/api/warning/warningService'
import { ApiError, errorMessage } from '../../shared/core/apiClient'
import { BlockIcon, CheckCircleIcon, ChevronLeftIcon, ExclamationIcon, InfoIcon, SearchIcon, XIcon } from '../../shared/ui/icons/Icons'
import { PageHead } from '../../shared/ui/PageHead'
import { SegmentedControl } from '../../shared/ui/SegmentedControl'
import { StatusPill } from '../../shared/ui/StatusPill'
import { WarningList } from '../../shared/ui/WarningList'

const TITLE_MAX = 100
const BODY_MAX = 5000
/** 입력이 멈추고 이만큼 지나면 위험 신호를 미리 검사한다 */
const PREVIEW_DELAY_MS = 600
const COMPANY_SEARCH_DELAY_MS = 300

/** 고른 업체. 검색 결과(Company)와 글에 붙은 업체(CompanyRef) 둘 다 담을 수 있게 등록번호는 선택 */
type PickedCompany = CompanyRef & { registrationNumber?: string }

interface FormValues {
  postType: PostTypeCode
  loanType: LoanTypeCode
  title: string
  body: string
  company: PickedCompany | null
}

const EMPTY: FormValues = { postType: 'QUESTION', loanType: 'CREDIT', title: '', body: '', company: null }

/** SCR-04 글쓰기 (FR-CM-001) */
export default function PostWritePage() {
  return <PostForm initial={EMPTY} />
}

/** SCR-04 글 고치기 (FR-CM-001). 글을 읽어 같은 화면에 채운다. 남의 글이면 서버가 403 을 준다 */
export function PostEditPage() {
  const { postId } = useParams()
  const id = Number(postId)
  const validId = Number.isInteger(id) && id > 0
  const [post, setPost] = useState<PostDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const error = validId ? loadError : '글을 찾을 수 없습니다.'

  useEffect(() => {
    if (!validId) return
    let cancelled = false
    communityApi
      .get(id)
      .then((data) => {
        if (cancelled) return
        if (data.mine) setPost(data)
        else setLoadError('내가 쓴 글만 고칠 수 있어요.')
      })
      .catch((e) => {
        if (!cancelled) setLoadError(errorMessage(e, '글을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [id, validId])

  if (error) {
    return (
      <p className="error-text" role="alert">
        {error}
      </p>
    )
  }
  if (!post) {
    return <div className="skeleton skeleton--detail" />
  }
  return (
    <PostForm
      postId={post.postId}
      initial={{
        postType: post.postType.code,
        loanType: post.loanType.code,
        title: post.title,
        body: post.body,
        company: post.company,
      }}
    />
  )
}

/** 글쓰기·고치기 공용 양식 (FR-WN-001). 입력하는 동안 위험 신호를 미리 보여 주고, 저장 때 서버가 다시 검사한다 */
function PostForm({ initial, postId }: { initial: FormValues; postId?: number }) {
  const navigate = useNavigate()
  const editing = postId !== undefined

  const [postType, setPostType] = useState<PostTypeCode>(initial.postType)
  const [loanType, setLoanType] = useState<LoanTypeCode>(initial.loanType)
  const [title, setTitle] = useState(initial.title)
  const [body, setBody] = useState(initial.body)
  const [company, setCompany] = useState<PickedCompany | null>(initial.company)

  const [companyKeyword, setCompanyKeyword] = useState('')
  const [companyResults, setCompanyResults] = useState<Company[]>([])

  const [preview, setPreview] = useState<WarningCheckResult | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [blocked, setBlocked] = useState<WarningItem[] | null>(null)

  const companyId = company?.companyId ?? null

  /** 입력이 멈추면 위험 신호 미리 보기 (저장하지 않는다) */
  useEffect(() => {
    if (!body.trim()) return
    let cancelled = false
    const timer = window.setTimeout(() => {
      warningApi
        .check({ title, body, companyId })
        .then((result) => {
          if (!cancelled) setPreview(result)
        })
        .catch(() => {
          // 미리 보기는 보조 기능이다. 실패해도 저장 때 서버가 다시 검사한다
        })
    }, PREVIEW_DELAY_MS)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [title, body, companyId])

  /** 업체 고르기: 입력이 멈추면 다섯 곳까지 찾는다 */
  useEffect(() => {
    const keyword = companyKeyword.trim()
    if (!keyword) return
    let cancelled = false
    const timer = window.setTimeout(() => {
      companyApi
        .search({ keyword, size: 5 })
        .then((page) => {
          if (!cancelled) setCompanyResults(page.content)
        })
        .catch(() => {
          if (!cancelled) setCompanyResults([])
        })
    }, COMPANY_SEARCH_DELAY_MS)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [companyKeyword])

  const shownPreview = body.trim() ? preview : null
  const shownResults = companyKeyword.trim() ? companyResults : []
  const previewBlocked = shownPreview?.blocked ?? false
  /** 미리 보기 배지는 종류별로 한 번만 (같은 신호가 본문 여러 곳에서 잡혀도) */
  const previewTypes = (shownPreview?.warnings ?? [])
    .map((warning) => warning.type)
    .filter((type, index, all) => all.findIndex((t) => t.code === type.code) === index)

  function pickCompany(picked: Company) {
    setCompany(picked)
    setCompanyKeyword('')
    setCompanyResults([])
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    setBlocked(null)
    try {
      const request = { postType, loanType, companyId, title, body }
      const saved = editing ? await communityApi.edit(postId, request) : await communityApi.write(request)
      navigate(postPath(saved.postId), { replace: true })
    } catch (e) {
      const blockedList = blockedWarnings(e)
      if (blockedList) {
        setBlocked(blockedList)
      } else {
        const fieldMessage = e instanceof ApiError ? e.fieldErrors[0]?.message : undefined
        setError(fieldMessage ?? errorMessage(e, editing ? '글을 고치지 못했습니다.' : '글을 올리지 못했습니다.'))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <Link to={editing ? postPath(postId) : ROUTES.SCR02_POSTS} className="back-link">
        <ChevronLeftIcon /> {editing ? '글로 돌아가기' : '커뮤니티'}
      </Link>
      <PageHead
        title={editing ? '글 고치기' : '글쓰기'}
        lede={editing ? '고친 내용도 다시 검사해요. 위험 신호가 바뀌면 글의 배지도 바뀌어요.' : '익명으로 올라가요. 쓰는 동안 위험 신호를 미리 알려 드릴게요.'}
      />

      <form className="stack write-form" onSubmit={handleSubmit}>
        <section className="card stack">
          <div className="form-field">
            <span className="form-field__label">글 유형</span>
            <SegmentedControl
              label="글 유형"
              value={postType}
              options={POST_TYPES.map((t) => ({ value: t.code, label: t.label }))}
              onChange={setPostType}
            />
          </div>

          <label className="form-field">
            <span className="form-field__label">대출 유형</span>
            <select className="select" value={loanType} onChange={(e) => setLoanType(e.target.value as LoanTypeCode)}>
              {LOAN_TYPES.map((t) => (
                <option key={t.code} value={t.code}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>

          <div className="form-field">
            <span className="form-field__label">업체 (선택)</span>
            {company ? (
              <div className="picked">
                <span className="avatar" aria-hidden="true">
                  {company.name.slice(0, 1)}
                </span>
                <span className="row__main">
                  <span className="row__title">{company.name}</span>
                  {company.registrationNumber ? <span className="row__sub">{company.registrationNumber}</span> : null}
                </span>
                <StatusPill status={company.registrationStatus} />
                <button type="button" className="icon-button" aria-label="업체 선택 지우기" onClick={() => setCompany(null)}>
                  <XIcon />
                </button>
              </div>
            ) : (
              <div className="stack stack--tight">
                <div className="search">
                  <SearchIcon className="search__icon" size={18} />
                  <input
                    className="search__input"
                    type="search"
                    aria-label="업체 찾기"
                    placeholder="후기·피해 사례라면 업체를 골라 주세요"
                    value={companyKeyword}
                    onChange={(e) => setCompanyKeyword(e.target.value)}
                  />
                </div>
                {shownResults.length > 0 ? (
                  <ul className="group" role="listbox" aria-label="업체 검색 결과">
                    {shownResults.map((result) => (
                      <li key={result.companyId}>
                        <button type="button" className="row row--with-avatar row--button" onClick={() => pickCompany(result)}>
                          <span className="avatar" aria-hidden="true">
                            {result.name.slice(0, 1)}
                          </span>
                          <span className="row__main">
                            <span className="row__title">{result.name}</span>
                            <span className="row__sub">
                              {result.companyType.description} · {result.registrationNumber}
                            </span>
                          </span>
                          <StatusPill status={result.registrationStatus} />
                        </button>
                      </li>
                    ))}
                  </ul>
                ) : null}
              </div>
            )}
          </div>
        </section>

        <section className="card stack">
          <label className="form-field">
            <span className="form-field__label">제목</span>
            <input
              className="input"
              type="text"
              required
              maxLength={TITLE_MAX}
              placeholder="무엇이 궁금한가요?"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </label>
          <label className="form-field">
            <span className="form-field__label">내용</span>
            <textarea
              className="textarea"
              rows={8}
              required
              maxLength={BODY_MAX}
              placeholder="상황을 자세히 적을수록 좋은 답변을 받을 수 있어요."
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          </label>
          <div className="cluster space-between">
            <span className="caption">
              <InfoIcon size={14} className="inline-icon" /> 전화번호·주민번호·계좌번호·메신저 아이디는 쓸 수 없어요
            </span>
            <span className="caption">
              {body.length.toLocaleString('ko-KR')} / {BODY_MAX.toLocaleString('ko-KR')}
            </span>
          </div>

          {shownPreview ? (
            <div className="preview-bar" aria-live="polite">
              {shownPreview.warnings.length === 0 ? (
                <span className="preview-bar__ok">
                  <CheckCircleIcon size={16} /> 찾은 위험 신호가 없어요
                </span>
              ) : (
                previewTypes.map((type) => {
                  const isBlock = type.severity.code === 'BLOCK'
                  return (
                    <span key={type.code} className={`pill pill--plain ${isBlock ? 'pill--red' : 'pill--orange'}`}>
                      {isBlock ? <BlockIcon size={12} /> : <ExclamationIcon size={12} />} {type.description}
                    </span>
                  )
                })
              )}
            </div>
          ) : null}
        </section>

        {previewBlocked ? (
          <div className="callout callout--orange">
            <BlockIcon className="callout__icon" />
            <span>연락처·개인정보가 들어 있어 올릴 수 없어요. 해당 부분을 지워 주세요.</span>
          </div>
        ) : null}
        {error ? (
          <p className="error-text" role="alert">
            {error}
          </p>
        ) : null}
        {blocked ? (
          <div className="stack stack--tight" role="alert">
            <p className="error-text">글에 쓸 수 없는 내용이 있어요. 아래 표시된 부분을 지우고 다시 올려 주세요.</p>
            <WarningList warnings={blocked} />
          </div>
        ) : null}

        <div className="write-form__actions">
          <Link to={editing ? postPath(postId) : ROUTES.SCR02_POSTS} className="btn btn--secondary">
            취소
          </Link>
          <button type="submit" className="btn btn--primary" disabled={submitting || previewBlocked || !title.trim() || !body.trim()}>
            {submitting ? '저장하는 중…' : editing ? '저장' : '올리기'}
          </button>
        </div>
      </form>
    </>
  )
}
