import { useState } from 'react';
import { Plus, Pencil, Power } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../../api/client';
import { useApi } from '../../lib/hooks';
import { Button, ErrorNote, Field, Input, Loading, Modal, PageHeader, Panel, Table, Textarea } from '../../components/ui';
import { apiError, categoryLabel } from '../../lib/format';

export default function AdminCategories() {
  const { data, loading, error, reload } = useApi('/admin/categories', { initial: [] });
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);

  const save = async () => {
    setBusy(true);
    try {
      const body = {
        name: editing.name,
        description: editing.description,
        keywords: editing.keywordsText.split(',').map((k) => k.trim()).filter(Boolean),
      };
      if (editing.id) await api.put(`/admin/categories/${editing.id}`, body);
      else await api.post('/admin/categories', body);
      toast.success('Category saved. It is available to reporters, filters and the categoriser now.');
      setEditing(null);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (c) => {
    try {
      if (c.active) await api.delete(`/admin/categories/${c.id}`);
      else await api.put(`/admin/categories/${c.id}/activate`);
      reload(true);
    } catch (e) {
      toast.error(apiError(e));
    }
  };

  return (
    <>
      <PageHeader title="Incident categories" description="Add categories without touching code. Keywords teach the automatic categoriser what each category looks like."
        actions={<Button icon={Plus} onClick={() => setEditing({ name: '', description: '', keywordsText: '' })}>Add category</Button>} />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <Panel padded={false}>
        {loading ? <Loading /> : (
          <Table rows={data} columns={[
            { key: 'name', header: 'Category', render: (c) => (<div><p className="font-semibold">{categoryLabel(c.name)}</p><p className="text-[13px] text-muted">{c.description}</p></div>) },
            { key: 'keywords', header: 'Keywords', className: 'hidden md:table-cell', render: (c) => <span className="text-[13px] text-steel">{c.keywords?.length ? c.keywords.join(', ') : 'Using built-in keywords'}</span> },
            { key: 'active', header: 'Status', render: (c) => <span className={c.active ? 'font-semibold text-ok' : 'text-muted'}>{c.active ? 'Active' : 'Hidden'}</span> },
            { key: 'a', header: '', className: 'text-right whitespace-nowrap', render: (c) => (
              <div className="flex justify-end gap-1">
                <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setEditing({ ...c, description: c.description || '', keywordsText: (c.keywords || []).join(', ') })}>Edit</Button>
                <Button size="sm" variant={c.active ? 'danger' : 'secondary'} icon={Power} onClick={() => toggle(c)}>{c.active ? 'Hide' : 'Show'}</Button>
              </div>
            ) },
          ]} />
        )}
      </Panel>
      <Modal open={Boolean(editing)} onClose={() => setEditing(null)} title={editing?.id ? `Edit ${categoryLabel(editing.name)}` : 'Add category'}
        footer={<><Button variant="secondary" onClick={() => setEditing(null)}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
        {editing && (
          <div className="space-y-4">
            <Field label="Name" htmlFor="cn" hint={editing.id ? 'Names are fixed once created because incidents refer to them.' : 'e.g. Printing, Telephony'}>
              <Input id="cn" value={editing.name} disabled={Boolean(editing.id)} onChange={(e) => setEditing({ ...editing, name: e.target.value })} />
            </Field>
            <Field label="Description" htmlFor="cd" optional><Input id="cd" value={editing.description} onChange={(e) => setEditing({ ...editing, description: e.target.value })} /></Field>
            <Field label="Keywords" htmlFor="ck" hint="Comma separated. Words that usually appear in incidents of this kind.">
              <Textarea id="ck" rows={3} value={editing.keywordsText} onChange={(e) => setEditing({ ...editing, keywordsText: e.target.value })} placeholder="printer, toner, paper jam, scanner" />
            </Field>
          </div>
        )}
      </Modal>
    </>
  );
}
