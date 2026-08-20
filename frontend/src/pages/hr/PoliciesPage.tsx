import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  LoadingSpinner,
  PageHeader,
} from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Policy } from '../../types';

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ category: '', title: '', content: '' });
  const [expanded, setExpanded] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setPolicies(await hrApi.policies());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load policies');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await hrApi.createPolicy(form);
      setShowForm(false);
      setForm({ category: '', title: '', content: '' });
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to create policy');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Company Policies"
        description="Manage HR policies and guidelines"
        action={
          <button onClick={() => setShowForm(!showForm)} className="btn-primary">
            {showForm ? 'Cancel' : 'Add Policy'}
          </button>
        }
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {showForm && (
        <form onSubmit={handleSubmit} className="card mb-6 space-y-4">
          <h2 className="card-title">New Policy</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label">Category</label>
              <input
                className="input"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
                placeholder="e.g. Leave, Conduct"
                required
              />
            </div>
            <div>
              <label className="label">Title</label>
              <input
                className="input"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                required
              />
            </div>
          </div>
          <div>
            <label className="label">Content</label>
            <textarea
              className="input min-h-[120px]"
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : 'Create Policy'}
          </button>
        </form>
      )}

      {policies.length === 0 ? (
        <div className="card"><EmptyState message="No policies yet" /></div>
      ) : (
        <div className="space-y-3">
          {policies.map((p) => (
            <div key={p.id} className="card">
              <button
                type="button"
                onClick={() => setExpanded(expanded === p.id ? null : p.id)}
                className="flex w-full items-start justify-between text-left"
              >
                <div>
                  <span className="badge-blue">{p.category}</span>
                  <h3 className="mt-2 font-semibold text-slate-900">{p.title}</h3>
                </div>
                <span className="text-slate-400">{expanded === p.id ? '▲' : '▼'}</span>
              </button>
              {expanded === p.id && (
                <p className="mt-4 whitespace-pre-wrap text-sm leading-relaxed text-slate-600">
                  {p.content}
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
