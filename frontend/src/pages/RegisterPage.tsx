import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiException } from '../lib/api';
import { ErrorAlert } from '../components/ui';

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const [form, setForm] = useState({
    companyName: '',
    slug: '',
    adminName: '',
    adminEmail: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const update = (field: string, value: string) => {
    setForm((prev) => {
      const next = { ...prev, [field]: value };
      if (field === 'companyName' && !prev.slug) {
        next.slug = value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
      }
      return next;
    });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register(form);
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-600 via-brand-700 to-slate-900 p-4">
      <div className="w-full max-w-lg">
        <div className="mb-8 text-center text-white">
          <h1 className="text-2xl font-bold">Register your company</h1>
          <p className="mt-1 text-brand-200">Set up your HR system in minutes</p>
        </div>

        <form onSubmit={handleSubmit} className="card space-y-4">
          {error && <ErrorAlert message={error} />}

          <div>
            <label htmlFor="companyName" className="label">Company name</label>
            <input
              id="companyName"
              className="input"
              value={form.companyName}
              onChange={(e) => update('companyName', e.target.value)}
              placeholder="Acme Corp"
              required
            />
          </div>

          <div>
            <label htmlFor="slug" className="label">Company slug</label>
            <input
              id="slug"
              className="input"
              value={form.slug}
              onChange={(e) => update('slug', e.target.value)}
              placeholder="acme-corp"
              required
            />
            <p className="mt-1 text-xs text-slate-400">Used in your portal URL identifier</p>
          </div>

          <div>
            <label htmlFor="adminName" className="label">Admin full name</label>
            <input
              id="adminName"
              className="input"
              value={form.adminName}
              onChange={(e) => update('adminName', e.target.value)}
              placeholder="Jane Doe"
              required
            />
          </div>

          <div>
            <label htmlFor="adminEmail" className="label">Admin email</label>
            <input
              id="adminEmail"
              type="email"
              className="input"
              value={form.adminEmail}
              onChange={(e) => update('adminEmail', e.target.value)}
              placeholder="admin@acme.com"
              required
            />
          </div>

          <div>
            <label htmlFor="password" className="label">Password</label>
            <input
              id="password"
              type="password"
              className="input"
              value={form.password}
              onChange={(e) => update('password', e.target.value)}
              placeholder="Min. 8 characters"
              minLength={8}
              required
            />
          </div>

          <button type="submit" className="btn-primary w-full" disabled={loading}>
            {loading ? 'Creating account…' : 'Create company account'}
          </button>

          <p className="text-center text-sm text-slate-500">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-brand-600 hover:text-brand-700">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
