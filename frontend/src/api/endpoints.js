import { api } from './client'

export const fetchMe = () => api.get('/api/v1/users/me').then((r) => r.data)
export const fetchTenantMe = () => api.get('/api/v1/tenants/me').then((r) => r.data)
export const updateTenantMe = (body) => api.put('/api/v1/tenants/me', body).then((r) => r.data)
export const bootstrapAcademic = (body) => api.post('/api/v1/tenants/me/bootstrap', body).then((r) => r.data)

export const listTenants = (page = 0, size = 20) =>
  api.get('/api/v1/platform/tenants', { params: { page, size } }).then((r) => r.data)
export const getTenant = (id) => api.get(`/api/v1/platform/tenants/${id}`).then((r) => r.data)
export const createTenant = (body) => api.post('/api/v1/platform/tenants', body).then((r) => r.data)
export const updateTenant = (id, body) => api.put(`/api/v1/platform/tenants/${id}`, body).then((r) => r.data)
export const suspendTenant = (id) => api.post(`/api/v1/platform/tenants/${id}/suspend`).then((r) => r.data)
export const activateTenant = (id) => api.post(`/api/v1/platform/tenants/${id}/activate`).then((r) => r.data)
export const deleteTenant = (id) => api.delete(`/api/v1/platform/tenants/${id}`).then((r) => r.data)

export const listUsers = (page = 0, size = 20) =>
  api.get('/api/v1/users', { params: { page, size } }).then((r) => r.data)
export const getUser = (id) => api.get(`/api/v1/users/${id}`).then((r) => r.data)
export const linkUser = (body) => api.post('/api/v1/users', body).then((r) => r.data)
export const provisionUser = (body) => api.post('/api/v1/users/provision', body).then((r) => r.data)
export const assignUserRoles = (id, roles) =>
  api.put(`/api/v1/users/${id}/roles`, { roles }).then((r) => r.data)
export const disableUser = (id) => api.post(`/api/v1/users/${id}/disable`).then((r) => r.data)
export const enableUser = (id) => api.post(`/api/v1/users/${id}/enable`).then((r) => r.data)
export const resetUserPassword = (id, newPassword, temporary = false) =>
  api.post(`/api/v1/users/${id}/reset-password`, { newPassword, temporary }).then((r) => r.data)
export const linkUserCompany = (id, companyId) =>
  api.put(`/api/v1/users/${id}/company`, { companyId }).then((r) => r.data)

export const listAuditLogs = (page = 0, size = 20) =>
  api.get('/api/v1/audit-logs', { params: { page, size } }).then((r) => r.data)

export const listNotifications = (page = 0, size = 20) =>
  api.get('/api/v1/notifications', { params: { page, size } }).then((r) => r.data)
export const unreadNotificationCount = () =>
  api.get('/api/v1/notifications/unread-count').then((r) => r.data)
export const markNotificationRead = (id) =>
  api.post(`/api/v1/notifications/${id}/read`).then((r) => r.data)

export const listPrograms = (page = 0, size = 20) =>
  api.get('/api/v1/academic/programs', { params: { page, size } }).then((r) => r.data)
export const getProgram = (id) => api.get(`/api/v1/academic/programs/${id}`).then((r) => r.data)
export const createProgram = (body) => api.post('/api/v1/academic/programs', body).then((r) => r.data)
export const updateProgram = (id, body) => api.put(`/api/v1/academic/programs/${id}`, body).then((r) => r.data)
export const deleteProgram = (id) => api.delete(`/api/v1/academic/programs/${id}`).then((r) => r.data)
export const listBranches = (page = 0, size = 20) =>
  api.get('/api/v1/academic/branches', { params: { page, size } }).then((r) => r.data)
export const getBranch = (id) => api.get(`/api/v1/academic/branches/${id}`).then((r) => r.data)
export const createBranch = (body) => api.post('/api/v1/academic/branches', body).then((r) => r.data)
export const updateBranch = (id, body) => api.put(`/api/v1/academic/branches/${id}`, body).then((r) => r.data)
export const deleteBranch = (id) => api.delete(`/api/v1/academic/branches/${id}`).then((r) => r.data)
export const listBatches = (page = 0, size = 20) =>
  api.get('/api/v1/academic/batches', { params: { page, size } }).then((r) => r.data)
export const getBatch = (id) => api.get(`/api/v1/academic/batches/${id}`).then((r) => r.data)
export const createBatch = (body) => api.post('/api/v1/academic/batches', body).then((r) => r.data)
export const updateBatch = (id, body) => api.put(`/api/v1/academic/batches/${id}`, body).then((r) => r.data)
export const deleteBatch = (id) => api.delete(`/api/v1/academic/batches/${id}`).then((r) => r.data)
export const listCourses = (page = 0, size = 20) =>
  api.get('/api/v1/academic/courses', { params: { page, size } }).then((r) => r.data)
