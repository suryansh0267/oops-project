import { Link } from 'react-router-dom';
import { AlertTriangle, CheckCircle2, Inbox, PlusCircle, Radio, Star } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, Cell, Legend } from 'recharts';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../lib/hooks';
import api from '../api/client';
import { toast } from 'react-toastify';
import { Button, EmptyState, ErrorNote, Loading, Meter, PageHeader, Panel, Stat, Table, Toggle } from '../components/ui';
import IncidentRows from '../components/IncidentRows';
import { OPEN_STATUSES, PRIORITY, apiError, categoryLabel, minutesHuman } from '../lib/format';

const bySlaUrgency = (a, b) => {
  const next = (i) => new Date(!i.respondedAt ? i.responseDeadline : i.resolutionDeadline).getTime();
  return next(a) - next(b);
};

export default function Home() {
  const { role } = useAuth();
  if (role === 'ADMIN') return <AdminHome />;
  if (role === 'TECHNICIAN') return <TechnicianHome />;
  return <EmployeeHome />;
}

/* ------------------------------------------------------------------ employee */

function EmployeeHome() {
  const { user } = useAuth();
  const { data: incidents, loading, error, reload } = useApi('/incidents/my', { initial: [] });
  const open = incidents.filter((i) => OPEN_STATUSES.includes(i.status));
  const toConfirm = incidents.filter((i) => i.status === 'RESOLVED');
  const toRate = incidents.filter((i) => ['VERIFIED', 'CLOSED'].includes(i.status) && !i.feedbackRating);

  return (
    <>
      <PageHeader
        title={`Hello, ${user?.fullName?.split(' ')[0] || 'there'}`}
        description="Something not working? Tell us in your own words and we'll take it from there."
        actions={
          <Link to="/report">
            <Button icon={PlusCircle}>Report an incident</Button>
          </Link>
        }
      />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      {loading ? (
        <Loading />
      ) : (
        <div className="space-y-6">
          {(toConfirm.length > 0 || toRate.length > 0) && (
            <Panel title="Waiting for you" padded={false}>
              <ul className="divide-y divide-line-soft">
                {toConfirm.map((i) => (
                  <li key={i.id} className="flex items-center justify-between gap-4 px-5 py-3">
                    <div className="min-w-0">
                      <p className="truncate font-semibold">{i.title}</p>
                      <p className="text-[13px] text-steel">Resolved by {i.assignedTechnicianName}. Is it fixed?</p>
                    </div>
                    <Link to={`/incidents/${i.id}`}>
                      <Button size="sm" icon={CheckCircle2}>Review fix</Button>
                    </Link>
                  </li>
                ))}
                {toRate.map((i) => (
                  <li key={i.id} className="flex items-center justify-between gap-4 px-5 py-3">
                    <div className="min-w-0">
                      <p className="truncate font-semibold">{i.title}</p>
                      <p className="text-[13px] text-steel">How was the support?</p>
                    </div>
                    <Link to={`/incidents/${i.id}#feedback`}>
                      <Button size="sm" variant="secondary" icon={Star}>Rate</Button>
                    </Link>
                  </li>
                ))}
              </ul>
            </Panel>
          )}

          <Panel title={`Open incidents (${open.length})`} padded={false} action={<Link to="/incidents" className="text-sm font-semibold text-p3 hover:underline">See all</Link>}>
            <IncidentRows
              incidents={open}
              empty={
                <EmptyState icon={CheckCircle2} title="Nothing open">
                  When you report an incident you can follow its progress here.
                </EmptyState>
              }
            />
          </Panel>
        </div>
      )}
    </>
  );
}

/* ------------------------------------------------------------------ technician */

