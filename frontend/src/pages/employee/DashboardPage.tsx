import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ErrorAlert,
  formatCurrency,
  formatDateTime,
  formatTime,
  LoadingSpinner,
  PageHeader,
  StatCard,
  StatusBadge,
} from '../../components/ui';
import { useAuth } from '../../context/AuthContext';
import { announcementsApi, attendanceApi, employeeApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Dashboard } from '../../types';

export default function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState('');
  const [clockLoading, setClockLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      setData(await employeeApi.dashboard());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load dashboard');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleClock = async () => {
    setClockLoading(true);
    setError('');
    try {
      if (data?.clockedInToday) {
        await attendanceApi.clockOut();
      } else {
        await attendanceApi.clockIn();
      }
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Clock action failed');
    } finally {
      setClockLoading(false);
    }
  };

  if (!data && !error) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title={`Good day, ${user?.name?.split(' ')[0] ?? 'there'}`}
        description="Here's your overview for today"
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {data && (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              label="Today's hours"
              value={data.todayWorkHours.toFixed(1)}
              sub={data.clockedInToday ? `Since ${formatTime(data.clockInTime)}` : 'Not clocked in'}
              accent={data.clockedInToday ? 'green' : 'amber'}
            />
            <StatCard label="Late this month" value={data.monthlyLateCount} accent="amber" />
            <StatCard label="Pending leaves" value={data.pendingLeaves} accent="brand" />
            <StatCard label="Pending claims" value={data.pendingClaims} accent="brand" />
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-3">
            <div className="card lg:col-span-1">
              <h2 className="card-title">Clock In / Out</h2>
              <p className="mt-2 text-sm text-slate-500">
                {data.clockedInToday
                  ? `Clocked in at ${formatTime(data.clockInTime)}`
                  : 'You have not clocked in today'}
              </p>
              <button
                onClick={handleClock}
                disabled={clockLoading}
                className={`mt-4 w-full ${data.clockedInToday ? 'btn-danger' : 'btn-success'}`}
              >
                {clockLoading
                  ? 'Processing…'
                  : data.clockedInToday
                    ? 'Clock Out'
                    : 'Clock In'}
              </button>
            </div>

            <div className="card lg:col-span-2">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="card-title">Leave Balances</h2>
                <Link to="/leave" className="text-sm font-medium text-brand-600 hover:text-brand-700">
                  Apply leave →
                </Link>
              </div>
              {data.leaveBalances.length === 0 ? (
                <p className="text-sm text-slate-500">No leave balances available</p>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  {data.leaveBalances.map((b) => (
                    <div key={b.leaveType} className="rounded-lg bg-slate-50 p-3">
                      <p className="text-sm font-medium text-slate-900">{b.leaveType}</p>
                      <p className="mt-1 text-2xl font-bold text-brand-600">{b.remaining}</p>
                      <p className="text-xs text-slate-500">
                        {b.used} used of {b.allocated}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <div className="card">
              <h2 className="card-title">Latest Payslip</h2>
              {data.latestPayslip ? (
                <div className="mt-3">
                  <p className="text-sm text-slate-500">{data.latestPayslip.yearMonth}</p>
                  <p className="mt-1 text-2xl font-bold text-slate-900">
                    {formatCurrency(data.latestPayslip.netSalary)}
                  </p>
                  <Link to="/payslips" className="mt-3 inline-block text-sm text-brand-600 hover:text-brand-700">
                    View all payslips →
                  </Link>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-500">No payslips yet</p>
              )}
            </div>

            <div className="card">
              <h2 className="card-title">AI Summary</h2>
              <p className="mt-3 text-sm leading-relaxed text-slate-600">
                {data.aiSummary || 'Your AI assistant will provide insights here.'}
              </p>
              <Link to="/ai-assistant" className="mt-3 inline-block text-sm text-brand-600 hover:text-brand-700">
                Chat with AI →
              </Link>
            </div>
          </div>

          <div className="card mt-6">
            <h2 className="card-title">Announcements</h2>
            {data.announcements.length === 0 ? (
              <p className="mt-3 text-sm text-slate-500">No announcements</p>
            ) : (
              <ul className="mt-4 divide-y divide-slate-100">
                {data.announcements.map((a) => (
                  <li key={a.id} className="flex items-start justify-between gap-4 py-3">
                    <div>
                      <p className="font-medium text-slate-900">{a.title}</p>
                      <p className="mt-1 text-sm text-slate-600 line-clamp-2">{a.content}</p>
                      <p className="mt-1 text-xs text-slate-400">{formatDateTime(a.publishedAt)}</p>
                    </div>
                    {a.requiresAcknowledgement && !a.acknowledged && (
                      <button
                        onClick={() => announcementsApi.acknowledge(a.id).then(load)}
                        className="btn-secondary shrink-0 text-xs"
                      >
                        Acknowledge
                      </button>
                    )}
                    {a.acknowledged && <StatusBadge status="Acknowledged" />}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  );
}
