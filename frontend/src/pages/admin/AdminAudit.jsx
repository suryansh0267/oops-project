import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApi } from '../../lib/hooks';
import { Button, EmptyState, ErrorNote, Input, Loading, PageHeader, Panel, Table } from '../../components/ui';
import { formatDateTime, titleCase } from '../../lib/format';

export default function AdminAudit() {
  const [range, setRange] = useState({ start: '', end: '' });
  const [url, setUrl] = useState('/admin/audit-logs');
  const [text, setText] = useState('');
  const { data, loading, error, reload } = useApi(url, { initial: [] });

  const apply = (e) => {
    e.preventDefault();
    const p = new URLSearchParams();
    if (range.start) p.set('start', `${range.start}T00:00:00`);
    if (range.end) p.set('end', `${range.end}T23:59:59`);
    setUrl(`/admin/audit-logs${p.toString() ? `?${p}` : ''}`);
  };

  const q = text.trim().toLowerCase();
  const rows = q ? data.filter((l) => [l.action, l.description, l.performedByName, l.incidentNumber].some((v) => v?.toLowerCase().includes(q))) : data;

  return (
    <>
      <PageHeader title="Audit log" description="Every important action, who did it and when. Automatic actions are recorded as System." />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <Panel padded={false}>
        <form onSubmit={apply} className="flex flex-wrap items-end gap-3 border-b border-line-soft p-4">
          <label className="text-sm"><span className="mb-1 block font-semibold">From</span><Input type="date" value={range.start} onChange={(e) => setRange({ ...range, start: e.target.value })} /></label>
          <label className="text-sm"><span className="mb-1 block font-semibold">To</span><Input type="date" value={range.end} onChange={(e) => setRange({ ...range, end: e.target.value })} /></label>
          <Button type="submit" variant="secondary">Apply dates</Button>
          <label className="ml-auto min-w-56 flex-1 text-sm sm:flex-none"><span className="mb-1 block font-semibold">Filter</span><Input value={text} onChange={(e) => setText(e.target.value)} placeholder="Action, person or INC number" /></label>
        </form>
        {loading ? <Loading /> : (
          <Table rows={rows} columns={[
            { key: 'timestamp', header: 'When', className: 'whitespace-nowrap text-steel tnum', render: (l) => formatDateTime(l.timestamp) },
            { key: 'incident', header: 'Incident', render: (l) => (l.incidentId ? <Link to={`/incidents/${l.incidentId}`} className="font-semibold hover:underline">{l.incidentNumber}</Link> : '—') },
            { key: 'action', header: 'Action', render: (l) => <span className="font-semibold">{titleCase(l.action)}</span> },
            { key: 'change', header: 'Change', className: 'hidden lg:table-cell text-steel', render: (l) => (l.previousValue || l.newValue ? `${l.previousValue || '—'} → ${l.newValue || '—'}` : '') },
            { key: 'description', header: 'Details', className: 'text-steel' },
            { key: 'performedByName', header: 'By', className: 'whitespace-nowrap' },
          ]} empty={<EmptyState title="No entries for this period" />} />
        )}
      </Panel>
    </>
  );
}
