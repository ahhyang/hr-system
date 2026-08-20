import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import type { UserRole } from '../types';

interface NavItem {
  to: string;
  label: string;
  roles: UserRole[];
}

const navGroups: { title: string; items: NavItem[] }[] = [
  {
    title: 'My work',
    items: [
      { to: '/', label: 'Home', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/attendance', label: 'Attendance', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/leave', label: 'Leave', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/claims', label: 'Claims', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/payslips', label: 'Payslips', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/ai-assistant', label: 'Ask AI', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
    ],
  },
  {
    title: 'Team',
    items: [
      { to: '/manager/team', label: 'My team', roles: ['MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/manager/approvals', label: 'Approvals', roles: ['MANAGER', 'HR', 'COMPANY_ADMIN'] },
    ],
  },
  {
    title: 'Company',
    items: [
      { to: '/hr/dashboard', label: 'HR overview', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/hr/employees', label: 'Employees', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/hr/payroll', label: 'Payroll', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/hr/announcements', label: 'Announcements', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/hr/reports', label: 'Reports', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/hr/policies', label: 'Policies', roles: ['HR', 'COMPANY_ADMIN'] },
      { to: '/admin/settings', label: 'Settings', roles: ['COMPANY_ADMIN'] },
    ],
  },
  {
    title: 'Account',
    items: [
      { to: '/profile', label: 'Profile', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
      { to: '/onboarding', label: 'Onboarding', roles: ['EMPLOYEE', 'MANAGER', 'HR', 'COMPANY_ADMIN'] },
    ],
  },
];

function roleLabel(role: UserRole) {
  const labels: Record<UserRole, string> = {
    EMPLOYEE: 'Employee',
    MANAGER: 'Manager',
    HR: 'HR',
    COMPANY_ADMIN: 'Admin',
  };
  return labels[role];
}

export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="flex min-h-screen bg-slate-50">
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-slate-900/40 lg:hidden" onClick={() => setSidebarOpen(false)} />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-slate-200 bg-white transition-transform lg:static lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center gap-3 border-b border-slate-200 px-5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-sm font-bold text-white">
            P
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-900">PulseHR</p>
            <p className="text-xs text-slate-500">Workplace portal</p>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto p-4">
          {navGroups.map((group) => {
            const items = group.items.filter((item) => item.roles.some((r) => hasRole(r)));
            if (items.length === 0) return null;
            return (
              <div key={group.title} className="mb-5">
                <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                  {group.title}
                </p>
                <ul className="space-y-0.5">
                  {items.map((item) => (
                    <li key={item.to}>
                      <NavLink
                        to={item.to}
                        end={item.to === '/'}
                        onClick={() => setSidebarOpen(false)}
                        className={({ isActive }) =>
                          `flex items-center rounded-lg px-3 py-2 text-sm font-medium ${
                            isActive
                              ? 'bg-brand-50 text-brand-700'
                              : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                          }`
                        }
                      >
                        {item.label}
                      </NavLink>
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </nav>

        <div className="border-t border-slate-200 p-4">
          <div className="mb-3 rounded-xl bg-slate-50 p-3">
            <p className="truncate text-sm font-medium text-slate-900">{user?.name}</p>
            <p className="truncate text-xs text-slate-500">{user?.email}</p>
            {user && (
              <span className="mt-2 inline-block rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-700">
                {roleLabel(user.role)}
              </span>
            )}
          </div>
          <button onClick={handleLogout} className="btn-secondary w-full text-sm">
            Sign out
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-slate-200 bg-white/90 px-4 backdrop-blur lg:px-8">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden"
            aria-label="Open menu"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <Link
            to="/ai-assistant"
            className="hidden rounded-full border border-slate-200 px-3 py-1.5 text-sm text-slate-600 hover:border-brand-200 hover:text-brand-700 sm:block"
          >
            Ask AI about leave, pay, or policy
          </Link>
          <div className="flex-1" />
          <Link to="/profile" className="text-sm font-medium text-slate-600 hover:text-brand-600">
            {user?.name}
          </Link>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 p-4 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
