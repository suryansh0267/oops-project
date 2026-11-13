import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Shield, Lock, User, Mail, Building, Phone, ArrowRight, Terminal, UserPlus, CheckCircle2 } from 'lucide-react';

export default function Register() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    department: '',
    phoneNumber: '',
    role: 'EMPLOYEE',
  });
  const [error, setError] = useState('');
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const res = await register(formData);
    if (res.success) {
      navigate('/dashboard');
    } else {
      setError(res.message);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] relative flex items-center justify-center p-4">
      {/* Background Subtle Geometric Grid */}
      <div className="absolute inset-0 bg-[radial-gradient(#CBD5E1_1px,transparent_1px)] [background-size:24px_24px] opacity-60 pointer-events-none" />
      <div className="absolute top-1/4 left-1/3 w-96 h-96 bg-blue-100/50 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/3 w-96 h-96 bg-slate-200/50 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-lg w-full relative z-10">
        {/* Terminal Header Bar */}
        <div className="flex items-center justify-between px-4 py-2.5 bg-slate-900 border border-slate-900 border-b-0 rounded-t-xl text-[11px] font-mono text-slate-300">
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-rose-500 inline-block"></span>
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500 inline-block"></span>
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 inline-block"></span>
            <span className="ml-2 text-slate-300 font-bold">I-ITSM // IDENTITY_PROVISIONING_v4</span>
          </div>
          <span className="text-slate-400 tracking-widest text-[10px]">AUTH_MODE: NEW_IDENTITY</span>
        </div>

        {/* Card Body */}
        <div className="bg-white rounded-b-xl rounded-t-none p-8 border border-slate-200 shadow-xl shadow-slate-200/60 relative overflow-hidden">
          {/* Top Title Section */}
          <div className="flex items-start justify-between mb-6 pb-4 border-b border-slate-100">
            <div>
              <div className="inline-flex items-center space-x-1.5 px-2.5 py-0.5 rounded bg-slate-100 border border-slate-200 text-[10px] font-mono font-bold text-slate-800 mb-2">
                <Terminal className="w-3 h-3 text-slate-700" />
                <span>ROLE: {formData.role === 'TECHNICIAN' ? 'SUPPORT_ENGINEER_OPERATOR' : 'EMPLOYEE_REPORTER'}</span>
              </div>
              <h1 className="text-xl font-bold text-slate-900 tracking-tight font-heading">
                Provision Identity
              </h1>
              <p className="text-xs text-slate-500 mt-0.5 font-sans">
                Create user credentials for ticket submission and service telemetry tracking.
              </p>
            </div>
            <div className="h-10 w-10 rounded-xl bg-slate-900 flex items-center justify-center text-white shadow-md shadow-slate-200">
              <UserPlus className="h-5 w-5 text-slate-100" />
            </div>
          </div>

          {error && (
            <div className="mb-5 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center space-x-2 font-mono">
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping"></span>
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="flex items-center space-x-2 pb-2">
              <label className="text-[10px] font-mono uppercase tracking-wider text-slate-600 font-bold">
                OPERATIONAL ROLE:
              </label>
              <div className="grid grid-cols-2 gap-2 flex-1">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, role: 'EMPLOYEE' })}
                  className={`py-1.5 px-3 rounded-lg text-[10px] font-mono font-bold border transition-all ${
                    formData.role === 'EMPLOYEE'
                      ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  EMPLOYEE / REPORTER
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, role: 'TECHNICIAN' })}
                  className={`py-1.5 px-3 rounded-lg text-[10px] font-mono font-bold border transition-all ${
                    formData.role === 'TECHNICIAN'
                      ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  TECHNICIAN / OPERATOR
                </button>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                Full Legal Name
              </label>
              <div className="relative">
                <User className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  required
                  placeholder="e.g. Alex Morgan"
                  className="w-full pl-9 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                  Operator Handle
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-2.5 font-mono text-xs text-slate-400">@</span>
                  <input
                    type="text"
                    name="username"
                    value={formData.username}
                    onChange={handleChange}
                    required
                    placeholder="amorgan"
                    className="w-full pl-8 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors font-mono"
                  />
                </div>
              </div>
              <div>
                <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                  Corporate Email
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    placeholder="alex@enterprise.corp"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                Access Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  minLength={6}
                  placeholder="Minimum 6 characters"
                  className="w-full pl-9 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors font-mono"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                  Department
                </label>
                <div className="relative">
                  <Building className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    name="department"
                    value={formData.department}
                    onChange={handleChange}
                    placeholder="Engineering / IT Support"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors"
                  />
                </div>
              </div>
              <div>
                <label className="block text-[11px] font-sans font-semibold text-slate-700 mb-1">
                  Comms Phone
                </label>
                <div className="relative">
                  <Phone className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    name="phoneNumber"
                    value={formData.phoneNumber}
                    onChange={handleChange}
                    placeholder="x4402"
                    className="w-full pl-9 pr-3 py-2 bg-slate-50/70 border border-slate-300 rounded-lg text-xs text-slate-900 placeholder-slate-400 focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-1 focus:ring-slate-200 transition-colors"
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-4 py-2.5 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl text-xs shadow-xs transition-all flex items-center justify-center space-x-2 disabled:opacity-50"
            >
              <span>{loading ? 'INITIALIZING PROVISIONING...' : 'INITIALIZE ACCOUNT & ACCESS CONSOLE'}</span>
              <ArrowRight className="h-4 w-4" />
            </button>
          </form>

          {/* Bottom link to login */}
          <div className="mt-6 pt-4 border-t border-slate-100 text-center text-xs text-slate-500 font-sans">
            <span>Already have active credentials?</span>{' '}
            <Link to="/login" className="text-slate-900 hover:text-slate-700 font-bold">
              [ RETURN TO OPERATOR LOGIN ]
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