function TechnicianHome() {
  const { data: queue, loading, error, reload } = useApi('/technician/incidents', { initial: [], poll: 30000 });
  const { data: me, setData: setMe } = useApi('/technician/workload');
  const open = queue.filter((i) => OPEN_STATUSES.includes(i.status)).sort(bySlaUrgency);
  const toAck = open.filter((i) => i.status === 'ASSIGNED').length;
  const breached = open.filter((i) => i.slaStatus === 'BREACHED').length;
  const atRisk = open.filter((i) => i.slaStatus === 'AT_RISK').length;

  const setAvailable = async (v) => {
    try {
      const res = await api.put('/technician/availability', { available: v });
      setMe(res.data);
      toast.success(v ? 'You are available for new incidents' : 'You will not receive new incidents');
    } catch (e) {
      toast.error(apiError(e));
    }
  };

  return (
    <>
      <PageHeader
        title="Your shift"
        description="Incidents assigned to you, most urgent first."
        actions={
          <Link to="/queue">
            <Button icon={Inbox}>Open my queue</Button>
          </Link>
        }
      />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>
      <div className="mb-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Waiting for acknowledgement" value={toAck} tone={toAck ? 'warn' : undefined} />
        <Stat label="Open in my queue" value={open.length} />
        <Stat label="SLA at risk" value={atRisk} tone={atRisk ? 'warn' : undefined} />
        <Stat label="SLA breached" value={breached} tone={breached ? 'bad' : undefined} />
      </div>
      <div className="grid gap-6 xl:grid-cols-[1fr_320px]">
        <Panel title="SLA watch" padded={false}>
          {loading ? (
            <Loading />
          ) : (
            <IncidentRows incidents={open} showAssignee={false} empty={<EmptyState icon={CheckCircle2} title="Queue clear">Nothing assigned to you right now.</EmptyState>} />
          )}
        </Panel>
        <Panel title="My workload">
          {me ? (
            <div className="space-y-4">
              <Meter label="Active incidents" value={me.currentActiveIncidents} max={me.maxActiveIncidents} />
              <dl className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <dt className="text-steel">Resolved</dt>
                  <dd className="tnum text-lg font-bold">{me.totalResolvedIncidents}</dd>
                </div>
                <div>
                  <dt className="text-steel">Avg. resolution</dt>
                  <dd className="tnum text-lg font-bold">{minutesHuman(me.avgResolutionMinutes)}</dd>
                </div>
              </dl>
              <div>
                <p className="mb-1.5 text-sm text-steel">Skills</p>
                <div className="flex flex-wrap gap-1.5">
                  {me.skills.map((s) => (
                    <span key={s} className="rounded-md bg-sunken px-2 py-0.5 text-xs font-semibold">
                      {categoryLabel(s)}
                    </span>
                  ))}
                </div>
              </div>
              <div className="border-t border-line-soft pt-4">
                <Toggle checked={me.available} onChange={setAvailable} label={me.available ? 'Available for new incidents' : 'Not taking new incidents'} />
              </div>
            </div>
          ) : (
            <Loading />
          )}
        </Panel>
      </div>
    </>
  );
}

/* ------------------------------------------------------------------ admin */

