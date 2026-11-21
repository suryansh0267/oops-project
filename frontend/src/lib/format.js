export const PRIORITY = {
  P1_CRITICAL: { short: 'P1', label: 'Critical', color: 'var(--color-p1)', soft: 'var(--color-p1-soft)' },
  P2_HIGH: { short: 'P2', label: 'High', color: 'var(--color-p2)', soft: 'var(--color-p2-soft)' },
  P3_MEDIUM: { short: 'P3', label: 'Medium', color: 'var(--color-p3)', soft: 'var(--color-p3-soft)' },
  P4_LOW: { short: 'P4', label: 'Low', color: 'var(--color-p4)', soft: 'var(--color-p4-soft)' },
};

export const STATUS_ORDER = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'];

export const STATUS_LABEL = {
  NEW: 'New',
  CLASSIFIED: 'Classified',
  ASSIGNED: 'Assigned',
  ACKNOWLEDGED: 'Acknowledged',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  VERIFIED: 'Verified',
  CLOSED: 'Closed',
};

export const OPEN_STATUSES = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'];

export const SLA = {
  ON_TRACK: { label: 'On track', color: 'var(--color-ok)', soft: 'var(--color-ok-soft)' },
  AT_RISK: { label: 'At risk', color: 'var(--color-warn)', soft: 'var(--color-warn-soft)' },
  BREACHED: { label: 'Breached', color: 'var(--color-bad)', soft: 'var(--color-bad-soft)' },
  MET: { label: 'Met', color: 'var(--color-ok)', soft: 'var(--color-ok-soft)' },
  MISSED: { label: 'Missed', color: 'var(--color-bad)', soft: 'var(--color-bad-soft)' },
};

export const ROLE = {
  EMPLOYEE: { label: 'Employee', color: 'var(--color-role-employee)' },
  TECHNICIAN: { label: 'Technician', color: 'var(--color-role-technician)' },
  ADMIN: { label: 'Administrator', color: 'var(--color-role-admin)' },
};

export const IMPACT_HELP = {
  HIGH: 'Whole department or customers affected',
  MEDIUM: 'A team or several people affected',
  LOW: 'Just me, or a minor annoyance',
};

export const URGENCY_HELP = {
  HIGH: 'Work has stopped',
  MEDIUM: 'There is a slow workaround',
  LOW: 'Can wait for a planned fix',
};

export const titleCase = (s) =>
  (s || '').toLowerCase().replace(/_/g, ' ').replace(/(^|\s)\S/g, (c) => c.toUpperCase());

export const categoryLabel = (c) => titleCase(c || 'Other');

export function formatDateTime(v) {
  if (!v) return '—';
  return new Date(v).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
}

export function formatDate(v) {
  if (!v) return '—';
  return new Date(v).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function timeAgo(v, now = Date.now()) {
  if (!v) return '';
  const s = Math.round((now - new Date(v).getTime()) / 1000);
  if (s < 45) return 'just now';
  const m = Math.round(s / 60);
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h} h ago`;
  const d = Math.round(h / 24);
  return d === 1 ? 'yesterday' : `${d} days ago`;
}

/** "1 h 05 min", "12 min", "45 s". Negative input is treated as absolute. */
export function duration(ms) {
  const total = Math.abs(Math.round(ms / 1000));
  const d = Math.floor(total / 86400);
  const h = Math.floor((total % 86400) / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  if (d > 0) return `${d} d ${h} h`;
  if (h > 0) return `${h} h ${String(m).padStart(2, '0')} min`;
  if (m > 0) return `${m} min`;
  return `${s} s`;
}

export const minutesHuman = (min) => (min == null ? '—' : duration(min * 60000));

export function apiError(err, fallback = 'Something went wrong. Please try again.') {
  return err?.response?.data?.message || err?.message || fallback;
}
