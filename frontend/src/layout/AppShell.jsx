import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { NAV_ITEMS, roleLabel } from '../auth/roles'
import { fetchMe, fetchTenantMe, unreadNotificationCount } from '../api/endpoints'
import { getErrorMessage } from '../api/client'
import './shell.css'

export default function AppShell() {
  const { roles, logout, claims, setProfile, setTenant, profile, tenant, can } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [navOpen, setNavOpen] = useState(false)
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    let alive = true
    ;(async () => {
      try {
        const me = await fetchMe()
        if (!alive) return
        setProfile(me)
        try {
          const count = await unreadNotificationCount()
          if (alive) setUnread(typeof count === 'number' ? count : count?.count ?? 0)
        } catch {
          if (alive) setUnread(0)
        }
        if (can(['PLATFORM_SUPER_ADMIN']) && !me.tenantId) {
          setTenant(null)
          return
        }
        try {
          const t = await fetchTenantMe()
          if (alive) setTenant(t)
        } catch (e) {
          if (alive) setTenant(null)
        }
      } catch (e) {
        if (alive) {
          const msg = getErrorMessage(e)
          if (e?.code === 'ERR_NETWORK' || msg.includes('Network Error')) {
            setError('API unreachable. Start the backend on :8080 (docker compose up -d app) and refresh.')
          } else {
            setError(msg)
          }
        }
      }
    })()
    return () => {
      alive = false
    }
  }, [can, setProfile, setTenant])

  const nav = NAV_ITEMS.filter((item) => can(item.roles))

  return (
    <div className="shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <aside className={`shell-nav ${navOpen ? 'open' : ''}`} aria-label="Primary">
        <div className="brand-row">
          <div className="brand">
            <div className="brand-mark" aria-hidden="true">
              CA
            </div>
            <div>
              <div className="brand-name">College Admin</div>
              <div className="brand-sub">{tenant?.name || 'Platform'}</div>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-ghost nav-toggle"
            aria-expanded={navOpen}
            aria-controls="app-nav"
            onClick={() => setNavOpen((v) => !v)}
          >
            {navOpen ? 'Close' : 'Menu'}
          </button>
        </div>
        <nav id="app-nav">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/app'}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              onClick={() => setNavOpen(false)}
            >
              {item.label}
              {item.to === '/app/notifications' && unread > 0 ? (
                <span className="badge" style={{ marginLeft: '0.35rem' }}>
                  {unread > 99 ? '99+' : unread}
                </span>
              ) : null}
            </NavLink>
          ))}
        </nav>
        <div className="shell-user">
          <div className="user-name">{profile?.displayName || claims?.name || claims?.preferred_username}</div>
          <div className="user-roles" aria-label="Roles">
            {roles.map((r) => (
              <span key={r} className="badge">
                {roleLabel(r)}
              </span>
            ))}
          </div>
          <button
            className="btn btn-ghost"
            type="button"
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main id="main-content" className="shell-main" tabIndex={-1}>
        {error ? (
          <div className="alert" role="alert">
            {error}
          </div>
        ) : null}
        <Outlet />
      </main>
    </div>
  )
}
