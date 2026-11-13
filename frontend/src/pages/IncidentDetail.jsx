import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import {
  Clock,
  User,
  Shield,
  CheckCircle2,
  AlertTriangle,
  Send,
  MessageSquare,
  History,
  Star,
  Zap,
  ArrowRight,
  Terminal,
  Activity,
  Layers,
  HelpCircle,
  MessageCircle,
  AlertOctagon
} from 'lucide-react';

const LIFECYCLE_STEPS = [
  'NEW',
  'CLASSIFIED',
  'ASSIGNED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
];

export default function IncidentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, isAdmin, isTechnician } = useAuth();

  const [incident, setIncident] = useState(null);
  const [auditLogs, setAuditLogs] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [selectedTech, setSelectedTech] = useState('');
  const [resolveNotes, setResolveNotes] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [isInternalNote, setIsInternalNote] = useState(false);
  const [feedback, setFeedback] = useState('');
  const [rating, setRating] = useState(5);
  const [actionLoading, setActionLoading] = useState(false);
  const [requestQuestion, setRequestQuestion] = useState('');
  const [respondAnswer, setRespondAnswer] = useState('');

  useEffect(() => {
    fetchIncident();
    fetchAuditLogs();
    if (isAdmin || isTechnician) {
      fetchTechnicians();
    }
  }, [id]);

  const fetchIncident = async () => {
    try {
      const res = await api.get(`/incidents/${id}`);
      setIncident(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchAuditLogs = async () => {
    try {
      const res = await api.get(`/admin/audit-logs/incident/${id}`);
      setAuditLogs(res.data);
    } catch (err) {
      // If employee, might not have admin rights for audit-logs endpoint; fail silently
    }
  };

  const fetchTechnicians = async () => {
    try {
      const res = await api.get('/admin/users/role/TECHNICIAN');
      setTechnicians(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  // State Transition Handlers
  const handleAutoAssign = async () => {
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/auto-assign`);
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Auto-assign failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleManualAssign = async () => {
    if (!selectedTech) return;
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/assign`, { technicianId: Number(selectedTech) });
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Assignment failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAcknowledge = async () => {
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/acknowledge`);
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Acknowledge failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartInvestigation = async () => {
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/investigate`);
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Start investigation failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResolve = async (e) => {
    e.preventDefault();
    if (!resolveNotes) return;
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/resolve`, { resolutionNotes: resolveNotes });
      setResolveNotes('');
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Resolve failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleVerify = async () => {
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/verify`);
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Verification failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleClose = async () => {
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/close`);
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Close failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRequestInfo = async (e) => {
    e.preventDefault();
    if (!requestQuestion.trim()) return;
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/request-info`, { question: requestQuestion });
      setRequestQuestion('');
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to send request');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRespondInfo = async (e) => {
    e.preventDefault();
    if (!respondAnswer.trim()) return;
    setActionLoading(true);
    try {
      await api.post(`/incidents/${id}/respond-info`, { response: respondAnswer });
      setRespondAnswer('');
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to submit response');
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddNote = async (e) => {
    e.preventDefault();
    if (!noteContent.trim()) return;
    try {
      await api.post(`/incidents/${id}/notes`, {
        content: noteContent,
        internalNote: isInternalNote,
      });
      setNoteContent('');
      fetchIncident();
      fetchAuditLogs();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to add note');
    }
  };

  const handleFeedback = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/incidents/${id}/feedback`, {
        feedback,
        rating,
      });
      fetchIncident();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to submit feedback');
    }
  };

  if (loading || !incident) {
    return (
      <div className="flex flex-col items-center justify-center h-80 space-y-4">
        <div className="w-10 h-10 border-2 border-slate-200 border-t-slate-900 rounded-full animate-spin"></div>
        <p className="font-mono text-xs text-slate-500">[ DECODING_INCIDENT_STREAM... ]</p>
      </div>
    );
  }

  const currentStepIndex = LIFECYCLE_STEPS.indexOf(incident.status);
  const isBreached =
    incident.resolutionDeadline &&
    new Date(incident.resolutionDeadline) < new Date() &&
    incident.status !== 'RESOLVED' &&
    incident.status !== 'VERIFIED' &&
    incident.status !== 'CLOSED';

  const priorityStyle =
    incident.priority === 'P1_CRITICAL'
      ? 'bg-rose-50 text-rose-700 border-rose-200'
      : incident.priority === 'P2_HIGH'
      ? 'bg-amber-50 text-amber-800 border-amber-200'
      : 'bg-slate-100 text-slate-700 border-slate-200';

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Top Banner / Telemetry Header */}
      <div className="hud-panel p-6 rounded-2xl relative overflow-hidden bg-white border border-slate-200 shadow-xs">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-6 border-b border-slate-200">
          <div>
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <span className="font-mono text-xs font-bold px-2.5 py-1 rounded-lg bg-slate-900 text-white shadow-xs">
                {incident.incidentNumber}
              </span>
              <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                DOMAIN: {incident.category}
              </span>
              <span className={`text-[11px] font-mono px-2 py-0.5 rounded border font-semibold ${priorityStyle}`}>
                PRIORITY: {incident.priority}
              </span>
            </div>
            <h1 className="text-xl lg:text-2xl font-bold text-slate-900 tracking-tight font-heading">
              {incident.title}
            </h1>
            <p className="text-xs text-slate-500 mt-1 font-mono">
              DISPATCHED BY <strong className="text-slate-800">{incident.reporterName}</strong> // AT{' '}
              {new Date(incident.createdAt).toLocaleString()}
            </p>
          </div>

          <div className="lg:text-right font-mono">
            <div className="text-[10px] text-slate-500 uppercase tracking-widest font-semibold">STATE_MACHINE_STATUS</div>
            <div className="text-lg font-bold text-slate-900">{incident.status}</div>
            {incident.resolutionDeadline && (
              <div
                className={`text-xs mt-1 ${
                  isBreached ? 'text-rose-700 font-bold bg-rose-50 px-2 py-0.5 rounded border border-rose-200' : 'text-slate-500'
                }`}
              >
                RESOLUTION_DEADLINE: {new Date(incident.resolutionDeadline).toLocaleTimeString()}
                {isBreached && <span className="ml-1 text-[10px] font-bold text-rose-700">[!SLA_BREACH]</span>}
              </div>
            )}
          </div>
        </div>

        {/* State Machine Stepper (OOP State Pattern Lifecycle) */}
        <div className="mt-6">
          <div className="text-[10px] uppercase font-mono tracking-widest text-slate-500 mb-3 flex items-center justify-between">
            <span className="flex items-center space-x-1.5 font-semibold">
              <Activity className="w-3.5 h-3.5 text-slate-700" />
              <span>OOP_STATE_PATTERN_MACHINE // 8_STAGE_VALIDATED_GRAPH</span>
            </span>
            <span className="text-slate-400">STAGE {currentStepIndex + 1} OF 8</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-2">
            {LIFECYCLE_STEPS.map((step, idx) => {
              const isPast = idx < currentStepIndex;
              const isCurrent = idx === currentStepIndex;

              return (
                <div
                  key={step}
                  className={`p-2 rounded-xl text-center font-mono text-[10px] font-bold border transition-all ${
                    isCurrent
                      ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                      : isPast
                      ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                      : 'bg-slate-50 text-slate-400 border-slate-200'
                  }`}
                >
                  <div className="truncate">{step}</div>
                  <div className="text-[9px] opacity-80 mt-0.5">
                    {isCurrent ? '● CURRENT' : isPast ? '✓ PASSED' : '○ QUEUED'}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Details & Lifecycle Actions */}
        <div className="lg:col-span-2 space-y-6">
          {/* Incident Description */}
          <div className="hud-panel p-5 rounded-2xl space-y-3 bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center space-x-2 text-xs font-mono uppercase text-slate-700 font-bold pb-2 border-b border-slate-200">
              <Terminal className="h-4 w-4 text-slate-700" />
              <span>INCIDENT_TELEMETRY_DESCRIPTION</span>
            </div>
            <p className="text-xs text-slate-700 whitespace-pre-wrap leading-relaxed font-sans">
              {incident.description}
            </p>

            {incident.resolutionNotes && (
              <div className="mt-4 p-4 rounded-xl bg-slate-50 border border-slate-200 text-xs font-mono text-slate-800 space-y-1">
                <div className="font-bold flex items-center space-x-1.5 text-slate-900">
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                  <span>RESOLUTION_TELEMETRY_LOG:</span>
                </div>
                <p className="whitespace-pre-wrap font-sans text-slate-700 pt-1">{incident.resolutionNotes}</p>
                <span className="text-[10px] text-slate-500 block pt-1">
                  TIMESTAMP: {new Date(incident.resolvedAt).toLocaleString()}
                </span>
              </div>
            )}
          </div>

          {/* Action Center (State Transitions) */}
          <div className="hud-panel p-5 rounded-2xl space-y-4 bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center space-x-2 text-xs font-mono uppercase text-slate-900 font-bold pb-2 border-b border-slate-200">
              <Zap className="h-4 w-4 text-amber-500" />
              <span>LIFECYCLE_CONTROL_CENTER // OPERATOR_ACTIONS</span>
            </div>

            {/* Step: ASSIGN */}
            {(incident.status === 'NEW' || incident.status === 'CLASSIFIED' || incident.status === 'ASSIGNED') && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
                <div className="text-xs font-mono font-semibold text-slate-800 flex items-center justify-between">
                  <span>DISPATCH_TECHNICIAN (STRATEGY_PATTERN)</span>
                  <button
                    onClick={handleAutoAssign}
                    disabled={actionLoading}
                    className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold shadow-xs transition-colors disabled:opacity-50"
                  >
                    <Zap className="h-3 w-3 text-amber-400" />
                    <span>AUTO_ASSIGN (SKILL & LOAD MATCH)</span>
                  </button>
                </div>

                <div className="flex gap-2">
                  <select
                    value={selectedTech}
                    onChange={(e) => setSelectedTech(e.target.value)}
                    className="flex-1 px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-xl text-slate-900 focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                  >
                    <option value="">Manual Dispatch: Select Engineer...</option>
                    {technicians.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.fullName} ({t.department || 'IT'})
                      </option>
                    ))}
                  </select>
                  <button
                    onClick={handleManualAssign}
                    disabled={!selectedTech || actionLoading}
                    className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold transition-colors disabled:opacity-50 shadow-xs"
                  >
                    DISPATCH
                  </button>
                </div>
              </div>
            )}

            {/* Step: ACKNOWLEDGE */}
            {incident.status === 'ASSIGNED' && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                <div>
                  <div className="text-xs font-mono font-bold text-slate-900">ACKNOWLEDGE_ASSIGNMENT</div>
                  <p className="text-[11px] text-slate-500">Confirm receipt of incident to stop response SLA timer countdown.</p>
                </div>
                <button
                  onClick={handleAcknowledge}
                  disabled={actionLoading}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono rounded-xl shadow-xs font-bold transition-colors"
                >
                  ACKNOWLEDGE
                </button>
              </div>
            )}

            {/* Step: START INVESTIGATION */}
            {incident.status === 'ACKNOWLEDGED' && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                <div>
                  <div className="text-xs font-mono font-bold text-slate-900">INITIATE_INVESTIGATION</div>
                  <p className="text-[11px] text-slate-500">Advance state to IN_PROGRESS to engage technical remediation.</p>
                </div>
                <button
                  onClick={handleStartInvestigation}
                  disabled={actionLoading}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono rounded-xl shadow-xs font-bold transition-colors"
                >
                  START INVESTIGATION
                </button>
              </div>
            )}

            {/* Step: RESOLVE */}
            {incident.status === 'IN_PROGRESS' && (
              <form onSubmit={handleResolve} className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
                <div className="text-xs font-mono font-bold text-slate-900">RESOLVE_INCIDENT_TICKET</div>
                <textarea
                  rows={3}
                  value={resolveNotes}
                  onChange={(e) => setResolveNotes(e.target.value)}
                  required
                  placeholder="Detail root cause analysis and technical remediation steps..."
                  className="w-full px-3 py-2 text-xs font-mono bg-white border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                />
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs font-mono rounded-xl shadow-xs transition-colors"
                >
                  SUBMIT RESOLUTION (SET RESOLVED)
                </button>
              </form>
            )}

            {/* Support Information Exchange */}
            {(incident.status === 'ASSIGNED' || incident.status === 'ACKNOWLEDGED' || incident.status === 'IN_PROGRESS') && (
              <div className="space-y-3">
                {/* Technician: Request Additional Information */}
                {(isTechnician || isAdmin) && (
                  <form onSubmit={handleRequestInfo} className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                    <div className="text-xs font-mono font-bold text-slate-900 flex items-center justify-between">
                      <span className="flex items-center space-x-1.5">
                        <HelpCircle className="w-3.5 h-3.5 text-amber-600" />
                        <span>REQUEST_INFO_FROM_REPORTER</span>
                      </span>
                      <span className="text-[10px] text-slate-500 font-normal">DISPATCHES TELEMETRY QUERY</span>
                    </div>
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={requestQuestion}
                        onChange={(e) => setRequestQuestion(e.target.value)}
                        placeholder="Request specific stack traces, screenshots, or reproducing steps..."
                        className="flex-1 px-3 py-1.5 text-xs font-mono bg-white border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                      />
                      <button
                        type="submit"
                        disabled={actionLoading || !requestQuestion.trim()}
                        className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-semibold rounded-xl disabled:opacity-50 shadow-xs"
                      >
                        DISPATCH QUERY
                      </button>
                    </div>
                  </form>
                )}

                {/* Employee: Respond to Support Request */}
                <form onSubmit={handleRespondInfo} className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                  <div className="text-xs font-mono font-bold text-slate-900 flex items-center justify-between">
                    <span className="flex items-center space-x-1.5">
                      <MessageCircle className="w-3.5 h-3.5 text-slate-700" />
                      <span>RESPOND_TO_SUPPORT_REQUEST // ADD_INFO</span>
                    </span>
                    <span className="text-[10px] text-slate-500 font-normal">DIRECT COMMS LINE</span>
                  </div>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={respondAnswer}
                      onChange={(e) => setRespondAnswer(e.target.value)}
                      placeholder="Input additional observations or requested details..."
                      className="flex-1 px-3 py-1.5 text-xs font-mono bg-white border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                    />
                    <button
                      type="submit"
                      disabled={actionLoading || !respondAnswer.trim()}
                      className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-semibold rounded-xl disabled:opacity-50 shadow-xs"
                    >
                      TRANSMIT
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* Step: VERIFY & CLOSE */}
            {(incident.status === 'RESOLVED' || incident.status === 'VERIFIED') && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                <div>
                  <div className="text-xs font-mono font-bold text-slate-900">VERIFY_AND_CLOSE_INCIDENT</div>
                  <p className="text-[11px] text-slate-500">Confirm operational restoration and archive lifecycle record.</p>
                </div>
                <div className="flex space-x-2">
                  {incident.status === 'RESOLVED' && (
                    <button
                      onClick={handleVerify}
                      disabled={actionLoading}
                      className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-mono rounded-xl font-bold shadow-xs transition-colors"
                    >
                      VERIFY
                    </button>
                  )}
                  <button
                    onClick={handleClose}
                    disabled={actionLoading}
                    className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs font-mono rounded-xl shadow-xs transition-colors"
                  >
                    CLOSE TICKET
                  </button>
                </div>
              </div>
            )}

            {/* Customer Feedback section */}
            {(incident.status === 'RESOLVED' || incident.status === 'VERIFIED' || incident.status === 'CLOSED') && (
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
                <div className="text-xs font-mono font-bold text-slate-900">RESOLUTION_SATISFACTION_METRICS</div>
                {incident.feedback ? (
                  <div className="text-xs text-slate-800 font-mono">
                    <div className="flex items-center space-x-1 mb-1 text-amber-500">
                      {[...Array(incident.feedbackRating || 5)].map((_, i) => (
                        <Star key={i} className="h-4 w-4 fill-amber-400" />
                      ))}
                    </div>
                    <p className="italic font-sans text-slate-700">"{incident.feedback}"</p>
                  </div>
                ) : (
                  <form onSubmit={handleFeedback} className="space-y-2">
                    <div className="flex items-center space-x-2 text-xs font-mono">
                      <span className="text-slate-700 font-bold">RATING:</span>
                      <select
                        value={rating}
                        onChange={(e) => setRating(Number(e.target.value))}
                        className="px-2 py-1 text-xs font-mono bg-white border border-slate-300 rounded-lg text-slate-900"
                      >
                        <option value={5}>5 STARS - EXCELLENT RESOLUTION</option>
                        <option value={4}>4 STARS - GOOD</option>
                        <option value={3}>3 STARS - SATISFACTORY</option>
                        <option value={2}>2 STARS - SUBOPTIMAL</option>
                        <option value={1}>1 STAR - UNRESOLVED / CRITICAL ISSUES</option>
                      </select>
                    </div>
                    <input
                      type="text"
                      value={feedback}
                      onChange={(e) => setFeedback(e.target.value)}
                      placeholder="Comment on resolution speed, technical communication..."
                      className="w-full px-3 py-1.5 text-xs font-mono bg-white border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:outline-none focus:border-slate-900"
                    />
                    <button
                      type="submit"
                      className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs font-mono rounded-xl shadow-xs"
                    >
                      RECORD FEEDBACK
                    </button>
                  </form>
                )}
              </div>
            )}
          </div>

          {/* Add Notes Section */}
          <div className="hud-panel p-5 rounded-2xl space-y-4 bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center space-x-2 text-xs font-mono uppercase text-slate-900 font-bold pb-2 border-b border-slate-200">
              <MessageSquare className="h-4 w-4 text-slate-700" />
              <span>INVESTIGATION_NOTES_&_COMMS_STREAM</span>
            </div>

            <form onSubmit={handleAddNote} className="space-y-3">
              <textarea
                rows={2}
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                placeholder="Log investigative telemetry, diagnostic outputs, or communication updates..."
                className="w-full px-3 py-2 text-xs font-mono bg-slate-50 border border-slate-300 rounded-xl text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
              />
              <div className="flex items-center justify-between">
                {(isAdmin || isTechnician) && (
                  <label className="flex items-center space-x-2 text-xs font-mono text-slate-600 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={isInternalNote}
                      onChange={(e) => setIsInternalNote(e.target.checked)}
                      className="rounded bg-white border-slate-300 text-slate-900 focus:ring-0"
                    />
                    <span>INTERNAL_ENGINEERING_NOTE (RESTRICTED_FROM_REPORTER)</span>
                  </label>
                )}
                <button
                  type="submit"
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-bold rounded-xl flex items-center space-x-1.5 ml-auto shadow-xs transition-colors"
                >
                  <Send className="h-3 w-3" />
                  <span>TRANSMIT_NOTE</span>
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right Column: Metadata & Audit Trail */}
        <div className="space-y-6">
          {/* Metadata Card */}
          <div className="hud-panel p-5 rounded-2xl space-y-3 text-xs font-mono bg-white border border-slate-200 shadow-xs">
            <h3 className="font-bold text-slate-900 uppercase tracking-wider text-[11px] pb-2 border-b border-slate-200 flex items-center justify-between">
              <span>INCIDENT_TELEMETRY_SPECS</span>
              <span className="text-slate-500 text-[10px]">ID: {incident.id}</span>
            </h3>

            <div className="flex justify-between py-1 border-b border-slate-100">
              <span className="text-slate-500">SERVICE:</span>
              <span className="text-slate-900 font-medium">{incident.affectedServiceName || 'UNMAPPED'}</span>
            </div>

            <div className="flex justify-between py-1 border-b border-slate-100">
              <span className="text-slate-500">IMPACT_TIER:</span>
              <span className="text-slate-900 font-semibold">{incident.impact}</span>
            </div>

            <div className="flex justify-between py-1 border-b border-slate-100">
              <span className="text-slate-500">URGENCY_TIER:</span>
              <span className="text-slate-900 font-semibold">{incident.urgency}</span>
            </div>

            <div className="flex justify-between py-1 border-b border-slate-100">
              <span className="text-slate-500">ASSIGNED_ENGINEER:</span>
              <span className="text-slate-900 font-semibold">
                {incident.assignedTechnicianName || '[UNASSIGNED]'}
              </span>
            </div>

            <div className="flex justify-between py-1 border-b border-slate-100">
              <span className="text-slate-500">RESPONSE_TARGET:</span>
              <span className="text-slate-800">
                {incident.responseDeadline
                  ? new Date(incident.responseDeadline).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                  : 'N/A'}
              </span>
            </div>

            <div className="flex justify-between py-1">
              <span className="text-slate-500">RESOLUTION_TARGET:</span>
              <span className="text-slate-800">
                {incident.resolutionDeadline
                  ? new Date(incident.resolutionDeadline).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                  : 'N/A'}
              </span>
            </div>
          </div>

          {/* Complete Audit Trail */}
          <div className="hud-panel p-5 rounded-2xl space-y-3 bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center space-x-2 text-xs font-mono uppercase text-slate-900 font-bold pb-2 border-b border-slate-200">
              <History className="h-4 w-4 text-slate-700" />
              <span>AUDIT_LOGGER // FORENSIC_TIMELINE</span>
            </div>

            <div className="space-y-2.5 max-h-96 overflow-y-auto pr-1">
              {auditLogs.length === 0 ? (
                <p className="text-xs font-mono text-slate-400 text-center py-6">
                  [ NO_AUDIT_TRAIL_RECORDS_INDEXED ]
                </p>
              ) : (
                auditLogs.map((log) => (
                  <div key={log.id} className="text-xs p-2.5 rounded-xl bg-slate-50 border border-slate-200 font-mono">
                    <div className="flex items-center justify-between text-[10px] text-slate-500 mb-1">
                      <span className="font-bold text-slate-900">{log.action}</span>
                      <span className="text-slate-400">
                        {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-slate-700 text-[11px] font-sans">{log.description}</p>
                    <span className="text-[10px] text-slate-500 mt-1 block">
                      ACTOR: {log.performedByName}
                    </span>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
