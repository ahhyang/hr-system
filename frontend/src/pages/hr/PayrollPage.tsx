import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  formatCurrency,
  formatDateTime,
  LoadingSpinner,
  PageHeader,
  StatusBadge,
} from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { PayrollRun } from '../../types';

function currentYearMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function PayrollPage() {
  const [runs, setRuns] = useState<PayrollRun[]>([]);
  const [yearMonth, setYearMonth] = useState(currentYearMonth());
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRuns(await hrApi.payrollRuns());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load payroll runs');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleRun = async (e: FormEvent) => {
    e.preventDefault();
    setRunning(true);
    setError('');
    setSuccess('');
    try {
      await hrApi.runPayroll(yearMonth);
      setSuccess(`Payroll run initiated for ${yearMonth}`);
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to run payroll');
    } finally {
      setRunning(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="Payroll" description="Run and manage monthly payroll" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}
      {success && (
        <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {success}
        </div>
      )}

      <form onSubmit={handleRun} className="card mb-6 flex flex-col gap-4 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label className="label">Payroll period</label>
          <input
            type="month"
            className="input max-w-xs"
            value={yearMonth}
            onChange={(e) => setYearMonth(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="btn-primary" disabled={running}>
          {running ? 'Running…' : 'Run Payroll'}
        </button>
      </form>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Period</th>
              <th>Employees</th>
              <th>Total amount</th>
              <th>Status</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {runs.length === 0 ? (
              <tr>
                <td colSpan={5}><EmptyState message="No payroll runs yet" /></td>
              </tr>
            ) : (
              runs.map((r) => (
                <tr key={r.id}>
                  <td className="font-medium">{r.yearMonth}</td>
                  <td>{r.employeeCount}</td>
                  <td>{formatCurrency(r.totalAmount)}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{formatDateTime(r.createdAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
