import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  Clock,
  CheckCircle2,
  AlertOctagon,
  Users,
  Server,
  TrendingUp,
  ShieldAlert,
  Terminal,
  Activity,
  Layers,
  Sparkles,
  ArrowUpRight
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
  CartesianGrid
} from 'recharts';

export default function Dashboard() {
  const { user, isAdmin, isTechnician } = useAuth();
  const [stats, setStats] = useState(null);
  const [workloads, setWorkloads] = useState([]);
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const [statsRes, workloadRes, serviceRes] = await Promise.all([
          api.get('/dashboard/stats'),
          api.get('/dashboard/technician-workloads'),
          api.get('/dashboard/service-analytics'),
        ]);
        setStats(statsRes.data);
        setWorkloads(workloadRes.data);
        setServices(serviceRes.data);
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-80 space-y-4">
        <div className="relative w-12 h-12">
          <div className="absolute inset-0 rounded-full border-2 border-slate-200 border-t-slate-900 animate-spin"></div>
        </div>
        <p className="font-mono text-xs text-slate-500 tracking-widest uppercase">
          [ SYNCHRONIZING_TELEMETRY_STREAM... ]
        </p>
      </div>
    );
  }

  const priorityData = stats?.incidentsByPriority
    ? Object.entries(stats.incidentsByPriority).map(([k, v]) => ({
        name: k.replace('_', ' '),
        count: v,
      }))
    : [];

  const categoryData = stats?.incidentsByCategory
    ? Object.entries(stats.incidentsByCategory)
        .filter(([_, v]) => v > 0)
        .map(([k, v]) => ({
          name: k,
          count: v,
        }))
    : [];

  const PRIORITY_COLORS = {
    'P1 CRITICAL': '#E11D48',
    'P2 HIGH': '#F97316',
    'P3 MEDIUM': '#0F172A',
    'P4 LOW': '#64748B',
  };

  return (
    <div className="space-y-6">
      {/* Top Banner / Operator Overview */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5 relative z-10">
          <div>
            <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500 mb-1.5 uppercase tracking-widest font-semibold">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span>NODE_ID: {user?.username?.toUpperCase()} // ROLE: {user?.role}</span>
              <span className="text-slate-300">|</span>
              <span className="text-slate-500">ENGINE: SPRING_BOOT_4.0 + POSTGRES_18</span>
            </div>
            <h1 className="text-2xl lg:text-3xl font-extrabold text-slate-900 tracking-tight font-heading">
              Mission Control & Incident Telemetry
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-1 max-w-2xl font-sans leading-relaxed">
              Real-time state transitions, automated SLA policy enforcement, dynamic technician assignment, and heuristic deduplication.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2.5">
            <Link
              to="/report"
              className="px-4 py-2.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-heading font-semibold rounded-xl shadow-xs transition-all flex items-center space-x-1.5"
            >
              <span>+ Dispatch Incident</span>
            </Link>
            <Link
              to="/incidents"
              className="px-4 py-2.5 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 text-xs font-heading font-semibold rounded-xl transition-colors flex items-center space-x-1.5 shadow-xs"
            >
              <span>Explore Ledger</span>
              <ArrowUpRight className="w-3.5 h-3.5 text-slate-500" />
            </Link>
          </div>
        </div>
      </div>

      {/* Telemetry Metric KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {/* Total Incidents */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-[11px] font-mono uppercase text-slate-500 font-bold">
            <span>TOTAL_LOGGED</span>
            <Activity className="h-4 w-4 text-slate-700" />
          </div>
          <div className="text-3xl font-bold font-heading text-slate-900 mt-2 tracking-tight">
            {stats?.totalIncidents ?? 0}
          </div>
          <div className="mt-2 flex items-center text-[10px] font-mono text-slate-500">
            <span className="w-1.5 h-1.5 rounded-full bg-slate-700 mr-1.5"></span>
            ALL LIFECYCLES
          </div>
        </div>

        {/* Active / Open */}
        <div className="bg-amber-50/50 p-4 rounded-xl border border-amber-200 shadow-xs hover:border-amber-300 transition-all">
          <div className="flex items-center justify-between text-[11px] font-mono uppercase text-amber-800 font-bold">
            <span>IN_FLIGHT</span>
            <Clock className="h-4 w-4 text-amber-600" />
          </div>
          <div className="text-3xl font-bold font-heading text-amber-900 mt-2 tracking-tight">
            {stats?.openIncidents ?? 0}
          </div>
          <div className="mt-2 flex items-center text-[10px] font-mono text-amber-700 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5 animate-pulse"></span>
            ACTION REQUIRED
          </div>
        </div>

        {/* P1 Critical */}
        <div className="bg-rose-50/50 p-4 rounded-xl border border-rose-200 shadow-xs hover:border-rose-300 transition-all">
          <div className="flex items-center justify-between text-[11px] font-mono uppercase text-rose-800 font-bold">
            <span>P1_CRITICAL</span>
            <AlertOctagon className="h-4 w-4 text-rose-600 animate-bounce" />
          </div>
          <div className="text-3xl font-bold font-heading text-rose-900 mt-2 tracking-tight">
            {stats?.criticalIncidents ?? 0}
          </div>
          <div className="mt-2 flex items-center text-[10px] font-mono text-rose-700 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-600 mr-1.5 animate-ping"></span>
            MAX SEVERITY LEVEL
          </div>
        </div>

        {/* Resolved */}
        <div className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-200 shadow-xs hover:border-emerald-300 transition-all">
          <div className="flex items-center justify-between text-[11px] font-mono uppercase text-emerald-800 font-bold">
            <span>RESOLVED</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <div className="text-3xl font-bold font-heading text-emerald-900 mt-2 tracking-tight">
            {stats?.resolvedIncidents ?? 0}
          </div>
          <div className="mt-2 flex items-center text-[10px] font-mono text-emerald-700 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 mr-1.5"></span>
            AWAITING CLOSE
          </div>
        </div>

        {/* SLA Breaches */}
        <div className="bg-orange-50/50 p-4 rounded-xl border border-orange-200 shadow-xs hover:border-orange-300 transition-all">
          <div className="flex items-center justify-between text-[11px] font-mono uppercase text-orange-800 font-bold">
            <span>SLA_VIOLATIONS</span>
            <ShieldAlert className="h-4 w-4 text-orange-600" />
          </div>
          <div className="text-3xl font-bold font-heading text-orange-900 mt-2 tracking-tight">
            {stats?.slaViolations ?? 0}
          </div>
          <div className="mt-2 flex items-center text-[10px] font-mono text-orange-700 font-medium">
            <span className="w-1.5 h-1.5 rounded-full bg-orange-600 mr-1.5"></span>
            TARGET EXCEEDED
          </div>
        </div>
      </div>

      {/* Visual Analytics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Priority Breakdown Chart */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 rounded bg-slate-900"></span>
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-800">
                PRIORITY_DISTRIBUTION_HISTOGRAM
              </h2>
            </div>
            <span className="text-[10px] font-mono text-slate-500">OOP_STRATEGY: WEIGHTED_PRIORITY</span>
          </div>
          <div className="h-60">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={priorityData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#64748B', fontFamily: 'monospace' }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: '#64748B', fontFamily: 'monospace' }} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#FFFFFF',
                    borderColor: '#E2E8F0',
                    borderRadius: '8px',
                    fontSize: '11px',
                    color: '#0F172A',
                    boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.08)',
                    fontFamily: 'monospace'
                  }}
                />
                <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                  {priorityData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={PRIORITY_COLORS[entry.name] || '#2563EB'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Categories Breakdown */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 rounded bg-indigo-600"></span>
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-800">
                DOMAIN_CATEGORY_BREAKDOWN
              </h2>
            </div>
            <span className="text-[10px] font-mono text-slate-500">RUNTIME_CATEGORIES: ACTIVE</span>
          </div>

          <div className="space-y-3.5 max-h-60 overflow-y-auto pr-1">
            {categoryData.length === 0 ? (
              <p className="text-xs font-mono text-slate-400 text-center py-10">[ NO_ACTIVE_TICKETS_CLASSIFIED ]</p>
            ) : (
              categoryData.map((cat) => {
                const total = stats?.totalIncidents || 1;
                const pct = Math.round((cat.count / total) * 100);
                return (
                  <div key={cat.name} className="space-y-1.5">
                    <div className="flex justify-between text-xs font-mono">
                      <span className="text-slate-800 font-semibold">{cat.name}</span>
                      <span className="text-slate-900 font-bold">
                        {cat.count} <span className="text-slate-400 font-normal">({pct}%)</span>
                      </span>
                    </div>
                    <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden border border-slate-200/60">
                      <div
                        className="h-full bg-slate-900 rounded-full transition-all duration-500"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Technician Workload & IT Services Analysis */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Workload Table */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <Users className="h-4 w-4 text-slate-700" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-800">
                TECHNICIAN_ROSTER_WORKLOAD
              </h2>
            </div>
            <span className="text-[10px] font-mono text-slate-800 bg-slate-100 border border-slate-200 px-2 py-0.5 rounded font-bold">
              STRATEGY: DYNAMIC_LOAD_AWARE
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px]">
                <tr>
                  <th className="py-2.5 px-3">OPERATOR</th>
                  <th className="py-2.5 px-3">CAPACITY_LOAD</th>
                  <th className="py-2.5 px-3">RESOLVED</th>
                  <th className="py-2.5 px-3">SKILL_DOMAINS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {workloads.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="py-6 text-center text-slate-400">
                      [ NO_TECHNICIAN_RECORDS_INDEXED ]
                    </td>
                  </tr>
                ) : (
                  workloads.map((tech) => {
                    const ratio = tech.maxIncidents ? Math.round((tech.activeIncidents / tech.maxIncidents) * 100) : 0;
                    return (
                      <tr key={tech.technicianId} className="hover:bg-slate-50/80 transition-colors">
                        <td className="py-2.5 px-3 font-semibold text-slate-900">
                          {tech.technicianName}
                        </td>
                        <td className="py-2.5 px-3">
                          <div className="flex items-center space-x-2">
                            <span className="text-slate-900 font-bold">{tech.activeIncidents}</span>
                            <span className="text-slate-400">/</span>
                            <span className="text-slate-500">{tech.maxIncidents}</span>
                            <div className="w-12 h-1.5 bg-slate-100 rounded-full overflow-hidden ml-1 border border-slate-200/80">
                              <div
                                className={`h-full ${ratio > 80 ? 'bg-rose-500' : ratio > 50 ? 'bg-amber-500' : 'bg-slate-900'}`}
                                style={{ width: `${Math.min(ratio, 100)}%` }}
                              />
                            </div>
                          </div>
                        </td>
                        <td className="py-2.5 px-3 text-emerald-700 font-bold">
                          {tech.totalResolved}
                        </td>
                        <td className="py-2.5 px-3">
                          <div className="flex flex-wrap gap-1">
                            {tech.skills?.map((s) => (
                              <span
                                key={s}
                                className="px-1.5 py-0.5 rounded text-[9px] bg-slate-100 border border-slate-200 text-slate-700 font-semibold"
                              >
                                {s}
                              </span>
                            ))}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Affected Services Table */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs">
          <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <Server className="h-4 w-4 text-slate-800" />
              <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-800">
                CRITICAL_IT_SERVICE_MATRIX
              </h2>
            </div>
            <span className="text-[10px] font-mono text-slate-500">IMPACT_ASSESSMENT</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px]">
                <tr>
                  <th className="py-2.5 px-3">SERVICE_NAME</th>
                  <th className="py-2.5 px-3">ACTIVE_IMPACT</th>
                  <th className="py-2.5 px-3">TOTAL_RESOLVED</th>
                  <th className="py-2.5 px-3">SLA_STATUS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {services.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="py-6 text-center text-slate-400">
                      [ NO_REGISTERED_SERVICES ]
                    </td>
                  </tr>
                ) : (
                  services.map((svc) => (
                    <tr key={svc.serviceId} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-2.5 px-3 font-semibold text-slate-900">{svc.serviceName}</td>
                      <td className="py-2.5 px-3">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold ${
                            svc.openIncidents > 0
                              ? 'bg-amber-50 text-amber-800 border border-amber-200'
                              : 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                          }`}
                        >
                          {svc.openIncidents} OPEN
                        </span>
                      </td>
                      <td className="py-2.5 px-3 text-slate-700">{svc.resolvedIncidents}</td>
                      <td className="py-2.5 px-3">
                        {svc.slaViolations > 0 ? (
                          <span className="text-rose-700 font-bold px-2 py-0.5 rounded bg-rose-50 text-[10px] border border-rose-200">
                            {svc.slaViolations} BREACHED
                          </span>
                        ) : (
                          <span className="text-emerald-700 font-bold text-[10px] flex items-center space-x-1">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                            <span>100% NOMINAL</span>
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