export const getCourse = (id) => api.get(`/api/v1/academic/courses/${id}`).then((r) => r.data)
export const createCourse = (body) => api.post('/api/v1/academic/courses', body).then((r) => r.data)
export const updateCourse = (id, body) => api.put(`/api/v1/academic/courses/${id}`, body).then((r) => r.data)
export const deleteCourse = (id) => api.delete(`/api/v1/academic/courses/${id}`).then((r) => r.data)
export const listStudents = (page = 0, size = 20) =>
  api.get('/api/v1/academic/students', { params: { page, size } }).then((r) => r.data)
export const getStudent = (id) => api.get(`/api/v1/academic/students/${id}`).then((r) => r.data)
export const createStudent = (body) => api.post('/api/v1/academic/students', body).then((r) => r.data)
export const updateStudent = (id, body) => api.put(`/api/v1/academic/students/${id}`, body).then((r) => r.data)
export const myStudent = () => api.get('/api/v1/academic/students/me').then((r) => r.data)
export const listFaculty = (page = 0, size = 20) =>
  api.get('/api/v1/academic/faculty', { params: { page, size } }).then((r) => r.data)
export const getFaculty = (id) => api.get(`/api/v1/academic/faculty/${id}`).then((r) => r.data)
export const createFaculty = (body) => api.post('/api/v1/academic/faculty', body).then((r) => r.data)
export const updateFaculty = (id, body) => api.put(`/api/v1/academic/faculty/${id}`, body).then((r) => r.data)
export const listOfferings = (page = 0, size = 20) =>
  api.get('/api/v1/academic/offerings', { params: { page, size } }).then((r) => r.data)
export const getOffering = (id) => api.get(`/api/v1/academic/offerings/${id}`).then((r) => r.data)
export const createOffering = (body) => api.post('/api/v1/academic/offerings', body).then((r) => r.data)
export const deleteOffering = (id) => api.delete(`/api/v1/academic/offerings/${id}`).then((r) => r.data)
export const listEnrollments = (page = 0, size = 20) =>
  api.get('/api/v1/academic/enrollments', { params: { page, size } }).then((r) => r.data)
export const createEnrollment = (body) => api.post('/api/v1/academic/enrollments', body).then((r) => r.data)
export const dropEnrollment = (id) => api.post(`/api/v1/academic/enrollments/${id}/drop`).then((r) => r.data)
export const deleteEnrollment = (id) => api.delete(`/api/v1/academic/enrollments/${id}`).then((r) => r.data)

export const listExamSessions = (page = 0, size = 20) =>
  api.get('/api/v1/exams/sessions', { params: { page, size } }).then((r) => r.data)
export const getExamSession = (id) => api.get(`/api/v1/exams/sessions/${id}`).then((r) => r.data)
export const createExamSession = (body) => api.post('/api/v1/exams/sessions', body).then((r) => r.data)
export const updateExamSession = (id, body) => api.put(`/api/v1/exams/sessions/${id}`, body).then((r) => r.data)
export const deleteExamSession = (id) => api.delete(`/api/v1/exams/sessions/${id}`).then((r) => r.data)
export const listExamSchedules = (page = 0, size = 20) =>
  api.get('/api/v1/exams/schedules', { params: { page, size } }).then((r) => r.data)
export const getExamSchedule = (id) => api.get(`/api/v1/exams/schedules/${id}`).then((r) => r.data)
export const createExamSchedule = (body) => api.post('/api/v1/exams/schedules', body).then((r) => r.data)
export const updateExamSchedule = (id, body) => api.put(`/api/v1/exams/schedules/${id}`, body).then((r) => r.data)
export const deleteExamSchedule = (id) => api.delete(`/api/v1/exams/schedules/${id}`).then((r) => r.data)
export const generateHallTickets = (id) =>
  api.post(`/api/v1/exams/schedules/${id}/hall-tickets/generate`).then((r) => r.data)
export const listHallTickets = (id) => api.get(`/api/v1/exams/schedules/${id}/hall-tickets`).then((r) => r.data)
export const allocateSeats = (id, rooms) =>
  api.post(`/api/v1/exams/schedules/${id}/seats/allocate`, { rooms }).then((r) => r.data)
