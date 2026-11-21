import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, ErrorNote, Field, Input } from '../components/ui';
import { ROLE } from '../lib/format';

const DEMO = [
  { username: 'admin', password: 'admin123', name: 'System Administrator', role: 'ADMIN' },
  { username: 'tech_app', password: 'tech123', name: 'Marcus Taylor · applications', role: 'TECHNICIAN' },
  { username: 'tech_network', password: 'tech123', name: 'Alex Chen · network', role: 'TECHNICIAN' },
  { username: 'tech_db', password: 'tech123', name: 'Priya Sharma · database', role: 'TECHNICIAN' },
  { username: 'emp_john', password: 'emp123', name: 'John Doe · finance', role: 'EMPLOYEE' },
  { username: 'emp_sarah', password: 'emp123', name: 'Sarah Connor · HR', role: 'EMPLOYEE' },
];

export default function Login() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const submit = async (username, password) => {
    setBusy(true);
    setError('');
    const res = await login(username, password);
    setBusy(false);
    if (res.success) navigate('/');
    else setError(res.message);
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-[1fr_520px]">
      <section className="hidden flex-col justify-between bg-ink p-12 text-paper lg:flex">
        <div className="flex items-center gap-3">
          <img src="/favicon.svg" alt="" className="h-9 w-9 rounded-lg ring-1 ring-white/20" />
          <span className="text-lg font-bold">I-ITSM</span>
        </div>
        <div className="max-w-lg">
          <h1 className="text-4xl font-bold leading-tight">Every incident, from report to resolution, on the clock.</h1>
          <p className="mt-5 text-lg text-paper/70">
            Report a problem in plain words. The desk classifies it, sets its priority and SLA, finds the right technician,
            and keeps everyone updated until it is closed.
          </p>
        </div>
        <p className="text-sm text-paper/50">CS F213 Object-Oriented Programming project · BITS Pilani, Hyderabad Campus</p>
      </section>

      <section className="flex items-center justify-center bg-surface px-6 py-12">
        <div className="w-full max-w-sm">
          <h2 className="text-2xl font-bold">Sign in</h2>
          <p className="mt-1 text-steel">Use your service desk account.</p>

          <div className="mt-6 space-y-4">
            {params.get('expired') && !error && (
              <div className="rounded-lg bg-warn-soft px-4 py-3 text-sm text-warn">Your session expired. Please sign in again.</div>
            )}
            <ErrorNote>{error}</ErrorNote>
            <form
              className="space-y-4"
              onSubmit={(e) => {
                e.preventDefault();
                submit(form.username, form.password);
              }}
            >
              <Field label="Username" htmlFor="u">
                <Input id="u" autoComplete="username" required value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
              </Field>
              <Field label="Password" htmlFor="p">
                <Input id="p" type="password" autoComplete="current-password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </Field>
              <Button type="submit" className="w-full" loading={busy}>
                Sign in
              </Button>
            </form>
          </div>

          <p className="mt-4 text-sm text-steel">
            New employee?{' '}
            <Link to="/register" className="font-semibold text-p3 hover:underline">
              Create an account
            </Link>
          </p>

          <div className="mt-10 border-t border-line-soft pt-6">
            <p className="text-sm font-semibold">Demo accounts</p>
            <p className="mb-3 text-[13px] text-muted">Pick one to see that role's workspace.</p>
            <ul className="divide-y divide-line-soft rounded-lg border border-line">
              {DEMO.map((d) => (
                <li key={d.username}>
                  <button
                    onClick={() => submit(d.username, d.password)}
                    disabled={busy}
                    className="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left hover:bg-sunken"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-semibold">{d.name}</span>
                      <span className="text-xs text-muted">{d.username}</span>
                    </span>
                    <span className="shrink-0 text-xs font-semibold" style={{ color: ROLE[d.role].color }}>
                      {ROLE[d.role].label}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
