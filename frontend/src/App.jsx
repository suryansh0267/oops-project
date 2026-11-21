import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import { lazy, Suspense } from 'react';
import { Loading } from './components/ui';
import IncidentList from './pages/IncidentList';
import IncidentDetail from './pages/IncidentDetail';
import ReportIncident from './pages/ReportIncident';
import MyQueue from './pages/MyQueue';

// Charts and admin screens load on demand to keep the first page light.
const Home = lazy(() => import('./pages/Home'));
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'));
const AdminTechnicians = lazy(() => import('./pages/admin/AdminTechnicians'));
const AdminServices = lazy(() => import('./pages/admin/AdminServices'));
const AdminSla = lazy(() => import('./pages/admin/AdminSla'));
const AdminCategories = lazy(() => import('./pages/admin/AdminCategories'));
const AdminAutomation = lazy(() => import('./pages/admin/AdminAutomation'));
const AdminAudit = lazy(() => import('./pages/admin/AdminAudit'));
const AdminOutbox = lazy(() => import('./pages/admin/AdminOutbox'));

function RequireAuth({ roles }) {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(role)) return <Navigate to="/" replace />;
  return <Outlet />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route element={<RequireAuth />}>
            <Route element={<Layout />}>
              <Route index element={<Home />} />
              <Route path="incidents" element={<IncidentList />} />
              <Route path="incidents/:id" element={<IncidentDetail />} />
              <Route path="report" element={<ReportIncident />} />

              <Route element={<RequireAuth roles={['TECHNICIAN', 'ADMIN']} />}>
                <Route path="queue" element={<MyQueue />} />
              </Route>

              <Route path="admin" element={<RequireAuth roles={['ADMIN']} />}>
                <Route index element={<Navigate to="users" replace />} />
                <Route path="users" element={<AdminUsers />} />
                <Route path="technicians" element={<AdminTechnicians />} />
                <Route path="services" element={<AdminServices />} />
                <Route path="sla" element={<AdminSla />} />
                <Route path="categories" element={<AdminCategories />} />
                <Route path="automation" element={<AdminAutomation />} />
                <Route path="audit" element={<AdminAudit />} />
                <Route path="outbox" element={<AdminOutbox />} />
              </Route>
            </Route>
          </Route>

          <Route path="dashboard" element={<Navigate to="/" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}
