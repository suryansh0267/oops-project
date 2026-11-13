import React, { useState, useEffect } from 'react';
import api from '../api/client';
import {
  Users,
  Server,
  Clock,
  ShieldAlert,
  History,
  Plus,
  Trash2,
  CheckCircle,
  AlertOctagon,
  Wrench,
  Tag,
  GitBranch,
  Edit,
  Save,
  X,
  Terminal,
  Activity,
  Layers,
  Cpu,
  UserPlus
} from 'lucide-react';

export default function AdminManagement() {
  const [activeTab, setActiveTab] = useState('users');

  // Data states
  const [users, setUsers] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [services, setServices] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [violations, setViolations] = useState([]);
  const [categories, setCategories] = useState([]);
  const [strategies, setStrategies] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  // New Service Form State
  const [newServiceName, setNewServiceName] = useState('');
  const [newServiceDesc, setNewServiceDesc] = useState('');
  const [newServiceCat, setNewServiceCat] = useState('Core');
  const [newServiceCrit, setNewServiceCrit] = useState(3);
  const [newServiceContact, setNewServiceContact] = useState('');

  // New SLA Form State
  const [newSlaPriority, setNewSlaPriority] = useState('P2_HIGH');
  const [newSlaResponse, setNewSlaResponse] = useState(15);
  const [newSlaResolution, setNewSlaResolution] = useState(120);
  const [newSlaDesc, setNewSlaDesc] = useState('');

  // New Category Form State
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');

  // Technician Edit Modal State
  const [editingTech, setEditingTech] = useState(null);
  const [editSkills, setEditSkills] = useState('');
  const [editSpecialization, setEditSpecialization] = useState('');
  const [editMaxActive, setEditMaxActive] = useState(10);
  const [editAvailable, setEditAvailable] = useState(true);

  // New User / Technician Form State
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newUserFullName, setNewUserFullName] = useState('');
  const [newUserUsername, setNewUserUsername] = useState('');
  const [newUserEmail, setNewUserEmail] = useState('');
  const [newUserPassword, setNewUserPassword] = useState('tech123');
  const [newUserRole, setNewUserRole] = useState('TECHNICIAN');
  const [newUserDept, setNewUserDept] = useState('IT Support');
  const [newUserPhone, setNewUserPhone] = useState('555-0150');
  const [newUserSkills, setNewUserSkills] = useState('NETWORK, CLOUD');
  const [newUserSpecialization, setNewUserSpecialization] = useState('Network Infrastructure & Firewalls');

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'users') {
        const res = await api.get('/admin/users');
        setUsers(res.data);
      } else if (activeTab === 'technicians') {
        const res = await api.get('/admin/technicians');
        setTechnicians(res.data);
      } else if (activeTab === 'services') {
        const res = await api.get('/admin/services');
        setServices(res.data);
      } else if (activeTab === 'sla') {
        const [pRes, vRes] = await Promise.all([
          api.get('/admin/sla-policies'),
          api.get('/admin/sla-violations'),
        ]);
        setPolicies(pRes.data);
        setViolations(vRes.data);
      } else if (activeTab === 'categories') {
        const res = await api.get('/admin/categories');
        setCategories(res.data);
      } else if (activeTab === 'strategies') {
        const res = await api.get('/admin/assignment-strategies');
        setStrategies(res.data);
      } else if (activeTab === 'audit') {
        const now = new Date();
        const start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
        const end = now.toISOString();
        const res = await api.get(`/admin/audit-logs?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
        setAuditLogs(res.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/admin/users', {
        fullName: newUserFullName,
        username: newUserUsername,
        email: newUserEmail,
        password: newUserPassword,
        role: newUserRole,
        department: newUserDept,
        phoneNumber: newUserPhone,
      });

      // If role is TECHNICIAN and skills were specified, update the technician profile immediately
      if (newUserRole === 'TECHNICIAN' && res.data && res.data.id) {
        const skillsArr = newUserSkills.split(',').map((s) => s.trim().toUpperCase()).filter(Boolean);
        await api.put(`/admin/technicians/${res.data.id}`, {
          skills: skillsArr,
          specialization: newUserSpecialization,
          maxActiveIncidents: 10,
          available: true,
        });
      }

      setShowAddUserModal(false);
      setNewUserFullName('');
      setNewUserUsername('');
      setNewUserEmail('');
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to provision technician / user');
    }
  };

  const handleCreateService = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/services', {
        name: newServiceName,
        description: newServiceDesc,
        category: newServiceCat,
        businessCriticality: Number(newServiceCrit),
        supportContact: newServiceContact,
      });
      setNewServiceName('');
      setNewServiceDesc('');
      setNewServiceContact('');
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create service');
    }
  };

  const handleCreateSla = async (e) => {
    e.preventDefault();
    try {
      await api.post('/admin/sla-policies', {
        priority: newSlaPriority,
        responseTimeMinutes: Number(newSlaResponse),
        resolutionTimeMinutes: Number(newSlaResolution),
        description: newSlaDesc,
      });
      setNewSlaDesc('');
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create policy');
    }
  };

  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!newCatName.trim()) return;
    try {
      await api.post('/admin/categories', {
        name: newCatName.trim(),
        description: newCatDesc.trim(),
      });
      setNewCatName('');
      setNewCatDesc('');
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to create category');
    }
  };

  const handleDeactivateCategory = async (id) => {
    if (!confirm('Are you sure you want to deactivate this category?')) return;
    try {
      await api.delete(`/admin/categories/${id}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to deactivate category');
    }
  };

  const handleSetStrategy = async (strategyKey) => {
    try {
      await api.put('/admin/assignment-strategy', { strategy: strategyKey });
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update assignment strategy');
    }
  };

  const openTechModal = (tech) => {
    setEditingTech(tech);
    setEditSkills(tech.skills?.join(', ') || '');
    setEditSpecialization(tech.specialization || '');
    setEditMaxActive(tech.maxActiveIncidents || 10);
    setEditAvailable(tech.available ?? true);
  };

  const handleSaveTech = async (e) => {
    e.preventDefault();
    if (!editingTech) return;
    try {
      const skillsArr = editSkills.split(',').map((s) => s.trim().toUpperCase()).filter(Boolean);
      await api.put(`/admin/technicians/${editingTech.userId}`, {
        skills: skillsArr,
        specialization: editSpecialization,
        maxActiveIncidents: Number(editMaxActive),
        available: editAvailable,
      });
      setEditingTech(null);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update technician profile');
    }
  };

  const handleDeactivateService = async (id) => {
    if (!confirm('Are you sure you want to deactivate this service?')) return;
    try {
      await api.delete(`/admin/services/${id}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Deactivation failed');
    }
  };

  const handleDeactivateUser = async (id) => {
    if (!confirm('Are you sure you want to deactivate this user?')) return;
    try {
      await api.delete(`/admin/users/${id}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data?.message || 'Deactivation failed');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header & Tabs */}
      <div className="hud-panel p-6 rounded-2xl relative overflow-hidden bg-white border border-slate-200 shadow-xs">
        <div className="flex items-center space-x-2 text-[10px] font-mono text-slate-500 mb-1 uppercase tracking-widest font-semibold">
          <Terminal className="w-3.5 h-3.5" />
          <span>ADMINISTRATOR_CONTROL_PLANE // GOVERNANCE_ROOT</span>
        </div>
        <h1 className="text-xl lg:text-2xl font-bold text-slate-900 tracking-tight font-heading">
          ITSM Architecture Administration & Policy Matrix
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Personnel directory, technician capacity limits, SLA rule sets, runtime category creation, and strategy toggles.
        </p>

        {/* Tactical Tab Switcher */}
        <div className="flex flex-wrap gap-2 mt-6 pt-4 border-t border-slate-100 text-xs font-mono">
          <button
            onClick={() => setActiveTab('users')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'users'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <Users className="h-3.5 w-3.5" />
            <span>USERS ({users.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('technicians')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'technicians'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <Wrench className="h-3.5 w-3.5" />
            <span>TECHNICIANS ({technicians.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('services')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'services'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <Server className="h-3.5 w-3.5" />
            <span>IT_SERVICES ({services.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('sla')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'sla'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <Clock className="h-3.5 w-3.5" />
            <span>SLA_POLICIES & BREACHES</span>
          </button>

          <button
            onClick={() => setActiveTab('categories')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'categories'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <Tag className="h-3.5 w-3.5" />
            <span>CATEGORIES ({categories.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('strategies')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'strategies'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <GitBranch className="h-3.5 w-3.5" />
            <span>ASSIGNMENT_STRATEGY</span>
          </button>

          <button
            onClick={() => setActiveTab('audit')}
            className={`px-3.5 py-2 rounded-xl border transition-all flex items-center space-x-1.5 ${
              activeTab === 'audit'
                ? 'bg-slate-900 text-white border-slate-900 shadow-xs font-bold'
                : 'bg-white text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300'
            }`}
          >
            <History className="h-3.5 w-3.5" />
            <span>AUDIT_TRAIL</span>
          </button>
        </div>
      </div>

      {/* TAB 1: USERS */}
      {activeTab === 'users' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => {
                setNewUserRole('EMPLOYEE');
                setShowAddUserModal(true);
              }}
              className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold flex items-center space-x-1.5 shadow-xs transition-colors"
            >
              <Plus className="h-4 w-4" />
              <span>+ PROVISION PERSONNEL</span>
            </button>
          </div>
          <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
                <tr>
                  <th className="py-3 px-4">FULL_LEGAL_NAME</th>
                  <th className="py-3 px-4">OPERATOR_HANDLE</th>
                  <th className="py-3 px-4">SYSTEM_ROLE</th>
                  <th className="py-3 px-4">DEPARTMENT</th>
                  <th className="py-3 px-4">STATUS</th>
                  <th className="py-3 px-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-semibold text-slate-900 font-sans">{u.fullName}</td>
                    <td className="py-3 px-4 text-slate-700 font-mono font-medium">@{u.username}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          u.role === 'ADMIN'
                            ? 'bg-purple-50 text-purple-700 border border-purple-200'
                            : u.role === 'TECHNICIAN'
                            ? 'bg-slate-100 text-slate-800 border border-slate-200'
                            : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-600 font-sans">{u.department || '—'}</td>
                    <td className="py-3 px-4">
                      {u.active ? (
                        <span className="text-emerald-700 text-[11px] font-medium flex items-center space-x-1.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          <span>ACTIVE</span>
                        </span>
                      ) : (
                        <span className="text-slate-400 text-[11px]">DEACTIVATED</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {u.active && u.username !== 'admin' && (
                        <button
                          onClick={() => handleDeactivateUser(u.id)}
                          className="text-rose-600 hover:text-rose-800 text-xs font-mono font-medium transition-colors"
                        >
                          [DEACTIVATE]
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: MANAGE TECHNICIANS */}
      {activeTab === 'technicians' && (
        <div className="space-y-6">
          <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
            <div className="p-4 border-b border-slate-200 flex items-center justify-between">
              <div>
                <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-900">
                  TECHNICIAN_ROSTER_&_SKILL_MATRIX_CONFIGURATION
                </h3>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Assign domain skills, specialization focus, and capacity bounds for auto-assignment engines.
                </p>
              </div>
              <button
                onClick={() => {
                  setNewUserRole('TECHNICIAN');
                  setShowAddUserModal(true);
                }}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-mono font-semibold flex items-center space-x-1.5 shadow-xs transition-colors"
              >
                <Plus className="h-4 w-4" />
                <span>+ PROVISION NEW TECHNICIAN</span>
              </button>
            </div>

            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
                <tr>
                  <th className="py-3 px-4">TECHNICIAN_OPERATOR</th>
                  <th className="py-3 px-4">SPECIALIZATION</th>
                  <th className="py-3 px-4">SKILLS_MATRIX</th>
                  <th className="py-3 px-4">WORKLOAD_CAP</th>
                  <th className="py-3 px-4">ACTIVE / RESOLVED</th>
                  <th className="py-3 px-4">AVAILABILITY</th>
                  <th className="py-3 px-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {technicians.map((t) => (
                  <tr key={t.userId} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-900 font-sans">{t.fullName}</div>
                      <div className="text-[10px] text-slate-500 font-mono">@{t.username}</div>
                    </td>
                    <td className="py-3 px-4 text-slate-600 font-sans">{t.specialization || 'General Systems'}</td>
                    <td className="py-3 px-4">
                      <div className="flex flex-wrap gap-1">
                        {t.skills?.map((s) => (
                          <span key={s} className="px-1.5 py-0.5 rounded text-[9px] bg-slate-100 border border-slate-200 text-slate-700 font-semibold font-mono">
                            {s}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-slate-900 font-bold font-mono">{t.maxActiveIncidents} MAX</td>
                    <td className="py-3 px-4">
                      <span className="text-amber-700 font-bold">{t.currentActiveIncidents}</span> active /{' '}
                      <span className="text-emerald-700 font-bold">{t.totalResolvedIncidents}</span> res
                    </td>
                    <td className="py-3 px-4">
                      {t.available ? (
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          AVAILABLE
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 text-slate-500 border border-slate-200">
                          ON_LEAVE
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => openTechModal(t)}
                        className="inline-flex items-center space-x-1 px-2.5 py-1 bg-white hover:bg-slate-50 border border-slate-200 rounded-lg text-xs font-mono text-slate-700 hover:text-slate-900 font-medium transition-colors shadow-xs"
                      >
                        <Edit className="h-3 w-3" />
                        <span>CONFIG</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Provision New User / Technician Modal */}
      {showAddUserModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 border border-slate-200 shadow-2xl relative">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <h3 className="text-sm font-mono font-bold text-slate-900 flex items-center space-x-2">
                <UserPlus className="w-4 h-4 text-slate-700" />
                <span>PROVISION_NEW_{newUserRole}</span>
              </h3>
              <button onClick={() => setShowAddUserModal(false)} className="text-slate-400 hover:text-slate-600">
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleCreateUser} className="mt-4 space-y-3.5 text-xs font-mono">
              <div>
                <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                  ORGANIZATIONAL ROLE
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {['TECHNICIAN', 'EMPLOYEE', 'ADMIN'].map((r) => (
                    <button
                      key={r}
                      type="button"
                      onClick={() => setNewUserRole(r)}
                      className={`py-1.5 px-2 rounded-lg text-[10px] font-bold border transition-all ${
                        newUserRole === r
                          ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                          : 'bg-slate-50 text-slate-600 border-slate-200 hover:text-slate-900 hover:bg-slate-100'
                      }`}
                    >
                      {r}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[10px] uppercase text-slate-600 font-bold mb-1">FULL LEGAL NAME *</label>
                <input
                  type="text"
                  required
                  value={newUserFullName}
                  onChange={(e) => setNewUserFullName(e.target.value)}
                  placeholder="e.g. Maya Lin"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 font-sans"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] uppercase text-slate-600 font-bold mb-1">OPERATOR HANDLE *</label>
                  <input
                    type="text"
                    required
                    value={newUserUsername}
                    onChange={(e) => setNewUserUsername(e.target.value)}
                    placeholder="tech_maya"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                  />
                </div>
                <div>
                  <label className="block text-[10px] uppercase text-slate-600 font-bold mb-1">CORPORATE EMAIL *</label>
                  <input
                    type="email"
                    required
                    value={newUserEmail}
                    onChange={(e) => setNewUserEmail(e.target.value)}
                    placeholder="maya@itsm.corp"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] uppercase text-slate-600 font-bold mb-1">INITIAL PASSWORD *</label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    value={newUserPassword}
                    onChange={(e) => setNewUserPassword(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200"
                  />
                </div>
                <div>
                  <label className="block text-[10px] uppercase text-slate-600 font-bold mb-1">DEPARTMENT / UNIT</label>
                  <input
                    type="text"
                    value={newUserDept}
                    onChange={(e) => setNewUserDept(e.target.value)}
                    placeholder="IT Support / Cloud Ops"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 font-sans"
                  />
                </div>
              </div>

              {newUserRole === 'TECHNICIAN' && (
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
                  <div>
                    <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                      PRIMARY SKILLS (Comma-separated)
                    </label>
                    <input
                      type="text"
                      value={newUserSkills}
                      onChange={(e) => setNewUserSkills(e.target.value)}
                      placeholder="e.g. NETWORK, CLOUD, DATABASE, SECURITY"
                      className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:border-slate-900"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                      SPECIALIZATION DESCRIPTION
                    </label>
                    <input
                      type="text"
                      value={newUserSpecialization}
                      onChange={(e) => setNewUserSpecialization(e.target.value)}
                      placeholder="e.g. Cloud Infrastructure & Security Specialist"
                      className="w-full px-3 py-1.5 bg-white border border-slate-300 rounded-lg text-slate-900 focus:outline-none focus:border-slate-900 font-sans"
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end space-x-2 pt-3 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setShowAddUserModal(false)}
                  className="px-3.5 py-1.5 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 font-medium"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-slate-900 hover:bg-slate-800 font-semibold shadow-xs text-white rounded-xl"
                >
                  PROVISION {newUserRole}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Technician Modal */}
      {editingTech && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 border border-slate-200 shadow-2xl relative">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <h3 className="text-sm font-mono font-bold text-slate-900 flex items-center space-x-2">
                <Wrench className="w-4 h-4 text-slate-700" />
                <span>CONFIGURE_PROFILE: {editingTech.fullName}</span>
              </h3>
              <button onClick={() => setEditingTech(null)} className="text-slate-400 hover:text-slate-600">
                <X className="h-4 w-4" />
              </button>
            </div>

            <form onSubmit={handleSaveTech} className="mt-4 space-y-4 text-xs font-mono">
              <div>
                <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                  SKILLS_MATRIX (Comma-separated uppercase categories)
                </label>
                <input
                  type="text"
                  value={editSkills}
                  onChange={(e) => setEditSkills(e.target.value)}
                  placeholder="e.g. NETWORK, DATABASE, APPLICATION, SECURITY"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900"
                />
              </div>

              <div>
                <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                  SPECIALIZATION_PROFILE
                </label>
                <input
                  type="text"
                  value={editSpecialization}
                  onChange={(e) => setEditSpecialization(e.target.value)}
                  placeholder="e.g. Senior Network and Gateway Engineer"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                    MAX_WORKLOAD_CAP
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="50"
                    value={editMaxActive}
                    onChange={(e) => setEditMaxActive(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900"
                  />
                </div>
                <div>
                  <label className="block text-[10px] uppercase text-slate-700 font-bold mb-1">
                    AVAILABILITY_STATE
                  </label>
                  <select
                    value={editAvailable ? 'true' : 'false'}
                    onChange={(e) => setEditAvailable(e.target.value === 'true')}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900"
                  >
                    <option value="true">AVAILABLE_FOR_DISPATCH</option>
                    <option value="false">ON_LEAVE / UNAVAILABLE</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end space-x-2 pt-4 border-t border-slate-200">
                <button
                  type="button"
                  onClick={() => setEditingTech(null)}
                  className="px-3.5 py-1.5 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 font-medium"
                >
                  CANCEL
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 bg-slate-900 hover:bg-slate-800 font-semibold shadow-xs text-white rounded-xl"
                >
                  COMMIT_CHANGES
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TAB 3: IT SERVICES */}
      {activeTab === 'services' && (
        <div className="space-y-6">
          <form onSubmit={handleCreateService} className="hud-panel p-5 rounded-2xl space-y-4 font-mono text-xs bg-white border border-slate-200 shadow-xs">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center space-x-1.5 pb-2 border-b border-slate-200">
              <Plus className="h-4 w-4 text-slate-700" />
              <span>PROVISION_NEW_IT_SERVICE</span>
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">SERVICE_NAME *</label>
                <input
                  type="text"
                  value={newServiceName}
                  onChange={(e) => setNewServiceName(e.target.value)}
                  required
                  placeholder="e.g. Identity Auth SSO"
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
                />
              </div>
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">CATEGORY</label>
                <input
                  type="text"
                  value={newServiceCat}
                  onChange={(e) => setNewServiceCat(e.target.value)}
                  placeholder="Core Infrastructure"
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
                />
              </div>
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">CRITICALITY (1-5)</label>
                <select
                  value={newServiceCrit}
                  onChange={(e) => setNewServiceCrit(e.target.value)}
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900"
                >
                  <option value={5}>5 - MISSION_CRITICAL</option>
                  <option value={4}>4 - HIGH_IMPACT</option>
                  <option value={3}>3 - MODERATE</option>
                  <option value={2}>2 - LOW</option>
                  <option value={1}>1 - INFORMATIONAL</option>
                </select>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">DESCRIPTION</label>
                <input
                  type="text"
                  value={newServiceDesc}
                  onChange={(e) => setNewServiceDesc(e.target.value)}
                  placeholder="Enterprise service scope..."
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
                />
              </div>
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">SUPPORT_CONTACT_EMAIL</label>
                <input
                  type="email"
                  value={newServiceContact}
                  onChange={(e) => setNewServiceContact(e.target.value)}
                  placeholder="infra-lead@corp.net"
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900"
                />
              </div>
            </div>
            <button
              type="submit"
              className="px-4 py-2 bg-slate-900 hover:bg-slate-800 font-semibold text-white text-xs font-mono rounded-xl shadow-xs transition-colors"
            >
              REGISTER_SERVICE
            </button>
          </form>

          <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
                <tr>
                  <th className="py-3 px-4">SERVICE_NAME</th>
                  <th className="py-3 px-4">DOMAIN</th>
                  <th className="py-3 px-4">CRITICALITY</th>
                  <th className="py-3 px-4">SUPPORT_CONTACT</th>
                  <th className="py-3 px-4">STATE</th>
                  <th className="py-3 px-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {services.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-semibold text-slate-900 font-sans">{s.name}</td>
                    <td className="py-3 px-4 text-slate-600 font-sans">{s.category}</td>
                    <td className="py-3 px-4 font-bold text-slate-900">{s.businessCriticality} / 5</td>
                    <td className="py-3 px-4 text-slate-500">{s.supportContact || '—'}</td>
                    <td className="py-3 px-4">
                      {s.active ? (
                        <span className="text-emerald-700 text-[11px] font-medium flex items-center space-x-1.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          <span>ACTIVE</span>
                        </span>
                      ) : (
                        <span className="text-slate-400 text-[11px]">DEACTIVATED</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {s.active && (
                        <button
                          onClick={() => handleDeactivateService(s.id)}
                          className="text-rose-600 hover:text-rose-800 text-xs font-mono font-medium transition-colors"
                        >
                          [DEACTIVATE]
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 4: SLA POLICIES & VIOLATIONS */}
      {activeTab === 'sla' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="hud-panel p-5 rounded-2xl space-y-4 font-mono bg-white border border-slate-200 shadow-xs">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 pb-2 border-b border-slate-200">
                ACTIVE_SLA_POLICY_DEFINITIONS
              </h3>
              <div className="divide-y divide-slate-100">
                {policies.map((p) => (
                  <div key={p.id} className="py-3 flex items-center justify-between text-xs">
                    <div>
                      <span className="font-bold text-slate-900">{p.priority}</span>
                      <p className="text-[11px] text-slate-500 mt-0.5 font-sans">{p.description}</p>
                    </div>
                    <div className="text-right text-xs">
                      <div>RESP: <strong className="text-slate-900 font-bold">{p.responseTimeMinutes}m</strong></div>
                      <div>RESOL: <strong className="text-slate-900 font-bold">{p.resolutionTimeMinutes}m</strong></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="hud-panel p-5 rounded-2xl border border-rose-200 bg-rose-50/30 space-y-3 font-mono shadow-xs">
              <div className="flex items-center space-x-2 text-rose-800 font-bold text-xs uppercase tracking-wider pb-2 border-b border-rose-200">
                <ShieldAlert className="h-4 w-4 text-rose-600" />
                <span>SLA_VIOLATION_MONITOR ({violations.length})</span>
              </div>
              <p className="text-[11px] text-slate-600">
                Incidents exceeding target resolution SLA parameters requiring immediate intervention.
              </p>

              <div className="space-y-2 max-h-60 overflow-y-auto">
                {violations.length === 0 ? (
                  <div className="p-4 text-center text-xs text-emerald-800 font-mono bg-emerald-50 border border-emerald-200 rounded-xl font-semibold">
                    ✓ 100% SLA COMPLIANCE: ZERO ACTIVE BREACHES DETECTED
                  </div>
                ) : (
                  violations.map((v) => (
                    <div key={v.id} className="p-3 bg-white rounded-xl border border-rose-200 text-xs shadow-xs">
                      <div className="flex justify-between font-bold text-rose-800">
                        <span>[{v.incidentNumber}] {v.title}</span>
                        <span>{v.priority}</span>
                      </div>
                      <div className="text-[11px] text-slate-600 mt-1 font-mono">
                        ENGINEER: {v.assignedTechnicianName || '[UNASSIGNED]'} | TARGET: {new Date(v.resolutionDeadline).toLocaleTimeString()}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 5: CONFIGURE INCIDENT CATEGORIES */}
      {activeTab === 'categories' && (
        <div className="space-y-6">
          <form onSubmit={handleCreateCategory} className="hud-panel p-5 rounded-2xl space-y-4 font-mono text-xs bg-white border border-slate-200 shadow-xs">
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-900 flex items-center space-x-1.5 pb-2 border-b border-slate-200">
                <Plus className="h-4 w-4 text-slate-700" />
                <span>EXTENSIBLE_CATEGORY_ENGINE // RUNTIME_PROVISIONING</span>
              </h3>
              <p className="text-[11px] text-slate-500 mt-1">
                Dynamically register new incident domains at runtime without recompilation or redeployment.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">
                  CATEGORY_IDENTIFIER *
                </label>
                <input
                  type="text"
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  required
                  placeholder="e.g. CYBER_FORENSICS"
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 uppercase focus:bg-white focus:outline-none focus:border-slate-900"
                />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-[10px] text-slate-600 uppercase font-bold mb-1">
                  DESCRIPTION
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newCatDesc}
                    onChange={(e) => setNewCatDesc(e.target.value)}
                    placeholder="Domain operational scope..."
                    className="flex-1 px-3 py-1.5 bg-slate-50 border border-slate-300 rounded-xl text-slate-900 focus:bg-white focus:outline-none focus:border-slate-900 font-sans"
                  />
                  <button
                    type="submit"
                    className="px-4 py-1.5 bg-slate-900 hover:bg-slate-800 font-semibold text-white text-xs font-mono rounded-xl shadow-xs transition-colors"
                  >
                    REGISTER_CATEGORY
                  </button>
                </div>
              </div>
            </div>
          </form>

          <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
            <table className="w-full text-left text-xs font-mono">
              <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
                <tr>
                  <th className="py-3 px-4">CATEGORY_IDENTIFIER</th>
                  <th className="py-3 px-4">DESCRIPTION</th>
                  <th className="py-3 px-4">INDEXED_DATE</th>
                  <th className="py-3 px-4">STATUS</th>
                  <th className="py-3 px-4 text-right">ACTION</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {categories.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-4 font-bold text-slate-900 font-mono">{c.name}</td>
                    <td className="py-3 px-4 text-slate-600 font-sans">{c.description || 'Standard category'}</td>
                    <td className="py-3 px-4 text-slate-500">
                      {c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'System default'}
                    </td>
                    <td className="py-3 px-4">
                      {c.active ? (
                        <span className="text-emerald-700 text-[11px] font-medium flex items-center space-x-1.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                          <span>ACTIVE</span>
                        </span>
                      ) : (
                        <span className="text-slate-400 text-[11px]">DEACTIVATED</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {c.active && (
                        <button
                          onClick={() => handleDeactivateCategory(c.id)}
                          className="text-rose-600 hover:text-rose-800 text-xs font-mono font-medium transition-colors"
                        >
                          [DEACTIVATE]
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 6: CONFIGURE ASSIGNMENT STRATEGIES */}
      {activeTab === 'strategies' && (
        <div className="space-y-6">
          <div className="hud-panel p-6 rounded-2xl bg-white border border-slate-200 shadow-xs">
            <div className="flex items-center space-x-3 pb-4 border-b border-slate-200">
              <div className="h-10 w-10 rounded-xl bg-slate-100 border border-slate-200 text-slate-800 flex items-center justify-center">
                <GitBranch className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-mono font-bold text-slate-900">
                  DYNAMIC_ASSIGNMENT_STRATEGY_ENGINE (STRATEGY_PATTERN)
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Hot-swap assignment heuristics in real-time. Polymorphic Strategy classes calculate optimal dispatching.
                </p>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4 font-mono">
              {strategies.map((strat) => (
                <div
                  key={strat.key}
                  className={`p-5 rounded-2xl border transition-all ${
                    strat.isActive
                      ? 'border-slate-900 bg-slate-50/90 shadow-sm ring-1 ring-slate-900'
                      : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] uppercase font-bold text-slate-500">{strat.key}</span>
                    {strat.isActive && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-900 text-white flex items-center space-x-1 shadow-xs">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                        <span>ACTIVE</span>
                      </span>
                    )}
                  </div>

                  <h4 className="text-sm font-bold text-slate-900 mt-2 font-heading">{strat.name}</h4>
                  <p className="text-xs text-slate-500 mt-2 leading-relaxed min-h-[50px] font-sans">
                    {strat.description}
                  </p>

                  <div className="mt-4 pt-3 border-t border-slate-200 flex items-center justify-between">
                    {strat.isActive ? (
                      <span className="text-xs text-emerald-700 font-semibold flex items-center space-x-1">
                        <CheckCircle className="h-4 w-4 text-emerald-600" />
                        <span>CURRENTLY_ENFORCED</span>
                      </span>
                    ) : (
                      <button
                        onClick={() => handleSetStrategy(strat.key)}
                        className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-mono font-semibold rounded-xl shadow-xs transition-colors"
                      >
                        ENGAGE_STRATEGY
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 7: AUDIT LOGS */}
      {activeTab === 'audit' && (
        <div className="hud-panel rounded-2xl overflow-hidden bg-white border border-slate-200 shadow-xs">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between font-mono">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-900">
              SECURITY_&_OPERATIONAL_AUDIT_TRAIL (PAST_7_DAYS)
            </span>
            <span className="text-xs text-slate-700 font-bold">{auditLogs.length} EVENTS RECORDED</span>
          </div>

          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-50 text-slate-600 uppercase border-b border-slate-200 text-[10px] tracking-wider">
              <tr>
                <th className="py-3 px-4">TIMESTAMP</th>
                <th className="py-3 px-4">INCIDENT_REF</th>
                <th className="py-3 px-4">ACTION_TYPE</th>
                <th className="py-3 px-4">PERFORMED_BY</th>
                <th className="py-3 px-4">EVENT_DESCRIPTION</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLogs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="py-2.5 px-4 text-slate-500">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                  <td className="py-2.5 px-4 font-bold text-slate-900">
                    {log.incidentNumber || '—'}
                  </td>
                  <td className="py-2.5 px-4 font-bold text-slate-800">{log.action}</td>
                  <td className="py-2.5 px-4 text-slate-600 font-sans">{log.performedByName}</td>
                  <td className="py-2.5 px-4 text-slate-600 font-sans text-xs">{log.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