function AdminHome() {
  const { data: stats, error, reload } = useApi('/dashboard/stats', { poll: 30000 });
  const { data: incidents } = useApi('/incidents', { initial: [], poll: 30000 });
  const { data: workloads } = useApi('/dashboard/technician-workloads', { initial: [] });
  const { data: services } = useApi('/dashboard/service-analytics', { initial: [] });
  const { data: alerts } = useApi('/dashboard/service-alerts', { initial: [], poll: 30000 });
  const { data: frustration } = useApi('/dashboard/frustration', { initial: { count: 0, trend: [], services: {}, incidents: [] }, poll: 30000 });

  const open = incidents.filter((i) => OPEN_STATUSES.includes(i.status)).sort(bySlaUrgency);
  const unassigned = open.filter((i) => !i.assignedTechnicianId);
  const priorityData = stats ? Object.entries(stats.incidentsByPriority).map(([k, v]) => ({ key: k, name: `${PRIORITY[k].short} ${PRIORITY[k].label}`, count: v })) : [];
  const categoryData = stats ? Object.entries(stats.incidentsByCategory).filter(([, v]) => v > 0).map(([k, v]) => ({ name: categoryLabel(k), count: v })).sort((a, b) => b.count - a.count) : [];
  const trend = stats?.dailyTrend?.map((d) => ({ ...d, day: new Date(d.date).toLocaleDateString(undefined, { day: 'numeric', month: 'short' }) })) || [];

  return (
    <>
      <PageHeader title="Service desk overview" description="Live view of open incidents, SLA health and team capacity. Refreshes every 30 seconds." />
      <ErrorNote onRetry={reload}>{error}</ErrorNote>

      {alerts.length > 0 && (
        <div className="mb-6 space-y-2">
          {alerts.map((a) => (
            <div key={a.serviceId} className="flex flex-wrap items-center gap-3 rounded-xl border border-bad/30 bg-bad-soft px-5 py-3 text-bad">
              <Radio className="h-5 w-5 shrink-0" aria-hidden />
              <p className="text-sm">
                <strong>Possible outage: {a.serviceName}.</strong> {a.message} ({a.incidentNumbers.join(', ')})
              </p>
            </div>
          ))}
        </div>
      )}

      {stats ? (
        <div className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
          <Stat label="Open incidents" value={stats.openIncidents} hint={`${stats.totalIncidents} total`} />
          <Stat label="Critical open" value={stats.criticalIncidents} tone={stats.criticalIncidents ? 'bad' : undefined} />
          <Stat label="SLA breached now" value={stats.slaViolations} tone={stats.slaViolations ? 'bad' : undefined} hint={`${stats.recordedViolations} recorded in total`} />
          <Stat label="SLA at risk" value={stats.slaAtRisk} tone={stats.slaAtRisk ? 'warn' : undefined} />
          <Stat label="SLA compliance" value={`${stats.slaCompliancePercent}%`} tone={stats.slaCompliancePercent >= 90 ? 'ok' : 'warn'} hint="of resolved incidents" />
          <Stat label="Mean time to resolve" value={minutesHuman(stats.meanTimeToResolveMinutes)} hint={`respond: ${minutesHuman(stats.meanTimeToRespondMinutes)}`} />
        </div>
      ) : (
        <Loading />
      )}

      <Panel title={`Frustration attention · ${frustration?.count || 0} flagged`}>
        <p className="mb-3 text-sm text-steel">Heuristic signals may misfire. Confirm context before escalating a ticket.</p>
        <div className="grid gap-4 md:grid-cols-[1fr_260px]">
          <div>
            {frustration?.incidents?.length ? frustration.incidents.slice(0, 4).map((f) => (
              <Link key={f.id} to={`/incidents/${f.id}`} className="mb-2 block rounded-lg border border-line p-3 text-sm hover:bg-sunken">
                <span className="font-semibold">{f.incidentNumber} · {f.level}</span>
                <span className="block text-steel">{f.reasons}</span>
              </Link>
            )) : <p className="rounded-lg bg-sunken p-4 text-sm text-muted">No frustrated incidents currently flagged.</p>}
            <p className="mt-2 text-xs text-muted">Top services: {Object.entries(frustration?.services || {}).sort((a, b) => b[1] - a[1]).slice(0, 3)
              .map(([name, count]) => `${name} (${count})`).join(', ') || 'None'}</p>
          </div>
          <div className="h-40" aria-label="Frustration alerts in the last 14 days">
            <ResponsiveContainer>
              <LineChart data={frustration?.trend || []} margin={{ top: 5, right: 10, left: -25, bottom: 0 }}>
                <XAxis dataKey="date" tick={{ fontSize: 9 }} interval={3} tickFormatter={(d) => d.slice(5)} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                <Tooltip />
                <Line type="monotone" dataKey="count" name="Alerts" stroke="#A45D22" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </Panel>

      <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
        <Panel title="SLA watch: open incidents by time left" padded={false}>
          <IncidentRows incidents={open.slice(0, 8)} empty={<EmptyState icon={CheckCircle2} title="No open incidents" />} />
          {open.length > 8 && (
            <Link to="/incidents?open=1" className="block border-t border-line-soft px-5 py-3 text-sm font-semibold text-p3 hover:underline">
              See all {open.length} open incidents
            </Link>
          )}
        </Panel>

        <Panel title={`Needs assigning (${unassigned.length})`} padded={false}>
          {unassigned.length === 0 ? (
            <EmptyState icon={CheckCircle2} title="Everything is assigned" />
          ) : (
            <ul className="divide-y divide-line-soft">
              {unassigned.map((i) => (
                <li key={i.id}>
                  <Link to={`/incidents/${i.id}`} className="flex items-center justify-between gap-3 px-5 py-3 hover:bg-sunken">
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold">{i.title}</span>
                      <span className="text-xs text-muted">
                        {i.incidentNumber} · {categoryLabel(i.category)}
                      </span>
                    </span>
                    {i.slaStatus === 'BREACHED' && <AlertTriangle className="h-4 w-4 shrink-0 text-bad" aria-label="SLA breached" />}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <Panel title="Reported vs resolved, last 14 days">
          <div className="h-60">
            <ResponsiveContainer>
              <LineChart data={trend} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid stroke="#E4E9EE" vertical={false} />
                <XAxis dataKey="day" tick={{ fontSize: 12, fill: '#6B7A8A' }} tickLine={false} axisLine={false} interval={1} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#6B7A8A' }} tickLine={false} axisLine={false} />
                <Tooltip />
                <Legend iconType="plainline" wrapperStyle={{ fontSize: 13 }} />
                <Line type="monotone" dataKey="created" name="Reported" stroke="#16202B" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="resolved" name="Resolved" stroke="#2F7A4F" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Panel>
        <Panel title="All incidents by priority">
          <div className="h-60">
            <ResponsiveContainer>
              <BarChart data={priorityData} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid stroke="#E4E9EE" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#6B7A8A' }} tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#6B7A8A' }} tickLine={false} axisLine={false} />
                <Tooltip cursor={{ fill: '#F5F7F9' }} />
                <Bar dataKey="count" name="Incidents" radius={[4, 4, 0, 0]}>
                  {priorityData.map((d) => (
                    <Cell key={d.key} fill={PRIORITY[d.key].color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <Panel title="Technician workload" padded={false} className="lg:col-span-1">
          <ul className="space-y-4 p-5">
            {workloads.map((w) => (
              <li key={w.technicianId}>
                <Meter label={`${w.technicianName}${w.available ? '' : ' (unavailable)'}`} value={w.activeIncidents} max={w.maxIncidents} />
              </li>
            ))}
          </ul>
        </Panel>
        <Panel title="Incidents by service" padded={false}>
          <Table
            columns={[
              { key: 'serviceName', header: 'Service' },
              { key: 'openIncidents', header: 'Open', className: 'tnum text-right' },
              { key: 'totalIncidents', header: 'Total', className: 'tnum text-right' },
              { key: 'slaViolations', header: 'SLA misses', className: 'tnum text-right', render: (r) => <span className={r.slaViolations ? 'font-semibold text-bad' : ''}>{r.slaViolations}</span> },
            ]}
            rows={services}
            rowKey="serviceId"
          />
        </Panel>
        <Panel title="By category" padded={false}>
          <Table
            columns={[
              { key: 'name', header: 'Category' },
              { key: 'count', header: 'Incidents', className: 'tnum text-right' },
            ]}
            rows={categoryData}
            rowKey="name"
          />
          {stats && (
            <p className="border-t border-line-soft px-4 py-3 text-sm text-steel">
              Average rating: <strong className="text-ink">{stats.averageRating || '—'}</strong> / 5
            </p>
          )}
        </Panel>
      </div>
    </>
  );
}
