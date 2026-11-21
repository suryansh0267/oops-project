import { toast } from 'react-toastify';
import api from '../../api/client';
import { useApi } from '../../lib/hooks';
import { ErrorNote, Loading, PageHeader, Panel } from '../../components/ui';
import { apiError } from '../../lib/format';

function StrategyPicker({ title, description, listUrl, setUrl }) {
  const { data, loading, error, reload, setData } = useApi(listUrl, { initial: [] });

  const choose = async (key) => {
    try {
      await api.put(setUrl, { strategy: key });
      setData(data.map((s) => ({ ...s, isActive: s.key === key })));
      toast.success('Strategy changed. It applies to the next incident.');
    } catch (e) {
      toast.error(apiError(e));
    }
  };

  return (
    <Panel title={title}>
      <p className="mb-4 text-sm text-steel">{description}</p>
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading ? <Loading /> : (
        <div className="space-y-2" role="radiogroup" aria-label={title}>
          {data.map((s) => (
            <label key={s.key} className={`flex cursor-pointer gap-3 rounded-lg border p-4 ${s.isActive ? 'border-ink bg-sunken' : 'border-line hover:bg-sunken'}`}>
              <input type="radio" name={title} checked={s.isActive} onChange={() => choose(s.key)} className="mt-1" />
              <span>
                <span className="block font-semibold">{s.name}</span>
                <span className="block text-sm text-steel">{s.description}</span>
              </span>
            </label>
          ))}
        </div>
      )}
    </Panel>
  );
}

export default function AdminAutomation() {
  const { data: frustration } = useApi('/admin/frustration-config');
  return (
    <>
      <PageHeader title="Automation" description="Swap the algorithms the desk uses, live. Each option is a separate strategy class behind a common interface." />
      <div className="grid gap-6 xl:grid-cols-2">
        <StrategyPicker title="Technician assignment" description="Used when a new incident arrives, when an admin clicks Auto-assign, and when the SLA monitor escalates." listUrl="/admin/assignment-strategies" setUrl="/admin/assignment-strategy" />
        <StrategyPicker title="Priority calculation" description="Turns impact, urgency and the service's business criticality into P1-P4. 50+ affected users raises the result one level, 500+ two levels." listUrl="/admin/priority-strategies" setUrl="/admin/priority-strategy" />
      </div>
      <div className="mt-6">
        <Panel title="Frustration detection · runtime settings">
          <p className="mb-3 text-sm text-steel">These settings are read-only here. To change them, edit the Docker environment variables and restart the backend. Detection is heuristic and may misfire.</p>
          {frustration ? <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
            {[
              ['Enabled', frustration.enabled ? 'Yes' : 'No'],
              ['Annoyed threshold', frustration.annoyed],
              ['Frustrated threshold', frustration.frustrated],
              ['Critical threshold', frustration.critical],
              ['Auto-escalate priority', frustration.autoEscalate ? 'Enabled' : 'Off'],
              ['LLM sentiment', frustration.llmSentiment ? 'Enabled' : 'Off'],
            ].map(([label, value]) => <div key={label} className="rounded-lg bg-sunken p-3">
              <dt className="text-muted">{label}</dt><dd className="mt-1 font-semibold">{value}</dd>
            </div>)}
          </dl> : <Loading />}
        </Panel>
      </div>
    </>
  );
}
