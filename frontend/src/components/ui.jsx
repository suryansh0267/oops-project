import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { X } from 'lucide-react';
import { PRIORITY, SLA, STATUS_LABEL, ROLE, categoryLabel, duration } from '../lib/format';
import { useNow } from '../lib/hooks';

const cx = (...c) => c.filter(Boolean).join(' ');

/* ------------------------------------------------------------------ buttons */

const BTN = {
  primary: 'bg-ink text-white hover:bg-[#25313f] disabled:bg-steel/60',
  secondary: 'bg-surface text-ink border border-line hover:bg-sunken',
  ghost: 'text-steel hover:text-ink hover:bg-sunken',
  danger: 'bg-surface text-bad border border-bad/40 hover:bg-bad-soft',
};

export function Button({ variant = 'primary', size = 'md', icon: Icon, className, children, loading, ...rest }) {
  return (
    <button
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60',
        size === 'sm' ? 'px-3 py-1.5 text-[13px]' : 'px-4 py-2 text-sm',
        BTN[variant],
        className
      )}
      disabled={loading || rest.disabled}
      {...rest}
    >
      {Icon && <Icon className={size === 'sm' ? 'h-3.5 w-3.5' : 'h-4 w-4'} aria-hidden />}
      {loading ? 'Working…' : children}
    </button>
  );
}

/* ------------------------------------------------------------------ layout */

