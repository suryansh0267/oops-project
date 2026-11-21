import { useState } from 'react';
import { Pencil } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/client';
import { useApi } from '../../lib/hooks';
import { Button, ErrorNote, Field, Input, Loading, Meter, Modal, PageHeader, Panel, Toggle } from '../../components/ui';
import { apiError, categoryLabel, minutesHuman } from '../../lib/format';

export default function AdminTechnicians() {
  const { data, loading, error, reload } = useApi('/admin/technicians', { initial: [] });
  const { data: categories } = useApi('/admin/categories', { initial: [] });
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);

  const save = async () => {
    setBusy(true);
    try {
      await api.put(`/admin/technicians/${editing.userId}`, {
        skills: editing.skills,
        specialization: editing.specialization,
        maxActiveIncidents: Number(editing.maxActiveIncidents),
        available: editing.available,
      });
      toast.success('Technician updated');
      setEditing(null);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  const toggleSkill = (name) =>
    setEditing((t) => ({ ...t, skills: t.skills.includes(name) ? t.skills.filter((s) => s !== name) : [...t.skills, name] }));

  return (
    <>
      <PageHeader title="Technicians" description="Skills decide which incidents a technician is matched to; capacity caps how many they hold at once. Add new technicians from Users." />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading ? <Loading /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {data.map((t) => (
            <Panel key={t.userId}>
              <div className="mb-3 flex items-start justify-between gap-2">
                <div>
                  <p className="font-semibold">{t.fullName}</p>
                  <p className="text-[13px] text-muted">{t.specialization || t.department}</p>
                </div>
                <span className={`text-xs font-semibold ${t.available && t.active ? 'text-ok' : 'text-muted'}`}>{!t.active ? 'Deactivated' : t.available ? 'Available' : 'Unavailable'}</span>
              </div>
              <Meter label="Workload" value={t.currentActiveIncidents} max={t.maxActiveIncidents} />
              <div className="mt-3 flex flex-wrap gap-1.5">
                {t.skills.map((s) => <span key={s} className="rounded-md bg-sunken px-2 py-0.5 text-xs font-semibold">{categoryLabel(s)}</span>)}
              </div>
              <div className="mt-4 flex items-center justify-between border-t border-line-soft pt-3 text-[13px] text-steel">
                <span>{t.totalResolvedIncidents} resolved · avg {minutesHuman(t.avgResolutionMinutes)}</span>
                <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setEditing({ ...t })}>Edit</Button>
              </div>
            </Panel>
          ))}
        </div>
      )}

      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing ? `Edit ${editing.fullName}` : ''}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
        {editing && (
          <div className="space-y-4">
            <Field label="Skills" hint="Matched against incident categories by the skill-based and smart strategies.">
              <div className="flex flex-wrap gap-2">
                {categories.filter((c) => c.active).map((c) => (
                  <button key={c.id} type="button" onClick={() => toggleSkill(c.name)} aria-pressed={editing.skills.includes(c.name)}
                    className={`rounded-full border px-3 py-1 text-sm font-semibold ${editing.skills.includes(c.name) ? 'border-ink bg-ink text-white' : 'border-line text-steel hover:bg-sunken'}`}>
                    {categoryLabel(c.name)}
                  </button>
                ))}
              </div>
            </Field>
            <Field label="Specialisation" htmlFor="sp"><Input id="sp" value={editing.specialization} onChange={(e) => setEditing({ ...editing, specialization: e.target.value })} /></Field>
            <Field label="Maximum active incidents" htmlFor="mx" hint={`Currently holding ${editing.currentActiveIncidents}.`}>
              <Input id="mx" type="number" min={1} max={50} className="max-w-32" value={editing.maxActiveIncidents} onChange={(e) => setEditing({ ...editing, maxActiveIncidents: e.target.value })} />
            </Field>
            <Toggle checked={editing.available} onChange={(v) => setEditing({ ...editing, available: v })} label="Available for new incidents" />
          </div>
        )}
      </Modal>
    </>
  );
}
