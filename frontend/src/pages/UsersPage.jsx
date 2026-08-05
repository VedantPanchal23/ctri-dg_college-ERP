import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ROLES } from '../auth/roles'
import Pagination from '../components/Pagination'
import {
  assignUserRoles,
  disableUser,
  enableUser,
  linkUser,
  linkUserCompany,
  listCompanies,
  listTenants,
  listUsers,
  provisionUser,
  resetUserPassword,
} from '../api/endpoints'
import { getErrorMessage, listAll } from '../api/client'

const ROLE_OPTIONS = [
  ROLES.PLATFORM_SUPER_ADMIN,
  ROLES.TENANT_ADMIN,
  ROLES.ACADEMIC_ADMIN,
  ROLES.EXAM_CONTROLLER,
  ROLES.FACULTY,
  ROLES.HOD,
  ROLES.STUDENT,
  ROLES.PLACEMENT_OFFICER,
  ROLES.RECRUITER,
]

const PAGE_SIZE = 10

export default function UsersPage() {
  const { can, tenant } = useAuth()
  const canManage = can([ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN])
  const isPlatform = can([ROLES.PLATFORM_SUPER_ADMIN])

  const [data, setData] = useState({ content: [] })
  const [page, setPage] = useState(0)
  const [tenants, setTenants] = useState([])
  const [companies, setCompanies] = useState([])
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [editRoles, setEditRoles] = useState(null)
  const [linkCompanyUser, setLinkCompanyUser] = useState(null)
  const [form, setForm] = useState({
    keycloakSub: '',
    email: '',
    displayName: '',
    tenantId: '',
    companyId: '',
    roles: [ROLES.STUDENT],
  })
  const [provisionForm, setProvisionForm] = useState({
    username: '',
    email: '',
    displayName: '',
    temporaryPassword: '',
    tenantId: '',
    companyId: '',
    roles: [ROLES.STUDENT],
  })

  async function load() {
    setError('')
    try {
      setData(await listUsers(page, PAGE_SIZE))
      if (isPlatform) {
        const t = await listTenants(0, 100)
        setTenants(t.content || [])
      }
      if (canManage && !isPlatform) {
        try {
          setCompanies(await listAll(listCompanies))
        } catch {
          setCompanies([])
        }
      }
    } catch (e) {
      setError(getErrorMessage(e))
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

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

  function toggleRole(list, role) {
    return list.includes(role) ? list.filter((r) => r !== role) : [...list, role]
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Users</h1>
          <p className="muted">
            Link Keycloak accounts to this app, assign local roles, and enable/disable access.
            {tenant ? ` Scope: ${tenant.name}.` : ' Platform scope.'}
          </p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      {canManage ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                provisionUser({
                  username: provisionForm.username.trim(),
                  email: provisionForm.email.trim(),
                  displayName: provisionForm.displayName.trim(),
                  temporaryPassword: provisionForm.temporaryPassword,
                  tenantId: isPlatform ? provisionForm.tenantId || null : tenant?.id || null,
                  companyId: provisionForm.companyId || null,
                  roles: provisionForm.roles,
                }).then(() =>
                  setProvisionForm({
                    username: '',
                    email: '',
                    displayName: '',
                    temporaryPassword: '',
                    tenantId: '',
                    companyId: '',
                    roles: [ROLES.STUDENT],
                  }),
                ),
              'User provisioned in Keycloak',
            )
          }}
        >
          <h2>Provision in Keycloak</h2>
          <p className="muted">
            Creates the Keycloak user (with a temporary password) and the local account in one step — no manual admin
            console steps needed.
          </p>
          <div className="row">
            <div className="field" style={{ flex: 1 }}>
              <label>Username</label>
              <input
                value={provisionForm.username}
                onChange={(e) => setProvisionForm({ ...provisionForm, username: e.target.value })}
                required
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Email</label>
              <input
                type="email"
                value={provisionForm.email}
                onChange={(e) => setProvisionForm({ ...provisionForm, email: e.target.value })}
                required
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Display name</label>
              <input
                value={provisionForm.displayName}
                onChange={(e) => setProvisionForm({ ...provisionForm, displayName: e.target.value })}
                required
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Temporary password</label>
              <input
                type="text"
                value={provisionForm.temporaryPassword}
                onChange={(e) => setProvisionForm({ ...provisionForm, temporaryPassword: e.target.value })}
                required
              />
            </div>
            {isPlatform ? (
              <div className="field" style={{ flex: 1 }}>
                <label>Tenant</label>
                <select
                  value={provisionForm.tenantId}
                  onChange={(e) => setProvisionForm({ ...provisionForm, tenantId: e.target.value })}
                >
                  <option value="">Platform (no tenant)</option>
                  {tenants.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.code} — {t.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}
            {!isPlatform && companies.length ? (
              <div className="field" style={{ flex: 1 }}>
                <label>Company (recruiters)</label>
                <select
                  value={provisionForm.companyId}
                  onChange={(e) => setProvisionForm({ ...provisionForm, companyId: e.target.value })}
                >
                  <option value="">None</option>
                  {companies.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.code} — {c.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}
          </div>
          <div className="row" style={{ marginTop: '0.75rem', flexWrap: 'wrap' }}>
            {ROLE_OPTIONS.filter((r) => (isPlatform ? true : r !== ROLES.PLATFORM_SUPER_ADMIN)).map((role) => (
              <label key={role} className="row" style={{ gap: '0.35rem', alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={provisionForm.roles.includes(role)}
                  onChange={() => setProvisionForm({ ...provisionForm, roles: toggleRole(provisionForm.roles, role) })}
                />
                {role.replaceAll('_', ' ')}
              </label>
            ))}
            <button className="btn" type="submit" disabled={!provisionForm.roles.length}>
              Provision user
            </button>
          </div>
        </form>
      ) : null}

      {canManage ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                linkUser({
                  keycloakSub: form.keycloakSub.trim(),
                  email: form.email.trim(),
                  displayName: form.displayName.trim(),
                  tenantId: isPlatform ? form.tenantId || null : tenant?.id || null,
                  companyId: form.companyId || null,
                  roles: form.roles,
                }).then(() =>
                  setForm({
                    keycloakSub: '',
                    email: '',
                    displayName: '',
                    tenantId: '',
                    companyId: '',
                    roles: [ROLES.STUDENT],
                  }),
                ),
              'User linked',
            )
          }}
        >
          <h2>Link existing Keycloak user</h2>
          <p className="muted">
            Already created the user directly in Keycloak? Copy their <code>sub</code> (ID) from Keycloak admin → Users →
            user → details, then link here.
          </p>
          <div className="row">
            <div className="field" style={{ flex: 1.2 }}>
              <label>Keycloak sub (UUID)</label>
              <input
                value={form.keycloakSub}
                onChange={(e) => setForm({ ...form, keycloakSub: e.target.value })}
                required
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div className="field" style={{ flex: 1 }}>
              <label>Display name</label>
              <input
                value={form.displayName}
                onChange={(e) => setForm({ ...form, displayName: e.target.value })}
                required
              />
            </div>
            {isPlatform ? (
              <div className="field" style={{ flex: 1 }}>
                <label>Tenant</label>
                <select value={form.tenantId} onChange={(e) => setForm({ ...form, tenantId: e.target.value })}>
                  <option value="">Platform (no tenant)</option>
                  {tenants.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.code} — {t.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}
            {!isPlatform && companies.length ? (
              <div className="field" style={{ flex: 1 }}>
                <label>Company (recruiters)</label>
                <select value={form.companyId} onChange={(e) => setForm({ ...form, companyId: e.target.value })}>
                  <option value="">None</option>
                  {companies.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.code} — {c.name}
                    </option>
                  ))}
                </select>
              </div>
            ) : null}
          </div>
          <div className="row" style={{ marginTop: '0.75rem', flexWrap: 'wrap' }}>
            {ROLE_OPTIONS.filter((r) => (isPlatform ? true : r !== ROLES.PLATFORM_SUPER_ADMIN)).map((role) => (
              <label key={role} className="row" style={{ gap: '0.35rem', alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={form.roles.includes(role)}
                  onChange={() => setForm({ ...form, roles: toggleRole(form.roles, role) })}
                />
                {role.replaceAll('_', ' ')}
              </label>
            ))}
            <button className="btn" type="submit" disabled={!form.roles.length}>
              Link user
            </button>
          </div>
        </form>
      ) : null}

      {editRoles ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(() => assignUserRoles(editRoles.id, editRoles.roles).then(() => setEditRoles(null)), 'Roles updated')
          }}
        >
          <h2>Roles for {editRoles.displayName || editRoles.email}</h2>
          <div className="row" style={{ flexWrap: 'wrap' }}>
            {ROLE_OPTIONS.filter((r) => (isPlatform ? true : r !== ROLES.PLATFORM_SUPER_ADMIN)).map((role) => (
              <label key={role} className="row" style={{ gap: '0.35rem', alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={editRoles.roles.includes(role)}
                  onChange={() =>
                    setEditRoles({ ...editRoles, roles: toggleRole(editRoles.roles, role) })
                  }
                />
                {role.replaceAll('_', ' ')}
              </label>
            ))}
            <button className="btn" type="submit">
              Save roles
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditRoles(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {linkCompanyUser ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                linkUserCompany(linkCompanyUser.id, linkCompanyUser.companyId || null).then(() =>
                  setLinkCompanyUser(null),
                ),
              'Company linked',
            )
          }}
        >
          <h2>Company for {linkCompanyUser.displayName || linkCompanyUser.email}</h2>
          <div className="row">
            <div className="field" style={{ flex: 1 }}>
              <label>Company</label>
              <select
                value={linkCompanyUser.companyId || ''}
                onChange={(e) => setLinkCompanyUser({ ...linkCompanyUser, companyId: e.target.value })}
              >
                <option value="">None (unlink)</option>
                {companies.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} — {c.name}
                  </option>
                ))}
              </select>
            </div>
            <button className="btn" type="submit">
              Save company
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setLinkCompanyUser(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      <div className="table-wrap">
        <table className="data">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Local ID</th>
              <th>Keycloak sub</th>
              <th>Company</th>
              <th>Status</th>
              <th>Roles</th>
              {canManage ? <th /> : null}
            </tr>
          </thead>
          <tbody>
            {(data.content || []).map((u) => (
              <tr key={u.id}>
                <td>{u.displayName}</td>
                <td>{u.email}</td>
                <td>
                  <code title={u.id}>{String(u.id).slice(0, 8)}…</code>
                </td>
                <td>
                  <code title={u.keycloakSub}>{String(u.keycloakSub || '').slice(0, 8)}…</code>
                </td>
                <td>
                  {u.companyId ? (
                    <code title={u.companyId}>{String(u.companyId).slice(0, 8)}…</code>
                  ) : (
                    <span className="muted">—</span>
                  )}
                </td>
                <td>
                  <span className={`badge ${u.status === 'DISABLED' ? 'danger' : ''}`}>{u.status}</span>
                </td>
                <td>{(u.roles || []).join(', ')}</td>
                {canManage ? (
                  <td className="row">
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => setEditRoles({ id: u.id, displayName: u.displayName, email: u.email, roles: [...(u.roles || [])] })}
                    >
                      Roles
                    </button>
                    {!isPlatform ? (
                      <button
                        className="btn btn-ghost"
                        type="button"
                        onClick={() =>
                          setLinkCompanyUser({
                            id: u.id,
                            displayName: u.displayName,
                            email: u.email,
                            companyId: u.companyId || '',
                          })
                        }
                      >
                        Company
                      </button>
                    ) : null}
                    {u.status === 'ACTIVE' ? (
                      <button className="btn btn-ghost" type="button" onClick={() => run(() => disableUser(u.id), 'User disabled')}>
                        Disable
                      </button>
                    ) : (
                      <button className="btn btn-ghost" type="button" onClick={() => run(() => enableUser(u.id), 'User enabled')}>
                        Enable
                      </button>
                    )}
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        const password = window.prompt('Enter new password for this user')
                        if (password == null || password === '') return
                        run(() => resetUserPassword(u.id, password, false), 'Password reset')
                      }}
                    >
                      Reset password
                    </button>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
        </table>
        {!data.content?.length ? (
          <div className="empty">No users yet. They appear after first login, or provision/link one above.</div>
        ) : null}
      </div>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  )
}
