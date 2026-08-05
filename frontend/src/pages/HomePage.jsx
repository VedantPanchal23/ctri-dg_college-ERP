import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ROLES, homeCardsForRoles, roleLabel } from '../auth/roles'
import { softGet, getErrorMessage } from '../api/client'
import {
  listApplications,
  listCompanies,
  listDrives,
  listExamSchedules,
  listExamSessions,
  listFaculty,
  listPrograms,
  listStudents,
  listTenants,
  listUsers,
  myHallTickets,
  myStudent,
  placementStats,
} from '../api/endpoints'
import './home.css'

export default function HomePage() {
  const { roles, tenant, profile, can } = useAuth()
  const cards = homeCardsForRoles(roles)
  const [stats, setStats] = useState([])
  const [actions, setActions] = useState([])
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    ;(async () => {
      setLoading(true)
      setError('')
      try {
        const nextStats = []
        const nextActions = []
        let nextDetail = null

        if (can([ROLES.PLATFORM_SUPER_ADMIN])) {
          const tenants = await listTenants()
          const list = tenants.content || []
          nextStats.push(
            { label: 'Colleges', value: list.length },
            { label: 'Active', value: list.filter((t) => t.status === 'ACTIVE').length },
            { label: 'Suspended', value: list.filter((t) => t.status === 'SUSPENDED').length },
          )
          nextActions.push({ label: 'Manage tenants', to: '/app/platform/tenants' })
          nextDetail = {
            title: 'Platform overview',
            lines: list.slice(0, 5).map((t) => `${t.code} — ${t.name} (${t.status})`),
            empty: 'No colleges yet. Create the first tenant.',
          }
        }

        if (can([ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.FACULTY, ROLES.HOD, ROLES.EXAM_CONTROLLER, ROLES.PLACEMENT_OFFICER])) {
          const programs = (await listPrograms()).content || []
          nextStats.push({ label: 'Programs', value: programs.length })
          nextActions.push({ label: 'Open academic', to: '/app/academic' })
          if (can([ROLES.TENANT_ADMIN]) && !programs.length) {
            nextDetail = {
              title: 'Get started',
              lines: [],
              empty: 'No academic structure yet for your college.',
              emptyActions: [{ label: 'Bootstrap academic setup', to: '/app/settings' }],
            }
          }
        }

        if (can([ROLES.TENANT_ADMIN])) {
          const [faculty, usersPage] = await Promise.all([
            softGet(listFaculty()),
            softGet(listUsers(0, 1)),
          ])
          if (!nextDetail && faculty && !(faculty.content || []).length) {
            nextDetail = {
              title: 'Get started',
              lines: [],
              empty: 'No faculty profiles linked yet.',
              emptyActions: [{ label: 'Add faculty', to: '/app/academic' }],
            }
          }
          if (usersPage && (usersPage.totalElements ?? usersPage.content?.length ?? 0) <= 1) {
            nextActions.push({ label: 'Provision a user', to: '/app/users' })
          }
        }

        if (can([ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.EXAM_CONTROLLER, ROLES.FACULTY, ROLES.HOD, ROLES.PLACEMENT_OFFICER])) {
          try {
            const students = (await listStudents()).content || []
            nextStats.push({ label: 'Students', value: students.length })
          } catch {
            /* role may lack list access */
          }
        }

        if (can([ROLES.TENANT_ADMIN, ROLES.EXAM_CONTROLLER, ROLES.FACULTY, ROLES.HOD])) {
          const sessions = (await listExamSessions()).content || []
          const schedules = (await listExamSchedules()).content || []
          nextStats.push(
            { label: 'Exam sessions', value: sessions.length },
            { label: 'Schedules', value: schedules.length },
          )
          nextActions.push({ label: 'Manage exams', to: '/app/exams' })
          if (!nextDetail) {
            nextDetail = {
              title: 'Upcoming / recent schedules',
              lines: schedules.slice(0, 5).map((s) => `${s.venue} · ${formatDate(s.examDatetime)} · max ${s.maxMarks}`),
              empty: 'No exam schedules yet.',
            }
          }
        }

        if (can([ROLES.STUDENT]) && !can([ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.EXAM_CONTROLLER, ROLES.FACULTY])) {
          const me = await softGet(myStudent())
          const tickets = (await softGet(myHallTickets())) || []
          const drives = (await listDrives()).content || []
          const openDrives = drives.filter((d) => d.status === 'OPEN')
          const apps = (await softGet(listApplications()))?.content || []
          nextStats.push(
            { label: 'CGPA', value: me?.cgpa ?? '—' },
            { label: 'Backlogs', value: me?.backlogCount ?? '—' },
            { label: 'Hall tickets', value: tickets.length },
            { label: 'Open drives', value: openDrives.length },
            { label: 'Applications', value: apps.length },
          )
          nextActions.push(
            { label: 'My academic profile', to: '/app/academic' },
            { label: 'Hall tickets', to: '/app/exams' },
            { label: 'Apply to drives', to: '/app/placements' },
          )
          nextDetail = {
            title: 'Student snapshot',
            lines: me
              ? [
                  `Roll ${me.rollNumber}`,
                  `Attendance ${me.attendancePercent}%`,
                  me.barredFromExams ? 'Barred from exams' : 'Eligible for exams',
                  `${openDrives.length} placement drive(s) open`,
                ]
              : ['No student profile linked to this account yet. Ask academic admin to create one.'],
            empty: null,
          }
        }

        if (can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN, ROLES.RECRUITER])) {
          const companies = can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN, ROLES.RECRUITER])
            ? (await listCompanies()).content || []
            : []
          const drives = (await listDrives()).content || []
          nextStats.push(
            { label: 'Companies', value: companies.length },
            { label: 'Drives', value: drives.length },
            { label: 'Open drives', value: drives.filter((d) => d.status === 'OPEN').length },
          )
          nextActions.push({ label: 'Open placements', to: '/app/placements' })
          if (can([ROLES.RECRUITER]) && !can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])) {
            nextActions.push({ label: 'Recruiter workspace', to: '/app/recruiter' })
          }
          if (can([ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN])) {
            const pstats = await placementStats()
            nextStats.push({ label: 'Placed', value: pstats.placedCount ?? 0 })
            nextDetail = nextDetail || {
              title: 'Placement pulse',
              lines: [
                `${pstats.totalApplications || 0} applications`,
                `${pstats.placedCount || 0} placed`,
                `Avg package ${pstats.averageAcceptedPackage ?? '—'} LPA`,
              ],
              empty: null,
            }
          }
        }

        if (!alive) return
        setStats(dedupeStats(nextStats))
        setActions(dedupeActions(nextActions))
        setDetail(nextDetail)
      } catch (e) {
        if (alive) setError(getErrorMessage(e))
      } finally {
        if (alive) setLoading(false)
      }
    })()
    return () => {
      alive = false
    }
  }, [can, roles])

  return (
    <div className="stack home">
      <div className="page-head">
        <div>
          <h1>Welcome{profile?.displayName ? `, ${profile.displayName.split(' ')[0]}` : ''}</h1>
          <p className="muted">
            {roles.map(roleLabel).join(' · ')}
            {tenant ? ` · ${tenant.name} (${tenant.code})` : profile?.tenantId ? '' : ' · Platform scope'}
          </p>
        </div>
        <div className="row">
          {actions.slice(0, 3).map((a) => (
            <Link key={a.to + a.label} className="btn" to={a.to}>
              {a.label}
            </Link>
          ))}
        </div>
      </div>

      {error ? <div className="alert">{error}</div> : null}

      <section className="stat-grid">
        {loading ? <div className="panel muted">Loading your workspace…</div> : null}
        {!loading &&
          stats.map((s) => (
            <div key={s.label} className="panel stat-card">
              <div className="stat-value">{s.value}</div>
              <div className="stat-label">{s.label}</div>
            </div>
          ))}
        {!loading && !stats.length ? <div className="panel muted">No summary metrics for this role yet.</div> : null}
      </section>

      <section className="home-split">
        <div className="panel">
          <h2>Your modules</h2>
          <p className="muted">Only areas your role can access are listed.</p>
          <div className="home-grid">
            {cards.map((c) => (
              <Link key={c.to} to={c.to} className="home-card">
                <h3>{c.title}</h3>
                <p>{c.text}</p>
                <span className="card-link">Open →</span>
              </Link>
            ))}
            {!cards.length ? <div className="empty">No modules available.</div> : null}
          </div>
        </div>

        <div className="panel">
          <h2>{detail?.title || 'Details'}</h2>
          {detail?.lines?.length ? (
            <ul className="detail-list">
              {detail.lines.map((line) => (
                <li key={line}>{line}</li>
              ))}
            </ul>
          ) : (
            <div className="stack">
              <div className="empty">{detail?.empty || 'Sign-in details appear here based on your role.'}</div>
              {detail?.emptyActions?.length ? (
                <div className="row" style={{ justifyContent: 'center' }}>
                  {detail.emptyActions.map((a) => (
                    <Link key={a.to} className="btn" to={a.to}>
                      {a.label}
                    </Link>
                  ))}
                </div>
              ) : null}
            </div>
          )}
          <div className="role-box">
            <h3>Active roles</h3>
            <div className="row">
              {roles.map((r) => (
                <span key={r} className="badge">
                  {roleLabel(r)}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}

function formatDate(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

function dedupeStats(items) {
  const seen = new Set()
  return items.filter((i) => {
    if (seen.has(i.label)) return false
    seen.add(i.label)
    return true
  })
}

function dedupeActions(items) {
  const seen = new Set()
  return items.filter((i) => {
    const key = i.to
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}
