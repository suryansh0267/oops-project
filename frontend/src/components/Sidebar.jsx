import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  AlertOctagon,
  PlusCircle,
  CheckSquare,
  Shield,
  Sliders,
  Cpu,
  Layers
} from 'lucide-react';

export default function Sidebar() {
  const { user, isAdmin, isTechnician } = useAuth();

  const links = [
    {
      to: '/dashboard',
      label: 'Telemetry Overview',
      code: 'HUD_01',
      icon: LayoutDashboard,
      show: true,
    },
    {
      to: '/incidents',
      label: 'Incident Directory',
      code: 'INC_DIR',
      icon: AlertOctagon,
      show: true,
    },
    {
      to: '/report',
      label: 'Dispatch Incident',
      code: 'NEW_TKT',
      icon: PlusCircle,
      show: true,
    },
    {
      to: '/my-queue',
      label: 'Assigned Work Queue',
      code: 'TECH_Q',
      icon: CheckSquare,
      show: isTechnician,
    },
    {
      to: '/admin/management',
      label: 'ITSM Admin Center',
      code: 'SYS_ADMIN',
      icon: Sliders,
      show: isAdmin,
    },
  ];

  return (
    <aside className="w-64 bg-white border-r border-slate-200/90 hidden md:flex flex-col justify-between p-4 min-h-[calc(100vh-4rem)] shadow-xs">
      <div className="space-y-1.5">
        <div className="px-3 py-2 text-[10px] font-mono font-bold uppercase tracking-widest text-slate-400 flex items-center justify-between">
          <span>OPERATIONAL MATRIX</span>
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
        </div>

        {links
          .filter((l) => l.show)
          .map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all group ${
                    isActive
                      ? 'bg-slate-900 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50 border border-transparent'
                  }`
                }
              >
                <div className="flex items-center space-x-3">
                  <Icon className="h-4 w-4 transition-transform group-hover:scale-110" />
                  <span className="font-heading">{item.label}</span>
                </div>
                <span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200 group-hover:border-slate-300">
                  {item.code}
                </span>
              </NavLink>
            );
          })}
      </div>

      {/* OOP Architecture Callout Card */}
      <div className="bg-slate-50 border border-slate-200 rounded-xl p-3.5 text-xs">
        <div className="flex items-center space-x-2 text-slate-900 font-heading font-bold text-xs uppercase mb-2">
          <Cpu className="h-3.5 w-3.5 text-slate-700" />
          <span>OOP Core Architecture</span>
        </div>
        <div className="space-y-1.5 text-[11px] font-mono text-slate-600">
          <div className="flex items-center justify-between">
            <span>• Strategy Engine</span>
            <span className="text-slate-900 font-bold">Active</span>
          </div>
          <div className="flex items-center justify-between">
            <span>• State Lifecycle</span>
            <span className="text-slate-800 font-bold">Enforced</span>
          </div>
          <div className="flex items-center justify-between">
            <span>• Multi-Observer</span>
            <span className="text-amber-600 font-bold">Broadcasting</span>
          </div>
          <div className="flex items-center justify-between">
            <span>• JPA Repository</span>
            <span className="text-emerald-600 font-bold">ACID PG18</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
