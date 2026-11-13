import React, { useState, useEffect } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import {
  Search,
  Filter,
  AlertCircle,
  Clock,
  CheckCircle2,
  User,
  Eye,
  Plus,
  FileText,
  Terminal,
  Activity,
  Layers,
  ShieldAlert,
  ArrowRight
} from 'lucide-react';

export default function IncidentList() {
  const { user } = useAuth();
  const [incidents, setIncidents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // View Scope: 'all' vs 'mine'
  const [scope, setScope] = useState('all');

  // 5+ Filter Criteria
  const [searchKeyword, setSearchKeyword] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterPriority, setFilterPriority] = useState('');
  const [filterCategory, setFilterCategory] = useState('');

  useEffect(() => {
    fetchIncidents();
    fetchCategories();
  }, [scope]);

  const fetchCategories = async () => {
    try {
      const res = await api.get('/public/categories');
      setCategories(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchIncidents = async () => {
    setLoading(true);
    try {
      const endpoint = scope === 'mine' ? '/incidents/my' : '/incidents';
      const res = await api.get(endpoint);
      setIncidents(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchKeyword.trim()) {
      fetchIncidents();
      return;
    }
    setLoading(true);
    try {
      const res = await api.get(`/incidents/search?keyword=${encodeURIComponent(searchKeyword)}`);
      setIncidents(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Client-side filtering across criteria
  const filtered = incidents.filter((inc) => {
    if (filterStatus && inc.status !== filterStatus) return false;
    if (filterPriority && inc.priority !== filterPriority) return false;
    if (filterCategory && inc.category !== filterCategory) return false;
    return true;
  });

  const getPriorityBadge = (p) => {
    switch (p) {
      case 'P1_CRITICAL':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-rose-50 text-rose-700 border border-rose-300 inline-flex items-center space-x-1">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-600 animate-ping"></span>
            <span>P1_CRITICAL</span>
          </span>
        );
      case 'P2_HIGH':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-orange-50 text-orange-700 border border-orange-300">
            P2_HIGH
          </span>
        );
      case 'P3_MEDIUM':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-slate-100 text-slate-800 border border-slate-200">
            P3_MEDIUM
          </span>
        );
      case 'P4_LOW':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-slate-100 text-slate-600 border border-slate-200">
            P4_LOW
          </span>
        );
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-100 text-slate-700">{p}</span>;
    }
  };

  const getStatusBadge = (s) => {
    switch (s) {
      case 'NEW':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-slate-100 text-slate-700 border border-slate-200">NEW</span>;
      case 'CLASSIFIED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-indigo-50 text-indigo-700 border border-indigo-200">CLASSIFIED</span>;
      case 'ASSIGNED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-slate-100 text-slate-800 border border-slate-200">ASSIGNED</span>;
      case 'ACKNOWLEDGED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-slate-100 text-slate-800 border border-slate-200">ACKNOWLEDGED</span>;
      case 'IN_PROGRESS':
        return (
          <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-amber-50 text-amber-700 border border-amber-300 inline-flex items-center space-x-1">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
            <span>IN_PROGRESS</span>
          </span>
        );
      case 'RESOLVED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-emerald-50 text-emerald-700 border border-emerald-200">RESOLVED</span>;
      case 'VERIFIED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-teal-50 text-teal-700 border border-teal-200">VERIFIED</span>;
      case 'CLOSED':
        return <span className="px-2 py-0.5 rounded font-mono text-[10px] bg-slate-100 text-slate-500 border border-slate-200">CLOSED</span>;
      default:
        return <span className="font-mono text-[10px] text-slate-600">{s}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header with Scope Switcher & Action */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200/90 shadow-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500 uppercase tracking-widest mb-1 font-semibold">
            <Terminal className="w-3.5 h-3.5 text-slate-700" />
            <span>INCIDENT_REGISTRY_LEDGER // LIVE_STREAM</span>
          </div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight font-space">
            Incident Operations Ledger
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Query across 8-stage state lifecycle, SLA compliance matrices, and technician dispatches.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Scope Selector: All vs Mine */}
          <div className="bg-slate-100 p-1 rounded-xl border border-slate-200 flex text-xs font-mono">
            <button
              onClick={() => setScope('all')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                scope === 'all'
                  ? 'bg-slate-900 text-white font-bold shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              GLOBAL_STREAM
            </button>
            <button
              onClick={() => setScope('mine')}
              className={`px-3 py-1.5 rounded-lg transition-all flex items-center space-x-1.5 ${
                scope === 'mine'
                  ? 'bg-slate-900 text-white font-bold shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <FileText className="h-3 w-3" />
              <span>MY_DISPATCHES</span>
            </button>
          </div>

          <Link
            to="/report"
            className="inline-flex items-center space-x-1.5 px-3.5 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-bold rounded-xl shadow-xs transition-all"
          >
            <Plus className="h-4 w-4" />
            <span>DISPATCH_NEW</span>
          </Link>
        </div>
      </div>

      {/* Query Bar & 5 Filter Criteria */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/90 shadow-xs space-y-4">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="relative flex-1">
            <Search className="h-4 w-4 text-slate-400 absolute left-3.5 top-2.5" />
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="Query by ID (e.g. INC1001), keywords, or root cause descriptors..."
              className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-mono text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-400 focus:ring-1 focus:ring-slate-300"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-bold rounded-xl transition-colors shadow-xs"
          >
            EXEC_QUERY
          </button>
          {searchKeyword && (
            <button
              type="button"
              onClick={() => {
                setSearchKeyword('');
                fetchIncidents();
              }}
              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-mono rounded-xl"
            >
              RESET
            </button>
          )}
        </form>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-100">
          {/* Criterion 2: Status */}
          <div>
            <label className="block text-[10px] font-mono text-slate-500 uppercase font-bold mb-1">
              [ FILTER_01: STATE_MACHINE ]
            </label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-3 py-1.5 text-xs font-mono bg-slate-50 border border-slate-200 rounded-lg text-slate-800 focus:bg-white focus:outline-none focus:border-slate-400"
            >
              <option value="">ALL_STATES (8 Stages)</option>
              <option value="NEW">NEW</option>
              <option value="CLASSIFIED">CLASSIFIED</option>
              <option value="ASSIGNED">ASSIGNED</option>
              <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="VERIFIED">VERIFIED</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>

          {/* Criterion 3: Priority */}
          <div>
            <label className="block text-[10px] font-mono text-slate-500 uppercase font-bold mb-1">
              [ FILTER_02: SEVERITY_LEVEL ]
            </label>
            <select
              value={filterPriority}
              onChange={(e) => setFilterPriority(e.target.value)}
              className="w-full px-3 py-1.5 text-xs font-mono bg-slate-50 border border-slate-200 rounded-lg text-slate-800 focus:bg-white focus:outline-none focus:border-slate-400"
            >
              <option value="">ALL_SEVERITIES</option>
              <option value="P1_CRITICAL">P1_CRITICAL</option>
              <option value="P2_HIGH">P2_HIGH</option>
              <option value="P3_MEDIUM">P3_MEDIUM</option>
              <option value="P4_LOW">P4_LOW</option>
            </select>
          </div>

          {/* Criterion 4: Configured Categories */}
          <div>
            <label className="block text-[10px] font-mono text-slate-500 uppercase font-bold mb-1">
              [ FILTER_03: DOMAIN_CATEGORY ]
            </label>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="w-full px-3 py-1.5 text-xs font-mono bg-slate-50 border border-slate-200 rounded-lg text-slate-800 focus:bg-white focus:outline-none focus:border-slate-400"
            >
              <option value="">ALL_DOMAINS</option>
              {categories.map((c) => (
                <option key={c.id} value={c.name}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Incident Ledger Table */}
      <div className="bg-white rounded-2xl border border-slate-200/90 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px]">
              <tr>
                <th className="py-3 px-4">INCIDENT_ID</th>
                <th className="py-3 px-4">TITLE & SUMMARY</th>
                <th className="py-3 px-4">DOMAIN</th>
                <th className="py-3 px-4">PRIORITY</th>
                <th className="py-3 px-4">LIFECYCLE</th>
                <th className="py-3 px-4">REPORTER</th>
                <th className="py-3 px-4">ASSIGNEE</th>
                <th className="py-3 px-4">SLA_WINDOW</th>
                <th className="py-3 px-4 text-right">TELEMETRY</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan="9" className="py-14 text-center text-slate-400">
                    <div className="inline-block animate-spin w-5 h-5 border-2 border-slate-300 border-t-slate-800 rounded-full mb-2"></div>
                    <div>[ RETRIEVING_DATA_STREAM... ]</div>
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="9" className="py-14 text-center text-slate-400">
                    {scope === 'mine'
                      ? '[ NO_DISPATCHED_INCIDENTS_FOR_CURRENT_OPERATOR ]'
                      : '[ ZERO_INCIDENT_RECORDS_MATCHING_SPECIFIED_CRITERIA ]'}
                  </td>
                </tr>
              ) : (
                filtered.map((inc) => {
                  const isBreached =
                    inc.resolutionDeadline &&
                    new Date(inc.resolutionDeadline) < new Date() &&
                    inc.status !== 'RESOLVED' &&
                    inc.status !== 'VERIFIED' &&
                    inc.status !== 'CLOSED';

                  return (
                    <tr key={inc.id} className="hover:bg-slate-50/80 transition-colors group">
                      <td className="py-3 px-4 font-bold text-slate-900 group-hover:text-slate-700">
                        {inc.incidentNumber}
                      </td>
                      <td className="py-3 px-4 max-w-xs font-sans">
                        <div className="font-semibold text-slate-900 truncate">
                          {inc.title}
                        </div>
                        <div className="text-[11px] text-slate-500 truncate font-mono">
                          {inc.description}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 border border-slate-200 text-slate-700 font-semibold">
                          {inc.category}
                        </span>
                      </td>
                      <td className="py-3 px-4">{getPriorityBadge(inc.priority)}</td>
                      <td className="py-3 px-4">{getStatusBadge(inc.status)}</td>
                      <td className="py-3 px-4 text-slate-700 font-sans">{inc.reporterName}</td>
                      <td className="py-3 px-4 text-slate-800">
                        {inc.assignedTechnicianName ? (
                          <span className="text-slate-900 font-semibold font-mono">{inc.assignedTechnicianName}</span>
                        ) : (
                          <span className="text-slate-400 italic font-mono">[UNASSIGNED]</span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        {inc.resolutionDeadline ? (
                          <div
                            className={`text-xs ${
                              isBreached
                                ? 'text-rose-700 font-bold bg-rose-50 px-2 py-0.5 rounded border border-rose-200 inline-flex items-center space-x-1'
                                : 'text-slate-600'
                            }`}
                          >
                            <span>
                              {new Date(inc.resolutionDeadline).toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                            {isBreached && <span className="text-[9px] uppercase tracking-wider text-rose-700 font-bold">!BREACH</span>}
                          </div>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Link
                          to={`/incidents/${inc.id}`}
                          className="inline-flex items-center space-x-1 px-2.5 py-1 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-700 rounded-lg text-xs font-mono font-medium transition-colors"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          <span>INSPECT</span>
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
