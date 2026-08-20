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
import { claimsApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Claim, ClaimCategory, ClaimItem } from '../../types';

const emptyItem = (): ClaimItem => ({
  categoryId: '',
  expenseDate: '',
  amount: 0,
  description: '',
});

export default function ClaimsPage() {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [categories, setCategories] = useState<ClaimCategory[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [title, setTitle] = useState('');
  const [items, setItems] = useState<ClaimItem[]>([emptyItem()]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [c, cats] = await Promise.all([claimsApi.list(), claimsApi.categories()]);
      setClaims(c);
      setCategories(cats);
      if (cats.length > 0) {
        setItems([{ ...emptyItem(), categoryId: cats[0].id }]);
      }
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load claims');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const updateItem = (index: number, field: keyof ClaimItem, value: string | number) => {
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [field]: value } : item)));
  };

  const addItem = () => setItems((prev) => [...prev, { ...emptyItem(), categoryId: categories[0]?.id ?? '' }]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await claimsApi.submit(title, items);
      setShowForm(false);
      setTitle('');
      setItems([{ ...emptyItem(), categoryId: categories[0]?.id ?? '' }]);
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to submit claim');
    } finally {
      setSubmitting(false);
    }
  };

  const total = items.reduce((sum, i) => sum + Number(i.amount || 0), 0);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Expense Claims"
        description="Submit and track your expense claims"
        action={
          <button onClick={() => setShowForm(!showForm)} className="btn-primary">
            {showForm ? 'Cancel' : 'New Claim'}
          </button>
        }
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {showForm && (
        <form onSubmit={handleSubmit} className="card mb-6 space-y-4">
          <h2 className="card-title">Submit Claim</h2>
          <div>
            <label className="label">Claim title</label>
            <input
              className="input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Client meeting expenses"
              required
            />
          </div>

          {items.map((item, idx) => (
            <div key={idx} className="rounded-lg border border-slate-200 p-4 space-y-3">
              <p className="text-sm font-medium text-slate-700">Item {idx + 1}</p>
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="label">Category</label>
                  <select
                    className="input"
                    value={item.categoryId}
                    onChange={(e) => updateItem(idx, 'categoryId', e.target.value)}
                    required
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label">Date</label>
                  <input
                    type="date"
                    className="input"
                    value={item.expenseDate}
                    onChange={(e) => updateItem(idx, 'expenseDate', e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label className="label">Amount (MYR)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    className="input"
                    value={item.amount || ''}
                    onChange={(e) => updateItem(idx, 'amount', parseFloat(e.target.value) || 0)}
                    required
                  />
                </div>
                <div>
                  <label className="label">Description</label>
                  <input
                    className="input"
                    value={item.description}
                    onChange={(e) => updateItem(idx, 'description', e.target.value)}
                    required
                  />
                </div>
              </div>
            </div>
          ))}

          <div className="flex items-center justify-between">
            <button type="button" onClick={addItem} className="btn-secondary text-sm">
              + Add item
            </button>
            <p className="text-sm font-medium text-slate-700">
              Total: {formatCurrency(total)}
            </p>
          </div>

          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit Claim'}
          </button>
        </form>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Submitted</th>
            </tr>
          </thead>
          <tbody>
            {claims.length === 0 ? (
              <tr>
                <td colSpan={4}><EmptyState message="No claims submitted yet" /></td>
              </tr>
            ) : (
              claims.map((c) => (
                <tr key={c.id}>
                  <td className="font-medium">{c.title}</td>
                  <td>{formatCurrency(c.totalAmount)}</td>
                  <td><StatusBadge status={c.status} /></td>
                  <td>{formatDateTime(c.submittedAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
