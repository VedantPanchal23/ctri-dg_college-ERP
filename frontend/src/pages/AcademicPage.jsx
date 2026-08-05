import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ROLES } from '../auth/roles'
import Pagination from '../components/Pagination'
import {
  createBatch,
  createBranch,
  createCourse,
  createEnrollment,
  createFaculty,
  createOffering,
  createProgram,
  createStudent,
  deleteBatch,
  deleteBranch,
  deleteCourse,
  deleteOffering,
  deleteProgram,
  dropEnrollment,
  listBatches,
  listBranches,
  listCourses,
  listEnrollments,
  listFaculty,
  listOfferings,
  listPrograms,
  listStudents,
  listUsers,
  myStudent,
  updateBatch,
  updateBranch,
  updateCourse,
  updateFaculty,
  updateProgram,
  updateStudent,
} from '../api/endpoints'
import { getErrorMessage, softGet, listAll } from '../api/client'

export default function AcademicPage() {
  const { can } = useAuth()
  const canManage = can([ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN])
  const isStudent = can([ROLES.STUDENT]) && !canManage
  const canListStudents = can([
    ROLES.TENANT_ADMIN,
    ROLES.ACADEMIC_ADMIN,
    ROLES.EXAM_CONTROLLER,
    ROLES.FACULTY,
    ROLES.HOD,
    ROLES.PLACEMENT_OFFICER,
  ])

  const [tab, setTab] = useState(isStudent ? 'me' : 'programs')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [programs, setPrograms] = useState([])
  const [branches, setBranches] = useState([])
  const [batches, setBatches] = useState([])
  const [courses, setCourses] = useState([])
  const [students, setStudents] = useState([])
  const [faculty, setFaculty] = useState([])
  const [offerings, setOfferings] = useState([])
  const [enrollments, setEnrollments] = useState([])
  const [users, setUsers] = useState([])
  const [me, setMe] = useState(null)
  // Catalog snapshots for dropdowns (first 100)
  const [programOpts, setProgramOpts] = useState([])
  const [branchOpts, setBranchOpts] = useState([])
  const [batchOpts, setBatchOpts] = useState([])
  const [courseOpts, setCourseOpts] = useState([])
  const [facultyOpts, setFacultyOpts] = useState([])
  const [studentOpts, setStudentOpts] = useState([])
  const [offeringOpts, setOfferingOpts] = useState([])

  const PAGE_SIZE = 20

  const [programForm, setProgramForm] = useState({ code: '', name: '', degreeType: 'BTECH', durationYears: 4 })
  const [branchForm, setBranchForm] = useState({ programId: '', code: '', name: '' })
  const [batchForm, setBatchForm] = useState({ branchId: '', code: '', admissionYear: 2024, graduationYear: 2028 })
  const [courseForm, setCourseForm] = useState({ programId: '', code: '', name: '', credits: 3, semesterNumber: 1 })
  const [studentForm, setStudentForm] = useState({ userId: '', batchId: '', rollNumber: '' })
  const [facultyForm, setFacultyForm] = useState({ userId: '', employeeCode: '', department: '' })
  const [editStudent, setEditStudent] = useState(null)
  const [editProgram, setEditProgram] = useState(null)
  const [editBranch, setEditBranch] = useState(null)
  const [editBatch, setEditBatch] = useState(null)
  const [editCourse, setEditCourse] = useState(null)
  const [editFaculty, setEditFaculty] = useState(null)
  const [offeringForm, setOfferingForm] = useState({ courseId: '', facultyId: '', academicYear: '2025-26', semesterNumber: 1 })
  const [enrollForm, setEnrollForm] = useState({ studentId: '', courseOfferingId: '' })

  async function load() {
    setError('')
    try {
      if (isStudent) {
        setMe(await softGet(myStudent()))
        if (tab === 'programs') {
          const p = await listPrograms(page, PAGE_SIZE)
          setPrograms(p.content || [])
          setTotalPages(p.totalPages || 1)
        } else if (tab === 'courses') {
          const c = await listCourses(page, PAGE_SIZE)
          setCourses(c.content || [])
          setTotalPages(c.totalPages || 1)
        } else {
          setTotalPages(1)
        }
        return
      }

      const [pOpts, bOpts, baOpts, cOpts, fOpts, sOpts, oOpts] = await Promise.all([
        listAll(listPrograms),
        listAll(listBranches),
        listAll(listBatches),
        listAll(listCourses),
        softGet(listAll(listFaculty)),
        softGet(listAll(listStudents)),
        softGet(listAll(listOfferings)),
      ])
      setProgramOpts(pOpts || [])
      setBranchOpts(bOpts || [])
      setBatchOpts(baOpts || [])
      setCourseOpts(cOpts || [])
      setFacultyOpts(fOpts || [])
      setStudentOpts(sOpts || [])
      setOfferingOpts(oOpts || [])
      if (canManage) setUsers((await softGet(listAll(listUsers))) || [])

      let result = { content: [], totalPages: 1 }
      if (tab === 'programs') result = await listPrograms(page, PAGE_SIZE)
      else if (tab === 'branches') result = await listBranches(page, PAGE_SIZE)
      else if (tab === 'batches') result = await listBatches(page, PAGE_SIZE)
      else if (tab === 'courses') result = await listCourses(page, PAGE_SIZE)
      else if (tab === 'students') result = await listStudents(page, PAGE_SIZE)
      else if (tab === 'faculty') result = await listFaculty(page, PAGE_SIZE)
      else if (tab === 'offerings') result = await listOfferings(page, PAGE_SIZE)
      else if (tab === 'enrollments') result = await listEnrollments(page, PAGE_SIZE)

      if (tab === 'programs') setPrograms(result.content || [])
      if (tab === 'branches') setBranches(result.content || [])
      if (tab === 'batches') setBatches(result.content || [])
      if (tab === 'courses') setCourses(result.content || [])
      if (tab === 'students') setStudents(result.content || [])
      if (tab === 'faculty') setFaculty(result.content || [])
      if (tab === 'offerings') setOfferings(result.content || [])
      if (tab === 'enrollments') setEnrollments(result.content || [])
      setTotalPages(result.totalPages || 1)
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
      setOk(successMsg)
      await load()
    } catch (err) {
      setError(friendlyError(err))
    }
  }

  const tabs = isStudent
    ? [
        { id: 'me', label: 'My profile' },
        { id: 'programs', label: 'Programs' },
        { id: 'courses', label: 'Courses' },
      ]
    : [
        { id: 'programs', label: 'Programs' },
        { id: 'branches', label: 'Branches' },
        { id: 'batches', label: 'Batches' },
        { id: 'courses', label: 'Courses' },
        ...(canListStudents
          ? [
              { id: 'students', label: 'Students' },
              { id: 'faculty', label: 'Faculty' },
              { id: 'enrollments', label: 'Enrollments' },
            ]
          : []),
        { id: 'offerings', label: 'Offerings' },
      ]

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Academic</h1>
          <p className="muted">
            {isStudent
              ? 'Your profile, programs, and course catalog.'
              : 'Create and manage programs, branches, batches, courses, students, and enrollments.'}
          </p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}
      <div className="row">
        {tabs.map((t) => (
          <button key={t.id} type="button" className={`btn ${tab === t.id ? '' : 'btn-ghost'}`} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'me' ? (
        <div className="panel">
          {me ? (
            <>
              <h2>Roll {me.rollNumber}</h2>
              <div className="row" style={{ gap: '1.5rem', marginTop: '0.5rem' }}>
                <div>
                  <div className="muted">CGPA</div>
                  <strong>{me.cgpa}</strong>
                </div>
                <div>
                  <div className="muted">Backlogs</div>
                  <strong>{me.backlogCount}</strong>
                </div>
                <div>
                  <div className="muted">Attendance</div>
                  <strong>{me.attendancePercent}%</strong>
                </div>
                <div>
                  <div className="muted">Exam status</div>
                  {me.barredFromExams ? <span className="badge danger">Barred</span> : <span className="badge">Eligible</span>}
                </div>
              </div>
            </>
          ) : (
            <div className="empty">
              No student profile is linked to this login yet. Ask an Academic Admin to create a student profile for your user
              account (Users → link roll number + batch).
            </div>
          )}
        </div>
      ) : null}

      {canManage && tab === 'programs' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createProgram({
                  code: programForm.code,
                  name: programForm.name,
                  degreeType: programForm.degreeType,
                  durationYears: Number(programForm.durationYears),
                }).then(() => setProgramForm({ code: '', name: '', degreeType: 'BTECH', durationYears: 4 })),
              'Program created',
            )
          }}
        >
          <h2>New program</h2>
          <div className="row">
            <Field label="Code">
              <input value={programForm.code} onChange={(e) => setProgramForm({ ...programForm, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={programForm.name} onChange={(e) => setProgramForm({ ...programForm, name: e.target.value })} required />
            </Field>
            <Field label="Degree">
              <select value={programForm.degreeType} onChange={(e) => setProgramForm({ ...programForm, degreeType: e.target.value })}>
                <option value="BTECH">BTECH</option>
                <option value="MTECH">MTECH</option>
                <option value="MSC">MSC</option>
                <option value="PHD">PHD</option>
              </select>
            </Field>
            <Field label="Years">
              <input
                type="number"
                min="1"
                value={programForm.durationYears}
                onChange={(e) => setProgramForm({ ...programForm, durationYears: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'programs' && editProgram ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateProgram(editProgram.id, {
                  code: editProgram.code,
                  name: editProgram.name,
                  degreeType: editProgram.degreeType,
                  durationYears: Number(editProgram.durationYears),
                }).then(() => setEditProgram(null)),
              'Program updated',
            )
          }}
        >
          <h2>Edit program {editProgram.code}</h2>
          <div className="row">
            <Field label="Code">
              <input value={editProgram.code} onChange={(e) => setEditProgram({ ...editProgram, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={editProgram.name} onChange={(e) => setEditProgram({ ...editProgram, name: e.target.value })} required />
            </Field>
            <Field label="Degree">
              <select value={editProgram.degreeType} onChange={(e) => setEditProgram({ ...editProgram, degreeType: e.target.value })}>
                <option value="BTECH">BTECH</option>
                <option value="MTECH">MTECH</option>
                <option value="MSC">MSC</option>
                <option value="PHD">PHD</option>
              </select>
            </Field>
            <Field label="Years">
              <input
                type="number"
                min="1"
                value={editProgram.durationYears}
                onChange={(e) => setEditProgram({ ...editProgram, durationYears: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditProgram(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'branches' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createBranch({
                  programId: branchForm.programId,
                  code: branchForm.code,
                  name: branchForm.name,
                }).then(() => setBranchForm({ programId: '', code: '', name: '' })),
              'Branch created',
            )
          }}
        >
          <h2>New branch</h2>
          <div className="row">
            <Field label="Program" grow>
              <select
                value={branchForm.programId}
                onChange={(e) => setBranchForm({ ...branchForm, programId: e.target.value })}
                required
              >
                <option value="">Select program</option>
                {programOpts.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={branchForm.code} onChange={(e) => setBranchForm({ ...branchForm, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={branchForm.name} onChange={(e) => setBranchForm({ ...branchForm, name: e.target.value })} required />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'branches' && editBranch ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateBranch(editBranch.id, {
                  programId: editBranch.programId,
                  code: editBranch.code,
                  name: editBranch.name,
                }).then(() => setEditBranch(null)),
              'Branch updated',
            )
          }}
        >
          <h2>Edit branch {editBranch.code}</h2>
          <div className="row">
            <Field label="Program" grow>
              <select
                value={editBranch.programId}
                onChange={(e) => setEditBranch({ ...editBranch, programId: e.target.value })}
                required
              >
                <option value="">Select program</option>
                {programOpts.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={editBranch.code} onChange={(e) => setEditBranch({ ...editBranch, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={editBranch.name} onChange={(e) => setEditBranch({ ...editBranch, name: e.target.value })} required />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditBranch(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'batches' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createBatch({
                  branchId: batchForm.branchId,
                  code: batchForm.code,
                  admissionYear: Number(batchForm.admissionYear),
                  graduationYear: Number(batchForm.graduationYear),
                }).then(() => setBatchForm({ branchId: '', code: '', admissionYear: 2024, graduationYear: 2028 })),
              'Batch created',
            )
          }}
        >
          <h2>New batch</h2>
          <div className="row">
            <Field label="Branch" grow>
              <select value={batchForm.branchId} onChange={(e) => setBatchForm({ ...batchForm, branchId: e.target.value })} required>
                <option value="">Select branch</option>
                {branchOpts.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} — {b.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={batchForm.code} onChange={(e) => setBatchForm({ ...batchForm, code: e.target.value })} required />
            </Field>
            <Field label="Admission">
              <input
                type="number"
                value={batchForm.admissionYear}
                onChange={(e) => setBatchForm({ ...batchForm, admissionYear: e.target.value })}
              />
            </Field>
            <Field label="Graduation">
              <input
                type="number"
                value={batchForm.graduationYear}
                onChange={(e) => setBatchForm({ ...batchForm, graduationYear: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'batches' && editBatch ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateBatch(editBatch.id, {
                  branchId: editBatch.branchId,
                  code: editBatch.code,
                  admissionYear: Number(editBatch.admissionYear),
                  graduationYear: Number(editBatch.graduationYear),
                }).then(() => setEditBatch(null)),
              'Batch updated',
            )
          }}
        >
          <h2>Edit batch {editBatch.code}</h2>
          <div className="row">
            <Field label="Branch" grow>
              <select value={editBatch.branchId} onChange={(e) => setEditBatch({ ...editBatch, branchId: e.target.value })} required>
                <option value="">Select branch</option>
                {branchOpts.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} — {b.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={editBatch.code} onChange={(e) => setEditBatch({ ...editBatch, code: e.target.value })} required />
            </Field>
            <Field label="Admission">
              <input
                type="number"
                value={editBatch.admissionYear}
                onChange={(e) => setEditBatch({ ...editBatch, admissionYear: e.target.value })}
              />
            </Field>
            <Field label="Graduation">
              <input
                type="number"
                value={editBatch.graduationYear}
                onChange={(e) => setEditBatch({ ...editBatch, graduationYear: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditBatch(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'courses' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createCourse({
                  programId: courseForm.programId,
                  code: courseForm.code,
                  name: courseForm.name,
                  credits: Number(courseForm.credits),
                  semesterNumber: Number(courseForm.semesterNumber),
                }).then(() => setCourseForm({ programId: '', code: '', name: '', credits: 3, semesterNumber: 1 })),
              'Course created',
            )
          }}
        >
          <h2>New course</h2>
          <div className="row">
            <Field label="Program" grow>
              <select
                value={courseForm.programId}
                onChange={(e) => setCourseForm({ ...courseForm, programId: e.target.value })}
                required
              >
                <option value="">Select program</option>
                {programOpts.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={courseForm.code} onChange={(e) => setCourseForm({ ...courseForm, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={courseForm.name} onChange={(e) => setCourseForm({ ...courseForm, name: e.target.value })} required />
            </Field>
            <Field label="Credits">
              <input
                type="number"
                min="1"
                value={courseForm.credits}
                onChange={(e) => setCourseForm({ ...courseForm, credits: e.target.value })}
              />
            </Field>
            <Field label="Sem">
              <input
                type="number"
                min="1"
                value={courseForm.semesterNumber}
                onChange={(e) => setCourseForm({ ...courseForm, semesterNumber: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'courses' && editCourse ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateCourse(editCourse.id, {
                  programId: editCourse.programId,
                  code: editCourse.code,
                  name: editCourse.name,
                  credits: Number(editCourse.credits),
                  semesterNumber: Number(editCourse.semesterNumber),
                }).then(() => setEditCourse(null)),
              'Course updated',
            )
          }}
        >
          <h2>Edit course {editCourse.code}</h2>
          <div className="row">
            <Field label="Program" grow>
              <select
                value={editCourse.programId}
                onChange={(e) => setEditCourse({ ...editCourse, programId: e.target.value })}
                required
              >
                <option value="">Select program</option>
                {programOpts.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.code} — {p.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Code">
              <input value={editCourse.code} onChange={(e) => setEditCourse({ ...editCourse, code: e.target.value })} required />
            </Field>
            <Field label="Name" grow>
              <input value={editCourse.name} onChange={(e) => setEditCourse({ ...editCourse, name: e.target.value })} required />
            </Field>
            <Field label="Credits">
              <input
                type="number"
                min="1"
                value={editCourse.credits}
                onChange={(e) => setEditCourse({ ...editCourse, credits: e.target.value })}
              />
            </Field>
            <Field label="Sem">
              <input
                type="number"
                min="1"
                value={editCourse.semesterNumber}
                onChange={(e) => setEditCourse({ ...editCourse, semesterNumber: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditCourse(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'faculty' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createFaculty({
                  userId: facultyForm.userId,
                  employeeCode: facultyForm.employeeCode,
                  department: facultyForm.department || null,
                }).then(() => setFacultyForm({ userId: '', employeeCode: '', department: '' })),
              'Faculty profile created',
            )
          }}
        >
          <h2>Link faculty profile</h2>
          <p className="muted">Required before creating course offerings. User must already exist (login or Users → Link).</p>
          <div className="row">
            <Field label="User" grow>
              <select
                value={facultyForm.userId}
                onChange={(e) => setFacultyForm({ ...facultyForm, userId: e.target.value })}
                required
              >
                <option value="">Select user</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.displayName || u.email} ({u.email})
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Employee code">
              <input
                value={facultyForm.employeeCode}
                onChange={(e) => setFacultyForm({ ...facultyForm, employeeCode: e.target.value })}
                required
              />
            </Field>
            <Field label="Department" grow>
              <input
                value={facultyForm.department}
                onChange={(e) => setFacultyForm({ ...facultyForm, department: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Link
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'faculty' && editFaculty ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateFaculty(editFaculty.id, {
                  employeeCode: editFaculty.employeeCode,
                  department: editFaculty.department || null,
                }).then(() => setEditFaculty(null)),
              'Faculty profile updated',
            )
          }}
        >
          <h2>Edit faculty {editFaculty.employeeCode}</h2>
          <div className="row">
            <Field label="Employee code">
              <input
                value={editFaculty.employeeCode}
                onChange={(e) => setEditFaculty({ ...editFaculty, employeeCode: e.target.value })}
                required
              />
            </Field>
            <Field label="Department" grow>
              <input
                value={editFaculty.department || ''}
                onChange={(e) => setEditFaculty({ ...editFaculty, department: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditFaculty(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'students' ? (
        <>
          <form
            className="panel"
            onSubmit={(e) => {
              e.preventDefault()
              run(
                () =>
                  createStudent({
                    userId: studentForm.userId,
                    batchId: studentForm.batchId,
                    rollNumber: studentForm.rollNumber,
                  }).then(() => setStudentForm({ userId: '', batchId: '', rollNumber: '' })),
                'Student profile created',
              )
            }}
          >
            <h2>Link student profile</h2>
            <p className="muted">Connect a linked user to a batch with a roll number.</p>
            <div className="row">
              <Field label="User" grow>
                <select
                  value={studentForm.userId}
                  onChange={(e) => setStudentForm({ ...studentForm, userId: e.target.value })}
                  required
                >
                  <option value="">Select user</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.displayName || u.email} ({u.email})
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Batch" grow>
                <select
                  value={studentForm.batchId}
                  onChange={(e) => setStudentForm({ ...studentForm, batchId: e.target.value })}
                  required
                >
                  <option value="">Select batch</option>
                  {batchOpts.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Roll">
                <input
                  value={studentForm.rollNumber}
                  onChange={(e) => setStudentForm({ ...studentForm, rollNumber: e.target.value })}
                  required
                />
              </Field>
              <button className="btn" type="submit">
                Link
              </button>
            </div>
          </form>

          {editStudent ? (
            <form
              className="panel"
              onSubmit={(e) => {
                e.preventDefault()
                run(
                  () =>
                    updateStudent(editStudent.id, {
                      cgpa: Number(editStudent.cgpa),
                      backlogCount: Number(editStudent.backlogCount),
                      barredFromExams: Boolean(editStudent.barredFromExams),
                      attendancePercent: Number(editStudent.attendancePercent),
                    }).then(() => setEditStudent(null)),
                  'Student updated',
                )
              }}
            >
              <h2>Update {editStudent.rollNumber}</h2>
              <div className="row">
                <Field label="CGPA">
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="10"
                    value={editStudent.cgpa}
                    onChange={(e) => setEditStudent({ ...editStudent, cgpa: e.target.value })}
                  />
                </Field>
                <Field label="Backlogs">
                  <input
                    type="number"
                    min="0"
                    value={editStudent.backlogCount}
                    onChange={(e) => setEditStudent({ ...editStudent, backlogCount: e.target.value })}
                  />
                </Field>
                <Field label="Attendance %">
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    value={editStudent.attendancePercent}
                    onChange={(e) => setEditStudent({ ...editStudent, attendancePercent: e.target.value })}
                  />
                </Field>
                <label className="row" style={{ alignItems: 'center', gap: '0.4rem' }}>
                  <input
                    type="checkbox"
                    checked={Boolean(editStudent.barredFromExams)}
                    onChange={(e) => setEditStudent({ ...editStudent, barredFromExams: e.target.checked })}
                  />
                  Barred from exams
                </label>
                <button className="btn" type="submit">
                  Save
                </button>
                <button className="btn btn-ghost" type="button" onClick={() => setEditStudent(null)}>
                  Cancel
                </button>
              </div>
            </form>
          ) : null}
        </>
      ) : null}

      {canManage && tab === 'offerings' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createOffering({
                  courseId: offeringForm.courseId,
                  facultyId: offeringForm.facultyId,
                  academicYear: offeringForm.academicYear,
                  semesterNumber: Number(offeringForm.semesterNumber),
                }).then(() =>
                  setOfferingForm({ courseId: '', facultyId: '', academicYear: '2025-26', semesterNumber: 1 }),
                ),
              'Offering created',
            )
          }}
        >
          <h2>New course offering</h2>
          <div className="row">
            <Field label="Course" grow>
              <select
                value={offeringForm.courseId}
                onChange={(e) => setOfferingForm({ ...offeringForm, courseId: e.target.value })}
                required
              >
                <option value="">Select course</option>
                {courseOpts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.code} — {c.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Faculty" grow>
              <select
                value={offeringForm.facultyId}
                onChange={(e) => setOfferingForm({ ...offeringForm, facultyId: e.target.value })}
                required
              >
                <option value="">Select faculty</option>
                {facultyOpts.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.employeeCode} {f.department ? `(${f.department})` : ''}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Year">
              <input
                value={offeringForm.academicYear}
                onChange={(e) => setOfferingForm({ ...offeringForm, academicYear: e.target.value })}
                required
              />
            </Field>
            <Field label="Sem">
              <input
                type="number"
                min="1"
                value={offeringForm.semesterNumber}
                onChange={(e) => setOfferingForm({ ...offeringForm, semesterNumber: e.target.value })}
              />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'enrollments' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createEnrollment({
                  studentId: enrollForm.studentId,
                  courseOfferingId: enrollForm.courseOfferingId,
                }).then(() => setEnrollForm({ studentId: '', courseOfferingId: '' })),
              'Enrollment created',
            )
          }}
        >
          <h2>Enroll student</h2>
          <div className="row">
            <Field label="Student" grow>
              <select
                value={enrollForm.studentId}
                onChange={(e) => setEnrollForm({ ...enrollForm, studentId: e.target.value })}
                required
              >
                <option value="">Select student</option>
                {studentOpts.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.rollNumber}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Offering" grow>
              <select
                value={enrollForm.courseOfferingId}
                onChange={(e) => setEnrollForm({ ...enrollForm, courseOfferingId: e.target.value })}
                required
              >
                <option value="">Select offering</option>
                {offeringOpts.map((o) => (
                  <option key={o.id} value={o.id}>
                    {labelById(courseOpts, o.courseId, 'code')} · {o.academicYear} S{o.semesterNumber}
                  </option>
                ))}
              </select>
            </Field>
            <button className="btn" type="submit">
              Enroll
            </button>
          </div>
        </form>
      ) : null}

      {tab === 'programs' ? (
        <SimpleTable
          rows={programs}
          cols={[
            ['code', 'Code'],
            ['name', 'Name'],
            ['degreeType', 'Degree'],
            ['durationYears', 'Years'],
          ]}
          actions={
            canManage
              ? (row) => (
                  <div className="row">
                    <button className="btn btn-ghost" type="button" onClick={() => setEditProgram({ ...row })}>
                      Edit
                    </button>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Delete program ${row.code}?`)) {
                          run(() => deleteProgram(row.id), 'Program deleted')
                        }
                      }}
                    >
                      Delete
                    </button>
                  </div>
                )
              : null
          }
        />
      ) : null}
      {tab === 'branches' ? (
        <SimpleTable
          rows={branches}
          cols={[
            ['code', 'Code'],
            ['name', 'Name'],
            ['programId', 'Program', (v) => labelById(programOpts, v, 'code')],
          ]}
          actions={
            canManage
              ? (row) => (
                  <div className="row">
                    <button className="btn btn-ghost" type="button" onClick={() => setEditBranch({ ...row })}>
                      Edit
                    </button>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Delete branch ${row.code}?`)) {
                          run(() => deleteBranch(row.id), 'Branch deleted')
                        }
                      }}
                    >
                      Delete
                    </button>
                  </div>
                )
              : null
          }
        />
      ) : null}
      {tab === 'batches' ? (
        <SimpleTable
          rows={batches}
          cols={[
            ['code', 'Code'],
            ['admissionYear', 'Admission'],
            ['graduationYear', 'Graduation'],
            ['branchId', 'Branch', (v) => labelById(branchOpts, v, 'code')],
          ]}
          actions={
            canManage
              ? (row) => (
                  <div className="row">
                    <button className="btn btn-ghost" type="button" onClick={() => setEditBatch({ ...row })}>
                      Edit
                    </button>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Delete batch ${row.code}?`)) {
                          run(() => deleteBatch(row.id), 'Batch deleted')
                        }
                      }}
                    >
                      Delete
                    </button>
                  </div>
                )
              : null
          }
        />
      ) : null}
      {tab === 'courses' ? (
        <SimpleTable
          rows={courses}
          cols={[
            ['code', 'Code'],
            ['name', 'Name'],
            ['credits', 'Credits'],
            ['semesterNumber', 'Sem'],
          ]}
          actions={
            canManage
              ? (row) => (
                  <div className="row">
                    <button className="btn btn-ghost" type="button" onClick={() => setEditCourse({ ...row })}>
                      Edit
                    </button>
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => {
                        if (window.confirm(`Delete course ${row.code}?`)) {
                          run(() => deleteCourse(row.id), 'Course deleted')
                        }
                      }}
                    >
                      Delete
                    </button>
                  </div>
                )
              : null
          }
        />
      ) : null}
      {tab === 'students' ? (
        <SimpleTable
          rows={students}
          cols={[
            ['rollNumber', 'Roll'],
            ['cgpa', 'CGPA'],
            ['backlogCount', 'Backlogs'],
            ['attendancePercent', 'Attendance'],
            ['barredFromExams', 'Barred', (v) => (v ? 'Yes' : 'No')],
          ]}
          actions={
            canManage
              ? (row) => (
                  <button className="btn btn-ghost" type="button" onClick={() => setEditStudent({ ...row })}>
                    Edit
                  </button>
                )
              : null
          }
        />
      ) : null}
      {tab === 'faculty' ? (
        <SimpleTable
          rows={faculty}
          cols={[
            ['employeeCode', 'Employee'],
            ['department', 'Department'],
            ['userId', 'User', (v) => labelById(users, v, 'email')],
          ]}
          actions={
            canManage
              ? (row) => (
                  <button className="btn btn-ghost" type="button" onClick={() => setEditFaculty({ ...row })}>
                    Edit
                  </button>
                )
              : null
          }
        />
      ) : null}
      {tab === 'offerings' ? (
        <SimpleTable
          rows={offerings}
          cols={[
            ['academicYear', 'Year'],
            ['semesterNumber', 'Sem'],
            ['courseId', 'Course', (v) => labelById(courseOpts, v, 'code')],
            ['facultyId', 'Faculty', (v) => labelById(facultyOpts, v, 'employeeCode')],
          ]}
          actions={
            canManage
              ? (row) => (
                  <button
                    className="btn btn-ghost"
                    type="button"
                    onClick={() => {
                      if (window.confirm('Delete this offering?')) {
                        run(() => deleteOffering(row.id), 'Offering deleted')
                      }
                    }}
                  >
                    Delete
                  </button>
                )
              : null
          }
        />
      ) : null}
      {tab === 'enrollments' ? (
        <SimpleTable
          rows={enrollments}
          cols={[
            ['studentId', 'Student', (v) => labelById(studentOpts, v, 'rollNumber')],
            ['courseOfferingId', 'Offering'],
            ['status', 'Status'],
          ]}
          actions={
            canManage
              ? (row) =>
                  row.status === 'ENROLLED' ? (
                    <button
                      className="btn btn-ghost"
                      type="button"
                      onClick={() => run(() => dropEnrollment(row.id), 'Enrollment dropped')}
                    >
                      Drop
                    </button>
                  ) : null
              : null
          }
        />
      ) : null}
      {!isStudent || tab === 'programs' || tab === 'courses' ? (
        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
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

function SimpleTable({ rows, cols, actions }) {
  return (
    <div className="table-wrap">
      <table className="data">
        <thead>
          <tr>
            {cols.map(([k, label]) => (
              <th key={k}>{label}</th>
            ))}
            {actions ? <th /> : null}
          </tr>
        </thead>
        <tbody>
          {(rows || []).map((row) => (
            <tr key={row.id || JSON.stringify(row)}>
              {cols.map(([k, , fmt]) => (
                <td key={k}>{fmt ? fmt(row[k], row) : String(row[k] ?? '')}</td>
              ))}
              {actions ? <td>{actions(row)}</td> : null}
            </tr>
          ))}
        </tbody>
      </table>
      {!rows?.length ? <div className="empty">No records yet. Use the form above to add the first one.</div> : null}
    </div>
  )
}

function friendlyError(e) {
  if (e?.code === 'ERR_NETWORK' || getErrorMessage(e).includes('Network Error')) {
    return 'API unreachable. Ensure backend is running on http://localhost:8080 and restart the Vite dev server after .env changes.'
  }
  return getErrorMessage(e)
}
