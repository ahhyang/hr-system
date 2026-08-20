import { useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  formatCurrency,
  LoadingSpinner,
  PageHeader,
} from '../../components/ui';
import { payslipApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Payslip } from '../../types';

export default function PayslipPage() {
  const [payslips, setPayslips] = useState<Payslip[]>([]);
  const [selected, setSelected] = useState<Payslip | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    payslipApi
      .list()
      .then((data) => {
        setPayslips(data);
        if (data.length > 0) setSelected(data[0]);
      })
      .catch((err) =>
        setError(err instanceof ApiException ? err.message : 'Failed to load payslips'),
      )
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="Payslips" description="View your salary statements" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {payslips.length === 0 ? (
        <div className="card"><EmptyState message="No payslips available yet" /></div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="space-y-2">
            {payslips.map((p) => (
              <button
                key={p.id}
                onClick={() => setSelected(p)}
                className={`w-full rounded-lg border p-4 text-left transition-colors ${
                  selected?.id === p.id
                    ? 'border-brand-500 bg-brand-50'
                    : 'border-slate-200 bg-white hover:bg-slate-50'
                }`}
              >
                <p className="font-medium text-slate-900">{p.yearMonth}</p>
                <p className="text-sm text-brand-600">{formatCurrency(p.netSalary)}</p>
              </button>
            ))}
          </div>

          {selected && (
            <div className="card lg:col-span-2">
              <h2 className="card-title">Payslip — {selected.yearMonth}</h2>
              <div className="mt-6 space-y-3">
                <Row label="Basic salary" value={formatCurrency(selected.basicSalary)} />
                <Row label="Allowances" value={formatCurrency(selected.allowanceTotal)} />
                <Row label="Overtime pay" value={formatCurrency(selected.overtimePay)} />
                <Row label="Bonus" value={formatCurrency(selected.bonus)} />
                <hr className="border-slate-200" />
                <Row label="EPF (employee)" value={`-${formatCurrency(selected.epfEmployee)}`} negative />
                <Row label="SOCSO" value={`-${formatCurrency(selected.socso)}`} negative />
                <Row label="EIS" value={`-${formatCurrency(selected.eis)}`} negative />
                <Row label="PCB" value={`-${formatCurrency(selected.pcb)}`} negative />
                <hr className="border-slate-200" />
                <div className="flex justify-between pt-2">
                  <span className="text-lg font-semibold text-slate-900">Net salary</span>
                  <span className="text-lg font-bold text-brand-600">
                    {formatCurrency(selected.netSalary)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function Row({ label, value, negative }: { label: string; value: string; negative?: boolean }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-slate-500">{label}</span>
      <span className={negative ? 'text-red-600' : 'font-medium text-slate-900'}>{value}</span>
    </div>
  );
}
