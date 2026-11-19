import { useState } from 'react';
import type { FormEvent } from 'react';
import { cancelDeletion, login, register } from '../api';

interface Props {
  onSuccess: () => void;
}

export default function WorkspaceLogin({ onSuccess }: Props) {
  const [mode, setMode] = useState<'login' | 'register' | 'cancel'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setNotice(null);
    setBusy(true);
    try {
      if (mode === 'register') {
        await register(username, password);
        onSuccess();
      } else if (mode === 'cancel') {
        await cancelDeletion(username, password);
        setNotice('注销申请已撤销，请重新登录。');
        setMode('login');
      } else {
        await login(username, password);
        onSuccess();
      }
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : '操作失败');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="login-brand">
          <span className="login-mark">羽</span>
          <div>
            <h1>羽鲸个人工具</h1>
            <p>
              {mode === 'register' && '创建你的本地账号'}
              {mode === 'cancel' && '撤销账号注销申请'}
              {mode === 'login' && '登录你的私人工作台'}
            </p>
          </div>
        </div>
        <label>
          用户名
          <input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            autoComplete="username"
            required
          />
        </label>
        <label>
          {mode === 'cancel' ? '账号密码' : '密码'}
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
            required
          />
        </label>
        {error && <p className="form-error">{error}</p>}
        {notice && <p className="form-notice">{notice}</p>}
        <button className="primary-button" type="submit" disabled={busy}>
          {busy ? '处理中…' : mode === 'register' ? '注册并登录' : mode === 'cancel' ? '撤销注销' : '登录'}
        </button>
        <div className="ws-login-links">
          {mode === 'login' && (
            <>
              <button type="button" className="text-button" onClick={() => setMode('register')}>
                还没有账号？注册
              </button>
              <button type="button" className="text-button" onClick={() => setMode('cancel')}>
                撤销注销申请
              </button>
            </>
          )}
          {mode !== 'login' && (
            <button type="button" className="text-button" onClick={() => setMode('login')}>
              返回登录
            </button>
          )}
        </div>
        <a className="login-back" href="/">
          返回前台
        </a>
      </form>
    </div>
  );
}
