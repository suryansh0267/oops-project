import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import {
  AlertTriangle,
  Send,
  Sparkles,
  Info,
  CheckCircle2,
  Server,
  Layers,
  Flame,
  Terminal,
  Activity,
  Radar
} from 'lucide-react';

export default function ReportIncident() {
  const navigate = useNavigate();
  const [services, setServices] = useState([]);
  const [categories, setCategories] = useState([]);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'APPLICATION',
    impact: 'MEDIUM',
    urgency: 'MEDIUM',
    affectedServiceId: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [duplicates, setDuplicates] = useState([]);
  const [allIncidents, setAllIncidents] = useState([]);

  useEffect(() => {
    async function loadData() {
      try {
        const [servicesRes, incRes, catsRes] = await Promise.all([
          api.get('/public/services'),
          api.get('/incidents'),
          api.get('/public/categories'),
        ]);
        setServices(servicesRes.data);
        setAllIncidents(incRes.data);
        setCategories(catsRes.data);
        if (servicesRes.data.length > 0) {
          setFormData((prev) => ({ ...prev, affectedServiceId: servicesRes.data[0].id }));
        }
      } catch (err) {
        console.error(err);
      }
    }
    loadData();
  }, []);

  // Client-side real-time Duplicate Detection (Intelligent Feature)
  useEffect(() => {
    if (!formData.title || formData.title.length < 5) {
      setDuplicates([]);
      return;
    }

    const titleTokens = new Set(
      formData.title.toLowerCase().split(/\W+/).filter((w) => w.length > 2)
    );

    const matches = allIncidents.filter((inc) => {
      if (inc.status === 'CLOSED' || inc.status === 'RESOLVED') return false;
      const otherTokens = new Set(
        inc.title.toLowerCase().split(/\W+/).filter((w) => w.length > 2)
      );

      let shared = 0;
      for (const t of titleTokens) {
        if (otherTokens.has(t)) shared++;
      }
      return shared >= 2;
    });

    setDuplicates(matches.slice(0, 3));
  }, [formData.title, allIncidents]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const payload = {
        title: formData.title,
        description: formData.description,
        category: formData.category,
        impact: formData.impact,
        urgency: formData.urgency,
        affectedServiceId: formData.affectedServiceId ? Number(formData.affectedServiceId) : null,
      };

      const res = await api.post('/incidents', payload);
      navigate(`/incidents/${res.data.id}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to dispatch incident');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="hud-panel p-6 rounded-2xl relative overflow-hidden bg-white border border-slate-200 shadow-xs">
        <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500 mb-1 uppercase tracking-widest font-semibold">
          <Terminal className="w-3.5 h-3.5" />
          <span>INCIDENT_DISPATCH_PORTAL // TELEMETRY_INGESTION</span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-4 border-b border-slate-200">
          <div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight font-heading">
              Dispatch Operational Incident
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Automated classification, Strategy-based priority calculation, and dynamic SLA deadline derivation.
            </p>
          </div>
          <div className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-700 font-mono text-[10px] font-semibold">
            <Radar className="w-3.5 h-3.5 text-slate-700 animate-spin" />
            <span>HEURISTIC_DEDUPLICATION: ARMED</span>
          </div>
        </div>

        {error && (
          <div className="mt-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-mono">
            [!ERROR]: {error}
          </div>
        )}

        {/* Intelligent Duplicate Warning Banner */}
        {duplicates.length > 0 && (
          <div className="mt-4 p-4 rounded-2xl bg-amber-50/80 border border-amber-200 text-slate-800 animate-in fade-in duration-200 font-mono">
            <div className="flex items-center space-x-2 font-bold text-xs text-amber-900">
              <AlertTriangle className="h-4 w-4 text-amber-600 flex-shrink-0 animate-pulse" />
              <span>[WARNING: HEURISTIC_MATCH_DETECTED] SIMILAR ACTIVE TICKETS EXIST</span>
            </div>
            <p className="text-[11px] text-amber-800/90 mt-1 font-sans">
              Similar operational disruptions are already under investigation. Please review existing tickets to prevent redundant dispatches:
            </p>
            <div className="mt-2.5 space-y-1.5">
              {duplicates.map((d) => (
                <div key={d.id} className="text-xs bg-white p-2.5 rounded-xl border border-amber-200 flex justify-between items-center shadow-xs">
                  <span className="font-semibold text-slate-900 font-sans">
                    <span className="font-mono text-slate-600 mr-2">[{d.incidentNumber}]</span>
                    {d.title}
                  </span>
                  <span className="text-[10px] font-mono text-amber-900 px-2 py-0.5 rounded bg-amber-50 border border-amber-300 font-bold">
                    STAGE: {d.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-5 font-mono text-xs">
          <div>
            <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5">
              INCIDENT_TITLE / DISRUPTION_SUMMARY *
            </label>
            <input
              type="text"
              name="title"
              value={formData.title}
              onChange={handleChange}
              required
              placeholder="e.g. Identity SSO Service returns HTTP 504 Gateway Timeout during auth handshake"
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 font-sans"
            />
          </div>

          <div>
            <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5">
              DETAILED_SYMPTOM_DIAGNOSTICS *
            </label>
            <textarea
              name="description"
              rows={4}
              value={formData.description}
              onChange={handleChange}
              required
              placeholder="Detail error trace, affected user groups, reproduction sequence, and observed operational degradation..."
              className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 font-sans leading-relaxed"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5">
                AFFECTED_IT_SERVICE
              </label>
              <select
                name="affectedServiceId"
                value={formData.affectedServiceId}
                onChange={handleChange}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
              >
                {services.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} (Criticality: {s.businessCriticality}/5)
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5">
                DOMAIN_CLASSIFICATION
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-mono"
              >
                {categories.length > 0 ? (
                  categories.map((c) => (
                    <option key={c.id} value={c.name}>
                      {c.name}
                    </option>
                  ))
                ) : (
                  <>
                    <option value="APPLICATION">APPLICATION</option>
                    <option value="DATABASE">DATABASE</option>
                    <option value="NETWORK">NETWORK</option>
                    <option value="SECURITY">SECURITY</option>
                    <option value="AUTHENTICATION">AUTHENTICATION</option>
                    <option value="CLOUD">CLOUD</option>
                    <option value="STORAGE">STORAGE</option>
                    <option value="HARDWARE">HARDWARE</option>
                    <option value="SOFTWARE">SOFTWARE</option>
                    <option value="OTHER">OTHER</option>
                  </>
                )}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 rounded-xl bg-slate-50 border border-slate-200">
            <div>
              <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5 flex items-center space-x-1.5">
                <Flame className="h-3.5 w-3.5 text-rose-600" />
                <span>BUSINESS_IMPACT_TIER</span>
              </label>
              <select
                name="impact"
                value={formData.impact}
                onChange={handleChange}
                className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-slate-900 focus:outline-none focus:border-slate-900"
              >
                <option value="HIGH">HIGH (Org-wide / multi-department disruption)</option>
                <option value="MEDIUM">MEDIUM (Group-level / partial degradation)</option>
                <option value="LOW">LOW (Isolated single user or cosmetic defect)</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] uppercase tracking-wider text-slate-700 font-bold mb-1.5 flex items-center space-x-1.5">
                <Layers className="h-3.5 w-3.5 text-amber-600" />
                <span>URGENCY_TIER</span>
              </label>
              <select
                name="urgency"
                value={formData.urgency}
                onChange={handleChange}
                className="w-full px-3 py-2 bg-white border border-slate-300 rounded-xl text-slate-900 focus:outline-none focus:border-slate-900"
              >
                <option value="HIGH">HIGH (Immediate remediation required)</option>
                <option value="MEDIUM">MEDIUM (Temporary workaround viable)</option>
                <option value="LOW">LOW (Scheduled sprint / non-urgent patch)</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end space-x-3 pt-3">
            <button
              type="button"
              onClick={() => navigate('/incidents')}
              className="px-4 py-2 border border-slate-300 text-slate-700 hover:bg-slate-50 text-xs font-mono font-medium rounded-xl transition-colors"
            >
              ABORT
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs font-mono rounded-xl shadow-xs transition-colors flex items-center space-x-1.5 disabled:opacity-50"
            >
              <Send className="h-3.5 w-3.5" />
              <span>{loading ? 'DISPATCHING_TELEMETRY...' : 'DISPATCH_INCIDENT'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
