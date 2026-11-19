import { useCallback, useEffect, useState } from 'react';
import { logout, me, registerDevice } from '../api';
import type { User } from '../types';
import WorkspaceLayout from './WorkspaceLayout';
import WorkspaceLogin from './WorkspaceLogin';

export default function WorkspaceGate() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const current = await me();
      setUser(current);
      registerDevice().catch(() => undefined);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function signOut() {
    await logout().catch(() => undefined);
    setUser(null);
  }

  if (loading) {
    return (
      <div className="admin-loading">
        <span className="login-mark">羽</span>
        <p>正在进入工作台…</p>
      </div>
    );
  }
  if (!user) {
    return <WorkspaceLogin onSuccess={() => void load()} />;
  }
  return (
    <WorkspaceLayout
      user={user}
      onUserChange={setUser}
      onSignOut={() => void signOut()}
    />
  );
}
