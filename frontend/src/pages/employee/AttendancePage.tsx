import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  formatTime,
  LoadingSpinner,
  PageHeader,
  StatCard,
  StatusBadge,
} from '../../components/ui';
import { attendanceApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { AttendanceMonth } from '../../types';

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

export default function AttendancePage() {
  const [month, setMonth] = useState(currentMonth());
  const [data, setData] = useState<AttendanceMonth | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await attendanceApi.month(month));
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load attendance');
    } finally {
      setLoading(false);
    }
  }, [month]);

  useEffect(() => {
    load();
  }, [load]);

  const shiftMonth = (delta: number) => {
    const [y, m] = month.split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    setMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  };

  return (
    <div>
      <PageHeader
        title="Attendance"
        description="Monthly attendance overview"
        action={
          <div className="flex items-center gap-2">
            <button onClick={() => shiftMonth(-1)} className="btn-secondary px-3">←</button>
            <input
              type="month"
              className="input w-auto"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
            <button onClick={() => shiftMonth(1)} className="btn-secondary px-3">→</button>
          </div>
        }
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {loading ? (
        <LoadingSpinner />
      ) : data ? (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total hours" value={data.totalHours.toFixed(1)} accent="brand" />
            <StatCard label="Late count" value={data.lateCount} accent="amber" />
            <StatCard label="Late minutes" value={data.lateMinutes} accent="amber" />
            <StatCard label="OT minutes" value={data.otMinutes} accent="green" />
          </div>

          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Clock In</th>
                  <th>Clock Out</th>
                  <th>Hours</th>
                  <th>Late</th>
                  <th>OT</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.days.length === 0 ? (
                  <tr>
                    <td colSpan={7}><EmptyState message="No attendance records for this month" /></td>
                  </tr>
                ) : (
                  data.days.map((day) => (
                    <tr key={day.date}>
                      <td className="font-medium">{day.date}</td>
                      <td>{formatTime(day.clockIn)}</td>
                      <td>{formatTime(day.clockOut)}</td>
                      <td>{day.workHours.toFixed(1)}</td>
                      <td>{day.lateMinutes > 0 ? `${day.lateMinutes}m` : '—'}</td>
                      <td>{day.otMinutes > 0 ? `${day.otMinutes}m` : '—'}</td>
                      <td><StatusBadge status={day.status} /></td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      ) : null}
    </div>
  );
}