export function PageHeader({ title, description, actions, back }) {
  return (
    <header className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        {back && (
          <Link to={back.to} className="mb-2 inline-block text-sm text-steel hover:text-ink">
            ← {back.label}
          </Link>
        )}
        <h1 className="text-2xl font-bold">{title}</h1>
        {description && <p className="mt-1 max-w-2xl text-[15px] text-steel">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  );
}

export function Panel({ title, action, children, className, padded = true }) {
  return (
    <section className={cx('rounded-xl border border-line bg-surface', className)}>
      {title && (
        <div className="flex items-center justify-between gap-3 border-b border-line-soft px-5 py-3">
          <h2 className="text-[15px] font-semibold">{title}</h2>
          {action}
        </div>
      )}
      <div className={padded ? 'p-5' : ''}>{children}</div>
    </section>
  );
}

export function Stat({ label, value, hint, tone }) {
  const color = tone === 'bad' ? 'text-bad' : tone === 'warn' ? 'text-warn' : tone === 'ok' ? 'text-ok' : 'text-ink';
  return (
    <div className="rounded-xl border border-line bg-surface px-4 py-3">
      <div className="text-[13px] text-steel">{label}</div>
      <div className={cx('tnum mt-0.5 text-2xl font-bold', color)}>{value}</div>
      {hint && <div className="mt-0.5 text-xs text-muted">{hint}</div>}
    </div>
  );
}

export function EmptyState({ icon: Icon, title, children, action }) {
  return (
    <div className="flex flex-col items-center px-6 py-10 text-center">
      {Icon && <Icon className="mb-3 h-8 w-8 text-muted" aria-hidden />}
      <p className="font-semibold">{title}</p>
      {children && <p className="mt-1 max-w-sm text-sm text-steel">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Loading({ label = 'Loading' }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-sm text-steel" role="status">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-line border-t-ink" />
      {label}…
    </div>
  );
}

export function ErrorNote({ children, onRetry }) {
  if (!children) return null;
  return (
    <div className="flex items-start justify-between gap-3 rounded-lg border border-bad/30 bg-bad-soft px-4 py-3 text-sm text-bad" role="alert">
      <span>{children}</span>
      {onRetry && (
        <button onClick={onRetry} className="shrink-0 font-semibold underline">
          Try again
        </button>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ badges */

function Pill({ color, soft, children, title, strong }) {
  return (
    <span
      title={title}
      className={cx('inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs', strong ? 'font-bold' : 'font-semibold')}
      style={{ color, background: soft }}
    >
      {children}
    </span>
  );
}

export function PriorityBadge({ priority, compact }) {
  const p = PRIORITY[priority];
  if (!p) return null;
  return (
    <Pill color={p.color} soft={p.soft} strong title={`${p.short} ${p.label}`}>
      {p.short}
      {!compact && <span className="font-medium">{p.label}</span>}
    </Pill>
  );
}

export function StatusBadge({ status }) {
  const open = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'].includes(status);
  return (
    <span
      className={cx(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-md border px-2 py-0.5 text-xs font-semibold',
        open ? 'border-line bg-surface text-ink' : 'border-transparent bg-sunken text-steel'
      )}
    >
      <span className={cx('h-1.5 w-1.5 rounded-full', open ? 'bg-ink' : 'bg-muted')} />
      {STATUS_LABEL[status] || status}
    </span>
  );
}

export function SlaBadge({ status }) {
  const s = SLA[status];
  if (!s) return null;
  return (
    <Pill color={s.color} soft={s.soft}>
      {s.label}
    </Pill>
  );
}

export function RoleBadge({ role }) {
  const r = ROLE[role];
  if (!r) return null;
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-semibold" style={{ color: r.color }}>
      <span className="h-2 w-2 rounded-full" style={{ background: r.color }} />
      {r.label}
    </span>
  );
}

export const CategoryText = ({ category }) => <span className="text-steel">{categoryLabel(category)}</span>;

/* ------------------------------------------------------------------ SLA clock (signature element) */

/**
 * Live countdown to an incident's next deadline, with a bar that fills as time runs out.
 * Before the first response it counts down to the response target, then to resolution.
 */
export function SlaClock({ incident, size = 'md' }) {
  const now = useNow(1000);
  const open = ['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'].includes(incident.status);
  const created = new Date(incident.createdAt).getTime();
  const waitingForResponse = open && !incident.respondedAt;
  const deadlineIso = waitingForResponse ? incident.responseDeadline : incident.resolutionDeadline;
  if (!deadlineIso || !created) return <span className="text-sm text-muted">No SLA</span>;

  const deadline = new Date(deadlineIso).getTime();
  if (!open) {
    const resolved = incident.resolvedAt ? new Date(incident.resolvedAt).getTime() : null;
    const met = resolved == null || resolved <= new Date(incident.resolutionDeadline).getTime();
    return (
      <div className={size === 'sm' ? 'text-xs' : 'text-sm'}>
        <span className={met ? 'font-semibold text-ok' : 'font-semibold text-bad'}>{met ? 'Resolved within SLA' : 'Resolved late'}</span>
        {resolved && <span className="text-muted"> · took {duration(resolved - created)}</span>}
      </div>
    );
  }

  const left = deadline - now;
  const fraction = Math.min(1, Math.max(0, (now - created) / Math.max(1, deadline - created)));
  const tone = left < 0 ? 'var(--color-bad)' : fraction >= 0.75 ? 'var(--color-warn)' : 'var(--color-ok)';
  const target = waitingForResponse ? 'response' : 'resolution';

  return (
    <div className={size === 'sm' ? 'min-w-[120px]' : 'min-w-[180px]'}>
      <div className={cx('tnum flex items-baseline justify-between gap-2', size === 'sm' ? 'text-xs' : 'text-sm')}>
        <span className="font-semibold" style={{ color: tone }}>
          {left < 0 ? `${duration(left)} overdue` : `${duration(left)} left`}
        </span>
        <span className="text-muted">{target}</span>
      </div>
      <div className="sla-track mt-1.5" aria-hidden>
        <div className="sla-fill" style={{ width: `${Math.round(fraction * 100)}%`, background: tone }} />
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ forms */

export function Field({ label, hint, error, children, htmlFor, optional }) {
  return (
    <div>
      <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-semibold">
        {label}
        {optional && <span className="ml-1 font-normal text-muted">(optional)</span>}
      </label>
      {children}
      {hint && !error && <p className="mt-1 text-[13px] text-muted">{hint}</p>}
      {error && <p className="mt-1 text-[13px] text-bad">{error}</p>}
    </div>
  );
}

const INPUT =
  'w-full rounded-lg border border-line bg-surface px-3 py-2 text-[15px] text-ink placeholder:text-muted focus:border-p3 focus:outline-none focus:ring-2 focus:ring-p3/20 disabled:bg-sunken';

export const Input = (props) => <input {...props} className={cx(INPUT, props.className)} />;
export const Textarea = (props) => <textarea rows={4} {...props} className={cx(INPUT, 'leading-relaxed', props.className)} />;
export const Select = ({ children, ...props }) => (
  <select {...props} className={cx(INPUT, 'pr-8', props.className)}>
    {children}
  </select>
);

export function Toggle({ checked, onChange, label }) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 text-sm">
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={cx('relative h-5 w-9 rounded-full transition-colors', checked ? 'bg-ok' : 'bg-line')}
      >
        <span className={cx('absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition-all', checked ? 'left-[18px]' : 'left-0.5')} />
      </button>
      {label}
    </label>
  );
}

/* ------------------------------------------------------------------ modal */

export function Modal({ open, onClose, title, children, footer, wide }) {
  const ref = useRef(null);
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) d.showModal();
    if (!open && d.open) d.close();
  }, [open]);
  return (
    <dialog
      ref={ref}
      onClose={onClose}
      onClick={(e) => e.target === ref.current && onClose()}
      className={cx('m-auto w-[calc(100%-2rem)] rounded-xl border border-line bg-surface p-0 text-ink backdrop:bg-ink/40', wide ? 'max-w-2xl' : 'max-w-lg')}
    >
      {open && (
        <div>
          <div className="flex items-center justify-between border-b border-line-soft px-5 py-3">
            <h2 className="font-semibold">{title}</h2>
            <button onClick={onClose} className="rounded p-1 text-steel hover:bg-sunken" aria-label="Close">
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div>
          {footer && <div className="flex justify-end gap-2 border-t border-line-soft px-5 py-3">{footer}</div>}
        </div>
      )}
    </dialog>
  );
}

/* ------------------------------------------------------------------ table */

export function Table({ columns, rows, rowKey = 'id', onRowClick, empty }) {
  if (!rows?.length) return empty || null;
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-line text-[13px] text-steel">
            {columns.map((c) => (
              <th key={c.key} className={cx('px-4 py-2.5 font-semibold', c.className)}>
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr
              key={r[rowKey]}
              onClick={onRowClick ? () => onRowClick(r) : undefined}
              className={cx('border-b border-line-soft last:border-0', onRowClick && 'cursor-pointer hover:bg-sunken')}
            >
              {columns.map((c) => (
                <td key={c.key} className={cx('px-4 py-3 align-middle', c.className)}>
                  {c.render ? c.render(r) : r[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Meter({ value, max, label }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  const tone = pct >= 100 ? 'var(--color-bad)' : pct >= 75 ? 'var(--color-warn)' : 'var(--color-ok)';
  return (
    <div>
      <div className="tnum mb-1 flex justify-between text-xs text-steel">
        <span>{label}</span>
        <span className="font-semibold text-ink">
          {value} / {max}
        </span>
      </div>
      <div className="sla-track">
        <div className="sla-fill" style={{ width: `${pct}%`, background: tone }} />
      </div>
    </div>
  );
}
