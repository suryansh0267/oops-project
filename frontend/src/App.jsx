import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import IncidentList from './pages/IncidentList';
import IncidentDetail from './pages/IncidentDetail';
import ReportIncident from './pages/ReportIncident';
import MyQueue from './pages/MyQueue';
import AdminManagement from './pages/AdminManagement';

const ProtectedLayout = ({ children }) => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen bg-[#F8FAFC] text-slate-900 flex flex-col">
      <Navbar />
      <div className="flex-1 flex max-w-7xl w-full mx-auto">
        <Sidebar />
        <main className="flex-1 p-4 sm:p-6 lg:p-8 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route
            path="/dashboard"
            element={
              <ProtectedLayout>
                <Dashboard />
              </ProtectedLayout>
            }
          />

          <Route
            path="/incidents"
            element={
              <ProtectedLayout>
                <IncidentList />
              </ProtectedLayout>
            }
          />

          <Route
            path="/incidents/:id"
            element={
              <ProtectedLayout>
                <IncidentDetail />
              </ProtectedLayout>
            }
          />

          <Route
            path="/report"
            element={
              <ProtectedLayout>
                <ReportIncident />
              </ProtectedLayout>
            }
          />

          <Route
            path="/my-queue"
            element={
              <ProtectedLayout>
                <MyQueue />
              </ProtectedLayout>
            }
          />

          <Route
            path="/admin/management"
            element={
              <ProtectedLayout>
                <AdminManagement />
              </ProtectedLayout>
            }
          />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
