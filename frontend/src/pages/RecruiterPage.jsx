import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ROLES } from '../auth/roles'
import Pagination from '../components/Pagination'
import {
  createRound,
  expireOffer,
  getOfferByApplication,
  issueOffer,
  listApplications,
  listCompanies,
  listDrives,
  listRounds,
  updateApplicationStatus,
  updateRound,
} from '../api/endpoints'
import { getErrorMessage, softGet, listAll } from '../api/client'

const APP_STATUSES = ['APPLIED', 'SHORTLISTED', 'REJECTED', 'SELECTED', 'WITHDRAWN']
const PAGE_SIZE = 20

export default function RecruiterPage() {
  const { can } = useAuth()
  const canIssueOffer = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])

  const [view, setView] = useState('drives')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [driveFilter, setDriveFilter] = useState('')
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [drives, setDrives] = useState([])
  const [applications, setApplications] = useState([])
  const [companyOpts, setCompanyOpts] = useState([])
  const [driveOpts, setDriveOpts] = useState([])
  const [selectedApp, setSelectedApp] = useState('')
  const [rounds, setRounds] = useState([])
  const [offer, setOffer] = useState(null)
  const [offerForm, setOfferForm] = useState({ packageLpa: 12, expiresAt: '2026-12-31T23:59' })
  const [roundForm, setRoundForm] = useState({ roundNumber: 1, roundName: 'Technical', scheduledAt: '' })

  async function load() {
    setError('')
    try {
      const [companies, allDrives] = await Promise.all([
        softGet(listAll(listCompanies)),
        softGet(listAll(listDrives)),
      ])
      setCompanyOpts(companies || [])
      setDriveOpts(allDrives || [])

      if (view === 'drives') {
        const result = await listDrives(page, PAGE_SIZE)
        setDrives(result.content || [])
        setTotalPages(result.totalPages || 1)
      } else {
        const result = (await softGet(listApplications(page, PAGE_SIZE))) || { content: [], totalPages: 1 }
        let apps = result.content || []
        if (driveFilter) apps = apps.filter((a) => a.jobDriveId === driveFilter)
        setApplications(apps)
        setTotalPages(result.totalPages || 1)
      }
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  useEffect(() => {
    setPage(0)
  }, [view, driveFilter])

  useEffect(() => {
    load()
  }, [view, page, driveFilter])

  async function run(action, successMsg) {
    setError('')
    setOk('')
    try {
      await action()
      if (successMsg) setOk(successMsg)
      await load()
    } catch (err) {
      setError(friendlyError(err))
    }
  }

  async function loadAppDetails(id) {
    setSelectedApp(id)
    setOffer(null)
    setRounds([])
    if (!id) return
    try {
      setRounds((await softGet(listRounds(id))) || [])
      setOffer(await softGet(getOfferByApplication(id)))
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  function companyLabel(companyId) {
    const c = companyOpts.find((row) => row.id === companyId)
    return c ? c.name : String(companyId || '').slice(0, 8)
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Recruiter workspace</h1>
          <p className="muted">Review open drives, manage applications, rounds, and offers.</p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      <div className="row">
        <button type="button" className={`btn ${view === 'drives' ? '' : 'btn-ghost'}`} onClick={() => setView('drives')}>
          Open drives
        </button>
        <button type="button" className={`btn ${view === 'applications' ? '' : 'btn-ghost'}`} onClick={() => setView('applications')}>
          Applications
        </button>
      </div>

      {view === 'drives' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Title</th>
                <th>Company</th>
                <th>Role</th>
                <th>Package</th>
                <th>Status</th>
                <th>Deadline</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {drives.map((d) => (
                <tr key={d.id}>
                  <td>{d.title}</td>
                  <td>{companyLabel(d.companyId)}</td>
                  <td>{d.roleName}</td>
                  <td>{d.packageLpa} LPA</td>
                  <td>
                    <span className="badge">{d.status}</span>
                  </td>
                  <td>{formatWhen(d.applicationDeadline)}</td>
                  <td>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        setDriveFilter(d.id)
                        setView('applications')
                      }}
                    >
                      View apps
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!drives.length ? <div className="empty">No open or upcoming drives.</div> : null}
        </div>
      ) : (
        <div className="stack">
          <div className="field">
            <label>Filter by drive</label>
            <select value={driveFilter} onChange={(e) => setDriveFilter(e.target.value)}>
              <option value="">All drives</option>
              {driveOpts.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.title} ({d.status})
                </option>
              ))}
            </select>
          </div>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Drive</th>
                  <th>Student</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {applications.map((a) => (
                  <tr key={a.id}>
                    <td>{labelById(driveOpts, a.jobDriveId, 'title')}</td>
                    <td>
                      <code title={a.studentId}>{String(a.studentId).slice(0, 8)}…</code>
                    </td>
                    <td>
                      <select
                        value={a.status}
                        onChange={(e) => run(() => updateApplicationStatus(a.id, e.target.value), 'Status updated')}
                      >
                        {APP_STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <button className="btn btn-ghost" type="button" onClick={() => loadAppDetails(a.id)}>
                        Rounds / offer
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!applications.length ? <div className="empty">No applications for this filter.</div> : null}
          </div>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      {selectedApp ? (
        <div className="panel stack">
          <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
            <h2>Application details</h2>
            <button className="btn btn-ghost" type="button" onClick={() => setSelectedApp('')}>
              Close
            </button>
          </div>

          <div>
            <h3>Interview rounds</h3>
            {!rounds.length ? <div className="empty">No rounds recorded yet.</div> : null}
            <ul className="detail-list">
              {rounds.map((r) => (
                <li key={r.id} className="row" style={{ alignItems: 'center', listStyle: 'none', marginLeft: '-1.1rem' }}>
                  <span>
                    #{r.roundNumber} {r.roundName}
                  </span>
                  <select
                    value={r.status}
                    onChange={(e) =>
                      run(
                        () =>
                          updateRound(r.id, {
                            roundNumber: r.roundNumber,
                            roundName: r.roundName,
                            status: e.target.value,
                            outcomeNotes: r.outcomeNotes || null,
                            scheduledAt: r.scheduledAt || new Date().toISOString(),
                          }).then(() => loadAppDetails(selectedApp)),
                        'Round updated',
                      )
                    }
                  >
                    <option value="SCHEDULED">SCHEDULED</option>
                    <option value="PASSED">PASSED</option>
                    <option value="FAILED">FAILED</option>
                    <option value="SKIPPED">SKIPPED</option>
                  </select>
                </li>
              ))}
            </ul>
            <form
              className="row"
              style={{ marginTop: '0.75rem', flexWrap: 'wrap' }}
              onSubmit={(e) => {
                e.preventDefault()
                run(
                  () =>
                    createRound(selectedApp, {
                      roundNumber: Number(roundForm.roundNumber),
                      roundName: roundForm.roundName.trim(),
                      status: 'SCHEDULED',
                      outcomeNotes: null,
                      scheduledAt: roundForm.scheduledAt
                        ? new Date(roundForm.scheduledAt).toISOString()
                        : new Date().toISOString(),
                    }).then(() => {
                      setRoundForm({
                        roundNumber: Number(roundForm.roundNumber) + 1,
                        roundName: 'HR',
                        scheduledAt: '',
                      })
                      return loadAppDetails(selectedApp)
                    }),
                  'Round created',
                )
              }}
            >
              <Field label="Round #">
                <input
                  type="number"
                  min="1"
                  value={roundForm.roundNumber}
                  onChange={(e) => setRoundForm({ ...roundForm, roundNumber: e.target.value })}
                  required
                />
              </Field>
              <Field label="Name" grow>
                <input
                  value={roundForm.roundName}
                  onChange={(e) => setRoundForm({ ...roundForm, roundName: e.target.value })}
                  required
                />
              </Field>
              <Field label="Scheduled">
                <input
                  type="datetime-local"
                  value={roundForm.scheduledAt}
                  onChange={(e) => setRoundForm({ ...roundForm, scheduledAt: e.target.value })}
                />
              </Field>
              <button className="btn" type="submit">
                Add round
              </button>
            </form>
          </div>

          <div>
            <h3>Offer</h3>
            {offer ? (
              <div className="row" style={{ alignItems: 'center' }}>
                <span>
                  {offer.packageLpa} LPA · <span className="badge">{offer.status}</span>
                </span>
                {offer.status === 'OFFERED' ? (
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => run(() => expireOffer(offer.id).then(() => loadAppDetails(selectedApp)), 'Offer expired')}
                  >
                    Expire
                  </button>
                ) : null}
              </div>
            ) : canIssueOffer ? (
              <form
                onSubmit={(e) => {
                  e.preventDefault()
                  run(
                    () =>
                      issueOffer(selectedApp, {
                        packageLpa: Number(offerForm.packageLpa),
                        expiresAt: new Date(offerForm.expiresAt).toISOString(),
                      }).then(() => loadAppDetails(selectedApp)),
                    'Offer issued',
                  )
                }}
              >
                <div className="row">
                  <Field label="Package LPA">
                    <input
                      type="number"
                      step="0.1"
                      value={offerForm.packageLpa}
                      onChange={(e) => setOfferForm({ ...offerForm, packageLpa: e.target.value })}
                    />
                  </Field>
                  <Field label="Expires">
                    <input
                      type="datetime-local"
                      value={offerForm.expiresAt}
                      onChange={(e) => setOfferForm({ ...offerForm, expiresAt: e.target.value })}
                    />
                  </Field>
                  <button className="btn" type="submit">
                    Issue offer
                  </button>
                </div>
              </form>
            ) : (
              <div className="empty">No offer yet.</div>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}

function Field({ label, children, grow }) {
  return (
    <div className="field" style={grow ? { flex: 1 } : undefined}>
      <label>{label}</label>
      {children}
    </div>
  )
}

function labelById(rows, id, field) {
  const row = (rows || []).find((r) => r.id === id)
  return row ? row[field] : String(id || '').slice(0, 8)
}

function formatWhen(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

function friendlyError(e) {
  if (e?.code === 'ERR_NETWORK' || getErrorMessage(e).includes('Network Error')) {
    return 'API unreachable. Ensure backend is running.'
  }
  return getErrorMessage(e)
}
