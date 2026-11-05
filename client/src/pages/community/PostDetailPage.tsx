import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { companyPath, postEditPath, ROUTES } from '../../routes'
import { blockedWarnings, communityApi, isEdited } from '../../shared/api/community/communityService'
import type { Answer, PostDetail } from '../../shared/api/community/communityService'
import type { ReportTargetCode } from '../../shared/api/moderation/reportService'
import type { WarningItem } from '../../shared/api/warning/warningService'
import { ApiError, errorMessage } from '../../shared/core/apiClient'
import { AuthorBadge } from '../../shared/ui/AuthorBadge'
import { BuildingIcon, ChatIcon, ChevronLeftIcon, ChevronRightIcon, ExclamationIcon, FlagIcon } from '../../shared/ui/icons/Icons'
import { ConfirmDialog } from '../../shared/ui/Modal'
import { ReportSheet } from '../../shared/ui/ReportSheet'
import { StatusPill } from '../../shared/ui/StatusPill'
import { WarningList } from '../../shared/ui/WarningList'
import { formatRelative } from '../../shared/utils/datetime'

const ANSWER_MAX = 3000

type Target = { type: ReportTargetCode; id: number }

/** 서버 오류를 화면에 보여 줄 형태로: 저장 불가 신호 목록 또는 문장 하나 */
type SaveError = { blocked: WarningItem[] } | { message: string }

function toSaveError(e: unknown, fallback: string): SaveError {
  const blocked = blockedWarnings(e)
  if (blocked) return { blocked }
  const fieldMessage = e instanceof ApiError ? e.fieldErrors[0]?.message : undefined
  return { message: fieldMessage ?? errorMessage(e, fallback) }
}

function SaveErrorView({ error }: { error: SaveError | null }) {
  if (!error) return null
  if ('message' in error) {
    return (
      <p className="error-text" role="alert">
        {error.message}
      </p>
    )
  }
  return (
    <div className="stack stack--tight" role="alert">
      <p className="error-text">글에 쓸 수 없는 내용이 있어요. 아래 표시된 부분을 지우고 다시 올려 주세요.</p>
      <WarningList warnings={error.blocked} />
    </div>
  )
}

/**
 * SCR-03 글 상세 + 답변 (FR-CM-002·003, FR-MD-001).
 * 내 글·답변에는 고치기·지우기, 남의 글·답변에는 신고하기를 보여 준다.
 */
