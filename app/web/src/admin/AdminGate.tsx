import { useCallback, useEffect, useState } from 'react';
import { ApiError, me, logout } from '../api';
import type { User } from '../types';
import LoginPage from './LoginPage';
import AdminLayout from './AdminLayout';

type GateState = 'loading' | 'anon' | 'forbidden' | 'error' | 'ready';

export default function AdminGate() {
  const [state, setState] = useState<GateState>('loading');
  const [user, setUser] = useState<User | null>(null);

  const check = useCallback(async () => {
    try {
      const current = await me();
      if (current.role === 'admin') {
        setUser(current);
        setState('ready');
      } else {
        setState('forbidden');
      }
    } catch (ex) {
      setState(ex instanceof ApiError && ex.status === 401 ? 'anon' : 'error');
    }
  }, []);

  useEffect(() => {
    check();
  }, [check]);

  if (state === 'loading') {
    return (
      <div className="admin-loading">
        <p className="muted">正在验证身份…</p>
      </div>
    );
  }

  if (state === 'anon') {
    return <LoginPage onSuccess={check} />;
  }

  if (state === 'forbidden') {
    return (
      <div className="admin-loading">
        <p>仅管理员可以访问后台。</p>
        <a className="ghost-button" href="/">
          返回前台
        </a>
      </div>
    );
  }

  if (state === 'error') {
    return (
      <div className="admin-loading">
        <p>无法连接服务器。</p>
        <button type="button" className="ghost-button" onClick={check}>
          重试
        </button>
      </div>
    );
  }

  return (
    <AdminLayout
      user={user!}
      onLogout={async () => {
        await logout().catch(() => undefined);
        setUser(null);
        setState('anon');
      }}
    />
  );
}
