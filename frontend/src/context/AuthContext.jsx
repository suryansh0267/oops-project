import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('itsm_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('itsm_token'));
  const [loading, setLoading] = useState(false);

  const login = async (username, password) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/login', { username, password });
      const data = res.data;
      setToken(data.token);
      setUser({
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
      });
      localStorage.setItem('itsm_token', data.token);
      localStorage.setItem(
        'itsm_user',
        JSON.stringify({
          username: data.username,
          email: data.email,
          fullName: data.fullName,
          role: data.role,
        })
      );
      return { success: true };
    } catch (err) {
      return {
        success: false,
        message: err.response?.data?.message || 'Login failed. Please check credentials.',
      };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    setLoading(true);
    try {
      const res = await api.post('/auth/register', userData);
      const data = res.data;
      setToken(data.token);
      setUser({
        username: data.username,
        email: data.email,
        fullName: data.fullName,
        role: data.role,
      });
      localStorage.setItem('itsm_token', data.token);
      localStorage.setItem(
        'itsm_user',
        JSON.stringify({
          username: data.username,
          email: data.email,
          fullName: data.fullName,
          role: data.role,
        })
      );
      return { success: true };
    } catch (err) {
      return {
        success: false,
        message: err.response?.data?.message || 'Registration failed.',
      };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('itsm_token');
    localStorage.removeItem('itsm_user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        login,
        register,
        logout,
        isAuthenticated: !!token,
        isAdmin: user?.role === 'ADMIN',
        isTechnician: user?.role === 'TECHNICIAN' || user?.role === 'ADMIN',
        isEmployee: user?.role === 'EMPLOYEE',
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
