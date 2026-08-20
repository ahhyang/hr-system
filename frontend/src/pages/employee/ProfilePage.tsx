import { FormEvent, useCallback, useEffect, useState } from 'react';
import { ErrorAlert, LoadingSpinner, PageHeader } from '../../components/ui';
import { employeeApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Employee } from '../../types';

export default function ProfilePage() {
  const [profile, setProfile] = useState<Employee | null>(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await employeeApi.profile();
      setProfile(data);
      setForm({
        firstName: data.firstName,
        lastName: data.lastName,
        phone: data.phone ?? '',
      });
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load profile');
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
      const updated = await employeeApi.updateProfile(form);
      setProfile(updated);
      setSuccess('Profile updated successfully');
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="Profile" description="Manage your personal information" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}
      {success && (
        <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {success}
        </div>
      )}
      <p className="mb-4 text-sm text-slate-500">
        Official name, job title, and bank details are managed by HR. Contact HR if something looks wrong.
      </p>

      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={handleSubmit} className="card space-y-4">
          <h2 className="card-title">Personal Details</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label">First name</label>
              <input
                className="input"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                required
              />
            </div>
            <div>
              <label className="label">Last name</label>
              <input
                className="input"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                required
              />
            </div>
          </div>
          <div>
            <label className="label">Phone</label>
            <input
              className="input"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              placeholder="+60 12-345 6789"
            />
          </div>
          <button type="submit" className="btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </form>

        {profile && (
          <div className="card space-y-4">
            <h2 className="card-title">Employment Info</h2>
            <InfoRow label="Employee code" value={profile.employeeCode} />
            <InfoRow label="Email" value={profile.email} />
            <InfoRow label="Join date" value={profile.joinDate ?? '—'} />
            <InfoRow label="Status" value={profile.employmentStatus ?? '—'} />
            <InfoRow label="Onboarding" value={`${profile.onboardingProgress ?? 0}%`} />
          </div>
        )}
      </div>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-slate-100 pb-3 text-sm last:border-0">
      <span className="text-slate-500">{label}</span>
      <span className="font-medium text-slate-900">{value}</span>
    </div>
  );
}
