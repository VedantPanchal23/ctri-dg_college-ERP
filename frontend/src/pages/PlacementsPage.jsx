import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ROLES } from '../auth/roles'
import Pagination from '../components/Pagination'
import {
  acceptOffer,
  applyToDrive,
  checkEligibility,
  closeDrive,
  createCompany,
  createDrive,
  createRound,
  declineOffer,
  deleteCompany,
  deleteDrive,
  expireOffer,
  getOfferByApplication,
  issueOffer,
  listApplications,
  listBatches,
  listBranches,
  listCompanies,
  listDrives,
  listRounds,
  openDrive,
  placementStats,
  updateApplicationStatus,
  updateCompany,
  updateDrive,
  updateRound,
} from '../api/endpoints'
import { getErrorMessage, softGet, listAll } from '../api/client'

const APP_STATUSES = ['APPLIED', 'SHORTLISTED', 'REJECTED', 'SELECTED', 'WITHDRAWN']
const PAGE_SIZE = 20

export default function PlacementsPage() {
  const { can } = useAuth()
  const isStudent = can([ROLES.STUDENT]) && !can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN, ROLES.RECRUITER])
  const canManage = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])
  const canRecruiter = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN, ROLES.RECRUITER])
  const canStats = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])
  const canCompanies = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN, ROLES.RECRUITER])

  const [tab, setTab] = useState(isStudent ? 'drives' : 'companies')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [companies, setCompanies] = useState([])
  const [drives, setDrives] = useState([])
  const [applications, setApplications] = useState([])
  const [companyOpts, setCompanyOpts] = useState([])
  const [driveOpts, setDriveOpts] = useState([])
  const [branches, setBranches] = useState([])
  const [batches, setBatches] = useState([])
  const [stats, setStats] = useState(null)
  const [eligibility, setEligibility] = useState(null)
  const [selectedApp, setSelectedApp] = useState('')
  const [rounds, setRounds] = useState([])
  const [offer, setOffer] = useState(null)
  const [companyForm, setCompanyForm] = useState({ name: '', code: '', contactEmail: '', website: '' })
  const [driveForm, setDriveForm] = useState({
    companyId: '',
    title: '',
    roleName: '',
    packageLpa: 12,
    locations: 'Bengaluru',
    applicationDeadline: '2026-12-31T23:59',
    minCgpa: 7,
    maxBacklogs: 0,
    graduationYear: 2028,
    allowedBranchIds: [],
    allowedBatchIds: [],
  })
  const [roundForm, setRoundForm] = useState({ roundNumber: 1, roundName: 'Technical', status: 'SCHEDULED', outcomeNotes: '' })
  const [offerForm, setOfferForm] = useState({ packageLpa: 12, expiresAt: '2026-12-31T23:59' })
  const [editCompany, setEditCompany] = useState(null)
  const [editDrive, setEditDrive] = useState(null)

  async function load() {
    setError('')
    try {
      if (canCompanies) {
        setCompanyOpts((await softGet(listAll(listCompanies))) || [])
      }
      setDriveOpts((await softGet(listAll(listDrives))) || [])

      if (canManage) {
        setBranches((await softGet(listAll(listBranches))) || [])
        setBatches((await softGet(listAll(listBatches))) || [])
      }
      if (canStats) setStats(await placementStats())

      let result = { content: [], totalPages: 1 }
      if (tab === 'companies' && canCompanies) result = await listCompanies(page, PAGE_SIZE)
      else if (tab === 'drives') result = await listDrives(page, PAGE_SIZE)
      else if (tab === 'applications') result = (await softGet(listApplications(page, PAGE_SIZE))) || { content: [], totalPages: 1 }

      if (tab === 'companies') setCompanies(result.content || [])
      if (tab === 'drives') setDrives(result.content || [])
      if (tab === 'applications') setApplications(result.content || [])
      setTotalPages(['companies', 'drives', 'applications'].includes(tab) ? result.totalPages || 1 : 1)
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  useEffect(() => {
    setPage(0)
  }, [tab])

  useEffect(() => {
    load()
  }, [tab, page])

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

  function toggleId(list, id) {
    return list.includes(id) ? list.filter((x) => x !== id) : [...list, id]
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Placements</h1>
          <p className="muted">
            {isStudent
              ? 'Drives, applications, and offers.'
              : 'Companies, drives, applications, rounds, and offers.'}
          </p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}
      {eligibility ? (
        <div className={`alert ${eligibility.eligible ? 'ok' : ''}`}>
          {eligibility.eligible
            ? 'Eligible for this drive.'
            : `Not eligible: ${(eligibility.reasons || []).join(', ') || 'criteria not met'}`}
        </div>
      ) : null}

      <div className="row">
        {canCompanies ? (
          <button type="button" className={`btn ${tab === 'companies' ? '' : 'btn-ghost'}`} onClick={() => setTab('companies')}>
            Companies
          </button>
        ) : null}
        <button type="button" className={`btn ${tab === 'drives' ? '' : 'btn-ghost'}`} onClick={() => setTab('drives')}>
          Drives
        </button>
        <button type="button" className={`btn ${tab === 'applications' ? '' : 'btn-ghost'}`} onClick={() => setTab('applications')}>
          Applications
        </button>
        {canStats ? (
          <button type="button" className={`btn ${tab === 'stats' ? '' : 'btn-ghost'}`} onClick={() => setTab('stats')}>
            Stats
          </button>
        ) : null}
      </div>

      {canManage && tab === 'companies' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () => createCompany(companyForm).then(() => setCompanyForm({ name: '', code: '', contactEmail: '', website: '' })),
              'Company created',
            )
          }}
        >
          <h2>Register company</h2>
          <div className="row">
            <Field label="Code">
              <input value={companyForm.code} onChange={(e) => setCompanyForm({ ...companyForm, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={companyForm.name} onChange={(e) => setCompanyForm({ ...companyForm, name: e.target.value })} required />
            </Field>
            <Field label="Email" grow>
              <input type="email" value={companyForm.contactEmail} onChange={(e) => setCompanyForm({ ...companyForm, contactEmail: e.target.value })} required />
            </Field>
            <Field label="Website" grow>
              <input value={companyForm.website} onChange={(e) => setCompanyForm({ ...companyForm, website: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'companies' && editCompany ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () => updateCompany(editCompany.id, editCompany).then(() => setEditCompany(null)),
              'Company updated',
            )
          }}
        >
          <h2>Edit company {editCompany.code}</h2>
          <div className="row">
            <Field label="Code">
              <input value={editCompany.code} onChange={(e) => setEditCompany({ ...editCompany, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={editCompany.name} onChange={(e) => setEditCompany({ ...editCompany, name: e.target.value })} required />
            </Field>
            <Field label="Email" grow>
              <input
                type="email"
                value={editCompany.contactEmail}
                onChange={(e) => setEditCompany({ ...editCompany, contactEmail: e.target.value })}
                required
              />
            </Field>
            <Field label="Website" grow>
              <input value={editCompany.website || ''} onChange={(e) => setEditCompany({ ...editCompany, website: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditCompany(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'drives' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createDrive({
                  companyId: driveForm.companyId,
                  title: driveForm.title,
                  roleName: driveForm.roleName,
                  packageLpa: Number(driveForm.packageLpa),
                  locations: driveForm.locations,
                  applicationDeadline: new Date(driveForm.applicationDeadline).toISOString(),
                  minCgpa: Number(driveForm.minCgpa),
                  maxBacklogs: Number(driveForm.maxBacklogs),
                  graduationYear: Number(driveForm.graduationYear) || null,
                  allowedBranchIds: driveForm.allowedBranchIds,
                  allowedBatchIds: driveForm.allowedBatchIds,
                }),
              'Drive created as DRAFT',
            )
          }}
        >
          <h2>New job drive</h2>
          <div className="row">
            <Field label="Company" grow>
              <select value={driveForm.companyId} onChange={(e) => setDriveForm({ ...driveForm, companyId: e.target.value })} required>
                <option value="">Select company</option>
                {companyOpts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} — {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Title" grow>
              <input value={driveForm.title} onChange={(e) => setDriveForm({ ...driveForm, title: e.target.value })} required />
            </Field>
            <Field label="Role" grow>
              <input value={driveForm.roleName} onChange={(e) => setDriveForm({ ...driveForm, roleName: e.target.value })} required />
            </Field>
            <Field label="Package LPA">
              <input type="number" step="0.1" value={driveForm.packageLpa} onChange={(e) => setDriveForm({ ...driveForm, packageLpa: e.target.value })} />
            </Field>
            <Field label="Locations">
              <input value={driveForm.locations} onChange={(e) => setDriveForm({ ...driveForm, locations: e.target.value })} />
            </Field>
            <Field label="Deadline">
              <input type="datetime-local" value={driveForm.applicationDeadline} onChange={(e) => setDriveForm({ ...driveForm, applicationDeadline: e.target.value })} required />
            </Field>
            <Field label="Min CGPA">
              <input type="number" step="0.01" value={driveForm.minCgpa} onChange={(e) => setDriveForm({ ...driveForm, minCgpa: e.target.value })} />
            </Field>
            <Field label="Max backlogs">
              <input type="number" min="0" value={driveForm.maxBacklogs} onChange={(e) => setDriveForm({ ...driveForm, maxBacklogs: e.target.value })} />
            </Field>
            <Field label="Grad year">
              <input type="number" value={driveForm.graduationYear} onChange={(e) => setDriveForm({ ...driveForm, graduationYear: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Create
            </button>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <div className="muted">Allowed branches (empty = all)</div>
            <div className="row" style={{ flexWrap: 'wrap', marginTop: '0.35rem' }}>
              {branches.map((b) => (
                <label key={b.id} className="row" style={{ gap: '0.3rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={driveForm.allowedBranchIds.includes(b.id)}
                    onChange={() =>
                      setDriveForm({ ...driveForm, allowedBranchIds: toggleId(driveForm.allowedBranchIds, b.id) })
                    }
                  />
                  {b.code}
                </label>
              ))}
            </div>
            <div className="muted" style={{ marginTop: '0.5rem' }}>
              Allowed batches (empty = all)
            </div>
            <div className="row" style={{ flexWrap: 'wrap', marginTop: '0.35rem' }}>
              {batches.map((b) => (
                <label key={b.id} className="row" style={{ gap: '0.3rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={driveForm.allowedBatchIds.includes(b.id)}
                    onChange={() =>
                      setDriveForm({ ...driveForm, allowedBatchIds: toggleId(driveForm.allowedBatchIds, b.id) })
                    }
                  />
                  {b.code}
                </label>
              ))}
            </div>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'drives' && editDrive ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateDrive(editDrive.id, {
                  companyId: editDrive.companyId,
                  title: editDrive.title,
                  roleName: editDrive.roleName,
                  packageLpa: Number(editDrive.packageLpa),
                  locations: editDrive.locations,
                  applicationDeadline: new Date(editDrive.applicationDeadline).toISOString(),
                  minCgpa: Number(editDrive.minCgpa),
                  maxBacklogs: Number(editDrive.maxBacklogs),
                  graduationYear: Number(editDrive.graduationYear) || null,
                  allowedBranchIds: editDrive.allowedBranchIds,
                  allowedBatchIds: editDrive.allowedBatchIds,
                }).then(() => setEditDrive(null)),
              'Drive updated',
            )
          }}
        >
          <h2>Edit drive {editDrive.title}</h2>
          <div className="row">
            <Field label="Company" grow>
              <select value={editDrive.companyId} onChange={(e) => setEditDrive({ ...editDrive, companyId: e.target.value })} required>
                <option value="">Select company</option>
                {companyOpts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} — {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Title" grow>
              <input value={editDrive.title} onChange={(e) => setEditDrive({ ...editDrive, title: e.target.value })} required />
            </Field>
            <Field label="Role" grow>
              <input value={editDrive.roleName} onChange={(e) => setEditDrive({ ...editDrive, roleName: e.target.value })} required />
            </Field>
            <Field label="Package LPA">
              <input type="number" step="0.1" value={editDrive.packageLpa} onChange={(e) => setEditDrive({ ...editDrive, packageLpa: e.target.value })} />
            </Field>
            <Field label="Locations">
              <input value={editDrive.locations} onChange={(e) => setEditDrive({ ...editDrive, locations: e.target.value })} />
            </Field>
            <Field label="Deadline">
              <input
                type="datetime-local"
                value={editDrive.applicationDeadline}
                onChange={(e) => setEditDrive({ ...editDrive, applicationDeadline: e.target.value })}
                required
              />
            </Field>
            <Field label="Min CGPA">
              <input type="number" step="0.01" value={editDrive.minCgpa} onChange={(e) => setEditDrive({ ...editDrive, minCgpa: e.target.value })} />
            </Field>
            <Field label="Max backlogs">
              <input type="number" min="0" value={editDrive.maxBacklogs} onChange={(e) => setEditDrive({ ...editDrive, maxBacklogs: e.target.value })} />
            </Field>
            <Field label="Grad year">
              <input type="number" value={editDrive.graduationYear} onChange={(e) => setEditDrive({ ...editDrive, graduationYear: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditDrive(null)}>
              Cancel
            </button>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <div className="muted">Allowed branches (empty = all)</div>
            <div className="row" style={{ flexWrap: 'wrap', marginTop: '0.35rem' }}>
              {branches.map((b) => (
                <label key={b.id} className="row" style={{ gap: '0.3rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={editDrive.allowedBranchIds.includes(b.id)}
                    onChange={() =>
                      setEditDrive({ ...editDrive, allowedBranchIds: toggleId(editDrive.allowedBranchIds, b.id) })
                    }
                  />
                  {b.code}
                </label>
              ))}
            </div>
            <div className="muted" style={{ marginTop: '0.5rem' }}>
              Allowed batches (empty = all)
            </div>
            <div className="row" style={{ flexWrap: 'wrap', marginTop: '0.35rem' }}>
              {batches.map((b) => (
                <label key={b.id} className="row" style={{ gap: '0.3rem', alignItems: 'center' }}>
                  <input
                    type="checkbox"
                    checked={editDrive.allowedBatchIds.includes(b.id)}
                    onChange={() =>
                      setEditDrive({ ...editDrive, allowedBatchIds: toggleId(editDrive.allowedBatchIds, b.id) })
                    }
                  />
                  {b.code}
                </label>
              ))}
            </div>
          </div>
        </form>
      ) : null}

      {tab === 'companies' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Email</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {companies.map((c) => (
                <tr key={c.id}>
                  <td>{c.code}</td>
                  <td>{c.name}</td>
                  <td>{c.contactEmail}</td>
                  <td>
                    <span className="badge">{c.status}</span>
                  </td>
                  <td>
                    {canManage ? (
                      <div className="row">
                        <button className="btn btn-ghost" type="button" onClick={() => setEditCompany({ ...c })}>
                          Edit
                        </button>
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => {
                            if (window.confirm(`Delete ${c.code}?`)) run(() => deleteCompany(c.id), 'Company deleted')
                          }}
                        >
                          Delete
                        </button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!companies.length ? <div className="empty">No companies yet.</div> : null}
        </div>
      ) : null}
      {tab === 'companies' ? <Pagination page={page} totalPages={totalPages} onChange={setPage} /> : null}

      {tab === 'drives' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Title</th>
                <th>Role</th>
                <th>Package</th>
                <th>Status</th>
                <th>Min CGPA</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {drives.map((d) => (
                <tr key={d.id}>
                  <td>{d.title}</td>
                  <td>{d.roleName}</td>
                  <td>{d.packageLpa} LPA</td>
                  <td>
                    <span className="badge">{d.status}</span>
                  </td>
                  <td>{d.minCgpa}</td>
                  <td className="row">
                    {canManage ? (
                      <button
                        className="btn btn-ghost"
                        type="button"
                        onClick={() =>
                          setEditDrive({ ...d, applicationDeadline: toDatetimeLocal(d.applicationDeadline) })
                        }
                      >
                        Edit
                      </button>
                    ) : null}
                    {canManage && d.status === 'DRAFT' ? (
                      <button className="btn btn-ghost" type="button" onClick={() => run(() => openDrive(d.id), 'Drive opened')}>
                        Open
                      </button>
                    ) : null}
                    {canManage && d.status === 'OPEN' ? (
                      <button className="btn btn-ghost" type="button" onClick={() => run(() => closeDrive(d.id), 'Drive closed')}>
                        Close
                      </button>
                    ) : null}
                    {canManage ? (
                      <button
                        className="btn btn-ghost"
                        type="button"
                        onClick={() => {
                          if (window.confirm('Delete drive?')) run(() => deleteDrive(d.id), 'Drive deleted')
                        }}
                      >
                        Delete
                      </button>
                    ) : null}
                    {isStudent && d.status === 'OPEN' ? (
                      <>
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() =>
                            checkEligibility(d.id)
                              .then((r) => {
                                setEligibility(r)
                                setOk('Eligibility checked')
                              })
                              .catch((e) => setError(friendlyError(e)))
                          }
                        >
                          Check
                        </button>
                        <button className="btn" type="button" onClick={() => run(() => applyToDrive(d.id), 'Applied')}>
                          Apply
                        </button>
                      </>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {!drives.length ? <div className="empty">No drives yet.</div> : null}
        </div>
      ) : null}
      {tab === 'drives' ? <Pagination page={page} totalPages={totalPages} onChange={setPage} /> : null}

      {tab === 'applications' ? (
        <div className="stack">
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
                      {canRecruiter ? (
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
                      ) : (
                        <span className="badge">{a.status}</span>
                      )}
                    </td>
                    <td>
                      <button className="btn btn-ghost" type="button" onClick={() => loadAppDetails(a.id)}>
                        Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!applications.length ? (
              <div className="empty">
                {isStudent ? 'No applications yet. Apply from Drives after linking a student profile.' : 'No applications yet.'}
              </div>
            ) : null}
          </div>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />

          {selectedApp ? (
            <div className="panel stack">
              <h2>Application details</h2>
              {canRecruiter ? (
                <>
                  <form
                    onSubmit={(e) => {
                      e.preventDefault()
                      run(
                        () =>
                          createRound(selectedApp, {
                            roundNumber: Number(roundForm.roundNumber),
                            roundName: roundForm.roundName,
                            status: roundForm.status,
                            outcomeNotes: roundForm.outcomeNotes || null,
                            scheduledAt: new Date().toISOString(),
                          }).then(() => loadAppDetails(selectedApp)),
                        'Round added',
                      )
                    }}
                  >
                    <h3>Add interview round</h3>
                    <div className="row">
                      <Field label="#">
                        <input type="number" min="1" value={roundForm.roundNumber} onChange={(e) => setRoundForm({ ...roundForm, roundNumber: e.target.value })} />
                      </Field>
                      <Field label="Name" grow>
                        <input value={roundForm.roundName} onChange={(e) => setRoundForm({ ...roundForm, roundName: e.target.value })} required />
                      </Field>
                      <Field label="Status">
                        <select value={roundForm.status} onChange={(e) => setRoundForm({ ...roundForm, status: e.target.value })}>
                          <option value="SCHEDULED">SCHEDULED</option>
                          <option value="PASSED">PASSED</option>
                          <option value="FAILED">FAILED</option>
                          <option value="SKIPPED">SKIPPED</option>
                        </select>
                      </Field>
                      <button className="btn" type="submit">
                        Add round
                      </button>
                    </div>
                  </form>
                  {!offer ? (
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
                      <h3>Issue offer</h3>
                      <div className="row">
                        <Field label="Package LPA">
                          <input type="number" step="0.1" value={offerForm.packageLpa} onChange={(e) => setOfferForm({ ...offerForm, packageLpa: e.target.value })} />
                        </Field>
                        <Field label="Expires">
                          <input type="datetime-local" value={offerForm.expiresAt} onChange={(e) => setOfferForm({ ...offerForm, expiresAt: e.target.value })} />
                        </Field>
                        <button className="btn" type="submit">
                          Issue
                        </button>
                      </div>
                    </form>
                  ) : null}
                </>
              ) : null}

              <div>
                <h3>Rounds</h3>
                {!rounds.length ? <div className="empty">No rounds yet.</div> : null}
                <ul className="detail-list">
                  {rounds.map((r) => (
                    <li key={r.id} className="row" style={{ alignItems: 'center', listStyle: 'none', marginLeft: '-1.1rem' }}>
                      <span>
                        #{r.roundNumber} {r.roundName}
                      </span>
                      {canRecruiter ? (
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
                      ) : (
                        <span className="badge">{r.status}</span>
                      )}
                    </li>
                  ))}
                </ul>
              </div>

              <div>
                <h3>Offer</h3>
                {offer ? (
                  <div className="row" style={{ alignItems: 'center' }}>
                    <span>
                      {offer.packageLpa} LPA · <span className="badge">{offer.status}</span>
                    </span>
                    {isStudent && offer.status === 'OFFERED' ? (
                      <>
                        <button className="btn" type="button" onClick={() => run(() => acceptOffer(offer.id).then(() => loadAppDetails(selectedApp)), 'Offer accepted')}>
                          Accept
                        </button>
                        <button className="btn btn-ghost" type="button" onClick={() => run(() => declineOffer(offer.id).then(() => loadAppDetails(selectedApp)), 'Offer declined')}>
                          Decline
                        </button>
                      </>
                    ) : null}
                    {canRecruiter && offer.status === 'OFFERED' ? (
                      <button className="btn btn-ghost" type="button" onClick={() => run(() => expireOffer(offer.id).then(() => loadAppDetails(selectedApp)), 'Offer expired')}>
                        Expire
                      </button>
                    ) : null}
                  </div>
                ) : (
                  <div className="empty">No offer yet.</div>
                )}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      {tab === 'stats' && stats ? (
        <div className="panel">
          <h2>Placement stats</h2>
          <div className="row" style={{ gap: '1.5rem' }}>
            <div>
              <div className="muted">Drives</div>
              <strong>{stats.totalDrives}</strong>
            </div>
            <div>
              <div className="muted">Applications</div>
              <strong>{stats.totalApplications}</strong>
            </div>
            <div>
              <div className="muted">Placed</div>
              <strong>{stats.placedCount}</strong>
            </div>
            <div>
              <div className="muted">Avg package</div>
              <strong>{stats.averageAcceptedPackage ?? '—'} LPA</strong>
            </div>
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

function toDatetimeLocal(value) {
  if (!value) return ''
  try {
    return new Date(value).toISOString().slice(0, 16)
  } catch {
    return ''
  }
}

function friendlyError(e) {
  if (e?.code === 'ERR_NETWORK' || getErrorMessage(e).includes('Network Error')) {
    return 'API unreachable. Ensure backend is running.'
  }
  return getErrorMessage(e)
}
