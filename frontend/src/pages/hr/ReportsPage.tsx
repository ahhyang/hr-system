import { useCallback, useEffect, useState } from 'react';
import { ErrorAlert, LoadingSpinner, PageHeader } from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';

const reportTypes = [
  { id: 'attendance', label: 'Attendance' },
  { id: 'leave', label: 'Leave' },
  { id: 'overtime', label: 'Overtime' },
  { id: 'payroll', label: 'Payroll' },
  { id: 'claims', label: 'Claims' },
];

export default function ReportsPage() {
  const [activeType, setActiveType] = useState('attendance');
  const [data, setData] = useState<unknown>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async (type: string) => {
    setLoading(true);
    setError('');
    try {
      setData(await hrApi.reports(type));
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load report');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(activeType);
  }, [activeType, load]);

  return (
    <div>
      <PageHeader title="Reports" description="Generate and view HR reports" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      <div className="mb-6 flex flex-wrap gap-2">
        {reportTypes.map((r) => (
          <button
            key={r.id}
            onClick={() => setActiveType(r.id)}
            className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
              activeType === r.id
                ? 'bg-brand-600 text-white'
                : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : data ? (
        <div className="card">
          <h2 className="card-title capitalize">{activeType} Report</h2>
          <pre className="mt-4 overflow-x-auto rounded-lg bg-slate-50 p-4 text-xs text-slate-700">
            {JSON.stringify(data, null, 2)}
          </pre>
        </div>
      ) : (
        <div className="card text-sm text-slate-500">Select a report type to view data</div>
      )}
    </div>
  );
}
