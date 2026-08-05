import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import './login.css'

const DEMO = [
  { u: 'superadmin', p: 'SuperAdmin@123', role: 'Platform' },
  { u: 'tenantadmin', p: 'TenantAdmin@123', role: 'Tenant Admin' },
  { u: 'examcontroller', p: 'Exam@123', role: 'Exam' },
  { u: 'placement', p: 'Placement@123', role: 'Placement' },
  { u: 'recruiter1', p: 'Recruiter@123', role: 'Recruiter' },
  { u: 'faculty1', p: 'Faculty@123', role: 'Faculty' },
  { u: 'student1', p: 'Student@123', role: 'Student' },
]

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('tenantadmin')
  const [password, setPassword] = useState('TenantAdmin@123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) return <Navigate to="/app" replace />

  async function onSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/app')
    } catch (err) {
      setError('Invalid credentials or Keycloak unavailable.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-screen">
      <div className="login-hero">
        <p className="eyebrow">IIITB · College Operations</p>
        <h1>College Admin</h1>
        <p className="lede">Exam and placement operations for multi-program campuses — signed in with your role.</p>
      </div>
      <form className="login-panel panel" onSubmit={onSubmit}>
        <h2>Sign in</h2>
        <p className="muted">Use your Keycloak account. Screens and data follow your assigned role.</p>
        {error ? <div className="alert">{error}</div> : null}
        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>
        <button className="btn" type="submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Continue'}
        </button>
        <div className="demo-list">
          <p className="muted">Quick fill (seed users)</p>
          <div className="row">
            {DEMO.map((d) => (
              <button
                key={d.u}
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setUsername(d.u)
                  setPassword(d.p)
                }}
              >
                {d.role}
              </button>
            ))}
          </div>
        </div>
      </form>
    </div>
  )
}
