import { useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Filter, PlusCircle, Search, X, ListChecks } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../lib/hooks';
import { Button, EmptyState, ErrorNote, Input, Loading, PageHeader, Panel, PriorityBadge, Select, SlaClock, StatusBadge, Table } from '../components/ui';
import { OPEN_STATUSES, PRIORITY, SLA, STATUS_LABEL, STATUS_ORDER, categoryLabel, formatDateTime } from '../lib/format';
import FrustrationBadge from '../components/FrustrationBadge';

const FILTER_KEYS = ['q', 'status', 'priority', 'category', 'serviceId', 'technicianId', 'slaStatus', 'from', 'to', 'mine', 'open', 'frustration', 'frustratedFirst'];

export default function IncidentList() {
  const { isStaff, isEmployee } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const [keyword, setKeyword] = useState(params.get('q') || '');
  const [showFilters, setShowFilters] = useState(() => FILTER_KEYS.some((k) => k !== 'q' && params.get(k)));

  // "open" is a UI shortcut; the API filters by a single status, so open-only is applied client-side.
  const query = useMemo(() => {
    const p = new URLSearchParams();
    FILTER_KEYS.filter((k) => k !== 'open').forEach((k) => params.get(k) && p.set(k, params.get(k)));
    return p.toString();
  }, [params]);

  const { data, loading, error, reload } = useApi(`/incidents${query ? `?${query}` : ''}`, { initial: [] });
  const { data: services } = useApi('/public/services', { initial: [] });
  const { data: categories } = useApi('/public/categories', { initial: [] });
  const { data: technicians } = useApi(isStaff ? '/dashboard/technician-workloads' : null, { initial: [] });
  const { data: flags } = useApi(isStaff ? '/dashboard/frustration' : null, { initial: { incidents: [] } });

  const rows = params.get('open') ? data.filter((i) => OPEN_STATUSES.includes(i.status)) : data;
  const active = FILTER_KEYS.filter((k) => params.get(k)).length;

  const set = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  const clear = () => {
    setKeyword('');
    setParams({}, { replace: true });
  };

  const columns = [
    {
      key: 'title',
      header: 'Incident',
      render: (i) => (
        <div className="min-w-[220px]">
          <Link to={`/incidents/${i.id}`} className="font-semibold hover:underline" onClick={(e) => e.stopPropagation()}>
            {i.title}
          </Link>
          <div className="text-[13px] text-muted">
            {i.incidentNumber} · {categoryLabel(i.category)}
          </div>
          {isStaff && <FrustrationBadge flag={flags?.incidents?.find((f) => f.id === i.id)} />}
        </div>
      ),
    },
    { key: 'priority', header: 'Priority', render: (i) => <PriorityBadge priority={i.priority} /> },
    { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
    { key: 'sla', header: 'SLA', render: (i) => <SlaClock incident={i} size="sm" /> },
    { key: 'service', header: 'Service', className: 'hidden lg:table-cell text-steel', render: (i) => i.affectedServiceName || '—' },
    ...(isStaff
      ? [{ key: 'tech', header: 'Assignee', className: 'hidden md:table-cell', render: (i) => i.assignedTechnicianName || <span className="font-semibold text-warn">Unassigned</span> }]
      : []),
    { key: 'created', header: 'Reported', className: 'hidden xl:table-cell text-steel whitespace-nowrap', render: (i) => formatDateTime(i.createdAt) },
  ];

  return (
    <>
      <PageHeader
        title={isEmployee ? 'My incidents' : 'Incidents'}
        description={isEmployee ? 'Everything you have reported, newest first.' : 'Search by number or text, then narrow down with filters.'}
        actions={
          <Link to="/report">
            <Button icon={PlusCircle}>{isEmployee ? 'Report an incident' : 'Log an incident'}</Button>
          </Link>
        }
      />

      <Panel padded={false}>
        <div className="flex flex-col gap-3 border-b border-line-soft p-4 sm:flex-row">
          <form
            className="relative flex-1"
            onSubmit={(e) => {
              e.preventDefault();
              set('q', keyword.trim());
            }}
          >
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted" aria-hidden />
            <Input className="pl-9" placeholder="Search by INC number, title or description" value={keyword} onChange={(e) => setKeyword(e.target.value)} aria-label="Search incidents" />
          </form>
          <div className="flex gap-2">
            <Button variant="secondary" icon={Filter} onClick={() => setShowFilters((s) => !s)} aria-expanded={showFilters}>
              Filters{active ? ` (${active})` : ''}
            </Button>
            {active > 0 && (
              <Button variant="ghost" icon={X} onClick={clear}>
                Clear
              </Button>
            )}
          </div>
        </div>

        {showFilters && (
          <div className="grid gap-3 border-b border-line-soft bg-sunken p-4 sm:grid-cols-2 lg:grid-cols-4">
            <FilterSelect label="Status" value={params.get('status')} onChange={(v) => set('status', v)} options={STATUS_ORDER.map((s) => [s, STATUS_LABEL[s]])} />
            <FilterSelect label="Priority" value={params.get('priority')} onChange={(v) => set('priority', v)} options={Object.entries(PRIORITY).map(([k, p]) => [k, `${p.short} ${p.label}`])} />
            <FilterSelect label="Category" value={params.get('category')} onChange={(v) => set('category', v)} options={categories.map((c) => [c.name, categoryLabel(c.name)])} />
            <FilterSelect label="SLA" value={params.get('slaStatus')} onChange={(v) => set('slaStatus', v)} options={Object.entries(SLA).map(([k, s]) => [k, s.label])} />
            <FilterSelect label="Service" value={params.get('serviceId')} onChange={(v) => set('serviceId', v)} options={services.map((s) => [String(s.id), s.name])} />
            {isStaff && (
              <FilterSelect label="Technician" value={params.get('technicianId')} onChange={(v) => set('technicianId', v)} options={technicians.map((t) => [String(t.technicianId), t.technicianName])} />
            )}
            {isStaff && <FilterSelect label="Frustration" value={params.get('frustration')} onChange={(v) => set('frustration', v)}
              options={[["FLAGGED", "Frustrated or critical"], ["FRUSTRATED", "Frustrated"], ["CRITICAL", "Critical"]]} />}
            <label className="text-sm">
              <span className="mb-1 block font-semibold">Reported from</span>
              <Input type="date" value={params.get('from') || ''} onChange={(e) => set('from', e.target.value)} />
            </label>
            <label className="text-sm">
              <span className="mb-1 block font-semibold">Reported to</span>
              <Input type="date" value={params.get('to') || ''} onChange={(e) => set('to', e.target.value)} />
            </label>
            <div className="flex flex-wrap items-center gap-4 sm:col-span-2 lg:col-span-4">
              <label className="inline-flex items-center gap-2 text-sm">
                <input type="checkbox" checked={Boolean(params.get('open'))} onChange={(e) => set('open', e.target.checked ? '1' : '')} />
                Open incidents only
              </label>
              {isStaff && (
                <label className="inline-flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={Boolean(params.get('mine'))} onChange={(e) => set('mine', e.target.checked ? 'true' : '')} />
                  Only ones I reported
                </label>
              )}
              {isStaff && <label className="inline-flex items-center gap-2 text-sm">
                <input type="checkbox" checked={Boolean(params.get('frustratedFirst'))} onChange={(e) => set('frustratedFirst', e.target.checked ? 'true' : '')} />
                Frustrated first
              </label>}
            </div>
          </div>
        )}

        <div className="px-1">
          <ErrorNote onRetry={reload}>{error}</ErrorNote>
          {loading ? (
            <Loading label="Searching" />
          ) : (
            <>
              <p className="px-4 pt-3 text-[13px] text-muted">
                {rows.length} {rows.length === 1 ? 'incident' : 'incidents'}
              </p>
              <Table
                columns={columns}
                rows={rows}
                onRowClick={(i) => navigate(`/incidents/${i.id}`)}
                empty={
                  <EmptyState icon={ListChecks} title={active ? 'No incidents match these filters' : 'No incidents yet'} action={active ? <Button variant="secondary" onClick={clear}>Clear filters</Button> : null}>
                    {active ? 'Try removing a filter or searching for different words.' : 'Reported incidents will appear here.'}
                  </EmptyState>
                }
              />
            </>
          )}
        </div>
      </Panel>
    </>
  );
}

function FilterSelect({ label, value, onChange, options }) {
  return (
    <label className="text-sm">
      <span className="mb-1 block font-semibold">{label}</span>
      <Select value={value || ''} onChange={(e) => onChange(e.target.value)}>
        <option value="">Any</option>
        {options.map(([v, l]) => (
          <option key={v} value={v}>
            {l}
          </option>
        ))}
      </Select>
    </label>
  );
}