export default function PostDetailPage() {
  const { postId } = useParams()
  const navigate = useNavigate()
  const id = Number(postId)
  const validId = Number.isInteger(id) && id > 0

  const [post, setPost] = useState<PostDetail | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const error = validId ? loadError : '글을 찾을 수 없습니다.'

  const [answerBody, setAnswerBody] = useState('')
  const [answerError, setAnswerError] = useState<SaveError | null>(null)
  const [sending, setSending] = useState(false)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editBody, setEditBody] = useState('')
  const [editError, setEditError] = useState<SaveError | null>(null)

  const [confirm, setConfirm] = useState<Target | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [reporting, setReporting] = useState<Target | null>(null)

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

  function replaceAnswer(next: Answer) {
    setPost((current) =>
      current ? { ...current, answers: current.answers.map((a) => (a.answerId === next.answerId ? next : a)) } : current,
    )
  }

  async function handleAnswer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!post) return
    setSending(true)
    setAnswerError(null)
    try {
      const answer = await communityApi.answer(post.postId, answerBody)
      setPost({ ...post, answers: [...post.answers, answer] })
      setAnswerBody('')
    } catch (e) {
      setAnswerError(toSaveError(e, '답변을 올리지 못했습니다.'))
    } finally {
      setSending(false)
    }
  }

  function startEdit(answer: Answer) {
    setEditingId(answer.answerId)
    setEditBody(answer.body)
    setEditError(null)
  }

  async function saveEdit(answerId: number) {
    if (!post) return
    setEditError(null)
    try {
      replaceAnswer(await communityApi.editAnswer(post.postId, answerId, editBody))
      setEditingId(null)
    } catch (e) {
      setEditError(toSaveError(e, '답변을 고치지 못했습니다.'))
    }
  }

  async function confirmDelete() {
    if (!post || !confirm) return
    setDeleting(true)
    try {
      if (confirm.type === 'POST') {
        await communityApi.remove(post.postId)
        navigate(ROUTES.SCR02_POSTS, { replace: true })
        return
      }
      await communityApi.removeAnswer(post.postId, confirm.id)
      setPost({ ...post, answers: post.answers.filter((a) => a.answerId !== confirm.id) })
      setConfirm(null)
    } catch (e) {
      setConfirm(null)
      setLoadError(errorMessage(e, '지우지 못했습니다.'))
    } finally {
      setDeleting(false)
    }
  }

  function markReported(target: Target) {
    setPost((current) => {
      if (!current) return current
      if (target.type === 'POST') return { ...current, reportedByMe: true }
      return { ...current, answers: current.answers.map((a) => (a.answerId === target.id ? { ...a, reportedByMe: true } : a)) }
    })
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
        <div className="post__meta-row">
          <div className="post-row__meta">
            <AuthorBadge role={post.authorRole} />
            <span>{formatRelative(post.createdAt)}</span>
            {isEdited(post) ? <span>· 수정됨</span> : null}
            {post.mine ? <span>· 내가 쓴 글</span> : null}
          </div>
          <div className="actions">
            {post.mine ? (
              <>
                <Link to={postEditPath(post.postId)} className="action">
                  수정
                </Link>
                <button type="button" className="action action--destructive" onClick={() => setConfirm({ type: 'POST', id: post.postId })}>
                  삭제
                </button>
              </>
            ) : post.reportedByMe ? (
              <span className="action action--done">
                <FlagIcon size={14} /> 신고함
              </span>
            ) : (
              <button type="button" className="action" onClick={() => setReporting({ type: 'POST', id: post.postId })}>
                <FlagIcon size={14} /> 신고
              </button>
            )}
          </div>
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
              <div className="post__meta-row">
                <div className="post-row__meta">
                  <AuthorBadge role={answer.authorRole} isPostAuthor={answer.byPostAuthor} />
                  <span>{formatRelative(answer.createdAt)}</span>
                  {isEdited(answer) ? <span>· 수정됨</span> : null}
                  {answer.warnings.length > 0 ? (
                    <span className="pill pill--plain pill--orange">
                      <ExclamationIcon size={12} /> 주의 신호 {answer.warnings.length}
                    </span>
                  ) : null}
                </div>
                {editingId === answer.answerId ? null : (
                  <div className="actions">
                    {answer.mine ? (
                      <>
                        <button type="button" className="action" onClick={() => startEdit(answer)}>
                          수정
                        </button>
                        <button
                          type="button"
                          className="action action--destructive"
                          onClick={() => setConfirm({ type: 'ANSWER', id: answer.answerId })}
                        >
                          삭제
                        </button>
                      </>
                    ) : answer.reportedByMe ? (
                      <span className="action action--done">
                        <FlagIcon size={14} /> 신고함
                      </span>
                    ) : (
                      <button type="button" className="action" onClick={() => setReporting({ type: 'ANSWER', id: answer.answerId })}>
                        <FlagIcon size={14} /> 신고
                      </button>
                    )}
                  </div>
                )}
              </div>

              {editingId === answer.answerId ? (
                <div className="stack stack--tight">
                  <label htmlFor={`edit-${answer.answerId}`} className="visually-hidden">
                    답변 고치기
                  </label>
                  <textarea
                    id={`edit-${answer.answerId}`}
                    className="textarea composer__input"
                    maxLength={ANSWER_MAX}
                    value={editBody}
                    onChange={(e) => setEditBody(e.target.value)}
                  />
                  <SaveErrorView error={editError} />
                  <div className="cluster answer__edit-actions">
                    <button type="button" className="btn btn--secondary btn--small" onClick={() => setEditingId(null)}>
                      취소
                    </button>
                    <button
                      type="button"
                      className="btn btn--primary btn--small"
                      disabled={!editBody.trim()}
                      onClick={() => void saveEdit(answer.answerId)}
                    >
                      저장
                    </button>
                  </div>
                </div>
              ) : (
                <p className="answer__body">{answer.body}</p>
              )}
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
        <SaveErrorView error={answerError} />
      </form>

      <ConfirmDialog
        open={confirm !== null}
        title={confirm?.type === 'POST' ? '이 글을 삭제할까요?' : '이 답변을 삭제할까요?'}
        message={
          confirm?.type === 'POST'
            ? '삭제한 글은 되돌릴 수 없어요. 달린 답변도 함께 보이지 않게 돼요.'
            : '삭제한 답변은 되돌릴 수 없어요.'
        }
        confirmLabel="삭제"
        busy={deleting}
        onConfirm={() => void confirmDelete()}
        onCancel={() => setConfirm(null)}
      />

      {reporting ? (
        <ReportSheet
          key={`${reporting.type}-${reporting.id}`}
          open
          target={reporting}
          onClose={() => setReporting(null)}
          onReported={() => markReported(reporting)}
        />
      ) : null}
    </>
  )
}
