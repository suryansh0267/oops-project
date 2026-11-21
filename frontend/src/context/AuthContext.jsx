import { createContext, useContext, useState } from 'react';
import api from '../api/client';
import { apiError } from '../lib/format';

const AuthContext = createContext(null);

function readUser() {
  try {
    const u = JSON.parse(localStorage.getItem('itsm_user'));
    return u?.id ? u : null; // sessions from older versions lack an id: sign in again

  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);
  const [token, setToken] = useState(() => localStorage.getItem('itsm_token'));

  const store = (data) => {
    const u = { id: data.id, username: data.username, email: data.email, fullName: data.fullName, role: data.role };
    localStorage.setItem('itsm_token', data.token);
    localStorage.setItem('itsm_user', JSON.stringify(u));
    setToken(data.token);
    setUser(u);
  };

  const login = async (username, password) => {
    try {
      const res = await api.post('/auth/login', { username, password });
      store(res.data);
      return { success: true };
    } catch (err) {
      return { success: false, message: apiError(err, 'Sign-in failed') };
    }
  };

  const register = async (form) => {
    try {
      const res = await api.post('/auth/register', form);
      store(res.data);
      return { success: true };
    } catch (err) {
      return { success: false, message: apiError(err, 'Registration failed') };
    }
  };

  const logout = () => {
    localStorage.removeItem('itsm_token');
    localStorage.removeItem('itsm_user');
    setToken(null);
    setUser(null);
  };

  const role = user?.role;
  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        login,
        register,
        logout,
        isAuthenticated: Boolean(token && user),
        isAdmin: role === 'ADMIN',
        isTechnician: role === 'TECHNICIAN',
        isStaff: role === 'TECHNICIAN' || role === 'ADMIN',
        isEmployee: role === 'EMPLOYEE',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);
