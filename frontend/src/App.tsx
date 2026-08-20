import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import SettingsPage from './pages/admin/SettingsPage';
import AiAssistantPage from './pages/employee/AiAssistantPage';
import AttendancePage from './pages/employee/AttendancePage';
import ClaimsPage from './pages/employee/ClaimsPage';
import DashboardPage from './pages/employee/DashboardPage';
import LeavePage from './pages/employee/LeavePage';
import PayslipPage from './pages/employee/PayslipPage';
import ProfilePage from './pages/employee/ProfilePage';
import AnnouncementsPage from './pages/hr/AnnouncementsPage';
import EmployeesPage from './pages/hr/EmployeesPage';
import HrDashboardPage from './pages/hr/HrDashboardPage';
import PayrollPage from './pages/hr/PayrollPage';
import PoliciesPage from './pages/hr/PoliciesPage';
import ReportsPage from './pages/hr/ReportsPage';
import LoginPage from './pages/LoginPage';
import ApprovalsPage from './pages/manager/ApprovalsPage';
import TeamPage from './pages/manager/TeamPage';
import OnboardingPage from './pages/OnboardingPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="attendance" element={<AttendancePage />} />
          <Route path="leave" element={<LeavePage />} />
          <Route path="payslips" element={<PayslipPage />} />
          <Route path="claims" element={<ClaimsPage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="ai-assistant" element={<AiAssistantPage />} />
          <Route path="onboarding" element={<OnboardingPage />} />

          <Route element={<ProtectedRoute roles={['MANAGER', 'HR', 'COMPANY_ADMIN']} />}>
            <Route path="manager/team" element={<TeamPage />} />
            <Route path="manager/approvals" element={<ApprovalsPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['HR', 'COMPANY_ADMIN']} />}>
            <Route path="hr/dashboard" element={<HrDashboardPage />} />
            <Route path="hr/employees" element={<EmployeesPage />} />
            <Route path="hr/payroll" element={<PayrollPage />} />
            <Route path="hr/announcements" element={<AnnouncementsPage />} />
            <Route path="hr/reports" element={<ReportsPage />} />
            <Route path="hr/policies" element={<PoliciesPage />} />
          </Route>

          <Route element={<ProtectedRoute roles={['COMPANY_ADMIN']} />}>
            <Route path="admin/settings" element={<SettingsPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
