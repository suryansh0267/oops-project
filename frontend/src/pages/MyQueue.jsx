import { useState } from 'react';
import { Inbox } from 'lucide-react';
import { useApi } from '../lib/hooks';
import { EmptyState, ErrorNote, Loading, PageHeader, Panel } from '../components/ui';
import IncidentRows from '../components/IncidentRows';

const TABS = [
  { key: 'todo', label: 'To acknowledge', statuses: ['ASSIGNED'] },
  { key: 'active', label: 'Working on', statuses: ['ACKNOWLEDGED', 'IN_PROGRESS'] },
  { key: 'resolved', label: 'Awaiting reporter', statuses: ['RESOLVED'] },
  { key: 'done', label: 'Done', statuses: ['VERIFIED', 'CLOSED'] },
];

const urgency = (a, b) =>
  new Date(!a.respondedAt ? a.responseDeadline : a.resolutionDeadline) - new Date(!b.respondedAt ? b.responseDeadline : b.resolutionDeadline);

export default function MyQueue() {
  const { data, loading, error, reload } = useApi('/technician/incidents', { initial: [], poll: 30000 });
  const [tab, setTab] = useState('todo');
  const [frustratedFirst, setFrustratedFirst] = useState(false);
  const { data: flags } = useApi('/dashboard/frustration', { initial: { incidents: [] } });
  const current = TABS.find((t) => t.key === tab);
  const rank = (i) => flags?.incidents?.find((f) => f.id === i.id)?.level === 'CRITICAL' ? 2
    : flags?.incidents?.some((f) => f.id === i.id) ? 1 : 0;
  const rows = data.filter((i) => current.statuses.includes(i.status))
    .sort((a, b) => frustratedFirst ? rank(b) - rank(a) || urgency(a, b) : urgency(a, b));

  return (
    <>
      <PageHeader title="My queue" description="Incidents assigned to you. Acknowledge new ones quickly: the response clock is running." />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <label className="mb-3 inline-flex items-center gap-2 text-sm"><input type="checkbox" checked={frustratedFirst}
        onChange={(e) => setFrustratedFirst(e.target.checked)} /> Frustrated first (heuristic)</label>
      <div className="mb-4 flex flex-wrap gap-1 rounded-xl border border-line bg-surface p-1" role="tablist">
        {TABS.map((t) => {
          const count = data.filter((i) => t.statuses.includes(i.status)).length;
          return (
            <button
              key={t.key}
              role="tab"
              aria-selected={tab === t.key}
              onClick={() => setTab(t.key)}
              className={`rounded-lg px-4 py-2 text-sm font-semibold ${tab === t.key ? 'bg-ink text-white' : 'text-steel hover:bg-sunken'}`}
            >
              {t.label} <span className="tnum ml-1 opacity-70">{count}</span>
            </button>
          );
        })}
      </div>
      <Panel padded={false}>
        {loading ? (
          <Loading />
        ) : (
          <IncidentRows
            incidents={rows}
            showAssignee={false}
            showSla={tab !== 'done'}
            empty={<EmptyState icon={Inbox} title="Nothing here">{tab === 'todo' ? 'New assignments will appear here.' : 'No incidents in this group.'}</EmptyState>}
          />
        )}
      </Panel>
    </>
  );
}
