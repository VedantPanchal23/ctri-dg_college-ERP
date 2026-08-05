import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth, RequireRoles } from './auth/RequireAuth'
import { ROLES } from './auth/roles'
import AppShell from './layout/AppShell'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import ForbiddenPage from './pages/ForbiddenPage'
import TenantsPage from './pages/TenantsPage'
import UsersPage from './pages/UsersPage'
import AcademicPage from './pages/AcademicPage'
import ExamsPage from './pages/ExamsPage'
import PlacementsPage from './pages/PlacementsPage'
import RecruiterPage from './pages/RecruiterPage'
import SettingsPage from './pages/SettingsPage'
import AuditPage from './pages/AuditPage'
import NotificationsPage from './pages/NotificationsPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter
        future={{
          v7_startTransition: true,
          v7_relativeSplatPath: true,
        }}
      >
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/app"
            element={
              <RequireAuth>
                <AppShell />
              </RequireAuth>
            }
          >
            <Route index element={<HomePage />} />
            <Route path="forbidden" element={<ForbiddenPage />} />
            <Route
              path="platform/tenants"
              element={
                <RequireRoles roles={[ROLES.PLATFORM_SUPER_ADMIN]}>
                  <TenantsPage />
                </RequireRoles>
              }
            />
            <Route
              path="users"
              element={
                <RequireRoles
                  roles={[ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN, ROLES.ACADEMIC_ADMIN, ROLES.PLACEMENT_OFFICER]}
                >
                  <UsersPage />
                </RequireRoles>
              }
            />
            <Route
              path="academic"
              element={
                <RequireRoles
                  roles={[
                    ROLES.TENANT_ADMIN,
                    ROLES.ACADEMIC_ADMIN,
                    ROLES.EXAM_CONTROLLER,
                    ROLES.FACULTY,
                    ROLES.HOD,
                    ROLES.PLACEMENT_OFFICER,
                    ROLES.STUDENT,
                  ]}
                >
                  <AcademicPage />
                </RequireRoles>
              }
            />
            <Route
              path="exams"
              element={
                <RequireRoles
                  roles={[ROLES.TENANT_ADMIN, ROLES.EXAM_CONTROLLER, ROLES.FACULTY, ROLES.HOD, ROLES.STUDENT]}
                >
                  <ExamsPage />
                </RequireRoles>
              }
            />
            <Route
              path="placements"
              element={
                <RequireRoles
                  roles={[ROLES.TENANT_ADMIN, ROLES.PLACEMENT_OFFICER, ROLES.RECRUITER, ROLES.STUDENT]}
                >
                  <PlacementsPage />
                </RequireRoles>
              }
            />
            <Route
              path="recruiter"
              element={
                <RequireRoles roles={[ROLES.RECRUITER, ROLES.PLACEMENT_OFFICER, ROLES.TENANT_ADMIN]}>
                  <RecruiterPage />
                </RequireRoles>
              }
            />
            <Route
              path="settings"
              element={
                <RequireRoles roles={[ROLES.TENANT_ADMIN]}>
                  <SettingsPage />
                </RequireRoles>
              }
            />
            <Route
              path="audit"
              element={
                <RequireRoles roles={[ROLES.PLATFORM_SUPER_ADMIN, ROLES.TENANT_ADMIN]}>
                  <AuditPage />
                </RequireRoles>
              }
            />
            <Route path="notifications" element={<NotificationsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/app" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
