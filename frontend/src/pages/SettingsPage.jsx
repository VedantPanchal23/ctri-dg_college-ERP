import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { bootstrapAcademic, fetchTenantMe, updateTenantMe } from '../api/endpoints'
import { getErrorMessage } from '../api/client'

export default function SettingsPage() {
  const { tenant, setTenant } = useAuth()
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ name: '', timezone: '', academicYearStartMonth: 8 })
  const [bootstrap, setBootstrap] = useState({
    programCode: '',
    programName: '',
    degreeType: 'BTECH',
    durationYears: 4,
    branchCode: '',
    branchName: '',
    batchCode: '',
    admissionYear: new Date().getFullYear(),
    graduationYear: new Date().getFullYear() + 4,
  })

  useEffect(() => {
    if (tenant) {
      setForm({
        name: tenant.name || '',
        timezone: tenant.timezone || '',
        academicYearStartMonth: tenant.academicYearStartMonth || 8,
      })
    }
  }, [tenant])

  async function onSaveSettings(e) {
    e.preventDefault()
    setError('')
    setOk('')
    setBusy(true)
    try {
      const updated = await updateTenantMe({
        name: form.name,
        timezone: form.timezone,
        academicYearStartMonth: Number(form.academicYearStartMonth),
      })
      setTenant(updated)
      setOk('College settings saved')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function onBootstrap(e) {
    e.preventDefault()
    setError('')
    setOk('')
    setBusy(true)
    try {
      await bootstrapAcademic({
        programCode: bootstrap.programCode,
        programName: bootstrap.programName,
        degreeType: bootstrap.degreeType,
        durationYears: Number(bootstrap.durationYears),
        branchCode: bootstrap.branchCode,
        branchName: bootstrap.branchName,
        batchCode: bootstrap.batchCode,
        admissionYear: Number(bootstrap.admissionYear),
        graduationYear: Number(bootstrap.graduationYear),
      })
      setOk('Academic structure bootstrapped. Open Academic to see the new program/branch/batch.')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Settings</h1>
          <p className="muted">College profile and one-shot academic structure setup for {tenant?.name || 'your college'}.</p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      <form className="panel" onSubmit={onSaveSettings}>
        <h2>College settings</h2>
        <p className="muted">Basic profile used across the app.</p>
        <div className="row">
          <div className="field" style={{ flex: 2 }}>
            <label>Name</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div className="field">
            <label>Timezone</label>
            <input
              value={form.timezone}
              onChange={(e) => setForm({ ...form, timezone: e.target.value })}
              placeholder="Asia/Kolkata"
              required
            />
          </div>
          <div className="field">
            <label>Academic year start month</label>
            <input
              type="number"
              min="1"
              max="12"
              value={form.academicYearStartMonth}
              onChange={(e) => setForm({ ...form, academicYearStartMonth: e.target.value })}
            />
          </div>
          <button className="btn" type="submit" disabled={busy}>
            Save settings
          </button>
        </div>
      </form>

      <form className="panel" onSubmit={onBootstrap}>
        <h2>Bootstrap academic structure</h2>
        <p className="muted">
          Creates a program, branch, and batch in one step — a quick start for a brand-new college. You can edit or add
          more later from Academic.
        </p>
        <div className="row">
          <div className="field">
            <label>Program code</label>
            <input
              value={bootstrap.programCode}
              onChange={(e) => setBootstrap({ ...bootstrap, programCode: e.target.value })}
              required
            />
          </div>
          <div className="field" style={{ flex: 1.4 }}>
            <label>Program name</label>
            <input
              value={bootstrap.programName}
              onChange={(e) => setBootstrap({ ...bootstrap, programName: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>Degree</label>
            <select value={bootstrap.degreeType} onChange={(e) => setBootstrap({ ...bootstrap, degreeType: e.target.value })}>
              <option value="BTECH">BTECH</option>
              <option value="MTECH">MTECH</option>
              <option value="MSC">MSC</option>
              <option value="PHD">PHD</option>
            </select>
          </div>
          <div className="field">
            <label>Duration (years)</label>
            <input
              type="number"
              min="1"
              value={bootstrap.durationYears}
              onChange={(e) => setBootstrap({ ...bootstrap, durationYears: e.target.value })}
            />
          </div>
        </div>
        <div className="row">
          <div className="field">
            <label>Branch code</label>
            <input
              value={bootstrap.branchCode}
              onChange={(e) => setBootstrap({ ...bootstrap, branchCode: e.target.value })}
              required
            />
          </div>
          <div className="field" style={{ flex: 1.4 }}>
            <label>Branch name</label>
            <input
              value={bootstrap.branchName}
              onChange={(e) => setBootstrap({ ...bootstrap, branchName: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>Batch code</label>
            <input
              value={bootstrap.batchCode}
              onChange={(e) => setBootstrap({ ...bootstrap, batchCode: e.target.value })}
              required
            />
          </div>
          <div className="field">
            <label>Admission year</label>
            <input
              type="number"
              value={bootstrap.admissionYear}
              onChange={(e) => setBootstrap({ ...bootstrap, admissionYear: e.target.value })}
            />
          </div>
          <div className="field">
            <label>Graduation year</label>
            <input
              type="number"
              value={bootstrap.graduationYear}
              onChange={(e) => setBootstrap({ ...bootstrap, graduationYear: e.target.value })}
            />
          </div>
          <button className="btn" type="submit" disabled={busy}>
            Bootstrap
          </button>
        </div>
      </form>
    </div>
  )
}
