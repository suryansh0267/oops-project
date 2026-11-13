import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';
import { Bell, Shield, LogOut, Radio, Terminal, AlertTriangle, Info, Clock } from 'lucide-react';

export default function Navbar() {
  const { user, logout, isAdmin, isTechnician } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date().toLocaleTimeString());
  const dropdownRef = useRef(null);

  const fetchNotifications = async () => {
    try {
      const [notifsRes, countRes] = await Promise.all([
        api.get('/notifications'),
        api.get('/notifications/unread/count')
      ]);
      setNotifications(notifsRes.data);
      setUnreadCount(countRes.data);
    } catch (err) {
      console.error('Failed to fetch notifications', err);
    }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 15000);
    const clock = setInterval(() => setCurrentTime(new Date().toLocaleTimeString()), 1000);
    return () => {
      clearInterval(interval);
      clearInterval(clock);
    };
  }, []);

  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const markAllRead = async () => {
    try {
      await api.put('/notifications/read-all');
      setUnreadCount(0);
      fetchNotifications();
    } catch (err) {
      console.error(err);
    }
  };

  const markOneRead = async (id) => {
    try {
      await api.put(`/notifications/${id}/read`);
      fetchNotifications();
    } catch (err) {
      console.error(err);
    }
  };

  const roleBadgeStyle = isAdmin
    ? 'bg-purple-50 text-purple-700 border-purple-200'
    : isTechnician
    ? 'bg-slate-100 text-slate-800 border-slate-300'
    : 'bg-emerald-50 text-emerald-700 border-emerald-200';

  return (
    <header className="bg-white/95 backdrop-blur-md border-b border-slate-200/90 shadow-xs sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand / Logo */}
        <div className="flex items-center space-x-3.5">
          <div className="h-10 w-10 rounded-xl bg-slate-900 flex items-center justify-center text-white font-bold shadow-md shadow-slate-200 ring-2 ring-slate-100">
            <Radio className="h-5 w-5 text-slate-100 animate-pulse" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-lg font-black tracking-wider text-slate-900 uppercase font-heading">
                I-ITSM <span className="text-slate-700">OPS</span>
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-700 border border-slate-200 tracking-wider">
                V4.0 • RUNNING
              </span>
            </div>
            <p className="text-[11px] font-mono text-slate-500 hidden sm:flex items-center space-x-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-ping" />
              <span>INCIDENT TELEMETRY & RESOLUTION ENGINE</span>
            </p>
          </div>
        </div>

        {/* Center Live Telemetry Clock */}
        <div className="hidden lg:flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-50 border border-slate-200 text-xs font-mono text-slate-600">
          <Clock className="h-3.5 w-3.5 text-slate-700" />
          <span>SYSTEM TIME: <strong className="text-slate-900 font-bold">{currentTime}</strong></span>
        </div>

        {/* Right Section */}
        <div className="flex items-center space-x-4">
          {/* Notifications Dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setShowDropdown(!showDropdown)}
              className="relative p-2 rounded-xl text-slate-600 hover:text-slate-900 bg-slate-50 hover:bg-slate-100 border border-slate-200 transition-all"
            >
              <Bell className="h-4 w-4" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-rose-600 text-white text-[10px] font-mono font-bold flex items-center justify-center ring-2 ring-white animate-pulse">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </button>

            {showDropdown && (
              <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
                <div className="px-4 py-2.5 border-b border-slate-100 flex items-center justify-between">
                  <div className="flex items-center space-x-1.5">
                    <span className="font-heading font-bold text-xs uppercase tracking-wider text-slate-800">
                      Telemetry Signals
                    </span>
                    <span className="text-[10px] font-mono text-slate-900 font-bold">({unreadCount} new)</span>
                  </div>
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllRead}
                      className="text-[11px] font-mono text-slate-700 hover:text-slate-900 font-bold transition-colors"
                    >
                      Acknowledge All
                    </button>
                  )}
                </div>

                <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                  {notifications.length === 0 ? (
                    <div className="px-4 py-8 text-center text-xs font-mono text-slate-400">
                      NO ACTIVE SIGNALS • FREQUENCY CLEAR
                    </div>
                  ) : (
                    notifications.slice(0, 10).map((n) => (
                      <div
                        key={n.id}
                        onClick={() => markOneRead(n.id)}
                        className={`px-4 py-3 hover:bg-slate-50 cursor-pointer transition-colors ${
                          !n.read ? 'bg-slate-50/80' : ''
                        }`}
                      >
                        <div className="flex items-start space-x-2.5">
                          <div className="mt-0.5">
                            {n.type === 'EMAIL' ? (
                              <Info className="h-4 w-4 text-slate-700" />
                            ) : (
                              <AlertTriangle className="h-4 w-4 text-amber-500" />
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-xs font-bold text-slate-900 truncate">{n.title}</p>
                            <p className="text-[11px] text-slate-600 line-clamp-2 mt-0.5 leading-relaxed">{n.message}</p>
                            <span className="text-[10px] font-mono text-slate-400 mt-1 block">
                              {new Date(n.createdAt).toLocaleTimeString()}
                            </span>
                          </div>
                          {!n.read && (
                            <span className="h-2 w-2 rounded-full bg-slate-900 flex-shrink-0 mt-1.5" />
                          )}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          {/* User Profile */}
          <div className="flex items-center space-x-3 border-l border-slate-200 pl-4">
            <div className="text-right hidden sm:block">
              <div className="text-xs font-bold text-slate-900 uppercase tracking-wide">
                {user?.fullName || user?.username}
              </div>
              <div className="text-[10px] font-mono mt-0.5">
                <span className={`px-2 py-0.5 rounded border uppercase tracking-wider font-semibold ${roleBadgeStyle}`}>
                  {user?.role}
                </span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Terminate Session / Logout"
              className="p-2 rounded-xl text-slate-400 hover:text-rose-600 bg-slate-50 hover:bg-rose-50 border border-slate-200 hover:border-rose-200 transition-colors"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
