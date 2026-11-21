import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Lightbulb, Copy } from 'lucide-react';
import { toast } from 'react-toastify';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useApi, useDebounced } from '../lib/hooks';
import { Button, ErrorNote, Field, Input, PageHeader, Panel, Select, StatusBadge, Textarea } from '../components/ui';
import { IMPACT_HELP, URGENCY_HELP, apiError, categoryLabel, timeAgo } from '../lib/format';

const AUTO = '';

export default function ReportIncident() {
  const navigate = useNavigate();
  const { isEmployee } = useAuth();
  const { data: services } = useApi('/public/services', { initial: [] });
  const { data: categories } = useApi('/public/categories', { initial: [] });

  const [form, setForm] = useState({
    title: '',
    description: '',
    category: AUTO,
    impact: 'MEDIUM',
    urgency: 'MEDIUM',
    affectedServiceId: '',
    affectedUsers: '',
  });
  const [suggestion, setSuggestion] = useState(null);
  const [similar, setSimilar] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const draft = useDebounced(`${form.title}\u0000${form.description}\u0000${form.affectedServiceId}`, 500);

  // Live classification + duplicate check (intelligent features F and A)
  useEffect(() => {
    const [title, description, serviceId] = draft.split('\u0000');
    if (title.trim().length < 5 && description.trim().length < 10) {
      setSuggestion(null);
      setSimilar([]);
      return;
    }
    let cancelled = false;
    const body = { title, description, affectedServiceId: serviceId ? Number(serviceId) : null };
    Promise.all([api.post('/incidents/classify', body), api.post('/incidents/similar', body)])
      .then(([c, s]) => {
        if (cancelled) return;
        setSuggestion(c.data.confidence > 0 ? c.data : null);
        setSimilar(s.data);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [draft]);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const res = await api.post('/incidents', {
        title: form.title.trim(),
        description: form.description.trim(),
        category: form.category || null,
        impact: form.impact,
        urgency: form.urgency,
        affectedServiceId: form.affectedServiceId ? Number(form.affectedServiceId) : null,
        affectedUsers: form.affectedUsers ? Number(form.affectedUsers) : null,
      });
      toast.success(`${res.data.incidentNumber} logged as ${res.data.priority.replace('_', ' ').toLowerCase()} priority`);
      navigate(`/incidents/${res.data.id}`);
    } catch (err) {
      setError(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  const chosenCategory = form.category || suggestion?.category;

  return (
    <>
      <PageHeader
        title={isEmployee ? 'Report an incident' : 'Log an incident'}
        description="Describe what's wrong. We work out the category, priority and deadlines, then route it to the right technician."
        back={{ to: '/incidents', label: 'Incidents' }}
      />

      <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
        <Panel>
          <form onSubmit={submit} className="space-y-5">
            <ErrorNote>{error}</ErrorNote>

            <Field label="What's the problem?" htmlFor="title" hint="A short summary, like 'Payment page shows error 503'">
              <Input id="title" required minLength={5} maxLength={150} value={form.title} onChange={set('title')} />
            </Field>

            <Field label="Details" htmlFor="desc" hint="What were you doing, what did you expect, and what happened? Include any error message.">
              <Textarea id="desc" required maxLength={5000} rows={5} value={form.description} onChange={set('description')} />
            </Field>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Affected service" htmlFor="svc" optional>
                <Select id="svc" value={form.affectedServiceId} onChange={set('affectedServiceId')}>
                  <option value="">Not sure / none</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </Select>
              </Field>

              <Field label="Category" htmlFor="cat">
                <Select id="cat" value={form.category} onChange={set('category')}>
                  <option value={AUTO}>{suggestion ? `Suggested: ${categoryLabel(suggestion.category)}` : 'Let the system decide'}</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.name}>
                      {categoryLabel(c.name)}
                    </option>
                  ))}
                </Select>
              </Field>
            </div>

            <fieldset className="grid gap-5 sm:grid-cols-2">
              <legend className="sr-only">Impact and urgency</legend>
              <Field label="How many people does it affect?" htmlFor="impact" hint={IMPACT_HELP[form.impact]}>
                <Select id="impact" value={form.impact} onChange={set('impact')}>
                  <option value="LOW">Just me</option>
                  <option value="MEDIUM">My team</option>
                  <option value="HIGH">A department or customers</option>
                </Select>
              </Field>
              <Field label="How urgent is it?" htmlFor="urgency" hint={URGENCY_HELP[form.urgency]}>
                <Select id="urgency" value={form.urgency} onChange={set('urgency')}>
                  <option value="LOW">It can wait</option>
                  <option value="MEDIUM">Slowing me down</option>
                  <option value="HIGH">Work has stopped</option>
                </Select>
              </Field>
            </fieldset>

            <Field label="Roughly how many users are affected?" htmlFor="users" optional hint="50 or more raises the priority one level; 500 or more, two levels.">
              <Input id="users" type="number" min={1} max={100000} className="max-w-40" value={form.affectedUsers} onChange={set('affectedUsers')} />
            </Field>

            <div className="flex justify-end gap-2 border-t border-line-soft pt-5">
              <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
                Cancel
              </Button>
              <Button type="submit" loading={busy}>
                {isEmployee ? 'Submit report' : 'Log incident'}
              </Button>
            </div>
          </form>
        </Panel>

        <div className="space-y-4">
          {similar.length > 0 && (
            <Panel title="Already reported?">
              <p className="mb-3 text-sm text-steel">{isEmployee ? 'Our team is already working on incidents that look like this. If yours is the same problem, you may not need to report it again.' : 'These open incidents look similar. Open one to link your work instead of filing a duplicate.'}</p>
              <ul className="space-y-2">
                {similar.map((s) => {
                  const body = (
                    <div className="flex items-start gap-2">
                      <Copy className="mt-0.5 h-4 w-4 shrink-0 text-warn" aria-hidden />
                      <div className="min-w-0">
                        <p className="text-sm font-semibold">{s.title}</p>
                        <p className="mt-1 flex flex-wrap items-center gap-2 text-xs text-muted">
                          {s.incidentNumber} <StatusBadge status={s.status} /> {Math.round(s.score * 100)}% match · {timeAgo(s.createdAt)}
                        </p>
                      </div>
                    </div>
                  );
                  // Employees cannot open other people's incidents, so they just see that it's known.
                  return (
                    <li key={s.id}>
                      {isEmployee ? (
                        <div className="rounded-lg border border-line p-3">{body}</div>
                      ) : (
                        <Link to={`/incidents/${s.id}`} className="block rounded-lg border border-line p-3 hover:bg-sunken">{body}</Link>
                      )}
                    </li>
                  );
                })}
              </ul>
            </Panel>
          )}

          <Panel title="What happens next">
            {suggestion && !form.category && (
              <div className="mb-4 flex gap-3 rounded-lg bg-p3-soft p-3 text-sm">
                <Lightbulb className="h-4 w-4 shrink-0 text-p3" aria-hidden />
                <p>
                  Looks like a <strong>{categoryLabel(suggestion.category)}</strong> issue ({Math.round(suggestion.confidence * 100)}% sure, based on
                  “{suggestion.matchedKeywords.slice(0, 3).join('”, “')}”).
                </p>
              </div>
            )}
            <ol className="space-y-3 text-sm text-steel">
              <li>
                <strong className="text-ink">1. Classified</strong> as {chosenCategory ? categoryLabel(chosenCategory) : 'the closest category'}.
              </li>
              <li>
                <strong className="text-ink">2. Prioritised</strong> from impact, urgency, the service's business criticality and number of users.
              </li>
              <li>
                <strong className="text-ink">3. Deadlines set</strong> from the SLA policy for that priority.
              </li>
              <li>
                <strong className="text-ink">4. Routed</strong> to a technician with matching skills and free capacity.
              </li>
              <li>
                <strong className="text-ink">5. You're kept updated</strong> in the app and by email until it is resolved.
              </li>
            </ol>
          </Panel>
        </div>
      </div>
    </>
  );
}
