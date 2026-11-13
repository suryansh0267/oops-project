import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Radio, Lock, User, ArrowRight, ShieldCheck, Terminal, Cpu } from 'lucide-react';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login, loading } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const res = await login(username, password);
    if (res.success) {
      navigate('/dashboard');
    } else {
      setError(res.message);
    }
  };

  const handleQuickLogin = async (u, p) => {
    setUsername(u);
    setPassword(p);
    setError('');
    const res = await login(u, p);
    if (res.success) {
      navigate('/dashboard');
    } else {
      setError(res.message);
    }
  };

  return (
    <div className="min-h-screen bg-[#F8FAFC] flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Subtle Geometric Grid */}
      <div className="absolute inset-0 bg-[radial-gradient(#CBD5E1_1px,transparent_1px)] [background-size:24px_24px] opacity-60 pointer-events-none" />
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-slate-200/40 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-slate-200/30 rounded-full blur-3xl pointer-events-none" />

      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl shadow-slate-200/60 p-8 border border-slate-200/90 relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex h-14 w-14 rounded-2xl bg-slate-900 items-center justify-center text-white shadow-lg shadow-slate-300 mb-4 ring-4 ring-slate-100">
            <Radio className="h-7 w-7 text-slate-100 animate-pulse" />
          </div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight font-heading">
            Incident Operations Command
          </h1>
          <p className="text-xs text-slate-500 mt-1 font-sans leading-relaxed">
            Unified authentication portal for ITSM telemetry & resolution workflows
          </p>
        </div>

        {error && (
          <div className="mb-5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-mono flex items-center space-x-2">
            <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping"></span>
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 tracking-tight mb-1.5 font-sans">
              Operator Username / Handle
            </label>
            <div className="relative">
              <User className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                placeholder="e.g. admin, tech_network, or emp_john"
                className="w-full pl-10 pr-4 py-2.5 text-sm font-sans bg-slate-50/70 text-slate-900 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-2 focus:ring-slate-200 transition-colors placeholder:text-slate-400"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 tracking-tight mb-1.5 font-sans">
              Security Key / Password
            </label>
            <div className="relative">
              <Lock className="h-4 w-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-2.5 text-sm font-sans bg-slate-50/70 text-slate-900 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:border-slate-900 focus:ring-2 focus:ring-slate-200 transition-colors placeholder:text-slate-400"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-3 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl shadow-xs text-sm font-heading tracking-tight transition-all flex items-center justify-center space-x-2 disabled:opacity-50"
          >
            <span>{loading ? 'Authenticating Identity...' : 'Sign In to Workspace'}</span>
            <ArrowRight className="h-4 w-4" />
          </button>
        </form>

        {/* Demo Fast Login Buttons */}
        <div className="mt-8 pt-6 border-t border-slate-100">
          <p className="text-[10px] font-mono font-bold text-slate-500 uppercase tracking-widest text-center mb-3">
            ONE-CLICK ACADEMIC EVALUATION PROFILES
          </p>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => handleQuickLogin('admin', 'admin123')}
              className="py-2.5 px-2 bg-slate-50 hover:bg-slate-100 text-slate-900 rounded-xl text-xs font-heading font-semibold border border-slate-200 transition-all text-center hover:border-slate-300 shadow-xs"
            >
              Commander<br/><span className="text-[10px] text-slate-500 font-sans font-normal">admin</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickLogin('tech_network', 'tech123')}
              className="py-2.5 px-2 bg-slate-50 hover:bg-slate-100 text-slate-900 rounded-xl text-xs font-heading font-semibold border border-slate-200 transition-all text-center hover:border-slate-300 shadow-xs"
            >
              Network Tech<br/><span className="text-[10px] text-slate-600 font-sans font-normal">Alex Chen</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickLogin('emp_john', 'emp123')}
              className="py-2.5 px-2 bg-slate-50 hover:bg-slate-100 text-slate-900 rounded-xl text-xs font-heading font-semibold border border-slate-200 transition-all text-center hover:border-slate-300 shadow-xs"
            >
              Reporter<br/><span className="text-[10px] text-slate-600 font-sans font-normal">emp_john</span>
            </button>
          </div>

          <div className="grid grid-cols-2 gap-2 mt-2">
            <button
              type="button"
              onClick={() => handleQuickLogin('tech_db', 'tech123')}
              className="py-2 px-2 bg-slate-50 hover:bg-slate-100 text-slate-900 rounded-xl text-xs font-heading font-semibold border border-slate-200 transition-all text-center hover:border-slate-300 shadow-xs"
            >
              Database Tech<br/><span className="text-[10px] text-slate-600 font-sans font-normal">Priya Sharma (tech_db)</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickLogin('tech_app', 'tech123')}
              className="py-2 px-2 bg-slate-50 hover:bg-slate-100 text-slate-900 rounded-xl text-xs font-heading font-semibold border border-slate-200 transition-all text-center hover:border-slate-300 shadow-xs"
            >
              App & Auth Tech<br/><span className="text-[10px] text-slate-600 font-sans font-normal">Marcus Taylor (tech_app)</span>
            </button>
          </div>
        </div>

        <p className="text-center text-xs font-sans text-slate-500 mt-6">
          New employee node?{' '}
          <Link to="/register" className="text-slate-900 hover:text-slate-700 font-semibold underline underline-offset-4 font-sans">
            Register Operator
          </Link>
        </p>
      </div>
    </div>
  );
}
