import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiException } from '../lib/api';
import { ErrorAlert } from '../components/ui';

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login({ email, password });
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,#4f46e5_0%,transparent_45%),radial-gradient(circle_at_bottom_right,#0ea5e9_0%,transparent_40%)] opacity-70" />
        <div className="relative">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 text-lg font-bold">P</div>
            <div>
              <p className="text-lg font-semibold">PulseHR</p>
              <p className="text-sm text-slate-300">AI-powered HR operating system</p>
            </div>
          </div>
        </div>
        <div className="relative max-w-md space-y-8">
          <h1 className="text-4xl font-semibold leading-tight">
            Clock in, apply leave, and ask HR questions in one place.
          </h1>
          <ul className="space-y-4 text-sm text-slate-200">
            <li>Clock in/out with late and overtime calculated automatically</li>
            <li>Leave, claims, and payslips without chasing Excel files</li>
            <li>Ask the AI assistant: “How many annual leave days do I have left?”</li>
          </ul>
        </div>
        <p className="relative text-xs text-slate-400">Built for Malaysian SMEs · EPF / SOCSO / EIS / PCB</p>
      </div>

      <div className="flex items-center justify-center bg-slate-50 p-6">
        <div className="w-full max-w-md">
          <div className="mb-8 lg:hidden">
            <p className="text-xl font-semibold text-slate-900">PulseHR</p>
            <p className="text-sm text-slate-500">Sign in to your workplace portal</p>
          </div>
          <form onSubmit={handleSubmit} className="card space-y-4 shadow-lg shadow-slate-200/60">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Welcome back</h2>
              <p className="mt-1 text-sm text-slate-500">Use your work email to continue</p>
            </div>
            {error && <ErrorAlert message={error} />}
            <div>
              <label htmlFor="email" className="label">Email</label>
              <input
                id="email"
                type="email"
                className="input"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                required
                autoComplete="email"
              />
            </div>
            <div>
              <label htmlFor="password" className="label">Password</label>
              <input
                id="password"
                type="password"
                className="input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                autoComplete="current-password"
              />
            </div>
            <button type="submit" className="btn-primary w-full" disabled={loading}>
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
            <p className="text-center text-sm text-slate-500">
              New company?{' '}
              <Link to="/register" className="font-medium text-brand-600 hover:text-brand-700">
                Create your workspace
              </Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}
