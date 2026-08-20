import { useCallback, useEffect, useState } from 'react';
import { ErrorAlert, LoadingSpinner, PageHeader, StatCard } from '../components/ui';
import { onboardingApi, ApiException } from '../lib/api';
import type { OnboardingProgress } from '../types';

export default function OnboardingPage() {
  const [data, setData] = useState<OnboardingProgress | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setData(await onboardingApi.progress());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load onboarding progress');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const completeTask = async (taskId: string) => {
    try {
      await onboardingApi.completeTask(taskId);
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to complete task');
    }
  };

  if (loading) return <LoadingSpinner />;

  const completed = data?.tasks.filter((t) => t.completed).length ?? 0;
  const total = data?.tasks.length ?? 0;

  return (
    <div>
      <PageHeader
        title="Welcome aboard!"
        description="Complete these tasks to get started with your new role"
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {data && (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-3">
            <StatCard label="Progress" value={`${data.progress}%`} accent="brand" />
            <StatCard label="Tasks completed" value={`${completed}/${total}`} accent="green" />
            <StatCard label="Remaining" value={total - completed} accent="amber" />
          </div>

          <div className="mb-6">
            <div className="h-3 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full rounded-full bg-brand-600 transition-all duration-500"
                style={{ width: `${data.progress}%` }}
              />
            </div>
          </div>

          <div className="space-y-3">
            {data.tasks.map((task) => (
              <div
                key={task.id}
                className={`card flex items-start gap-4 ${
                  task.completed ? 'opacity-75' : ''
                }`}
              >
                <div
                  className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                    task.completed
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'border-2 border-slate-300 text-slate-400'
                  }`}
                >
                  {task.completed ? '✓' : ''}
                </div>
                <div className="flex-1">
                  <p className={`font-medium ${task.completed ? 'line-through text-slate-500' : 'text-slate-900'}`}>
                    {task.title}
                  </p>
                  {task.description && (
                    <p className="mt-1 text-sm text-slate-500">{task.description}</p>
                  )}
                </div>
                {!task.completed && (
                  <button onClick={() => completeTask(task.id)} className="btn-secondary text-sm shrink-0">
                    Mark done
                  </button>
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
