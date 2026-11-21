import { Link } from 'react-router-dom';
import { PriorityBadge, SlaClock, StatusBadge } from './ui';
import { timeAgo } from '../lib/format';
import { useAuth } from '../context/AuthContext';
import { useApi } from '../lib/hooks';
import FrustrationBadge from './FrustrationBadge';

/** Compact incident list used on dashboards and the technician queue. */
export default function IncidentRows({ incidents, showAssignee = true, showSla = true, empty }) {
  const { isStaff } = useAuth();
  const { data: flags } = useApi(isStaff ? '/dashboard/frustration' : null, { initial: { incidents: [] } });
  if (!incidents?.length) return empty || null;
  return (
    <ul className="divide-y divide-line-soft">
      {incidents.map((i) => (
        <li key={i.id}>
          <Link to={`/incidents/${i.id}`} className="grid items-center gap-x-4 gap-y-2 px-5 py-3 hover:bg-sunken sm:grid-cols-[1fr_auto_200px]">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <PriorityBadge priority={i.priority} compact />
                <span className="truncate font-semibold">{i.title}</span>
                {isStaff && <FrustrationBadge flag={flags?.incidents?.find((f) => f.id === i.id)} />}
              </div>
              <p className="mt-0.5 truncate text-[13px] text-muted">
                {i.incidentNumber} · {i.affectedServiceName || 'No service'} · {timeAgo(i.createdAt)}
                {showAssignee && <> · {i.assignedTechnicianName || <span className="font-semibold text-warn">Unassigned</span>}</>}
              </p>
            </div>
            <StatusBadge status={i.status} />
            {showSla ? <SlaClock incident={i} size="sm" /> : <span />}
          </Link>
        </li>
      ))}
    </ul>
  );
}
