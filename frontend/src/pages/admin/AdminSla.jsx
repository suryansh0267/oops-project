import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Pencil, Plus } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/client';
import { useApi } from '../../lib/hooks';
import { Button, EmptyState, ErrorNote, Field, Input, Loading, Modal, PageHeader, Panel, PriorityBadge, Select, SlaClock, Table } from '../../components/ui';
import { PRIORITY, apiError, formatDateTime, minutesHuman } from '../../lib/format';

export default function AdminSla() {
  const policies = useApi('/admin/sla-policies', { initial: [] });
  const breached = useApi('/admin/sla-violations', { initial: [], poll: 30000 });
  const atRisk = useApi('/admin/sla-approaching', { initial: [], poll: 30000 });
  const history = useApi('/admin/sla-violations/history', { initial: [] });
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);

  const missing = Object.keys(PRIORITY).filter((p) => !policies.data.some((x) => x.priority === p));

  const save = async () => {
    setBusy(true);
    try {
      const body = { priority: editing.priority, responseTimeMinutes: Number(editing.responseTimeMinutes), resolutionTimeMinutes: Number(editing.resolutionTimeMinutes), description: editing.description };
      if (editing.id) await api.put(`/admin/sla-policies/${editing.id}`, body);
      else await api.post('/admin/sla-policies', body);
      toast.success('Policy saved. New incidents use it straight away.');
      setEditing(null);
      policies.reload(true);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  const incidentCols = [
    { key: 't', header: 'Incident', render: (i) => (<Link to={`/incidents/${i.id}`} className="font-semibold hover:underline">{i.incidentNumber} · {i.title}</Link>) },
    { key: 'p', header: 'Priority', render: (i) => <PriorityBadge priority={i.priority} compact /> },
    { key: 'a', header: 'Assignee', className: 'hidden md:table-cell', render: (i) => i.assignedTechnicianName || <span className="font-semibold text-warn">Unassigned</span> },
    { key: 's', header: 'Clock', render: (i) => <SlaClock incident={i} size="sm" /> },
  ];

  return (
    <>
      <PageHeader title="SLA policies" description="Response and resolution targets per priority. The SLA monitor checks open incidents every minute, warns at 75% of the window, records breaches and escalates unacknowledged P1/P2 incidents."
        actions={missing.length > 0 && <Button icon={Plus} onClick={() => setEditing({ priority: missing[0], responseTimeMinutes: 60, resolutionTimeMinutes: 480, description: '' })}>Add policy</Button>} />
      <ErrorNote onRetry={policies.reload}>{policies.error}</ErrorNote>

      <Panel padded={false} className="mb-6">
        {policies.loading ? <Loading /> : (
          <Table rows={policies.data} columns={[
            { key: 'priority', header: 'Priority', render: (p) => <PriorityBadge priority={p.priority} /> },
            { key: 'r', header: 'Respond within', className: 'tnum', render: (p) => minutesHuman(p.responseTimeMinutes) },
            { key: 'x', header: 'Resolve within', className: 'tnum', render: (p) => minutesHuman(p.resolutionTimeMinutes) },
            { key: 'description', header: 'Description', className: 'hidden md:table-cell text-steel' },
            { key: 'e', header: '', className: 'text-right', render: (p) => <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setEditing({ ...p, description: p.description || '' })}>Edit</Button> },
          ]} />
        )}
        {missing.length > 0 && <p className="border-t border-line-soft px-4 py-3 text-sm text-warn">No policy for {missing.join(', ')}. Those incidents get default targets of 2 h response and 24 h resolution.</p>}
      </Panel>

      <div className="grid gap-6 xl:grid-cols-2">
        <Panel title={`Breached now (${breached.data.length})`} padded={false}>
          <Table rows={breached.data} columns={incidentCols} empty={<EmptyState title="No open incident is past its deadline" />} />
        </Panel>
        <Panel title={`At risk (${atRisk.data.length})`} padded={false}>
          <Table rows={atRisk.data} columns={incidentCols} empty={<EmptyState title="Nothing close to breaching" />} />
        </Panel>
      </div>

      <Panel title="Breach record" padded={false} className="mt-6">
        <Table rows={history.data} columns={[
          { key: 'i', header: 'Incident', render: (v) => <Link to={`/incidents/${v.incidentId}`} className="font-semibold hover:underline">{v.incidentNumber} · {v.title}</Link> },
          { key: 'type', header: 'Target missed', render: (v) => (v.type === 'RESPONSE' ? 'Response' : 'Resolution') },
          { key: 'technician', header: 'Technician', className: 'hidden md:table-cell' },
          { key: 'd', header: 'Deadline', className: 'hidden lg:table-cell text-steel', render: (v) => formatDateTime(v.deadline) },
          { key: 'detectedAt', header: 'Detected', className: 'text-steel', render: (v) => formatDateTime(v.detectedAt) },
        ]} empty={<EmptyState title="No breaches recorded yet">The SLA monitor records each missed target here, once.</EmptyState>} />
      </Panel>

      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing?.id ? 'Edit policy' : 'Add policy'}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
        {editing && (
          <div className="space-y-4">
            <Field label="Priority" htmlFor="pp">
              <Select id="pp" value={editing.priority} onChange={(e) => setEditing({ ...editing, priority: e.target.value })} disabled={Boolean(editing.id)}>
                {Object.entries(PRIORITY).filter(([k]) => editing.id || missing.includes(k)).map(([k, p]) => <option key={k} value={k}>{p.short} {p.label}</option>)}
              </Select>
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Respond within (minutes)" htmlFor="pr"><Input id="pr" type="number" min={1} value={editing.responseTimeMinutes} onChange={(e) => setEditing({ ...editing, responseTimeMinutes: e.target.value })} /></Field>
              <Field label="Resolve within (minutes)" htmlFor="px"><Input id="px" type="number" min={1} value={editing.resolutionTimeMinutes} onChange={(e) => setEditing({ ...editing, resolutionTimeMinutes: e.target.value })} /></Field>
            </div>
            <Field label="Description" htmlFor="pd" optional><Input id="pd" value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} /></Field>
          </div>
        )}
      </Modal>
    </>
  );
}
