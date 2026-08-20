import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  formatDate,
  formatDateTime,
  LoadingSpinner,
  PageHeader,
  StatusBadge,
} from '../../components/ui';
import { leaveApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { LeaveBalance, LeaveRequest, LeaveType } from '../../types';

export default function LeavePage() {
  const [balances, setBalances] = useState<LeaveBalance[]>([]);
  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [types, setTypes] = useState<LeaveType[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    leaveTypeId: '',
    startDate: '',
    endDate: '',
    reason: '',
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [b, r, t] = await Promise.all([
        leaveApi.balances(),
        leaveApi.requests(),
        leaveApi.types(),
      ]);
      setBalances(b);
      setRequests(r);
      setTypes(t);
      if (t.length > 0 && !form.leaveTypeId) {
        setForm((prev) => ({ ...prev, leaveTypeId: t[0].id }));
      }
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load leave data');
    } finally {
      setLoading(false);
    }
  }, [form.leaveTypeId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await leaveApi.apply(form);
      setShowForm(false);
      setForm({ leaveTypeId: types[0]?.id ?? '', startDate: '', endDate: '', reason: '' });
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to submit leave request');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Leave"
        description="Apply for leave and track your requests"
        action={
          <button onClick={() => setShowForm(!showForm)} className="btn-primary">
            {showForm ? 'Cancel' : 'Apply Leave'}
          </button>
        }
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {balances.map((b) => (
          <div key={b.leaveType} className="card">
            <p className="text-sm font-medium text-slate-500">{b.leaveType}</p>
            <p className="mt-2 text-3xl font-bold text-brand-600">{b.remaining}</p>
            <p className="text-xs text-slate-400">{b.used} used / {b.allocated} allocated</p>
          </div>
        ))}
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="card mb-6 space-y-4">
          <h2 className="card-title">New Leave Request</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label">Leave type</label>
              <select
                className="input"
                value={form.leaveTypeId}
                onChange={(e) => setForm({ ...form, leaveTypeId: e.target.value })}
                required
              >
                {types.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Start date</label>
              <input
                type="date"
                className="input"
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="label">End date</label>
              <input
                type="date"
                className="input"
                value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                required
              />
            </div>
          </div>
          <div>
            <label className="label">Reason</label>
            <textarea
              className="input min-h-[80px]"
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
              placeholder="Brief reason for leave"
              required
            />
          </div>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit Request'}
          </button>
        </form>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Period</th>
              <th>Days</th>
              <th>Reason</th>
              <th>Status</th>
              <th>Submitted</th>
            </tr>
          </thead>
          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan={6}><EmptyState message="No leave requests yet" /></td>
              </tr>
            ) : (
              requests.map((r) => (
                <tr key={r.id}>
                  <td className="font-medium">{r.leaveType}</td>
                  <td>{formatDate(r.startDate)} – {formatDate(r.endDate)}</td>
                  <td>{r.days}</td>
                  <td className="max-w-xs truncate">{r.reason}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{formatDateTime(r.submittedAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
