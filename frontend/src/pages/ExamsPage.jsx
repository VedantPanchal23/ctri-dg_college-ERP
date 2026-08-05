import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ROLES } from '../auth/roles'
import Pagination from '../components/Pagination'
import {
  allocateSeats,
  createExamSchedule,
  createExamSession,
  decideRevaluation,
  deleteExamSchedule,
  deleteExamSession,
  enterMarks,
  generateHallTickets,
  listExamSchedules,
  listExamSessions,
  listHallTickets,
  listMarks,
  listOfferings,
  listRevaluations,
  listSeats,
  listStudents,
  lockMarks,
  myHallTickets,
  myPublishedMarks,
  publishGrades,
  requestRevaluation,
  updateExamSchedule,
  updateExamSession,
} from '../api/endpoints'
import { getErrorMessage, softGet, listAll } from '../api/client'

export default function ExamsPage() {
  const { can } = useAuth()
  const isStudent =
    can([ROLES.STUDENT]) && !can([ROLES.EXAM_CONTROLLER, ROLES.TENANT_ADMIN, ROLES.FACULTY, ROLES.HOD])
  const canManage = can([ROLES.EXAM_CONTROLLER, ROLES.TENANT_ADMIN])
  const canStaff = can([ROLES.EXAM_CONTROLLER, ROLES.TENANT_ADMIN, ROLES.FACULTY, ROLES.HOD])
  const canMarks = can([ROLES.EXAM_CONTROLLER, ROLES.TENANT_ADMIN, ROLES.FACULTY])

  const PAGE_SIZE = 20

  const [tab, setTab] = useState(isStudent ? 'tickets' : 'sessions')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [error, setError] = useState('')
  const [ok, setOk] = useState('')
  const [sessions, setSessions] = useState([])
  const [schedules, setSchedules] = useState([])
  const [sessionOpts, setSessionOpts] = useState([])
  const [scheduleOpts, setScheduleOpts] = useState([])
  const [offerings, setOfferings] = useState([])
  const [students, setStudents] = useState([])
  const [tickets, setTickets] = useState([])
  const [marks, setMarks] = useState([])
  const [myGrades, setMyGrades] = useState([])
  const [printTicket, setPrintTicket] = useState(null)
  const [revals, setRevals] = useState([])
  const [seatList, setSeatList] = useState([])
  const [ticketList, setTicketList] = useState([])
  const [selectedSchedule, setSelectedSchedule] = useState('')
  const [detailSchedule, setDetailSchedule] = useState('')
  const [markForm, setMarkForm] = useState({ studentId: '', marksObtained: '' })
  const [revalReason, setRevalReason] = useState('')
  const [editSession, setEditSession] = useState(null)
  const [editSchedule, setEditSchedule] = useState(null)
  const [sessionForm, setSessionForm] = useState({
    name: '',
    sessionType: 'END_TERM',
    academicYear: '2025-26',
    semesterNumber: 1,
    startDate: '2026-10-01',
    endDate: '2026-10-15',
    minAttendancePercent: 75,
  })
  const [scheduleForm, setScheduleForm] = useState({
    examSessionId: '',
    courseOfferingId: '',
    examDatetime: '2026-10-05T10:00',
    durationMinutes: 180,
    venue: '',
    maxMarks: 100,
  })
  const [rooms, setRooms] = useState('Hall A:40,Hall B:40')

  async function load() {
    setError('')
    try {
      if (isStudent) {
        setTickets((await softGet(myHallTickets())) || [])
        setScheduleOpts((await softGet(listAll(listExamSchedules))) || [])
        setMyGrades((await softGet(myPublishedMarks())) || [])
        setTotalPages(1)
        return
      }
      if (canStaff) {
        const [sessOpts, schedOpts, offOpts, studOpts] = await Promise.all([
          listAll(listExamSessions),
          listAll(listExamSchedules),
          softGet(listAll(listOfferings)),
          softGet(listAll(listStudents)),
        ])
        setSessionOpts(sessOpts || [])
        setScheduleOpts(schedOpts || [])
        setOfferings(offOpts || [])
        setStudents(studOpts || [])

        let result = { content: [], totalPages: 1 }
        if (tab === 'sessions') result = await listExamSessions(page, PAGE_SIZE)
        else if (tab === 'schedules') result = await listExamSchedules(page, PAGE_SIZE)

        if (tab === 'sessions') setSessions(result.content || [])
        if (tab === 'schedules') setSchedules(result.content || [])
        setTotalPages(['sessions', 'schedules'].includes(tab) ? result.totalPages || 1 : 1)
      }
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

  async function loadMarks(id) {
    setSelectedSchedule(id)
    if (!id) {
      setMarks([])
      return
    }
    try {
      setMarks(await listMarks(id))
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  async function loadDetails(id) {
    setDetailSchedule(id)
    if (!id) {
      setSeatList([])
      setTicketList([])
      setRevals([])
      return
    }
    try {
      setTicketList((await softGet(listHallTickets(id))) || [])
      setSeatList((await softGet(listSeats(id))) || [])
      if (canManage) setRevals((await softGet(listRevaluations(id))) || [])
    } catch (e) {
      setError(friendlyError(e))
    }
  }

  return (
    <div className="stack">
      <div className="page-head">
        <div>
          <h1>Exams</h1>
          <p className="muted">
            {isStudent
              ? 'Hall tickets and revaluation requests.'
              : 'Sessions, schedules, tickets, seats, marks, and revaluation decisions.'}
          </p>
        </div>
      </div>
      {error ? <div className="alert">{error}</div> : null}
      {ok ? <div className="alert ok">{ok}</div> : null}

      <div className="row">
        {isStudent ? (
          <>
            <button type="button" className={`btn ${tab === 'tickets' ? '' : 'btn-ghost'}`} onClick={() => setTab('tickets')}>
              Hall tickets
            </button>
            <button type="button" className={`btn ${tab === 'grades' ? '' : 'btn-ghost'}`} onClick={() => setTab('grades')}>
              My grades
            </button>
            <button type="button" className={`btn ${tab === 'reval' ? '' : 'btn-ghost'}`} onClick={() => setTab('reval')}>
              Revaluation
            </button>
          </>
        ) : (
          <>
            <button type="button" className={`btn ${tab === 'sessions' ? '' : 'btn-ghost'}`} onClick={() => setTab('sessions')}>
              Sessions
            </button>
            <button type="button" className={`btn ${tab === 'schedules' ? '' : 'btn-ghost'}`} onClick={() => setTab('schedules')}>
              Schedules
            </button>
            {canMarks ? (
              <button type="button" className={`btn ${tab === 'marks' ? '' : 'btn-ghost'}`} onClick={() => setTab('marks')}>
                Marks
              </button>
            ) : null}
            {canManage ? (
              <button
                type="button"
                className={`btn ${tab === 'details' ? '' : 'btn-ghost'}`}
                onClick={() => setTab('details')}
              >
                Tickets / seats / reval
              </button>
            ) : null}
          </>
        )}
      </div>

      {canManage && tab === 'sessions' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createExamSession({
                  ...sessionForm,
                  semesterNumber: Number(sessionForm.semesterNumber),
                  minAttendancePercent: Number(sessionForm.minAttendancePercent),
                }),
              'Exam session created',
            )
          }}
        >
          <h2>New exam session</h2>
          <div className="row">
            <Field label="Name" grow>
              <input value={sessionForm.name} onChange={(e) => setSessionForm({ ...sessionForm, name: e.target.value })} required />
            </Field>
            <Field label="Type">
              <select value={sessionForm.sessionType} onChange={(e) => setSessionForm({ ...sessionForm, sessionType: e.target.value })}>
                <option value="MID_TERM">MID_TERM</option>
                <option value="END_TERM">END_TERM</option>
                <option value="SUPPLEMENTARY">SUPPLEMENTARY</option>
              </select>
            </Field>
            <Field label="Year">
              <input value={sessionForm.academicYear} onChange={(e) => setSessionForm({ ...sessionForm, academicYear: e.target.value })} required />
            </Field>
            <Field label="Sem">
              <input type="number" min="1" value={sessionForm.semesterNumber} onChange={(e) => setSessionForm({ ...sessionForm, semesterNumber: e.target.value })} />
            </Field>
            <Field label="Start">
              <input type="date" value={sessionForm.startDate} onChange={(e) => setSessionForm({ ...sessionForm, startDate: e.target.value })} required />
            </Field>
            <Field label="End">
              <input type="date" value={sessionForm.endDate} onChange={(e) => setSessionForm({ ...sessionForm, endDate: e.target.value })} required />
            </Field>
            <Field label="Min att %">
              <input type="number" value={sessionForm.minAttendancePercent} onChange={(e) => setSessionForm({ ...sessionForm, minAttendancePercent: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Create
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'sessions' && editSession ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateExamSession(editSession.id, {
                  name: editSession.name,
                  sessionType: editSession.sessionType,
                  academicYear: editSession.academicYear,
                  semesterNumber: Number(editSession.semesterNumber),
                  startDate: editSession.startDate,
                  endDate: editSession.endDate,
                  minAttendancePercent: Number(editSession.minAttendancePercent),
                }).then(() => setEditSession(null)),
              'Exam session updated',
            )
          }}
        >
          <h2>Edit session {editSession.name}</h2>
          <div className="row">
            <Field label="Name" grow>
              <input value={editSession.name} onChange={(e) => setEditSession({ ...editSession, name: e.target.value })} required />
            </Field>
            <Field label="Type">
              <select value={editSession.sessionType} onChange={(e) => setEditSession({ ...editSession, sessionType: e.target.value })}>
                <option value="MID_TERM">MID_TERM</option>
                <option value="END_TERM">END_TERM</option>
                <option value="SUPPLEMENTARY">SUPPLEMENTARY</option>
              </select>
            </Field>
            <Field label="Year">
              <input value={editSession.academicYear} onChange={(e) => setEditSession({ ...editSession, academicYear: e.target.value })} required />
            </Field>
            <Field label="Sem">
              <input type="number" min="1" value={editSession.semesterNumber} onChange={(e) => setEditSession({ ...editSession, semesterNumber: e.target.value })} />
            </Field>
            <Field label="Start">
              <input type="date" value={editSession.startDate} onChange={(e) => setEditSession({ ...editSession, startDate: e.target.value })} required />
            </Field>
            <Field label="End">
              <input type="date" value={editSession.endDate} onChange={(e) => setEditSession({ ...editSession, endDate: e.target.value })} required />
            </Field>
            <Field label="Min att %">
              <input type="number" value={editSession.minAttendancePercent} onChange={(e) => setEditSession({ ...editSession, minAttendancePercent: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditSession(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'schedules' ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                createExamSchedule({
                  examSessionId: scheduleForm.examSessionId,
                  courseOfferingId: scheduleForm.courseOfferingId,
                  examDatetime: new Date(scheduleForm.examDatetime).toISOString(),
                  durationMinutes: Number(scheduleForm.durationMinutes),
                  venue: scheduleForm.venue,
                  maxMarks: Number(scheduleForm.maxMarks),
                }),
              'Schedule created',
            )
          }}
        >
          <h2>New exam schedule</h2>
          <div className="row">
            <Field label="Session" grow>
              <select value={scheduleForm.examSessionId} onChange={(e) => setScheduleForm({ ...scheduleForm, examSessionId: e.target.value })} required>
                <option value="">Select session</option>
                {sessionOpts.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Offering" grow>
              <select value={scheduleForm.courseOfferingId} onChange={(e) => setScheduleForm({ ...scheduleForm, courseOfferingId: e.target.value })} required>
                <option value="">Select offering</option>
                {offerings.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.academicYear} S{o.semesterNumber} · {String(o.courseId).slice(0, 8)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="When">
              <input type="datetime-local" value={scheduleForm.examDatetime} onChange={(e) => setScheduleForm({ ...scheduleForm, examDatetime: e.target.value })} required />
            </Field>
            <Field label="Minutes">
              <input type="number" min="1" value={scheduleForm.durationMinutes} onChange={(e) => setScheduleForm({ ...scheduleForm, durationMinutes: e.target.value })} />
            </Field>
            <Field label="Venue">
              <input value={scheduleForm.venue} onChange={(e) => setScheduleForm({ ...scheduleForm, venue: e.target.value })} required />
            </Field>
            <Field label="Max marks">
              <input type="number" min="1" value={scheduleForm.maxMarks} onChange={(e) => setScheduleForm({ ...scheduleForm, maxMarks: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Add
            </button>
          </div>
        </form>
      ) : null}

      {canManage && tab === 'schedules' && editSchedule ? (
        <form
          className="panel"
          onSubmit={(e) => {
            e.preventDefault()
            run(
              () =>
                updateExamSchedule(editSchedule.id, {
                  examSessionId: editSchedule.examSessionId,
                  courseOfferingId: editSchedule.courseOfferingId,
                  examDatetime: new Date(editSchedule.examDatetime).toISOString(),
                  durationMinutes: Number(editSchedule.durationMinutes),
                  venue: editSchedule.venue,
                  maxMarks: Number(editSchedule.maxMarks),
                }).then(() => setEditSchedule(null)),
              'Schedule updated',
            )
          }}
        >
          <h2>Edit schedule {editSchedule.venue}</h2>
          <div className="row">
            <Field label="Session" grow>
              <select value={editSchedule.examSessionId} onChange={(e) => setEditSchedule({ ...editSchedule, examSessionId: e.target.value })} required>
                <option value="">Select session</option>
                {sessionOpts.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Offering" grow>
              <select value={editSchedule.courseOfferingId} onChange={(e) => setEditSchedule({ ...editSchedule, courseOfferingId: e.target.value })} required>
                <option value="">Select offering</option>
                {offerings.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.academicYear} S{o.semesterNumber} · {String(o.courseId).slice(0, 8)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="When">
              <input type="datetime-local" value={editSchedule.examDatetime} onChange={(e) => setEditSchedule({ ...editSchedule, examDatetime: e.target.value })} required />
            </Field>
            <Field label="Minutes">
              <input type="number" min="1" value={editSchedule.durationMinutes} onChange={(e) => setEditSchedule({ ...editSchedule, durationMinutes: e.target.value })} />
            </Field>
            <Field label="Venue">
              <input value={editSchedule.venue} onChange={(e) => setEditSchedule({ ...editSchedule, venue: e.target.value })} required />
            </Field>
            <Field label="Max marks">
              <input type="number" min="1" value={editSchedule.maxMarks} onChange={(e) => setEditSchedule({ ...editSchedule, maxMarks: e.target.value })} />
            </Field>
            <button className="btn" type="submit">
              Save
            </button>
            <button className="btn btn-ghost" type="button" onClick={() => setEditSchedule(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}

      {tab === 'tickets' ? (
        <div className="stack">
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Schedule</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {(tickets || []).map((t) => (
                  <tr key={t.id}>
                    <td>{t.ticketNumber}</td>
                    <td>{String(t.examScheduleId).slice(0, 8)}</td>
                    <td>
                      <span className={`badge ${t.status === 'INELIGIBLE' ? 'danger' : ''}`}>{t.status}</span>
                    </td>
                    <td>{t.eligibilityNotes || '—'}</td>
                    <td>
                      <button className="btn btn-ghost" type="button" onClick={() => setPrintTicket(t)}>
                        View / print
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!tickets?.length ? (
              <div className="empty">No hall tickets yet. Needs a linked student profile and ticket generation.</div>
            ) : null}
          </div>

          {printTicket ? (
            <div className="panel hall-ticket-print">
              <h2>Hall Ticket</h2>
              <div className="detail-list">
                <p>
                  <strong>Ticket number:</strong> {printTicket.ticketNumber}
                </p>
                <p>
                  <strong>Status:</strong> {printTicket.status}
                </p>
                <p>
                  <strong>Schedule:</strong> {describeSchedule(scheduleOpts, printTicket.examScheduleId)}
                </p>
                {printTicket.eligibilityNotes ? (
                  <p>
                    <strong>Notes:</strong> {printTicket.eligibilityNotes}
                  </p>
                ) : null}
              </div>
              <div className="row no-print">
                <button className="btn" type="button" onClick={() => window.print()}>
                  Print
                </button>
                <button className="btn btn-ghost" type="button" onClick={() => setPrintTicket(null)}>
                  Close
                </button>
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      {isStudent && tab === 'grades' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Schedule</th>
                <th>Marks</th>
                <th>Grade</th>
              </tr>
            </thead>
            <tbody>
              {(myGrades || []).map((m) => (
                <tr key={m.id}>
                  <td>{describeSchedule(scheduleOpts, m.examScheduleId)}</td>
                  <td>{m.marksObtained}</td>
                  <td>{m.grade || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!myGrades?.length ? <div className="empty">No published grades yet.</div> : null}
        </div>
      ) : null}

      {isStudent && tab === 'reval' ? (
        <div className="stack">
          <form
            className="panel"
            onSubmit={(e) => {
              e.preventDefault()
              run(() => requestRevaluation(selectedSchedule, revalReason).then(() => setRevalReason('')), 'Revaluation requested')
            }}
          >
            <h2>Request revaluation</h2>
            <div className="row">
              <Field label="Schedule" grow>
                <select value={selectedSchedule} onChange={(e) => setSelectedSchedule(e.target.value)} required>
                  <option value="">Select schedule</option>
                  {scheduleOpts.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.venue} · {formatWhen(s.examDatetime)}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Reason" grow>
                <input value={revalReason} onChange={(e) => setRevalReason(e.target.value)} required />
              </Field>
              <button className="btn" type="submit">
                Submit
              </button>
            </div>
          </form>
        </div>
      ) : null}

      {tab === 'sessions' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Year</th>
                <th>Status</th>
                <th>Dates</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{s.sessionType}</td>
                  <td>{s.academicYear}</td>
                  <td>
                    <span className="badge">{s.status}</span>
                  </td>
                  <td>
                    {s.startDate} → {s.endDate}
                  </td>
                  <td>
                    {canManage ? (
                      <div className="row">
                        <button className="btn btn-ghost" type="button" onClick={() => setEditSession({ ...s })}>
                          Edit
                        </button>
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => {
                            if (window.confirm('Delete session?')) run(() => deleteExamSession(s.id), 'Session deleted')
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
          {!sessions.length ? <div className="empty">No sessions yet.</div> : null}
        </div>
      ) : null}
      {!isStudent && tab === 'sessions' ? (
        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      ) : null}

      {tab === 'schedules' ? (
        <div className="table-wrap">
          <table className="data">
            <thead>
              <tr>
                <th>Venue</th>
                <th>When</th>
                <th>Max</th>
                <th>Locked</th>
                <th>Published</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {schedules.map((s) => (
                <tr key={s.id}>
                  <td>{s.venue}</td>
                  <td>{formatWhen(s.examDatetime)}</td>
                  <td>{s.maxMarks}</td>
                  <td>{s.marksLocked ? 'Yes' : 'No'}</td>
                  <td>{s.gradesPublished ? 'Yes' : 'No'}</td>
                  <td>
                    {canManage ? (
                      <div className="row">
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => setEditSchedule({ ...s, examDatetime: toDatetimeLocal(s.examDatetime) })}
                        >
                          Edit
                        </button>
                        <button className="btn btn-ghost" type="button" onClick={() => run(() => generateHallTickets(s.id), 'Tickets generated')}>
                          Tickets
                        </button>
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => run(() => allocateSeats(s.id, parseRooms(rooms)), 'Seats allocated')}
                        >
                          Seats
                        </button>
                        {!s.marksLocked ? (
                          <button className="btn btn-ghost" type="button" onClick={() => run(() => lockMarks(s.id), 'Marks locked')}>
                            Lock
                          </button>
                        ) : null}
                        {s.marksLocked && !s.gradesPublished ? (
                          <button className="btn btn-ghost" type="button" onClick={() => run(() => publishGrades(s.id), 'Grades published')}>
                            Publish
                          </button>
                        ) : null}
                        <button
                          className="btn btn-ghost"
                          type="button"
                          onClick={() => {
                            if (window.confirm('Delete schedule?')) run(() => deleteExamSchedule(s.id), 'Schedule deleted')
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
          {canManage ? (
            <div className="panel" style={{ marginTop: '0.75rem' }}>
              <label className="muted">Seat rooms (name:capacity, comma-separated)</label>
              <input value={rooms} onChange={(e) => setRooms(e.target.value)} style={{ width: '100%', marginTop: '0.35rem' }} />
            </div>
          ) : null}
          {!schedules.length ? <div className="empty">No schedules yet.</div> : null}
        </div>
      ) : null}
      {!isStudent && tab === 'schedules' ? (
        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      ) : null}

      {tab === 'marks' ? (
        <div className="stack">
          <div className="field">
            <label>Schedule</label>
            <select value={selectedSchedule} onChange={(e) => loadMarks(e.target.value)}>
              <option value="">Select schedule</option>
              {scheduleOpts.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.venue} · {formatWhen(s.examDatetime)}
                </option>
              ))}
            </select>
          </div>
          {canMarks && selectedSchedule ? (
            <form
              className="panel"
              onSubmit={(e) => {
                e.preventDefault()
                run(
                  () =>
                    enterMarks(selectedSchedule, {
                      studentId: markForm.studentId,
                      marksObtained: Number(markForm.marksObtained),
                    }).then(() => {
                      setMarkForm({ studentId: '', marksObtained: '' })
                      return loadMarks(selectedSchedule)
                    }),
                  'Marks saved',
                )
              }}
            >
              <h2>Enter marks</h2>
              <div className="row">
                <Field label="Student" grow>
                  <select value={markForm.studentId} onChange={(e) => setMarkForm({ ...markForm, studentId: e.target.value })} required>
                    <option value="">Select student</option>
                    {students.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.rollNumber} (CGPA {s.cgpa})
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Marks">
                  <input type="number" step="0.01" value={markForm.marksObtained} onChange={(e) => setMarkForm({ ...markForm, marksObtained: e.target.value })} required />
                </Field>
                <button className="btn" type="submit">
                  Save
                </button>
              </div>
            </form>
          ) : null}
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Marks</th>
                  <th>Grade</th>
                </tr>
              </thead>
              <tbody>
                {marks.map((m) => (
                  <tr key={m.id}>
                    <td>{labelById(students, m.studentId, 'rollNumber')}</td>
                    <td>{m.marksObtained}</td>
                    <td>{m.grade || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {!marks.length ? <div className="empty">Select a schedule to view marks.</div> : null}
          </div>
        </div>
      ) : null}

      {tab === 'details' ? (
        <div className="stack">
          <div className="field">
            <label>Schedule</label>
            <select value={detailSchedule} onChange={(e) => loadDetails(e.target.value)}>
              <option value="">Select schedule</option>
              {scheduleOpts.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.venue} · {formatWhen(s.examDatetime)}
                </option>
              ))}
            </select>
          </div>
          <div className="panel">
            <h2>Hall tickets</h2>
            <div className="table-wrap">
              <table className="data">
                <thead>
                  <tr>
                    <th>Ticket</th>
                    <th>Student</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {ticketList.map((t) => (
                    <tr key={t.id}>
                      <td>{t.ticketNumber}</td>
                      <td>{labelById(students, t.studentId, 'rollNumber')}</td>
                      <td>{t.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!ticketList.length ? <div className="empty">No tickets for this schedule.</div> : null}
            </div>
          </div>
          <div className="panel">
            <h2>Seats</h2>
            <div className="table-wrap">
              <table className="data">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Room</th>
                    <th>Seat</th>
                  </tr>
                </thead>
                <tbody>
                  {seatList.map((s) => (
                    <tr key={s.id}>
                      <td>{labelById(students, s.studentId, 'rollNumber')}</td>
                      <td>{s.roomCode}</td>
                      <td>{s.seatNumber}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!seatList.length ? <div className="empty">No seats allocated.</div> : null}
            </div>
          </div>
          <div className="panel">
            <h2>Revaluation requests</h2>
            <div className="table-wrap">
              <table className="data">
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {revals.map((r) => (
                    <tr key={r.id}>
                      <td>{labelById(students, r.studentId, 'rollNumber')}</td>
                      <td>{r.reason}</td>
                      <td>{r.status}</td>
                      <td>
                        {r.status === 'PENDING' ? (
                          <div className="row">
                            <button
                              className="btn btn-ghost"
                              type="button"
                              onClick={() => {
                                const revised = window.prompt('Revised marks (required to approve)')
                                if (revised == null || revised === '') return
                                run(
                                  () =>
                                    decideRevaluation(r.id, {
                                      status: 'APPROVED',
                                      decisionNotes: 'Approved',
                                      revisedMarks: Number(revised),
                                    }).then(() => loadDetails(detailSchedule)),
                                  'Approved',
                                )
                              }}
                            >
                              Approve
                            </button>
                            <button
                              className="btn btn-ghost"
                              type="button"
                              onClick={() =>
                                run(
                                  () =>
                                    decideRevaluation(r.id, {
                                      status: 'REJECTED',
                                      decisionNotes: 'Rejected',
                                      revisedMarks: null,
                                    }).then(() => loadDetails(detailSchedule)),
                                  'Rejected',
                                )
                              }
                            >
                              Reject
                            </button>
                          </div>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {!revals.length ? <div className="empty">No revaluation requests.</div> : null}
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

function formatWhen(value) {
  if (!value) return '—'
  try {
    return new Date(value).toLocaleString()
  } catch {
    return value
  }
}

function toDatetimeLocal(value) {
  if (!value) return ''
  try {
    return new Date(value).toISOString().slice(0, 16)
  } catch {
    return ''
  }
}

function describeSchedule(schedules, id) {
  const s = (schedules || []).find((row) => row.id === id)
  if (!s) return String(id || '').slice(0, 8)
  return `${s.venue} · ${formatWhen(s.examDatetime)}`
}

function parseRooms(text) {
  return text
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const [name, cap] = part.split(':')
      return { roomCode: name.trim(), capacity: Number(cap) }
    })
}

function friendlyError(e) {
  if (e?.code === 'ERR_NETWORK' || getErrorMessage(e).includes('Network Error')) {
    return 'API unreachable. Ensure backend is running on http://localhost:8080.'
  }
  return getErrorMessage(e)
}
