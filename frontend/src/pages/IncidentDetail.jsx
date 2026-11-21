import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import {
  UserCheck, Play, CircleCheck, RotateCcw, Lock, Send, Star, Sparkles, MessageSquare, Shuffle, Pencil, HelpCircle, Link2, Lightbulb,
} from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../lib/hooks';
import {
  Button, EmptyState, ErrorNote, Field, Loading, Modal, PageHeader, Panel, PriorityBadge, Select, SlaBadge, SlaClock, StatusBadge, Textarea,
} from '../components/ui';
import { OPEN_STATUSES, STATUS_LABEL, STATUS_ORDER, apiError, categoryLabel, formatDateTime, timeAgo, titleCase } from '../lib/format';
import FrustrationBadge from '../components/FrustrationBadge';

export default function IncidentDetail() {
  const { id } = useParams();
  const { user, role, isAdmin, isStaff } = useAuth();
  const { data: incident, error, loading, reload, setData: setIncident } = useApi(`/incidents/${id}`);
  const notes = useApi(`/incidents/${id}/notes`, { initial: [] });
  const history = useApi(`/incidents/${id}/history`, { initial: [] });
  const related = useApi(isStaff ? `/incidents/${id}/related` : null, { initial: [] });
  const suggestions = useApi(isStaff ? `/incidents/${id}/suggested-resolutions` : null, { initial: [] });
  const flags = useApi(isStaff ? '/dashboard/frustration' : null, { initial: { incidents: [] } });
  const flag = flags.data?.incidents?.find((f) => String(f.id) === String(id));
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);

  if (loading) return <Loading label="Loading incident" />;
  if (error || !incident) return <ErrorNote onRetry={reload}>{error || 'Incident not found'}</ErrorNote>;

  const refreshAll = (updated) => {
    if (updated) setIncident(updated);
    else reload(true);
    notes.reload(true);
    history.reload(true);
  };

  const act = async (path, body, success) => {
    setBusy(true);
    try {
      const res = await api.post(`/incidents/${id}/${path}`, body);
      refreshAll(res.data?.id ? res.data : null);
      setModal(null);
      if (success) toast.success(success);
      return true;
    } catch (e) {
      toast.error(apiError(e));
      return false;
    } finally {
      setBusy(false);
    }
  };

  const dismissFlag = async () => {
    try {
      await api.post(`/incidents/${id}/frustration/dismiss`);
      flags.reload(true);
      toast.success('Heuristic flag dismissed until a new rise');
    } catch (e) { toast.error(apiError(e)); }
  };

  const isOpen = OPEN_STATUSES.includes(incident.status);
  const isReporter = incident.reporterId === user?.id;
  const isAssignee = role === 'TECHNICIAN' && incident.assignedTechnicianId === user?.id;
  const canWork = isAdmin || isAssignee;
  const canReporterAct = isAdmin || isReporter;
  const next = incident.allowedNextStatuses || [];

  const lastRequest = [...notes.data].reverse().find((n) => n.kind === 'INFO_REQUEST' || n.kind === 'INFO_RESPONSE');
  const awaitingReply = lastRequest?.kind === 'INFO_REQUEST' && isOpen;

  return (
    <>
      <PageHeader
        back={{ to: '/incidents', label: 'Incidents' }}
        title={incident.title}
        description={
          <span className="flex flex-wrap items-center gap-x-3 gap-y-1">
            <span className="font-semibold text-ink">{incident.incidentNumber}</span>
            <span>Reported by {incident.reporterName} · {formatDateTime(incident.createdAt)}</span>
          </span>
        }
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <PriorityBadge priority={incident.priority} />
            <StatusBadge status={incident.status} />
            <SlaBadge status={incident.slaStatus} />
            {isStaff && <FrustrationBadge flag={flag} />}
          </div>
        }
      />

      <Lifecycle status={incident.status} />

      {isStaff && flag && <div className="mb-5 rounded-xl border border-warn/30 bg-warn/10 p-4 text-sm">
        <p className="font-semibold">Frustration flag · staff review recommended</p>
        <p className="mt-1">{flag.reasons || 'Recent repeated frustration signals'} · This heuristic can misfire.</p>
        <button onClick={dismissFlag} className="mt-2 font-semibold underline">Dismiss flag</button>
      </div>}

      {awaitingReply && isReporter && (
        <div className="mb-6 rounded-xl border border-p3/30 bg-p3-soft p-5">
          <p className="flex items-center gap-2 font-semibold text-p3">
            <HelpCircle className="h-4 w-4" aria-hidden /> The technician has a question for you
          </p>
          <p className="mt-2 text-[15px]">“{lastRequest.content}”</p>
          <ReplyBox busy={busy} onSend={(message) => act('respond-info', { message }, 'Reply sent to the technician')} />
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[1fr_340px]">
        <div className="min-w-0 space-y-6">
          <Panel title="Description">
            <p className="whitespace-pre-wrap leading-relaxed">{incident.description}</p>
          </Panel>

          {incident.resolutionNotes && (
            <Panel title="Resolution">
              <p className="whitespace-pre-wrap leading-relaxed">{incident.resolutionNotes}</p>
              <p className="mt-2 text-[13px] text-muted">
                By {incident.assignedTechnicianName} · {formatDateTime(incident.resolvedAt)}
              </p>
              {incident.status === 'RESOLVED' && canReporterAct && (
                <div className="mt-4 flex flex-wrap gap-2 border-t border-line-soft pt-4">
                  <Button icon={CircleCheck} loading={busy} onClick={() => act('verify', null, 'Thanks for confirming the fix')}>
                    Yes, it's fixed
                  </Button>
                  <Button variant="secondary" icon={RotateCcw} onClick={() => setModal('reopen')}>
                    No, still broken
                  </Button>
                </div>
              )}
            </Panel>
          )}

          {!isOpen && isReporter && <Feedback incident={incident} onDone={refreshAll} />}
          {!isOpen && incident.feedbackRating > 0 && !isReporter && (
            <Panel title="Reporter feedback">
              <Stars value={incident.feedbackRating} />
              {incident.feedback && <p className="mt-2 text-steel">“{incident.feedback}”</p>}
            </Panel>
          )}

          <Conversation notes={notes.data} isStaff={isStaff} canPost={isOpen || isStaff} onPost={() => refreshAll()} incidentId={id} />

          <Panel title="History" padded={false}>
            <ol className="relative px-5 py-4">
              {history.data.map((h, idx) => (
                <li key={h.id} className="relative pb-4 pl-6 last:pb-0">
                  {idx < history.data.length - 1 && <span className="absolute left-[5px] top-3 h-full w-px bg-line" aria-hidden />}
                  <span className={`absolute left-0 top-1.5 h-[11px] w-[11px] rounded-full border-2 ${h.performedByName === 'System' ? 'border-p3 bg-surface' : 'border-ink bg-ink'}`} aria-hidden />
                  <p className="text-sm">
                    <span className="font-semibold">{titleCase(h.action)}</span>
                    {h.description && <span className="text-steel"> · {h.description}</span>}
                  </p>
                  <p className="text-xs text-muted">
                    {h.performedByName} · {formatDateTime(h.timestamp)}
                  </p>
                </li>
              ))}
            </ol>
          </Panel>
        </div>

        <aside className="space-y-6">
          <Panel title="Service level">
            <div className="space-y-4">
              <SlaClock incident={incident} />
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <Item label="Respond by" value={formatDateTime(incident.responseDeadline)} sub={incident.respondedAt ? `Responded ${timeAgo(incident.respondedAt)}` : 'Not yet acknowledged'} />
                <Item label="Resolve by" value={formatDateTime(incident.resolutionDeadline)} sub={`${Math.min(incident.slaElapsedPercent, 999)}% of window used`} />
              </dl>
              {incident.escalationLevel > 0 && (
                <p className="rounded-lg bg-bad-soft px-3 py-2 text-[13px] font-semibold text-bad">
                  Escalated: level {incident.escalationLevel} {incident.escalationLevel >= 2 ? '(IT manager alerted)' : '(reassigned)'}
                </p>
              )}
            </div>
          </Panel>

          {(canWork || isAdmin || (canReporterAct && ['RESOLVED', 'VERIFIED'].includes(incident.status))) && (
            <Panel title="Actions">
              <div className="flex flex-col gap-2">
                {isAdmin && isOpen && (
                  <Button icon={Shuffle} onClick={() => setModal('assign')}>
                    {incident.assignedTechnicianId ? 'Reassign' : 'Assign technician'}
                  </Button>
                )}
                {canWork && incident.status === 'ASSIGNED' && (
                  <Button icon={UserCheck} loading={busy} onClick={() => act('acknowledge', null, 'Acknowledged. The reporter has been told.')}>
                    Acknowledge
                  </Button>
                )}
                {canWork && incident.status === 'ACKNOWLEDGED' && (
                  <Button icon={Play} loading={busy} onClick={() => act('investigate', null, 'Investigation started')}>
                    Start investigation
                  </Button>
                )}
                {canWork && incident.status === 'IN_PROGRESS' && (
                  <Button icon={CircleCheck} onClick={() => setModal('resolve')}>
                    Resolve
                  </Button>
                )}
                {canWork && isOpen && incident.assignedTechnicianId && (
                  <Button variant="secondary" icon={HelpCircle} onClick={() => setModal('ask')}>
                    Ask the reporter
                  </Button>
                )}
                {isAdmin && isOpen && (
                  <Button variant="secondary" icon={Pencil} onClick={() => setModal('triage')}>
                    Change category or priority
                  </Button>
                )}
                {next.includes('CLOSED') && (canReporterAct || canWork) && (
                  <Button variant="secondary" icon={Lock} loading={busy} onClick={() => act('close', null, 'Incident closed')}>
                    Close incident
                  </Button>
                )}
                {!isOpen && next.length === 0 && <p className="text-sm text-muted">This incident is closed.</p>}
              </div>
            </Panel>
          )}

          <Panel title="Details">
            <dl className="grid grid-cols-2 gap-x-3 gap-y-4 text-sm">
              <Item label="Category" value={categoryLabel(incident.category)} />
              <Item label="Service" value={incident.affectedServiceName || '—'} />
              <Item label="Impact" value={titleCase(incident.impact)} />
              <Item label="Urgency" value={titleCase(incident.urgency)} />
              <Item label="Users affected" value={incident.affectedUsers ?? '—'} />
              <Item label="Assigned to" value={incident.assignedTechnicianName || 'Unassigned'} />
              <Item label="Reporter" value={incident.reporterName} />
              <Item label="Last update" value={timeAgo(incident.updatedAt)} />
            </dl>
          </Panel>

          {isStaff && suggestions.data.length > 0 && (
            <Panel title="Fixes that worked before">
              <ul className="space-y-3">
                {suggestions.data.map((s) => (
                  <li key={s.incidentId} className="rounded-lg border border-line p-3">
                    <div className="flex items-start gap-2">
                      <Lightbulb className="mt-0.5 h-4 w-4 shrink-0 text-warn" aria-hidden />
                      <div className="min-w-0">
                        <p className="text-sm">{s.resolutionNotes}</p>
                        <p className="mt-1 text-xs text-muted">
                          <Link to={`/incidents/${s.incidentId}`} className="hover:underline">
                            {s.incidentNumber}
                          </Link>{' '}
                          · {Math.round(s.score * 100)}% similar
                        </p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </Panel>
          )}

          {isStaff && related.data.length > 0 && (
            <Panel title="Related open incidents">
              <ul className="space-y-2">
                {related.data.map((r) => (
                  <li key={r.id}>
                    <Link to={`/incidents/${r.id}`} className="flex items-start gap-2 rounded-lg p-2 hover:bg-sunken">
                      <Link2 className="mt-0.5 h-4 w-4 shrink-0 text-muted" aria-hidden />
                      <span className="min-w-0">
                        <span className="block truncate text-sm font-semibold">{r.title}</span>
                        <span className="text-xs text-muted">
                          {r.incidentNumber} · {r.reason}
                        </span>
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </Panel>
          )}
        </aside>
      </div>

      <ResolveModal open={modal === 'resolve'} onClose={() => setModal(null)} busy={busy} suggestions={suggestions.data} onSubmit={(resolutionNotes) => act('resolve', { resolutionNotes }, 'Resolved. The reporter has been asked to confirm.')} />
      <TextModal open={modal === 'ask'} onClose={() => setModal(null)} busy={busy} title="Ask the reporter" label="Your question" action="Send question" onSubmit={(message) => act('request-info', { message }, 'Question sent to the reporter')} />
      <TextModal open={modal === 'reopen'} onClose={() => setModal(null)} busy={busy} title="Reopen incident" label="What is still wrong?" action="Reopen" onSubmit={(message) => act('reopen', { message }, 'Reopened and sent back to the technician')} />
      {isAdmin && <AssignModal open={modal === 'assign'} onClose={() => setModal(null)} incident={incident} onDone={(r) => { refreshAll(r); setModal(null); }} />}
      {isAdmin && <TriageModal open={modal === 'triage'} onClose={() => setModal(null)} incident={incident} onDone={(r) => { refreshAll(r); setModal(null); }} />}
    </>
  );
}

/* ------------------------------------------------------------------ pieces */

function Item({ label, value, sub }) {
  return (
    <div className="min-w-0">
      <dt className="text-[13px] text-steel">{label}</dt>
      <dd className="truncate font-semibold">{value}</dd>
      {sub && <dd className="text-xs text-muted">{sub}</dd>}
    </div>
  );
}

function Lifecycle({ status }) {
  const current = STATUS_ORDER.indexOf(status);
  return (
    <ol className="mb-6 grid grid-cols-4 gap-1 overflow-hidden rounded-xl border border-line bg-surface p-1 sm:grid-cols-8" aria-label="Lifecycle">
      {STATUS_ORDER.map((s, idx) => {
        const done = idx < current;
        const here = idx === current;
        return (
          <li
            key={s}
            aria-current={here ? 'step' : undefined}
            className={`rounded-lg px-2 py-2 text-center text-xs font-semibold ${here ? 'bg-ink text-white' : done ? 'bg-sunken text-ink' : 'text-muted'}`}
          >
            <span className="tnum mr-1 opacity-60">{idx + 1}</span>
            {STATUS_LABEL[s]}
          </li>
        );
      })}
    </ol>
  );
}

function ReplyBox({ onSend, busy }) {
  const [text, setText] = useState('');
  return (
    <form
      className="mt-3 flex flex-col gap-2 sm:flex-row"
      onSubmit={async (e) => {
        e.preventDefault();
        if (text.trim() && (await onSend(text.trim()))) setText('');
      }}
    >
      <Textarea rows={2} value={text} onChange={(e) => setText(e.target.value)} placeholder="Type your answer" className="flex-1" aria-label="Your answer" />
      <Button type="submit" icon={Send} loading={busy} className="self-end">
        Send reply
      </Button>
    </form>
  );
}

const KIND = {
  INFO_REQUEST: { label: 'Question for the reporter', className: 'border-p3/30 bg-p3-soft' },
  INFO_RESPONSE: { label: 'Reporter replied', className: 'border-ok/30 bg-ok-soft' },
};

function Conversation({ notes, isStaff, canPost, onPost, incidentId }) {
  const [text, setText] = useState('');
  const [internal, setInternal] = useState(false);
  const [busy, setBusy] = useState(false);

  const post = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    try {
      await api.post(`/incidents/${incidentId}/notes`, { content: text.trim(), internalNote: internal });
      setText('');
      onPost();
    } catch (err) {
      toast.error(apiError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Panel title="Conversation" padded={false}>
      {notes.length === 0 ? (
        <EmptyState icon={MessageSquare} title="No messages yet">
          {isStaff ? 'Investigation notes and questions to the reporter appear here.' : 'Updates from the technician appear here. You can add more details below.'}
        </EmptyState>
      ) : (
        <ul className="space-y-3 p-5">
          {notes.map((n) => {
            const kind = KIND[n.kind];
            return (
              <li key={n.id} className={`rounded-lg border p-3 ${n.internalNote ? 'border-dashed border-warn/50 bg-warn-soft/50' : kind ? kind.className : 'border-line bg-surface'}`}>
                <div className="mb-1 flex flex-wrap items-center gap-2 text-xs text-muted">
                  <span className="font-semibold text-ink">{n.authorName}</span>
                  {n.authorRole && <span>{titleCase(n.authorRole)}</span>}
                  <span>· {timeAgo(n.createdAt)}</span>
                  {n.internalNote && <span className="font-semibold text-warn">Internal note</span>}
                  {kind && <span className="font-semibold">{kind.label}</span>}
                </div>
                <p className="whitespace-pre-wrap text-[15px]">{n.content}</p>
              </li>
            );
          })}
        </ul>
      )}
      {canPost && (
        <form onSubmit={post} className="border-t border-line-soft p-5">
          <Textarea rows={3} value={text} onChange={(e) => setText(e.target.value)} placeholder={isStaff ? 'Add an investigation note' : 'Add more information for the technician'} aria-label="New message" />
          <div className="mt-2 flex items-center justify-between gap-3">
            {isStaff ? (
              <label className="inline-flex items-center gap-2 text-sm text-steel">
                <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                Internal (hidden from the reporter)
              </label>
            ) : (
              <span />
            )}
            <Button type="submit" size="sm" icon={Send} loading={busy} disabled={!text.trim()}>
              Post
            </Button>
          </div>
        </form>
      )}
    </Panel>
  );
}

function Stars({ value, onChange }) {
  return (
    <div className="flex gap-1" role={onChange ? 'radiogroup' : undefined} aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          disabled={!onChange}
          onClick={() => onChange?.(n)}
          aria-label={`${n} star${n > 1 ? 's' : ''}`}
          aria-checked={onChange ? value === n : undefined}
          role={onChange ? 'radio' : undefined}
          className="disabled:cursor-default"
        >
          <Star className={`h-6 w-6 ${n <= value ? 'fill-warn text-warn' : 'text-line'}`} />
        </button>
      ))}
    </div>
  );
}

function Feedback({ incident, onDone }) {
  const [rating, setRating] = useState(0);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (window.location.hash === '#feedback') document.getElementById('feedback')?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  if (incident.feedbackRating > 0) {
    return (
      <Panel title="Your feedback">
        <Stars value={incident.feedbackRating} />
        {incident.feedback && <p className="mt-2 text-steel">“{incident.feedback}”</p>}
      </Panel>
    );
  }
  return (
    <div id="feedback">
      <Panel title="How did we do?">
        <form
          className="space-y-3"
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            try {
              const res = await api.post(`/incidents/${incident.id}/feedback`, { rating, feedback: text });
              toast.success('Thanks for your feedback');
              onDone(res.data);
            } catch (err) {
              toast.error(apiError(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Stars value={rating} onChange={setRating} />
          <Textarea rows={2} value={text} onChange={(e) => setText(e.target.value)} placeholder="Anything we could have done better? (optional)" aria-label="Comments" />
          <Button type="submit" size="sm" disabled={!rating} loading={busy}>
            Send feedback
          </Button>
        </form>
      </Panel>
    </div>
  );
}

function TextModal({ open, onClose, title, label, action, onSubmit, busy }) {
  const [text, setText] = useState('');
  useEffect(() => {
    if (open) setText('');
  }, [open]);
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} disabled={!text.trim()} onClick={() => onSubmit(text.trim())}>{action}</Button>
        </>
      }
    >
      <Field label={label} htmlFor="tm">
        <Textarea id="tm" value={text} onChange={(e) => setText(e.target.value)} autoFocus />
      </Field>
    </Modal>
  );
}

function ResolveModal({ open, onClose, onSubmit, busy, suggestions }) {
  const [text, setText] = useState('');
  useEffect(() => {
    if (open) setText('');
  }, [open]);
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Resolve incident"
      wide
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} disabled={text.trim().length < 10} onClick={() => onSubmit(text.trim())}>Mark resolved</Button>
        </>
      }
    >
      <Field label="What fixed it?" htmlFor="rn" hint="The reporter sees this, and it is suggested for similar incidents in future. At least 10 characters.">
        <Textarea id="rn" rows={5} value={text} onChange={(e) => setText(e.target.value)} autoFocus />
      </Field>
      {suggestions.length > 0 && (
        <div className="mt-4">
          <p className="mb-2 flex items-center gap-1.5 text-sm font-semibold">
            <Sparkles className="h-4 w-4 text-warn" aria-hidden /> Start from a past fix
          </p>
          <ul className="space-y-2">
            {suggestions.map((s) => (
              <li key={s.incidentId}>
                <button type="button" onClick={() => setText(s.resolutionNotes)} className="w-full rounded-lg border border-line p-3 text-left text-sm hover:bg-sunken">
                  {s.resolutionNotes}
                  <span className="mt-1 block text-xs text-muted">{s.incidentNumber} · {Math.round(s.score * 100)}% similar</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Modal>
  );
}

function AssignModal({ open, onClose, incident, onDone }) {
  const { data: recs, loading, error } = useApi(open ? `/incidents/${incident.id}/recommendations` : null, { initial: [] });
  const [busy, setBusy] = useState(null);

  const assign = async (path, body, key) => {
    setBusy(key);
    try {
      const res = await api.post(`/incidents/${incident.id}/${path}`, body);
      toast.success(`Assigned to ${res.data.assignedTechnicianName}`);
      onDone(res.data);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(null);
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Assign a technician" wide footer={<Button variant="secondary" onClick={onClose}>Close</Button>}>
      <div className="mb-4 flex items-center justify-between gap-3 rounded-lg bg-sunken p-3">
        <p className="text-sm text-steel">Let the active assignment strategy choose.</p>
        <Button size="sm" icon={Shuffle} loading={busy === 'auto'} onClick={() => assign('auto-assign', null, 'auto')}>Auto-assign</Button>
      </div>
      <ErrorNote>{error}</ErrorNote>
      {loading ? (
        <Loading label="Scoring technicians" />
      ) : (
        <ul className="divide-y divide-line-soft rounded-lg border border-line">
          {recs.map((r, idx) => {
            const current = r.technicianId === incident.assignedTechnicianId;
            return (
              <li key={r.technicianId} className="flex flex-wrap items-center gap-4 p-3">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">
                    {r.name}
                    {idx === 0 && r.available && !current && <span className="ml-2 rounded bg-ok-soft px-1.5 py-0.5 text-xs text-ok">Best match</span>}
                    {current && <span className="ml-2 text-xs text-muted">Current</span>}
                  </p>
                  <p className="text-xs text-muted">
                    {r.skills.map(categoryLabel).join(', ')} · {r.activeIncidents}/{r.maxIncidents} active{!r.available && ' · at capacity or unavailable'}
                  </p>
                  <p className="tnum mt-1 text-xs text-steel">
                    Skill {r.score.skill} · Capacity {r.score.availability} · Experience {r.score.experience} · This service {r.score.serviceHistory}
                  </p>
                </div>
                <div className="tnum w-12 text-right text-lg font-bold">{Math.round(r.score.total)}</div>
                <Button size="sm" variant={idx === 0 ? 'primary' : 'secondary'} disabled={!r.available || current} loading={busy === r.technicianId} onClick={() => assign('assign', { technicianId: r.technicianId }, r.technicianId)}>
                  Assign
                </Button>
              </li>
            );
          })}
        </ul>
      )}
      <p className="mt-3 text-xs text-muted">Score out of 100: skill match 40, spare capacity 30, experience 15, past work on this service 15.</p>
    </Modal>
  );
}

function TriageModal({ open, onClose, incident, onDone }) {
  const { data: categories } = useApi(open ? '/public/categories' : null, { initial: [] });
  const [form, setForm] = useState({});
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (open) setForm({ category: incident.category, impact: incident.impact, urgency: incident.urgency });
  }, [open, incident]);

  const save = async () => {
    setBusy(true);
    try {
      const res = await api.put(`/incidents/${incident.id}`, form);
      toast.success(res.data.priority !== incident.priority ? `Priority is now ${res.data.priority.replace('_', ' ')}; deadlines updated` : 'Saved');
      onDone(res.data);
    } catch (e) {
      toast.error(apiError(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Change category or priority"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={busy} onClick={save}>Save</Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field label="Category" htmlFor="tc">
          <Select id="tc" value={form.category || ''} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            {categories.map((c) => (
              <option key={c.id} value={c.name}>{categoryLabel(c.name)}</option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Impact" htmlFor="ti">
            <Select id="ti" value={form.impact || ''} onChange={(e) => setForm({ ...form, impact: e.target.value })}>
              {['LOW', 'MEDIUM', 'HIGH'].map((v) => <option key={v} value={v}>{titleCase(v)}</option>)}
            </Select>
          </Field>
          <Field label="Urgency" htmlFor="tu">
            <Select id="tu" value={form.urgency || ''} onChange={(e) => setForm({ ...form, urgency: e.target.value })}>
              {['LOW', 'MEDIUM', 'HIGH'].map((v) => <option key={v} value={v}>{titleCase(v)}</option>)}
            </Select>
          </Field>
        </div>
        <p className="text-[13px] text-muted">Changing impact or urgency recalculates the priority and SLA deadlines (measured from when it was reported).</p>
      </div>
    </Modal>
  );
}
