import { useState } from 'react';
import { Plus, Pencil, Power } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/client';
import { useApi } from '../../lib/hooks';
import { Button, ErrorNote, Field, Input, Loading, Modal, PageHeader, Panel, Select, Table, Textarea } from '../../components/ui';
import { apiError } from '../../lib/format';

const EMPTY = { name: '', description: '', category: '', businessCriticality: 3, supportContact: '' };
const CRIT = { 1: 'Minimal', 2: 'Low', 3: 'Moderate', 4: 'High', 5: 'Business critical' };

export default function AdminServices() {
  const { data, loading, error, reload } = useApi('/admin/services', { initial: [] });
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const save = async () => {
    setBusy(true);
    try {
      const body = { ...form, businessCriticality: Number(form.businessCriticality) };
      if (editing === 'new') await api.post('/admin/services', body);
      else await api.put(`/admin/services/${editing.id}`, body);
      toast.success('Service saved');
      setEditing(null);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (s) => {
    try {
      if (s.active) await api.delete(`/admin/services/${s.id}`);
      else await api.put(`/admin/services/${s.id}/activate`);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    }
  };

  return (
    <>
      <PageHeader title="IT services" description="Business criticality (1-5) feeds directly into incident priority." actions={<Button icon={Plus} onClick={() => { setEditing('new'); setForm(EMPTY); }}>Add service</Button>} />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <Panel padded={false}>
        {loading ? <Loading /> : (
          <Table rows={data} columns={[
            { key: 'name', header: 'Service', render: (s) => (<div><p className="font-semibold">{s.name}</p><p className="text-[13px] text-muted">{s.description}</p></div>) },
            { key: 'businessCriticality', header: 'Criticality', render: (s) => <span className="tnum"><strong>{s.businessCriticality}</strong> <span className="text-steel">{CRIT[s.businessCriticality]}</span></span> },
            { key: 'supportContact', header: 'Contact', className: 'hidden md:table-cell text-steel' },
            { key: 'active', header: 'Status', render: (s) => <span className={s.active ? 'font-semibold text-ok' : 'text-muted'}>{s.active ? 'Active' : 'Retired'}</span> },
            { key: 'a', header: '', className: 'text-right whitespace-nowrap', render: (s) => (
              <div className="flex justify-end gap-1">
                <Button size="sm" variant="ghost" icon={Pencil} onClick={() => { setEditing(s); setForm({ ...EMPTY, ...s, description: s.description || '', category: s.category || '', supportContact: s.supportContact || '' }); }}>Edit</Button>
                <Button size="sm" variant={s.active ? 'danger' : 'secondary'} icon={Power} onClick={() => toggle(s)}>{s.active ? 'Retire' : 'Restore'}</Button>
              </div>
            ) },
          ]} />
        )}
      </Panel>
      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing === 'new' ? 'Add service' : 'Edit service'}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
        <div className="space-y-4">
          <Field label="Name" htmlFor="s1"><Input id="s1" value={form.name} onChange={set('name')} /></Field>
          <Field label="Description" htmlFor="s2" optional><Textarea id="s2" rows={2} value={form.description} onChange={set('description')} /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Business criticality" htmlFor="s3">
              <Select id="s3" value={form.businessCriticality} onChange={set('businessCriticality')}>{[1, 2, 3, 4, 5].map((n) => <option key={n} value={n}>{n} · {CRIT[n]}</option>)}</Select>
            </Field>
            <Field label="Group" htmlFor="s4" optional><Input id="s4" value={form.category} onChange={set('category')} placeholder="e.g. Finance" /></Field>
          </div>
          <Field label="Support contact" htmlFor="s5" optional><Input id="s5" value={form.supportContact} onChange={set('supportContact')} /></Field>
        </div>
      </Modal>
    </>
  );
}
