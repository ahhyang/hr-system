import type { ReactNode } from 'react';

export function PageHeader({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
        {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-600" />
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="py-12 text-center text-sm text-slate-500">{message}</div>
  );
}

export function ErrorAlert({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </div>
  );
}

export function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  if (['APPROVED', 'COMPLETED', 'PRESENT', 'ACTIVE', 'PAID'].includes(normalized)) {
    return <span className="badge-green">{status}</span>;
  }
  if (['PENDING', 'SUBMITTED', 'IN_PROGRESS', 'PROCESSING'].includes(normalized)) {
    return <span className="badge-yellow">{status}</span>;
  }
  if (['REJECTED', 'CANCELLED', 'ABSENT', 'INACTIVE'].includes(normalized)) {
    return <span className="badge-red">{status}</span>;
  }
  return <span className="badge-gray">{status}</span>;
}

export function StatCard({
  label,
  value,
  sub,
  accent = 'brand',
}: {
  label: string;
  value: string | number;
  sub?: string;
  accent?: 'brand' | 'green' | 'amber' | 'red';
}) {
  const accents = {
    brand: 'bg-brand-50 text-brand-700',
    green: 'bg-emerald-50 text-emerald-700',
    amber: 'bg-amber-50 text-amber-700',
    red: 'bg-red-50 text-red-700',
  };

  return (
    <div className="card">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className={`mt-2 text-3xl font-bold ${accents[accent].split(' ')[1]}`}>{value}</p>
      {sub && <p className="mt-1 text-xs text-slate-400">{sub}</p>}
      <div className={`mt-3 h-1 w-12 rounded-full ${accents[accent].split(' ')[0]}`} />
    </div>
  );
}

export function formatCurrency(amount: number) {
  return new Intl.NumberFormat('en-MY', { style: 'currency', currency: 'MYR' }).format(amount);
}

export function formatDate(date: string | null | undefined) {
  if (!date) return '—';
  return new Date(date).toLocaleDateString('en-MY', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function formatDateTime(date: string | null | undefined) {
  if (!date) return '—';
  return new Date(date).toLocaleString('en-MY', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatTime(date: string | null | undefined) {
  if (!date) return '—';
  return new Date(date).toLocaleTimeString('en-MY', {
    hour: '2-digit',
    minute: '2-digit',
  });
}
