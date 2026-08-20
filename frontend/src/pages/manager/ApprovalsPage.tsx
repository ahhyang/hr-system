import { useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  formatCurrency,
  formatDateTime,
  LoadingSpinner,
  PageHeader,
  StatusBadge,
} from '../../components/ui';
import { managerApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { PendingApproval } from '../../types';

export default function ApprovalsPage() {
  const [approvals, setApprovals] = useState<PendingApproval[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setApprovals(await managerApi.approvals());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load approvals');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleAction = async (
    item: PendingApproval,
    action: 'approve' | 'reject',
  ) => {
    setActionId(item.id);
    setError('');
    try {
      if (item.type === 'LEAVE') {
        if (action === 'approve') await managerApi.approveLeave(item.id);
        else await managerApi.rejectLeave(item.id);
      } else {
        if (action === 'approve') await managerApi.approveClaim(item.id);
        else await managerApi.rejectClaim(item.id);
      }
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Action failed');
    } finally {
      setActionId(null);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Approvals"
        description="Review and action pending leave and claim requests"
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Employee</th>
              <th>Details</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Submitted</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {approvals.length === 0 ? (
              <tr>
                <td colSpan={7}><EmptyState message="No pending approvals" /></td>
              </tr>
            ) : (
              approvals.map((a) => (
                <tr key={a.id}>
                  <td>
                    <span className={`badge ${a.type === 'LEAVE' ? 'badge-blue' : 'badge-gray'}`}>
                      {a.type}
                    </span>
                  </td>
                  <td className="font-medium">{a.employeeName}</td>
                  <td>{a.title}</td>
                  <td>{a.amount != null ? formatCurrency(a.amount) : '—'}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td>{formatDateTime(a.submittedAt)}</td>
                  <td>
                    {a.status.toUpperCase() === 'PENDING' && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleAction(a, 'approve')}
                          disabled={actionId === a.id}
                          className="btn-success px-3 py-1 text-xs"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => handleAction(a, 'reject')}
                          disabled={actionId === a.id}
                          className="btn-danger px-3 py-1 text-xs"
                        >
                          Reject
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