export const listSeats = (id) => api.get(`/api/v1/exams/schedules/${id}/seats`).then((r) => r.data)
export const myHallTickets = () => api.get('/api/v1/exams/hall-tickets/me').then((r) => r.data)
export const listMarks = (scheduleId) =>
  api.get(`/api/v1/exams/schedules/${scheduleId}/marks`).then((r) => r.data)
export const enterMarks = (scheduleId, body) =>
  api.post(`/api/v1/exams/schedules/${scheduleId}/marks`, body).then((r) => r.data)
export const myPublishedMarks = () => api.get('/api/v1/exams/marks/me').then((r) => r.data)
export const lockMarks = (id) => api.post(`/api/v1/exams/schedules/${id}/marks/lock`).then((r) => r.data)
export const publishGrades = (id) => api.post(`/api/v1/exams/schedules/${id}/grades/publish`).then((r) => r.data)
export const requestRevaluation = (scheduleId, reason) =>
  api.post(`/api/v1/exams/schedules/${scheduleId}/revaluations`, { reason }).then((r) => r.data)
export const listRevaluations = (scheduleId) =>
  api.get(`/api/v1/exams/schedules/${scheduleId}/revaluations`).then((r) => r.data)
export const decideRevaluation = (id, body) =>
  api.put(`/api/v1/exams/revaluations/${id}/decide`, body).then((r) => r.data)

export const listCompanies = (page = 0, size = 20) =>
  api.get('/api/v1/placements/companies', { params: { page, size } }).then((r) => r.data)
export const getCompany = (id) => api.get(`/api/v1/placements/companies/${id}`).then((r) => r.data)
export const createCompany = (body) => api.post('/api/v1/placements/companies', body).then((r) => r.data)
export const updateCompany = (id, body) => api.put(`/api/v1/placements/companies/${id}`, body).then((r) => r.data)
export const deleteCompany = (id) => api.delete(`/api/v1/placements/companies/${id}`).then((r) => r.data)
export const listDrives = (page = 0, size = 20) =>
  api.get('/api/v1/placements/drives', { params: { page, size } }).then((r) => r.data)
export const getDrive = (id) => api.get(`/api/v1/placements/drives/${id}`).then((r) => r.data)
export const createDrive = (body) => api.post('/api/v1/placements/drives', body).then((r) => r.data)
export const updateDrive = (id, body) => api.put(`/api/v1/placements/drives/${id}`, body).then((r) => r.data)
export const deleteDrive = (id) => api.delete(`/api/v1/placements/drives/${id}`).then((r) => r.data)
export const listApplications = (page = 0, size = 20) =>
  api.get('/api/v1/placements/applications', { params: { page, size } }).then((r) => r.data)
export const getApplication = (id) => api.get(`/api/v1/placements/applications/${id}`).then((r) => r.data)
export const updateApplicationStatus = (id, status) =>
  api.put(`/api/v1/placements/applications/${id}/status`, { status }).then((r) => r.data)
export const listRounds = (applicationId) =>
  api.get(`/api/v1/placements/applications/${applicationId}/rounds`).then((r) => r.data)
export const createRound = (applicationId, body) =>
  api.post(`/api/v1/placements/applications/${applicationId}/rounds`, body).then((r) => r.data)
export const updateRound = (id, body) => api.put(`/api/v1/placements/rounds/${id}`, body).then((r) => r.data)
export const issueOffer = (applicationId, body) =>
  api.post(`/api/v1/placements/applications/${applicationId}/offer`, body).then((r) => r.data)
export const getOfferByApplication = (applicationId) =>
  api.get(`/api/v1/placements/applications/${applicationId}/offer`).then((r) => r.data)
export const getOffer = (id) => api.get(`/api/v1/placements/offers/${id}`).then((r) => r.data)
export const acceptOffer = (id) => api.post(`/api/v1/placements/offers/${id}/accept`).then((r) => r.data)
export const declineOffer = (id) => api.post(`/api/v1/placements/offers/${id}/decline`).then((r) => r.data)
export const expireOffer = (id) => api.post(`/api/v1/placements/offers/${id}/expire`).then((r) => r.data)
export const placementStats = () => api.get('/api/v1/placements/stats').then((r) => r.data)
export const applyToDrive = (id) => api.post(`/api/v1/placements/drives/${id}/apply`).then((r) => r.data)
export const openDrive = (id) => api.post(`/api/v1/placements/drives/${id}/open`).then((r) => r.data)
export const closeDrive = (id) => api.post(`/api/v1/placements/drives/${id}/close`).then((r) => r.data)
export const checkEligibility = (id) =>
  api.get(`/api/v1/placements/drives/${id}/eligibility`).then((r) => r.data)
