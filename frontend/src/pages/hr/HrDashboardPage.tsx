import { useCallback, useEffect, useState } from 'react';
import {
  ErrorAlert,
  formatCurrency,
  LoadingSpinner,
  PageHeader,
  StatCard,
} from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { HrDashboard } from '../../types';

export default function HrDashboardPage() {
  const [data, setData] = useState<HrDashboard | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await hrApi.dashboard());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load HR dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="HR Dashboard" description="Workforce overview and key metrics" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {data && (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total employees" value={data.totalEmployees} accent="brand" />
            <StatCard label="Present today" value={data.presentToday} accent="green" />
            <StatCard label="Absent today" value={data.absentToday} accent="red" />
            <StatCard label="On leave" value={data.onLeaveToday} accent="amber" />
            <StatCard label="Late today" value={data.lateToday} accent="amber" />
            <StatCard label="Pending leaves" value={data.pendingLeaves} accent="brand" />
            <StatCard label="Pending claims" value={data.pendingClaims} accent="brand" />
            <StatCard label="New employees" value={data.newEmployees} accent="green" />
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <div className="card">
              <h2 className="card-title">Payroll Summary</h2>
              <div className="mt-4 space-y-3">
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Total payroll</span>
                  <span className="font-semibold">{formatCurrency(data.totalPayroll)}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-slate-500">Total overtime</span>
                  <span className="font-semibold">{formatCurrency(data.totalOvertime)}</span>
                </div>
              </div>
            </div>

            <div className="card">
              <h2 className="card-title">AI Insights</h2>
              <p className="mt-3 text-sm leading-relaxed text-slate-600">
                {data.aiSummary || 'No AI insights available.'}
              </p>
            </div>
          </div>

          {data.departmentBreakdown.length > 0 && (
            <div className="card mb-6">
              <h2 className="card-title">Department Breakdown</h2>
              <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {data.departmentBreakdown.map((dept, i) => (
                  <div key={i} className="rounded-lg bg-slate-50 p-3">
                    <p className="text-sm font-medium text-slate-900">
                      {String(dept.name ?? dept.department ?? 'Department')}
                    </p>
                    <p className="text-2xl font-bold text-brand-600">
                      {String(dept.count ?? dept.employeeCount ?? '—')}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {data.alerts.length > 0 && (
            <div className="card">
              <h2 className="card-title">Alerts</h2>
              <ul className="mt-4 space-y-2">
                {data.alerts.map((alert, i) => (
                  <li key={i} className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
                    {String(alert.message ?? alert.title ?? JSON.stringify(alert))}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
    </div>
  );
}
