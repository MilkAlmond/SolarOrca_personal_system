import { useCallback, useEffect, useState } from 'react';
import { ApiError, listUsers, setUserStatus } from '../api';
import type { AdminUser, User } from '../types';

interface Props {
  currentUser: User;
}

function formatTime(value: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`);
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export default function UsersPanel({ currentUser }: Props) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (pageToLoad: number, search: string) => {
    setStatus('loading');
    setError(null);
    try {
      const result = await listUsers(pageToLoad, search);
      setUsers(result.items);
      setTotal(result.total);
      setPage(result.page);
      setStatus('ready');
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : '加载失败');
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    load(1, '');
  }, [load]);

  async function toggle(user: AdminUser) {
    if (user.status === 'active') {
      const confirmed = window.confirm(
        `确定停用账号 ${user.username}？停用后该账号的所有登录状态会立即失效。`,
      );
      if (!confirmed) {
        return;
      }
    }
    setBusyId(user.id);
    setError(null);
    try {
      await setUserStatus(user.id, user.status === 'active' ? 'disabled' : 'active');
      await load(page, keyword);
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : '操作失败');
    } finally {
      setBusyId(null);
    }
  }

  const pages = Math.max(1, Math.ceil(total / 50));

  return (
    <div className="users-panel">
      <div className="users-toolbar">
        <form
          className="search-box"
          onSubmit={(event) => {
            event.preventDefault();
            load(1, keyword);
          }}
        >
          <input
            placeholder="按用户名搜索"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <button type="submit" className="ghost-button">
            搜索
          </button>
        </form>
        <span className="hint">共 {total} 个账号</span>
      </div>

      {error && <div className="banner-error">{error}</div>}

      {status === 'loading' && users.length === 0 ? (
        <p className="muted">加载中…</p>
      ) : status === 'error' ? (
        <button type="button" className="ghost-button" onClick={() => load(page, keyword)}>
          重试
        </button>
      ) : (
        <div className="table-wrap">
          <table className="user-table">
            <thead>
              <tr>
                <th>账号</th>
                <th>显示名称</th>
                <th>角色</th>
                <th>状态</th>
                <th>最后登录</th>
                <th>注册时间</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <strong>{user.username}</strong>
                  </td>
                  <td>{user.displayName || '—'}</td>
                  <td>{user.role === 'admin' ? '管理员' : '用户'}</td>
                  <td>
                    <span className={`status-badge ${user.status}`}>
                      {user.status === 'active' ? '正常' : '已停用'}
                    </span>
                  </td>
                  <td>{formatTime(user.lastLoginAt)}</td>
                  <td>{formatTime(user.createdAt)}</td>
                  <td className="table-action">
                    <button
                      type="button"
                      className={user.status === 'active' ? 'text-button danger' : 'text-button'}
                      onClick={() => toggle(user)}
                      disabled={busyId === user.id || (user.id === currentUser.id && user.status === 'active')}
                      title={user.id === currentUser.id ? '不能停用当前登录的管理员账号' : undefined}
                    >
                      {busyId === user.id ? '处理中…' : user.status === 'active' ? '停用' : '恢复启用'}
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={7} className="table-empty">
                    暂无账号
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {pages > 1 && (
        <div className="pager">
          <button
            type="button"
            className="ghost-button"
            disabled={page <= 1}
            onClick={() => load(page - 1, keyword)}
          >
            上一页
          </button>
          <span>
            {page} / {pages}
          </span>
          <button
            type="button"
            className="ghost-button"
            disabled={page >= pages}
            onClick={() => load(page + 1, keyword)}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  );
}
