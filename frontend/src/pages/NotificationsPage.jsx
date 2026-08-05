import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination'
import { listNotifications, markNotificationRead } from '../api/endpoints'
import { getErrorMessage } from '../api/client'

export default function NotificationsPage() {
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [loading, setLoading] = useState(true)

  async function load() {
    setError('')
    setLoading(true)
    try {
      setData(await listNotifications(page, 20))
    } catch (e) {
      setError(getErrorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function onMarkRead(id) {
    setError('')
    setOk('')
    try {
      await markNotificationRead(id)
      setOk('Marked as read')
      await load()
    } catch (e) {
      setError(getErrorMessage(e))
    }
  }

  const rows = data.content || []

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Notifications</h1>
          <p className="muted">Updates relevant to your account and role.</p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      <div className="stack">
        {rows.map((n) => {
          const isRead = Boolean(n.readAt)
          return (
            <div key={n.id} className="panel" style={{ opacity: isRead ? 0.65 : 1 }}>
              <div className="row" style={{ justifyContent: 'space-between', alignItems: 'start' }}>
                <div>
                  <div className="row" style={{ gap: '0.5rem', marginBottom: '0.3rem' }}>
                    <strong>{n.title || n.subject || 'Notification'}</strong>
                    {isRead ? null : <span className="badge warn">New</span>}
                  </div>
                  <p style={{ marginBottom: '0.25rem' }}>{n.message || n.body || ''}</p>
                  <span className="muted" style={{ fontSize: '0.8rem' }}>
                    {formatTime(n.createdAt)}
                  </span>
                </div>
                {!isRead ? (
                  <button className="btn btn-ghost" type="button" onClick={() => onMarkRead(n.id)}>
                    Mark read
                  </button>
                ) : null}
              </div>
            </div>
          )
        })}
        {!loading && !rows.length ? <div className="empty">No notifications yet.</div> : null}
        {loading ? <div className="empty">Loading…</div> : null}
      </div>

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  )
}

function formatTime(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}
