import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import {
  CheckSquare,
  Clock,
  ArrowRight,
  Eye,
  ShieldAlert,
  Activity,
  CheckCircle2,
  AlertOctagon,
  Terminal,
  Zap,
  Cpu
} from 'lucide-react';

export default function MyQueue() {
  const [incidents, setIncidents] = useState([]);
  const [workloadInfo, setWorkloadInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();

  useEffect(() => {
    fetchAssigned();
    fetchWorkloadMetrics();
  }, []);

  const fetchAssigned = async () => {
    setLoading(true);
    try {
      const res = await api.get('/technician/incidents');
      setIncidents(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchWorkloadMetrics = async () => {
    try {
      const res = await api.get('/dashboard/technician-workloads');
      const myWorkload = res.data.find(
        (w) =>
          w.technicianName?.toLowerCase() === user?.fullName?.toLowerCase() ||
          w.technicianName?.toLowerCase() === user?.username?.toLowerCase()
      );
      if (myWorkload) {
        setWorkloadInfo(myWorkload);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleAcknowledge = async (id) => {
    try {
      await api.post(`/technician/incidents/${id}/acknowledge`);
      fetchAssigned();
      fetchWorkloadMetrics();
    } catch (err) {
      alert(err.response?.data?.message || 'Acknowledge failed');
    }
  };

  const handleInvestigate = async (id) => {
    try {
      await api.post(`/technician/incidents/${id}/investigate`);
      fetchAssigned();
      fetchWorkloadMetrics();
    } catch (err) {
      alert(err.response?.data?.message || 'Start investigation failed');
    }
  };

  const activeCount = incidents.filter(
    (i) => i.status !== 'RESOLVED' && i.status !== 'VERIFIED' && i.status !== 'CLOSED'
  ).length;

  const criticalCount = incidents.filter(
    (i) => i.priority === 'P1_CRITICAL' && i.status !== 'CLOSED'
  ).length;

  const cap = workloadInfo?.maxIncidents || 10;
  const loadPercentage = Math.min(100, Math.round((activeCount / cap) * 100));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="hud-panel p-6 rounded-2xl relative overflow-hidden bg-white border border-slate-200 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500 mb-1.5 uppercase tracking-widest font-semibold">
              <Terminal className="w-3.5 h-3.5" />
              <span>TECHNICIAN_ROSTER_QUEUE // DISPATCH_STATION</span>
            </div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight font-heading">
              Engineer Dispatch Queue & Workload Telemetry
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              Assigned incidents requiring acknowledgement, triage notes, and resolution actions.
            </p>
          </div>
          <div className="font-mono text-xs bg-slate-50 px-3.5 py-2 rounded-xl border border-slate-200 text-slate-700">
            OPERATOR: <span className="text-slate-900 font-bold">{user?.username}</span>
          </div>
        </div>
      </div>

      {/* Technician Workload & Capacity Gauges */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 font-mono">
        {/* Active Load & Meter */}
        <div className="hud-panel p-4 rounded-2xl border border-slate-200 bg-white shadow-xs">
          <div className="flex items-center justify-between text-slate-500 text-xs uppercase font-semibold">
            <span>ACTIVE_WORKLOAD</span>
            <Activity className="h-4 w-4 text-slate-700" />
          </div>
          <div className="text-2xl font-bold text-slate-900 mt-2">
            <span>{activeCount}</span>
            <span className="text-xs font-normal text-slate-400"> / {cap} MAX</span>
          </div>
          <div className="mt-2.5 h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-500 ${
                loadPercentage > 80 ? 'bg-rose-500' : loadPercentage > 50 ? 'bg-amber-500' : 'bg-slate-900'
              }`}
              style={{ width: `${loadPercentage}%` }}
            />
          </div>
          <div className="text-[10px] text-slate-500 mt-1.5 flex justify-between font-medium">
            <span>UTILIZATION</span>
            <span className="font-bold text-slate-900">{loadPercentage}%</span>
          </div>
        </div>

        {/* Critical P1 */}
        <div className="hud-panel p-4 rounded-2xl border border-rose-200 bg-rose-50/40 shadow-xs">
          <div className="flex items-center justify-between text-rose-800 text-xs uppercase font-semibold">
            <span>P1_CRITICAL_QUEUE</span>
            <AlertOctagon className="h-4 w-4 text-rose-600 animate-pulse" />
          </div>
          <div className="text-2xl font-bold text-rose-800 mt-2">{criticalCount}</div>
          <div className="text-[10px] text-rose-700 mt-2 flex items-center font-semibold">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-600 mr-1.5 animate-ping"></span>
            URGENT SLA PRIORITY
          </div>
        </div>

        {/* Lifetime Resolved */}
        <div className="hud-panel p-4 rounded-2xl border border-emerald-200 bg-emerald-50/40 shadow-xs">
          <div className="flex items-center justify-between text-emerald-800 text-xs uppercase font-semibold">
            <span>LIFETIME_RESOLVED</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <div className="text-2xl font-bold text-emerald-800 mt-2">
            {workloadInfo?.totalResolved ?? incidents.filter((i) => i.status === 'RESOLVED' || i.status === 'CLOSED').length}
          </div>
          <div className="text-[10px] text-emerald-700 mt-2 font-medium">
            VERIFIED RESTORATIONS
          </div>
        </div>

        {/* Specialization / Skills */}
        <div className="hud-panel p-4 rounded-2xl border border-slate-200 bg-slate-50/50 shadow-xs">
          <div className="flex items-center justify-between text-slate-700 text-xs uppercase font-semibold">
            <span>ROSTER_SKILLS</span>
            <Cpu className="h-4 w-4 text-slate-600" />
          </div>
          <div className="mt-2.5 flex flex-wrap gap-1">
            {workloadInfo?.skills?.length > 0 ? (
              workloadInfo.skills.map((s) => (
                <span
                  key={s}
                  className="px-1.5 py-0.5 rounded text-[10px] bg-white text-slate-800 border border-slate-200 font-semibold shadow-xs"
                >
                  {s}
                </span>
              ))
            ) : (
              <span className="text-xs text-slate-400">[ ALL_CATEGORIES ]</span>
            )}
          </div>
        </div>
      </div>

      {/* Incidents Table */}
      <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
              <tr>
                <th className="py-3 px-4">INCIDENT_ID</th>
                <th className="py-3 px-4">TITLE & SUMMARY</th>
                <th className="py-3 px-4">REPORTER</th>
                <th className="py-3 px-4">DOMAIN</th>
                <th className="py-3 px-4">PRIORITY</th>
                <th className="py-3 px-4">STATUS</th>
                <th className="py-3 px-4">TARGET_DEADLINE</th>
                <th className="py-3 px-4 text-right">OPERATIONS</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan="8" className="py-12 text-center text-slate-400">
                    [ RETRIEVING_QUEUE_STREAM... ]
                  </td>
                </tr>
              ) : incidents.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-12 text-center text-slate-400">
                    [ WORK_QUEUE_NOMINAL: ZERO_ASSIGNED_TICKETS ]
                  </td>
                </tr>
              ) : (
                incidents.map((inc) => {
                  const isBreached =
                    inc.resolutionDeadline &&
                    new Date(inc.resolutionDeadline) < new Date() &&
                    inc.status !== 'RESOLVED' &&
                    inc.status !== 'VERIFIED' &&
                    inc.status !== 'CLOSED';

                  return (
                    <tr key={inc.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-4 font-bold text-slate-900 font-mono">
                        {inc.incidentNumber}
                      </td>
                      <td className="py-3 px-4 max-w-xs font-sans font-semibold text-slate-900 truncate">
                        {inc.title}
                      </td>
                      <td className="py-3 px-4 text-slate-600 font-sans">{inc.reporterName}</td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 border border-slate-200 text-slate-700">
                          {inc.category}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            inc.priority === 'P1_CRITICAL'
                              ? 'bg-rose-50 text-rose-700 border border-rose-200'
                              : inc.priority === 'P2_HIGH'
                              ? 'bg-amber-50 text-amber-800 border border-amber-200'
                              : 'bg-slate-100 text-slate-700 border border-slate-200'
                          }`}
                        >
                          {inc.priority}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
                          {inc.status}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        {inc.resolutionDeadline ? (
                          <span className={isBreached ? 'text-rose-700 font-bold bg-rose-50 px-2 py-0.5 rounded border border-rose-200' : 'text-slate-600'}>
                            {new Date(inc.resolutionDeadline).toLocaleTimeString([], {
                              hour: '2-digit',
                              minute: '2-digit',
                            })}
                          </span>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td className="py-3 px-4 text-right space-x-2">
                        {inc.status === 'ASSIGNED' && (
                          <button
                            onClick={() => handleAcknowledge(inc.id)}
                            className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold shadow-xs transition-colors"
                          >
                            ACKNOWLEDGE
                          </button>
                        )}
                        {inc.status === 'ACKNOWLEDGED' && (
                          <button
                            onClick={() => handleInvestigate(inc.id)}
                            className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold shadow-xs transition-colors"
                          >
                            INVESTIGATE
                          </button>
                        )}
                        <Link
                          to={`/incidents/${inc.id}`}
                          className="inline-flex items-center space-x-1 px-3 py-1.5 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 hover:text-slate-900 rounded-xl text-xs font-mono font-medium shadow-xs transition-colors"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          <span>MANAGE</span>
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
