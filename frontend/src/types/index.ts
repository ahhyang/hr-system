export type UserRole = 'EMPLOYEE' | 'MANAGER' | 'HR' | 'COMPANY_ADMIN';

export interface UserSummary {
  id: string;
  tenantId: string;
  employeeId: string | null;
  email: string;
  role: UserRole;
  name: string;
}

export interface AuthResponse {
  token: string;
  user: UserSummary;
}

export interface RegisterTenantRequest {
  companyName: string;
  slug: string;
  adminName: string;
  adminEmail: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Employee {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  joinDate?: string;
  employmentStatus?: string;
  onboardingProgress?: number;
}

export interface EmployeeRequest {
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  joinDate?: string;
  role?: UserRole;
  password?: string;
}

export interface Department {
  id: string;
  name: string;
  code?: string;
}

export interface ClockResponse {
  id: string;
  clockIn: string | null;
  clockOut: string | null;
  status: string;
  message: string;
}

export interface AttendanceDay {
  date: string;
  clockIn: string | null;
  clockOut: string | null;
  workHours: number;
  lateMinutes: number;
  otMinutes: number;
  status: string;
}

export interface AttendanceMonth {
  month: string;
  totalHours: number;
  lateCount: number;
  lateMinutes: number;
  otMinutes: number;
  days: AttendanceDay[];
}

export interface LeaveBalance {
  leaveType: string;
  allocated: number;
  used: number;
  remaining: number;
}

export interface LeaveType {
  id: string;
  name: string;
}

export interface LeaveRequest {
  id: string;
  leaveType: string;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: string;
  submittedAt: string;
}

export interface LeaveApplyRequest {
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason: string;
  attachmentUrl?: string;
}

export interface ClaimItem {
  categoryId: string;
  expenseDate: string;
  amount: number;
  description: string;
  receiptUrl?: string;
}

export interface Claim {
  id: string;
  title: string;
  totalAmount: number;
  status: string;
  submittedAt: string;
}

export interface ClaimCategory {
  id: string;
  name: string;
}

export interface Payslip {
  id: string;
  yearMonth: string;
  basicSalary: number;
  allowanceTotal: number;
  overtimePay: number;
  bonus: number;
  epfEmployee: number;
  socso: number;
  eis: number;
  pcb: number;
  netSalary: number;
}

export interface Announcement {
  id: string;
  title: string;
  content: string;
  requiresAcknowledgement: boolean;
  publishedAt: string;
  acknowledged: boolean;
}

export interface Dashboard {
  clockedInToday: boolean;
  clockInTime: string | null;
  todayWorkHours: number;
  monthlyLateCount: number;
  leaveBalances: LeaveBalance[];
  pendingLeaves: number;
  pendingClaims: number;
  latestPayslip: Payslip | null;
  announcements: Announcement[];
  aiSummary: string;
}

export interface HrDashboard {
  totalEmployees: number;
  presentToday: number;
  absentToday: number;
  onLeaveToday: number;
  lateToday: number;
  pendingLeaves: number;
  pendingClaims: number;
  totalPayroll: number;
  totalOvertime: number;
  newEmployees: number;
  departmentBreakdown: Record<string, unknown>[];
  aiSummary: string;
  alerts: Record<string, unknown>[];
}

export interface ManagerDashboard {
  teamSize: number;
  present: number;
  onLeave: number;
  absent: number;
  pendingApprovals: number;
  team: Employee[];
}

export interface PendingApproval {
  id: string;
  type: 'LEAVE' | 'CLAIM';
  employeeName: string;
  title: string;
  amount?: number;
  submittedAt: string;
  status: string;
}

export interface Policy {
  id: string;
  category: string;
  title: string;
  content: string;
}

export interface OnboardingProgress {
  employeeId: string;
  progress: number;
  tasks: { id: string; title: string; completed: boolean; description?: string }[];
}

export interface AiChatResponse {
  reply: string;
  sources: string[];
}

export interface PayrollRun {
  id: string;
  yearMonth: string;
  status: string;
  totalAmount: number;
  employeeCount: number;
  createdAt: string;
}

export interface TenantSettings {
  companyName: string;
  slug: string;
  timezone: string;
  workStartTime: string;
  workEndTime: string;
}

export interface ApiError {
  message: string;
}
