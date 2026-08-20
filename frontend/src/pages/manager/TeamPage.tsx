import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  LoadingSpinner,
  PageHeader,
  StatCard,
  StatusBadge,
} from '../../components/ui';
import { managerApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Employee, ManagerDashboard } from '../../types';

export default function TeamPage() {
  const [dashboard, setDashboard] = useState<ManagerDashboard | null>(null);
  const [team, setTeam] = useState<Employee[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [dash, members] = await Promise.all([
        managerApi.dashboard(),
        managerApi.team(),
      ]);
      setDashboard(dash);
      setTeam(members.length > 0 ? members : dash.team);
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load team');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader title="My Team" description="Overview of your direct reports" />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {dashboard && (
        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          <StatCard label="Team size" value={dashboard.teamSize} accent="brand" />
          <StatCard label="Present" value={dashboard.present} accent="green" />
          <StatCard label="On leave" value={dashboard.onLeave} accent="amber" />
          <StatCard label="Absent" value={dashboard.absent} accent="red" />
          <StatCard label="Pending approvals" value={dashboard.pendingApprovals} accent="brand" />
        </div>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Name</th>
              <th>Email</th>
              <th>Join date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {team.length === 0 ? (
              <tr>
                <td colSpan={5}><EmptyState message="No team members found" /></td>
              </tr>
            ) : (
              team.map((e) => (
                <tr key={e.id}>
                  <td className="font-mono text-xs">{e.employeeCode}</td>
                  <td className="font-medium">{e.firstName} {e.lastName}</td>
                  <td>{e.email}</td>
                  <td>{e.joinDate ?? '—'}</td>
                  <td><StatusBadge status={e.employmentStatus ?? 'ACTIVE'} /></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
