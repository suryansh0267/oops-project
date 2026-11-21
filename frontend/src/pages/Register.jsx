import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Button, ErrorNote, Field, Input } from '../components/ui';

export default function Register() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', username: '', email: '', password: '', department: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    setBusy(true);
    setError('');
    const res = await register(form);
    setBusy(false);
    if (res.success) navigate('/');
    else setError(res.message);
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-lg rounded-xl border border-line bg-surface p-8">
        <Link to="/login" className="text-sm text-steel hover:text-ink">
          ← Back to sign in
        </Link>
        <h1 className="mt-3 text-2xl font-bold">Create an employee account</h1>
        <p className="mt-1 text-steel">
          You'll be able to report incidents and follow them to resolution. Technician and admin accounts are created by an administrator.
        </p>

        <form onSubmit={submit} className="mt-6 space-y-4">
          <ErrorNote>{error}</ErrorNote>
          <Field label="Full name" htmlFor="fn">
            <Input id="fn" required value={form.fullName} onChange={set('fullName')} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Username" htmlFor="un" hint="Letters, numbers, . _ -">
              <Input id="un" required minLength={3} maxLength={30} pattern="[a-zA-Z0-9._\-]+" value={form.username} onChange={set('username')} />
            </Field>
            <Field label="Work email" htmlFor="em">
              <Input id="em" type="email" required value={form.email} onChange={set('email')} />
            </Field>
          </div>
          <Field label="Password" htmlFor="pw" hint="At least 8 characters">
            <Input id="pw" type="password" required minLength={8} autoComplete="new-password" value={form.password} onChange={set('password')} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Department" htmlFor="dp" optional>
              <Input id="dp" value={form.department} onChange={set('department')} />
            </Field>
            <Field label="Phone" htmlFor="ph" optional hint="For urgent SMS updates">
              <Input id="ph" type="tel" value={form.phoneNumber} onChange={set('phoneNumber')} />
            </Field>
          </div>
          <Button type="submit" className="w-full" loading={busy}>
            Create account
          </Button>
        </form>
      </div>
    </div>
  );
}
