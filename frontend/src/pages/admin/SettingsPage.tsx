import { FormEvent, useCallback, useEffect, useState } from 'react';
import { ErrorAlert, LoadingSpinner, PageHeader } from '../../components/ui';
import { adminApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { TenantSettings } from '../../types';

export default function SettingsPage() {
  const [settings, setSettings] = useState<TenantSettings | null>(null);
  const [form, setForm] = useState<TenantSettings>({
    companyName: '',
    slug: '',
    timezone: 'Asia/Kuala_Lumpur',
    workStartTime: '09:00',
    workEndTime: '18:00',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await adminApi.settings();
      setSettings(data);
      setForm(data);
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const updated = await adminApi.updateSettings(form);
      setSettings(updated);
      setForm(updated);
      setSuccess('Settings saved successfully');
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="Company Settings" description="Configure your organization preferences" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}
      {success && (
        <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {success}
        </div>
      )}

      <form onSubmit={handleSubmit} className="card max-w-2xl space-y-4">
        <h2 className="card-title">General</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Company name</label>
            <input
              className="input"
              value={form.companyName}
              onChange={(e) => setForm({ ...form, companyName: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="label">Slug</label>
            <input
              className="input"
              value={form.slug}
              onChange={(e) => setForm({ ...form, slug: e.target.value })}
              required
            />
          </div>
          <div>
            <label className="label">Timezone</label>
            <select
              className="input"
              value={form.timezone}
              onChange={(e) => setForm({ ...form, timezone: e.target.value })}
            >
              <option value="Asia/Kuala_Lumpur">Asia/Kuala_Lumpur</option>
              <option value="Asia/Singapore">Asia/Singapore</option>
              <option value="UTC">UTC</option>
            </select>
          </div>
        </div>

        <h2 className="card-title pt-2">Work Hours</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Start time</label>
            <input
              type="time"
              className="input"
              value={form.workStartTime}
              onChange={(e) => setForm({ ...form, workStartTime: e.target.value })}
            />
          </div>
          <div>
            <label className="label">End time</label>
            <input
              type="time"
              className="input"
              value={form.workEndTime}
              onChange={(e) => setForm({ ...form, workEndTime: e.target.value })}
            />
          </div>
        </div>

        {settings && (
          <p className="text-xs text-slate-400">
            Last loaded settings for {settings.companyName}
          </p>
        )}

        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? 'Saving…' : 'Save Settings'}
        </button>
      </form>
    </div>
  );
}
