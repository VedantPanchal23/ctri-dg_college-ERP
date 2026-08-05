import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination'
import {
  activateTenant,
  createTenant,
  deleteTenant,
  listTenants,
  provisionUser,
  suspendTenant,
  updateTenant,
} from '../api/endpoints'
import { getErrorMessage } from '../api/client'

const PAGE_SIZE = 10

export default function TenantsPage() {
  const [data, setData] = useState({ content: [] })
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [created, setCreated] = useState(null)
  const [edit, setEdit] = useState(null)
  const [adminForm, setAdminForm] = useState({ username: '', email: '', displayName: '', temporaryPassword: '' })

  async function load() {
    try {
      setError('')
      setData(await listTenants(page, PAGE_SIZE))
    } catch (e) {
      setError(getErrorMessage(e))
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  async function onCreate(e) {
    e.preventDefault()
    setBusy(true)
    setOk('')
    try {
      const tenant = await createTenant({
        code,
        name,
        timezone: 'Asia/Kolkata',
        academicYearStartMonth: 8,
      })
      setCode('')
      setName('')
      setCreated(tenant)
      setOk(`Tenant ${tenant.code} created. Provision a tenant admin below, or set it up manually in Keycloak.`)
      await load()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function run(action, msg) {
    setError('')
    setOk('')
    try {
      await action()
      setOk(msg)
      await load()
    } catch (e) {
      setError(getErrorMessage(e))
    }
  }

  async function onProvisionAdmin(e) {
    e.preventDefault()
    if (!created) return
    setError('')
    setOk('')
    setBusy(true)
    try {
      await provisionUser({
        username: adminForm.username.trim(),
        email: adminForm.email.trim(),
        displayName: adminForm.displayName.trim(),
        temporaryPassword: adminForm.temporaryPassword,
        tenantId: created.id,
        roles: ['TENANT_ADMIN'],
      })
      setAdminForm({ username: '', email: '', displayName: '', temporaryPassword: '' })
      setOk(`Tenant admin provisioned for ${created.name}. They can now sign in with the temporary password.`)
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function copyText(text) {
    navigator.clipboard?.writeText(text).then(
      () => setOk('Copied to clipboard'),
      () => setError('Could not copy'),
    )
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Tenants</h1>
          <p className="muted">Create colleges, then provision a tenant admin in Keycloak with one form.</p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      <form className="panel" onSubmit={onCreate}>
        <h2>Create tenant</h2>
        <div className="row">
          <div className="field" style={{ flex: 1, minWidth: 140 }}>
            <label>Code</label>
            <input value={code} onChange={(e) => setCode(e.target.value)} required />
          </div>
          <div className="field" style={{ flex: 2, minWidth: 180 }}>
            <label>Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <button className="btn" disabled={busy} type="submit">
            Create
          </button>
        </div>
      </form>

      {created ? (
        <div className="stack">
          <form className="panel" onSubmit={onProvisionAdmin}>
            <h2>Provision tenant admin for {created.name}</h2>
            <p className="muted">
              Creates a Keycloak user with the <code>TENANT_ADMIN</code> role scoped to this tenant, plus a local
              account — no manual Keycloak console steps needed.
            </p>
            <div className="row">
              <div className="field" style={{ flex: 1 }}>
                <label>Username</label>
                <input
                  value={adminForm.username}
                  onChange={(e) => setAdminForm({ ...adminForm, username: e.target.value })}
                  required
                />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>Email</label>
                <input
                  type="email"
                  value={adminForm.email}
                  onChange={(e) => setAdminForm({ ...adminForm, email: e.target.value })}
                  required
                />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>Display name</label>
                <input
                  value={adminForm.displayName}
                  onChange={(e) => setAdminForm({ ...adminForm, displayName: e.target.value })}
                  required
                />
              </div>
              <div className="field" style={{ flex: 1 }}>
                <label>Temporary password</label>
                <input
                  value={adminForm.temporaryPassword}
                  onChange={(e) => setAdminForm({ ...adminForm, temporaryPassword: e.target.value })}
                  required
                />
              </div>
              <button className="btn" type="submit" disabled={busy}>
                Provision admin
              </button>
            </div>
          </form>

          <div className="panel">
            <h2>Or onboard manually</h2>
            <div className="row" style={{ alignItems: 'center', marginBottom: '0.75rem' }}>
              <div className="field" style={{ flex: 1 }}>
                <label>Tenant UUID (set as Keycloak user attribute tenant_id)</label>
                <input readOnly value={created.id} />
              </div>
              <button className="btn" type="button" onClick={() => copyText(created.id)}>
                Copy UUID
              </button>
            </div>
            <ol className="detail-list" style={{ paddingLeft: '1.2rem' }}>
              <li>
                Open Keycloak → <a href="http://localhost:8081" target="_blank" rel="noreferrer">localhost:8081</a> (admin /
                admin) → realm <strong>college-admin</strong>.
              </li>
              <li>Users → Add user → set email/username → Credentials → set password.</li>
              <li>
                Attributes → add <code>tenant_id</code> = the UUID above (exact match).
              </li>
              <li>
                Role mapping → assign realm roles (e.g. <code>TENANT_ADMIN</code>, <code>STUDENT</code>).
              </li>
              <li>
                Optional: Users page in this app → Link user with Keycloak <code>sub</code> + roles (or let them auto-link on
                first login).
              </li>
              <li>Sign in as that user at this UI → build Academic → Exams → Placements for the new college.</li>
            </ol>
            <button className="btn btn-ghost" type="button" onClick={() => setCreated(null)}>
              Dismiss
            </button>
          </div>
        </div>
      ) : (
        <div className="panel">
          <h2>How new tenants get users</h2>
          <p className="muted" style={{ marginBottom: 0 }}>
            IDs: Keycloak assigns <code>sub</code>; this app creates a separate local user UUID on first login/link;
            student/faculty profiles get their own UUIDs. Demo users (tenantadmin, student1, …) belong only to the seed
            IIITB tenant.
          </p>
        </div>
      )}

      {edit ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateTenant(edit.id, {
                  name: edit.name,
                  timezone: edit.timezone,
                  academicYearStartMonth: Number(edit.academicYearStartMonth),
                }).then(() => setEdit(null)),
              'Tenant updated',
            )
          }}
        >
          <h2>Edit {edit.code}</h2>
          <div className="row">
            <div className="field" style={{ flex: 2 }}>
              <label>Name</label>
              <input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} required />
            </div>
            <div className="field">
              <label>Timezone</label>
              <input value={edit.timezone} onChange={(e) => setEdit({ ...edit, timezone: e.target.value })} />
            </div>
            <div className="field">
              <label>Year start month</label>
              <input
                type="number"
                min="1"
                max="12"
                value={edit.academicYearStartMonth}
                onChange={(e) => setEdit({ ...edit, academicYearStartMonth: e.target.value })}
              />
            </div>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEdit(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Tenant UUID</th>
              <th>Status</th>
              <th>Timezone</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {(data.content || []).map((t) => (
              <tr key={t.id}>
                <td>{t.code}</td>
                <td>{t.name}</td>
                <td>
                  <button className="btn btn-ghost" type="button" title={t.id} onClick={() => copyText(t.id)}>
                    {String(t.id).slice(0, 8)}… copy
                  </button>
                </td>
                <td>
                  <span className={`badge ${t.status === 'SUSPENDED' ? 'danger' : ''}`}>{t.status}</span>
                </td>
                <td>{t.timezone}</td>
                <td className="row">
                  <button className="btn btn-ghost" type="button" onClick={() => setEdit({ ...t })}>
                    Edit
                  </button>
                  <button className="btn btn-ghost" type="button" onClick={() => setCreated(t)}>
                    Onboard
                  </button>
                  {t.status === 'ACTIVE' ? (
                    <button className="btn btn-ghost" type="button" onClick={() => run(() => suspendTenant(t.id), 'Suspended')}>
                      Suspend
                    </button>
                  ) : (
                    <button className="btn btn-ghost" type="button" onClick={() => run(() => activateTenant(t.id), 'Activated')}>
                      Activate
                    </button>
                  )}
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Soft-delete tenant ${t.code}?`)) {
                        run(() => deleteTenant(t.id), 'Tenant deleted')
                      }
                    }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!data.content?.length ? <div className="empty">No tenants yet. Create the first one above.</div> : null}
      </div>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  )
}
