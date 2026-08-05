import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination'
import { listAuditLogs } from '../api/endpoints'
import { getErrorMessage } from '../api/client'

export default function AuditPage() {
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true)
      setError('')
      try {
        const res = await listAuditLogs(page, 20)
        if (alive) setData(res)
      } catch (e) {
        if (alive) setError(getErrorMessage(e))
      } finally {
        if (alive) setLoading(false)
      }
    })()
    return () => {
      alive = false
    }
  }, [page])

  const rows = data.content || []

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Audit log</h1>
          <p className="muted">Recent administrative and data-changing actions across the system.</p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}

      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>Time</th>
              <th>Actor</th>
              <th>Action</th>
              <th>Entity</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((log) => (
              <tr key={log.id}>
                <td>{formatTime(log.createdAt || log.timestamp)}</td>
                <td>{log.actorEmail || log.actorDisplayName || log.actorUserId || '—'}</td>
                <td>
                  <span className="badge">{log.action}</span>
                </td>
                <td>
                  {log.entityType || '—'}
                  {log.entityId ? <code style={{ marginLeft: '0.35rem' }}>{String(log.entityId).slice(0, 8)}…</code> : null}
                </td>
                <td>{formatDetails(log)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && !rows.length ? <div className="empty">No audit entries yet.</div> : null}
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

function formatDetails(log) {
  const detail = log.details || log.metadata || log.description
  if (!detail) return '—'
  if (typeof detail === 'string') return detail
  try {
    return JSON.stringify(detail)
  } catch {
    return String(detail)
  }
}
