import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Bell, LogOut, Gauge, ListChecks, PlusCircle, Inbox, Users, Wrench, Server, Timer, Tags,
  Shuffle, ScrollText, Mail, Menu, X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { ROLE, timeAgo } from '../lib/format';
import ChatWidget from './ChatWidget';

function navFor(role) {
  const home = { to: '/', label: role === 'EMPLOYEE' ? 'Home' : 'Overview', icon: Gauge, end: true };
  if (role === 'EMPLOYEE') {
    return [
      { items: [home, { to: '/report', label: 'Report an incident', icon: PlusCircle }, { to: '/incidents', label: 'My incidents', icon: ListChecks }] },
    ];
  }
  if (role === 'TECHNICIAN') {
    return [
      { items: [home, { to: '/queue', label: 'My queue', icon: Inbox }, { to: '/incidents', label: 'All incidents', icon: ListChecks }, { to: '/report', label: 'Log an incident', icon: PlusCircle }] },
    ];
  }
  return [
    { items: [home, { to: '/incidents', label: 'Incidents', icon: ListChecks }, { to: '/report', label: 'Log an incident', icon: PlusCircle }] },
    {
      heading: 'Manage',
      items: [
        { to: '/admin/users', label: 'Users', icon: Users },
        { to: '/admin/technicians', label: 'Technicians', icon: Wrench },
        { to: '/admin/services', label: 'IT services', icon: Server },
        { to: '/admin/categories', label: 'Categories', icon: Tags },
        { to: '/admin/sla', label: 'SLA policies', icon: Timer },
        { to: '/admin/automation', label: 'Automation', icon: Shuffle },
      ],
    },
    {
      heading: 'Records',
      items: [
        { to: '/admin/audit', label: 'Audit log', icon: ScrollText },
        { to: '/admin/outbox', label: 'Email & SMS outbox', icon: Mail },
      ],
    },
  ];
}

function Notifications() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const ref = useRef(null);
  const navigate = useNavigate();

  const load = async () => {
    try {
      const [list, count] = await Promise.all([api.get('/notifications'), api.get('/notifications/unread/count')]);
      setItems(list.data);
      setUnread(count.data);
    } catch {
      /* the bell is non-critical; stay quiet */
    }
  };

  useEffect(() => {
    load();
    const id = setInterval(load, 15000);
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false);
    document.addEventListener('mousedown', close);
    return () => {
      clearInterval(id);
      document.removeEventListener('mousedown', close);
    };
  }, []);

  const openItem = async (n) => {
    setOpen(false);
    if (!n.read) {
      api.put(`/notifications/${n.id}/read`).then(load).catch(() => {});
    }
    if (n.incidentId) navigate(`/incidents/${n.incidentId}`);
  };

  const readAll = async () => {
    await api.put('/notifications/read-all').catch(() => {});
    load();
  };

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="relative rounded-lg p-2 text-steel hover:bg-sunken hover:text-ink"
        aria-label={`Notifications, ${unread} unread`}
      >
        <Bell className="h-5 w-5" />
        {unread > 0 && (
          <span className="tnum absolute right-0.5 top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-bad px-1 text-[10px] font-bold text-white">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-50 mt-2 w-[22rem] overflow-hidden rounded-xl border border-line bg-surface shadow-lg">
          <div className="flex items-center justify-between border-b border-line-soft px-4 py-2.5">
            <span className="text-sm font-semibold">Notifications</span>
            {unread > 0 && (
              <button onClick={readAll} className="text-[13px] font-semibold text-p3 hover:underline">
                Mark all as read
              </button>
            )}
          </div>
          <ul className="max-h-96 overflow-y-auto">
            {items.length === 0 && <li className="px-4 py-8 text-center text-sm text-muted">You're all caught up.</li>}
            {items.slice(0, 15).map((n) => (
              <li key={n.id}>
                <button onClick={() => openItem(n)} className={`block w-full px-4 py-3 text-left hover:bg-sunken ${n.read ? '' : 'bg-p3-soft/50'}`}>
                  <div className="flex items-start gap-2">
                    {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-p3" />}
                    <div className="min-w-0">
                      <p className="text-sm font-semibold">{n.title}</p>
                      <p className="mt-0.5 line-clamp-2 text-[13px] text-steel">{n.message}</p>
                      <p className="mt-1 text-xs text-muted">{timeAgo(n.createdAt)}</p>
                    </div>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default function Layout() {
  const { user, role, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const groups = navFor(role);
  const tint = ROLE[role]?.color || 'var(--color-ink)';

  useEffect(() => setMenuOpen(false), [location.pathname]);

  const nav = (
    <nav className="flex flex-col gap-5" aria-label="Main">
      {groups.map((g, i) => (
        <div key={i}>
          {g.heading && <p className="mb-1.5 px-3 text-xs font-semibold text-muted">{g.heading}</p>}
          <ul className="space-y-0.5">
            {g.items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-lg px-3 py-2 text-[14px] font-medium transition-colors ${
                      isActive ? 'bg-ink text-white' : 'text-steel hover:bg-sunken hover:text-ink'
                    }`
                  }
                >
                  <item.icon className="h-4 w-4 shrink-0" aria-hidden />
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );

  return (
    <div className="min-h-screen">
      {/* Role band: each user type gets its own colour across the top edge */}
      <div className="h-1" style={{ background: tint }} />
      <header className="sticky top-0 z-40 border-b border-line bg-surface/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1400px] items-center justify-between gap-4 px-4 lg:px-6">
          <div className="flex items-center gap-3">
            <button className="rounded-lg p-2 text-steel hover:bg-sunken lg:hidden" onClick={() => setMenuOpen((o) => !o)} aria-label="Menu">
              {menuOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
            <NavLink to="/" className="flex items-center gap-2.5">
              <img src="/favicon.svg" alt="" className="h-7 w-7" />
              <span className="font-bold">I-ITSM</span>
              <span className="hidden text-sm text-muted sm:inline">IT service desk</span>
            </NavLink>
          </div>
          <div className="flex items-center gap-2">
            <Notifications />
            <div className="ml-1 hidden text-right sm:block">
              <p className="text-sm font-semibold leading-tight">{user?.fullName}</p>
              <p className="text-xs font-semibold leading-tight" style={{ color: tint }}>
                {ROLE[role]?.label}
              </p>
            </div>
            <button onClick={logout} className="rounded-lg p-2 text-steel hover:bg-sunken hover:text-ink" title="Sign out" aria-label="Sign out">
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-[1400px]">
        <aside className="sticky top-[60px] hidden h-[calc(100vh-60px)] w-60 shrink-0 overflow-y-auto border-r border-line px-3 py-6 lg:block">{nav}</aside>
        {menuOpen && (
          <div className="fixed inset-0 top-[60px] z-30 bg-paper px-4 py-6 lg:hidden">{nav}</div>
        )}
        <main className="min-w-0 flex-1 px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
      <ChatWidget />
    </div>
  );
}
