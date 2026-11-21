import { useState } from 'react';
import { Mail, MessageSquare } from 'lucide-react';
import { useApi } from '../../lib/hooks';
import { EmptyState, ErrorNote, Loading, PageHeader, Panel } from '../../components/ui';
import { formatDateTime } from '../../lib/format';

export default function AdminOutbox() {
  const { data, loading, error, reload } = useApi('/admin/notifications/outbox', { initial: [], poll: 20000 });
  const [channel, setChannel] = useState('');
  const rows = data.filter((m) => !channel || m.channel === channel);

  return (
    <>
      <PageHeader title="Email & SMS outbox" description="Messages the notification channels have sent. Delivery is simulated: plug a mail or SMS provider into the channel classes to send them for real." />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <div className="mb-4 flex gap-1 rounded-xl border border-line bg-surface p-1" role="tablist">
        {[['', 'All'], ['EMAIL', 'Email'], ['SMS', 'SMS']].map(([k, l]) => (
          <button key={k} role="tab" aria-selected={channel === k} onClick={() => setChannel(k)} className={`rounded-lg px-4 py-2 text-sm font-semibold ${channel === k ? 'bg-ink text-white' : 'text-steel hover:bg-sunken'}`}>{l}</button>
        ))}
      </div>
      <Panel padded={false}>
        {loading ? <Loading /> : rows.length === 0 ? <EmptyState icon={Mail} title="Nothing sent yet" /> : (
          <ul className="divide-y divide-line-soft">
            {rows.map((m) => (
              <li key={m.id} className="flex gap-3 px-5 py-3">
                {m.channel === 'SMS' ? <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-p2" aria-label="SMS" /> : <Mail className="mt-0.5 h-4 w-4 shrink-0 text-p3" aria-label="Email" />}
                <div className="min-w-0">
                  <p className="text-sm"><span className="font-semibold">{m.title}</span> <span className="text-muted">to {m.recipientName} ({m.to})</span></p>
                  <p className="text-[13px] text-steel">{m.message}</p>
                  <p className="text-xs text-muted">{formatDateTime(m.createdAt)}{m.incidentNumber && ` · ${m.incidentNumber}`}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Panel>
    </>
  );
}
