import { AlertTriangle } from 'lucide-react';

/** Staff-only heuristic; badge uses text and icon, never just colour. */
export default function FrustrationBadge({ flag }) {
  if (!flag || !['FRUSTRATED', 'CRITICAL'].includes(flag.level)) return null;
  return <span title={`Heuristic detection can misfire. Reasons: ${flag.reasons || 'Behavioural context'}`}
    className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-xs font-semibold ${flag.level === 'CRITICAL' ? 'bg-bad/10 text-bad' : 'bg-warn/10 text-warn'}`}>
    <AlertTriangle className="h-3.5 w-3.5" aria-hidden /> {flag.level === 'CRITICAL' ? 'Critical frustration' : 'Frustrated reporter'}
  </span>;
}
