import type { ApiError } from '../types';

const API_URL = import.meta.env.VITE_API_URL || '/api/v1';

export class ApiException extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
    this.name = 'ApiException';
  }
}

function getToken(): string | null {
  return localStorage.getItem('token');
}

export function setToken(token: string): void {
  localStorage.setItem('token', token);
}

export function clearToken(): void {
  localStorage.removeItem('token');
}

export async function api<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const err = (await response.json()) as ApiError;
      if (err.message) message = err.message;
    } catch {
      // ignore parse errors
    }
    throw new ApiException(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

export const authApi = {
  login: (email: string, password: string) =>
    api<{ token: string; user: import('../types').UserSummary }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  register: (data: import('../types').RegisterTenantRequest) =>
    api<{ token: string; user: import('../types').UserSummary }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};

export const employeeApi = {
  dashboard: () => api<import('../types').Dashboard>('/me/dashboard'),
  profile: () => api<import('../types').Employee>('/me/employee'),
  updateProfile: (data: Partial<import('../types').Employee>) =>
    api<import('../types').Employee>('/me/employee', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
};

export const attendanceApi = {
  clockIn: () =>
    api<import('../types').ClockResponse>('/attendance/clock-in', { method: 'POST' }),
  clockOut: () =>
    api<import('../types').ClockResponse>('/attendance/clock-out', { method: 'POST' }),
  month: (month: string) =>
    api<import('../types').AttendanceMonth>(`/attendance/me?month=${month}`),
};

export const leaveApi = {
  balances: () => api<import('../types').LeaveBalance[]>('/leaves/balances'),
  types: () => api<import('../types').LeaveType[]>('/leaves/types'),
  requests: () => api<import('../types').LeaveRequest[]>('/leaves'),
  apply: (data: import('../types').LeaveApplyRequest) =>
    api<import('../types').LeaveRequest>('/leaves', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};

export const payslipApi = {
  list: () => api<import('../types').Payslip[]>('/payroll/payslips/me'),
};

export const claimsApi = {
  list: () => api<import('../types').Claim[]>('/claims'),
  categories: () => api<import('../types').ClaimCategory[]>('/claims/categories'),
  submit: (title: string, items: import('../types').ClaimItem[]) =>
    api<import('../types').Claim>('/claims', {
      method: 'POST',
      body: JSON.stringify({ title, items }),
    }),
};

export const announcementsApi = {
  list: () => api<import('../types').Announcement[]>('/announcements'),
  acknowledge: (id: string) =>
    api<void>(`/announcements/${id}/acknowledge`, { method: 'POST' }),
  create: (data: { title: string; content: string; requiresAcknowledgement: boolean }) =>
    api<import('../types').Announcement>('/announcements', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};

export const managerApi = {
  dashboard: () => api<import('../types').ManagerDashboard>('/manager/dashboard'),
  team: () => api<import('../types').Employee[]>('/manager/team'),
  approvals: async (): Promise<import('../types').PendingApproval[]> => {
    const data = await api<{ leaves: Array<Record<string, unknown>>; claims: Array<Record<string, unknown>> }>(
      '/manager/pending-approvals',
    );
    const leaves = (data.leaves ?? []).map((item) => ({
      id: String(item.id),
      type: 'LEAVE' as const,
      employeeName: String(item.leaveType ?? 'Leave'),
      title: `${item.startDate ?? ''} → ${item.endDate ?? ''}`.trim(),
      submittedAt: String(item.submittedAt ?? ''),
      status: String(item.status ?? ''),
    }));
    const claims = (data.claims ?? []).map((item) => ({
      id: String(item.id),
      type: 'CLAIM' as const,
      employeeName: String(item.title ?? 'Claim'),
      title: String(item.title ?? 'Claim'),
      amount: item.totalAmount != null ? Number(item.totalAmount) : undefined,
      submittedAt: String(item.submittedAt ?? ''),
      status: String(item.status ?? ''),
    }));
    return [...leaves, ...claims];
  },
  approveLeave: (id: string, comments?: string) =>
    api<void>(`/leaves/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ comments }),
    }),
  rejectLeave: (id: string, comments?: string) =>
    api<void>(`/leaves/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ comments }),
    }),
  approveClaim: (id: string, comments?: string) =>
    api<void>(`/claims/${id}/approve`, {
      method: 'POST',
      body: JSON.stringify({ comments }),
    }),
  rejectClaim: (id: string, comments?: string) =>
    api<void>(`/claims/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ comments }),
    }),
};

export const hrApi = {
  dashboard: () => api<import('../types').HrDashboard>('/hr/dashboard'),
  employees: () => api<import('../types').Employee[]>('/employees'),
  createEmployee: (data: import('../types').EmployeeRequest) =>
    api<import('../types').Employee>('/employees', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  updateEmployee: (id: string, data: import('../types').EmployeeRequest) =>
    api<import('../types').Employee>(`/employees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  departments: () => api<import('../types').Department[]>('/departments'),
  createAnnouncement: (data: { title: string; content: string; requiresAcknowledgement: boolean }) =>
    announcementsApi.create(data),
  payrollRuns: async () => {
    const runs = await api<Array<{
      id: string;
      yearMonth: string;
      status: string;
      totalPayroll?: number;
      createdAt?: string;
    }>>('/payroll/runs');
    return runs.map((r) => ({
      id: r.id,
      yearMonth: r.yearMonth,
      status: r.status,
      totalAmount: r.totalPayroll ?? 0,
      employeeCount: 0,
      createdAt: r.createdAt ?? '',
    }));
  },
  runPayroll: (yearMonth: string) =>
    api<import('../types').PayrollRun>(`/payroll/runs?month=${yearMonth}`, {
      method: 'POST',
    }),
  policies: () => api<import('../types').Policy[]>('/policies'),
  createPolicy: (data: { category: string; title: string; content: string }) =>
    api<import('../types').Policy>('/policies', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  reports: (type: string) =>
    api<Record<string, unknown>[]>(`/reports/${type}`),
};

export const adminApi = {
  settings: () => api<import('../types').TenantSettings>('/admin/settings'),
  updateSettings: (data: Partial<import('../types').TenantSettings>) =>
    api<import('../types').TenantSettings>('/admin/settings', {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
};

export const onboardingApi = {
  progress: async (): Promise<import('../types').OnboardingProgress> => {
    const raw = await api<{
      employeeId: string;
      progress: number;
      tasks: Array<{ id: string; name?: string; title?: string; completed: boolean; key?: string }>;
    }>('/onboarding/me');
    return {
      employeeId: raw.employeeId,
      progress: raw.progress,
      tasks: (raw.tasks ?? []).map((t) => ({
        id: t.id,
        title: t.title ?? t.name ?? t.key ?? 'Task',
        completed: t.completed,
      })),
    };
  },
  completeTask: (taskId: string) =>
    api<void>(`/onboarding/tasks/${taskId}/complete`, { method: 'POST' }),
};

export const aiApi = {
  chat: (message: string) =>
    api<import('../types').AiChatResponse>('/ai/chat', {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),
};
