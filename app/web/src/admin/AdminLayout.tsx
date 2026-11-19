import { useState } from 'react';
import type { User } from '../types';
import ContentEditor from './ContentEditor';
import UsersPanel from './UsersPanel';

interface Props {
  user: User;
  onLogout: () => void;
}

type Tab = 'content' | 'users';

export default function AdminLayout({ user, onLogout }: Props) {
  const [tab, setTab] = useState<Tab>('content');

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="admin-brand">
          <span className="admin-mark">羽</span>
          <div>
            <strong>羽鲸</strong>
            <small>管理后台</small>
          </div>
        </div>
        <nav className="admin-nav">
          <button
            type="button"
            className={tab === 'content' ? 'active' : ''}
            onClick={() => setTab('content')}
          >
            内容管理
          </button>
          <button
            type="button"
            className={tab === 'users' ? 'active' : ''}
            onClick={() => setTab('users')}
          >
            账号管理
          </button>
        </nav>
        <div className="admin-side-foot">
          <a href="/" target="_blank" rel="noreferrer">
            查看前台
          </a>
          <button type="button" onClick={onLogout}>
            退出登录
          </button>
        </div>
      </aside>

      <div className="admin-main">
        <header className="admin-top">
          <h1>{tab === 'content' ? '内容管理' : '账号管理'}</h1>
          <span className="admin-user">
            {user.displayName || user.username}
            <em>管理员</em>
          </span>
        </header>
        <div className="admin-body">
          {tab === 'content' ? <ContentEditor /> : <UsersPanel currentUser={user} />}
        </div>
      </div>
    </div>
  );
}
