import { useState } from 'react';
import { UserPlus, Pencil, Power } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { useApi } from '../../lib/hooks';
import { Button, ErrorNote, Field, Input, Loading, Modal, PageHeader, Panel, RoleBadge, Select, Table } from '../../components/ui';
import { ROLE, apiError, formatDate } from '../../lib/format';

const EMPTY = { fullName: '', username: '', email: '', password: '', department: '', phoneNumber: '', role: 'EMPLOYEE' };

export default function AdminUsers() {
  const { user: me } = useAuth();
  const { data, loading, error, reload } = useApi('/admin/users', { initial: [] });
  const [roleFilter, setRoleFilter] = useState('');
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);

  const rows = data.filter((u) => !roleFilter || u.role === roleFilter);
  const open = (u) => {
    setEditing(u || 'new');
    setForm(u ? { ...EMPTY, ...u, password: '', department: u.department || '', phoneNumber: u.phoneNumber || '' } : EMPTY);
  };
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const save = async () => {
    setBusy(true);
    try {
      if (editing === 'new') await api.post('/admin/users', form);
      else await api.put(`/admin/users/${editing.id}`, form);
      toast.success(editing === 'new' ? `${form.fullName} added` : 'Changes saved');
      setEditing(null);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (u) => {
    try {
      if (u.active) await api.delete(`/admin/users/${u.id}`);
      else await api.put(`/admin/users/${u.id}/activate`);
      toast.success(u.active ? `${u.fullName} deactivated` : `${u.fullName} reactivated`);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    }
  };

  return (
    <>
      <PageHeader title="Users" description="Create accounts for employees, technicians and administrators. Deactivated users cannot sign in." actions={<Button icon={UserPlus} onClick={() => open(null)}>Add user</Button>} />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <Panel padded={false}>
        <div className="flex items-center gap-3 border-b border-line-soft p-4">
          <label className="text-sm font-semibold" htmlFor="rf">Role</label>
          <Select id="rf" className="max-w-48" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">All roles</option>
            {Object.entries(ROLE).map(([k, r]) => <option key={k} value={k}>{r.label}</option>)}
          </Select>
          <span className="text-[13px] text-muted">{rows.length} users</span>
        </div>
        {loading ? <Loading /> : (
          <Table
            rows={rows}
            columns={[
              { key: 'fullName', header: 'Name', render: (u) => (<div><p className="font-semibold">{u.fullName}</p><p className="text-[13px] text-muted">{u.username} · {u.email}</p></div>) },
              { key: 'role', header: 'Role', render: (u) => <RoleBadge role={u.role} /> },
              { key: 'department', header: 'Department', className: 'hidden md:table-cell text-steel', render: (u) => u.department || '—' },
              { key: 'active', header: 'Status', render: (u) => <span className={u.active ? 'text-ok font-semibold' : 'text-muted'}>{u.active ? 'Active' : 'Deactivated'}</span> },
              { key: 'createdAt', header: 'Joined', className: 'hidden lg:table-cell text-steel', render: (u) => formatDate(u.createdAt) },
              {
                key: 'actions', header: '', className: 'text-right whitespace-nowrap', render: (u) => (
                  <div className="flex justify-end gap-1">
                    <Button size="sm" variant="ghost" icon={Pencil} onClick={() => open(u)}>Edit</Button>
                    {u.username !== me?.username && (
                      <Button size="sm" variant={u.active ? 'danger' : 'secondary'} icon={Power} onClick={() => toggle(u)}>{u.active ? 'Deactivate' : 'Reactivate'}</Button>
                    )}
                  </div>
                ),
              },
            ]}
          />
        )}
      </Panel>

      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing === 'new' ? 'Add user' : `Edit ${form.fullName}`}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={save}>{editing === 'new' ? 'Add user' : 'Save changes'}</Button></>}>
        <div className="space-y-4">
          <Field label="Full name" htmlFor="f1"><Input id="f1" value={form.fullName} onChange={set('fullName')} /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Username" htmlFor="f2"><Input id="f2" value={form.username} onChange={set('username')} disabled={editing !== 'new'} /></Field>
            <Field label="Email" htmlFor="f3"><Input id="f3" type="email" value={form.email} onChange={set('email')} /></Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Role" htmlFor="f4" hint={form.role === 'TECHNICIAN' ? 'A technician profile is created automatically.' : undefined}>
              <Select id="f4" value={form.role} onChange={set('role')}>{Object.entries(ROLE).map(([k, r]) => <option key={k} value={k}>{r.label}</option>)}</Select>
            </Field>
            <Field label="Password" htmlFor="f5" hint={editing === 'new' ? 'At least 8 characters' : 'Leave blank to keep the current one'}>
              <Input id="f5" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} />
            </Field>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Department" htmlFor="f6" optional><Input id="f6" value={form.department} onChange={set('department')} /></Field>
            <Field label="Phone" htmlFor="f7" optional hint="Needed for SMS alerts"><Input id="f7" value={form.phoneNumber} onChange={set('phoneNumber')} /></Field>
          </div>
        </div>
      </Modal>
    </>
  );
}
