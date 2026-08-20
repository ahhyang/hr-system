import { FormEvent, useCallback, useEffect, useState } from 'react';
import {
  EmptyState,
  ErrorAlert,
  LoadingSpinner,
  PageHeader,
  StatusBadge,
} from '../../components/ui';
import { hrApi } from '../../lib/api';
import { ApiException } from '../../lib/api';
import type { Employee, EmployeeRequest } from '../../types';

const emptyForm = (): EmployeeRequest => ({
  employeeCode: '',
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  joinDate: new Date().toISOString().slice(0, 10),
  role: 'EMPLOYEE',
  password: '',
});

export default function EmployeesPage() {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<EmployeeRequest>(emptyForm());
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setEmployees(await hrApi.employees());
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to load employees');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm());
    setShowForm(true);
  };

  const openEdit = (emp: Employee) => {
    setEditingId(emp.id);
    setForm({
      employeeCode: emp.employeeCode,
      firstName: emp.firstName,
      lastName: emp.lastName,
      email: emp.email,
      phone: emp.phone ?? '',
      departmentId: emp.departmentId,
      positionId: emp.positionId,
      managerId: emp.managerId,
      joinDate: emp.joinDate,
      role: 'EMPLOYEE',
    });
    setShowForm(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      if (editingId) {
        await hrApi.updateEmployee(editingId, form);
      } else {
        await hrApi.createEmployee(form);
      }
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm());
      await load();
    } catch (err) {
      setError(err instanceof ApiException ? err.message : 'Failed to save employee');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <PageHeader
        title="Employees"
        description="Manage employee records"
        action={
          <button onClick={openCreate} className="btn-primary">
            Add Employee
          </button>
        }
      />

      {error && <div className="mb-4"><ErrorAlert message={error} /></div>}

      {showForm && (
        <form onSubmit={handleSubmit} className="card mb-6 space-y-4">
          <h2 className="card-title">{editingId ? 'Edit Employee' : 'New Employee'}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Field label="Employee code" value={form.employeeCode} onChange={(v) => setForm({ ...form, employeeCode: v })} required />
            <Field label="First name" value={form.firstName} onChange={(v) => setForm({ ...form, firstName: v })} required />
            <Field label="Last name" value={form.lastName} onChange={(v) => setForm({ ...form, lastName: v })} required />
            <Field label="Email" type="email" value={form.email} onChange={(v) => setForm({ ...form, email: v })} required />
            <Field label="Phone" value={form.phone ?? ''} onChange={(v) => setForm({ ...form, phone: v })} />
            <Field label="Join date" type="date" value={form.joinDate ?? ''} onChange={(v) => setForm({ ...form, joinDate: v })} />
            {!editingId && (
              <Field label="Password" type="password" value={form.password ?? ''} onChange={(v) => setForm({ ...form, password: v })} required />
            )}
            <div>
              <label className="label">Role</label>
              <select
                className="input"
                value={form.role ?? 'EMPLOYEE'}
                onChange={(e) => setForm({ ...form, role: e.target.value as EmployeeRequest['role'] })}
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="HR">HR</option>
                <option value="COMPANY_ADMIN">Admin</option>
              </select>
            </div>
          </div>
          <div className="flex gap-2">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : editingId ? 'Update' : 'Create'}
            </button>
            <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">
              Cancel
            </button>
          </div>
        </form>
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
              <th>Onboarding</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {employees.length === 0 ? (
              <tr>
                <td colSpan={7}><EmptyState message="No employees yet" /></td>
              </tr>
            ) : (
              employees.map((e) => (
                <tr key={e.id}>
                  <td className="font-mono text-xs">{e.employeeCode}</td>
                  <td className="font-medium">{e.firstName} {e.lastName}</td>
                  <td>{e.email}</td>
                  <td>{e.joinDate ?? '—'}</td>
                  <td><StatusBadge status={e.employmentStatus ?? 'ACTIVE'} /></td>
                  <td>{e.onboardingProgress ?? 0}%</td>
                  <td>
                    <button onClick={() => openEdit(e)} className="text-sm font-medium text-brand-600 hover:text-brand-700">
                      Edit
                    </button>
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

function Field({
  label,
  value,
  onChange,
  type = 'text',
  required,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <div>
      <label className="label">{label}</label>
      <input
        type={type}
        className="input"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
      />
    </div>
  );
}
