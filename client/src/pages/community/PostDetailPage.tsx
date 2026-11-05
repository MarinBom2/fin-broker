import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { companyPath, ROUTES } from '../../routes'
import { blockedWarnings, communityApi } from '../../shared/api/community/communityService'
import type { PostDetail } from '../../shared/api/community/communityService'
import type { WarningItem } from '../../shared/api/warning/warningService'
import { ApiError, errorMessage } from '../../shared/core/apiClient'
import { AuthorBadge } from '../../shared/ui/AuthorBadge'
import { BuildingIcon, ChatIcon, ChevronLeftIcon, ChevronRightIcon, ExclamationIcon } from '../../shared/ui/icons/Icons'
import { StatusPill } from '../../shared/ui/StatusPill'
import { WarningList } from '../../shared/ui/WarningList'
import { formatRelative } from '../../shared/utils/datetime'

const ANSWER_MAX = 3000

/** SCR-03 글 상세 + 답변 (FR-CM-002·003). 주의 신호가 있는 글은 본문 위에 안내를 먼저 보여 준다 */
export default function PostDetailPage() {
  const { postId } = useParams()
  const id = Number(postId)
  const validId = Number.isInteger(id) && id > 0

  const [post, setPost] = useState<PostDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const error = validId ? loadError : '글을 찾을 수 없습니다.'

  const [answerBody, setAnswerBody] = useState('')
  const [answerError, setAnswerError] = useState<string | null>(null)
  const [answerBlocked, setAnswerBlocked] = useState<WarningItem[] | null>(null)
  const [sending, setSending] = useState(false)

  useEffect(() => {
    if (!validId) return
    let cancelled = false
    communityApi
      .get(id)
      .then((data) => {
        if (!cancelled) setPost(data)
      })
      .catch((e) => {
        if (!cancelled) setLoadError(errorMessage(e, '글을 불러오지 못했습니다.'))
      })
    return () => {
      cancelled = true
    }
  }, [id, validId])

  async function handleAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!post) return
    setSending(true)
    setAnswerError(null)
    setAnswerBlocked(null)
    try {
      const answer = await communityApi.answer(post.postId, answerBody)
      setPost({ ...post, answers: [...post.answers, answer] })
      setAnswerBody('')
    } catch (e) {
      const blocked = blockedWarnings(e)
      if (blocked) {
        setAnswerBlocked(blocked)
      } else {
        const fieldMessage = e instanceof ApiError ? e.fieldErrors[0]?.message : undefined
        setAnswerError(fieldMessage ?? errorMessage(e, '답변을 올리지 못했습니다.'))
      }
    } finally {
      setSending(false)
    }
  }

  const back = (
    <Link to={ROUTES.SCR02_POSTS} className="back-link">
      <ChevronLeftIcon /> 커뮤니티
    </Link>
  )

  if (error) {
    return (
      <>
        {back}
        <p className="error-text" role="alert">
          {error}
        </p>
      </>
    )
  }
  if (!post) {
    return (
      <>
        {back}
        <div className="skeleton skeleton--detail" />
      </>
    )
  }

  return (
    <>
      {back}
      <article className="post">
        <div className="post-row__tags">
          <span className="pill pill--plain pill--blue">{post.postType.description}</span>
          <span className="caption">{post.loanType.description}</span>
        </div>
        <h1 className="post__title">{post.title}</h1>
        <div className="post-row__meta">
          <AuthorBadge role={post.authorRole} />
          <span>{formatRelative(post.createdAt)}</span>
          {post.mine ? <span>· 내가 쓴 글</span> : null}
        </div>

        {post.warnings.length > 0 ? (
          <div className="callout callout--orange stack stack--tight">
            <strong className="callout__head">
              <ExclamationIcon size={18} className="callout__icon" /> 이 글에서 주의 신호를 찾았어요
            </strong>
            <ul className="callout__list">
              {post.warnings.map((warning) => (
                <li key={warning.code}>
                  <strong>{warning.description}</strong> — {warning.guidance}
                </li>
              ))}
            </ul>
          </div>
        ) : null}

        <p className="post__body">{post.body}</p>

        {post.company ? (
          <Link to={companyPath(post.company.companyId)} className="company-ref">
            <span className="card__icon card__icon--blue company-ref__icon">
              <BuildingIcon size={20} />
            </span>
            <span className="row__main">
              <span className="caption">이 글이 가리키는 업체</span>
              <span className="row__title">{post.company.name}</span>
            </span>
            <StatusPill status={post.company.registrationStatus} />
            <ChevronRightIcon className="row__chevron" />
          </Link>
        ) : null}
      </article>

      <h2 className="section-title">
        답변 <span className="section-title__count">{post.answers.length}</span>
      </h2>

      {post.answers.length === 0 ? (
        <div className="empty">
          <ChatIcon size={32} />
          <p className="caption">아직 답변이 없어요. 아는 내용이 있다면 나눠 주세요.</p>
        </div>
      ) : (
        <ul className="answers">
          {post.answers.map((answer) => (
            <li key={answer.answerId} className={answer.authorRole.code === 'COMPANY' ? 'answer answer--company' : 'answer'}>
              <div className="post-row__meta">
                <AuthorBadge role={answer.authorRole} isPostAuthor={answer.byPostAuthor} />
                <span>{formatRelative(answer.createdAt)}</span>
                {answer.mine ? <span>· 내 답변</span> : null}
                {answer.warnings.length > 0 ? (
                  <span className="pill pill--plain pill--orange">
                    <ExclamationIcon size={12} /> 주의 신호 {answer.warnings.length}
                  </span>
                ) : null}
              </div>
              <p className="answer__body">{answer.body}</p>
            </li>
          ))}
        </ul>
      )}

      <form className="composer" onSubmit={handleAnswer}>
        <label htmlFor="answer-body" className="visually-hidden">
          답변
        </label>
        <textarea
          id="answer-body"
          className="textarea composer__input"
          rows={3}
          maxLength={ANSWER_MAX}
          placeholder="답변을 남겨 주세요. 연락처는 쓸 수 없어요."
          value={answerBody}
          onChange={(e) => setAnswerBody(e.target.value)}
        />
        <div className="cluster space-between">
          <span className="caption">익명으로 올라가요</span>
          <button type="submit" className="btn btn--primary" disabled={sending || !answerBody.trim()}>
            {sending ? '올리는 중…' : '답변 달기'}
          </button>
        </div>
        {answerError ? (
          <p className="error-text" role="alert">
            {answerError}
          </p>
        ) : null}
        {answerBlocked ? (
          <div className="stack stack--tight" role="alert">
            <p className="error-text">글에 쓸 수 없는 내용이 있어요. 아래 표시된 부분을 지우고 다시 올려 주세요.</p>
            <WarningList warnings={answerBlocked} />
          </div>
        ) : null}
      </form>
    </>
  )
}
