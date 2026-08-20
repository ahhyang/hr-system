import { FormEvent, useState } from 'react';
import { ErrorAlert, PageHeader } from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';

export default function AnnouncementsPage() {
  const [form, setForm] = useState({
    title: '',
    content: '',
    requiresAcknowledgement: false,
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    setSuccess('');
    try {
      await hrApi.createAnnouncement(form);
      setSuccess('Announcement published successfully');
      setForm({ title: '', content: '', requiresAcknowledgement: false });
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to publish announcement');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <PageHeader title="Announcements" description="Publish company-wide announcements" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}
      {success && (
        <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {success}
        </div>
      )}

      <form onSubmit={handleSubmit} className="card max-w-2xl space-y-4">
        <h2 className="card-title">New Announcement</h2>
        <div>
          <label className="label">Title</label>
          <input
            className="input"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            placeholder="Announcement title"
            required
          />
        </div>
        <div>
          <label className="label">Content</label>
          <textarea
            className="input min-h-[120px]"
            value={form.content}
            onChange={(e) => setForm({ ...form, content: e.target.value })}
            placeholder="Write your announcement…"
            required
          />
        </div>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={form.requiresAcknowledgement}
            onChange={(e) => setForm({ ...form, requiresAcknowledgement: e.target.checked })}
            className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
          />
          Require employee acknowledgement
        </label>
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Publishing…' : 'Publish Announcement'}
        </button>
      </form>
    </div>
  );
}
