import { useState } from 'react';
import type { User } from '../types';
import BirthdaysPage from './BirthdaysPage';
import CoursesPage from './CoursesPage';
import NotesPage from './NotesPage';
import RemindersPage from './RemindersPage';
import SettingsPage from './SettingsPage';
import TasksPage from './TasksPage';

type Tab = 'birthdays' | 'courses' | 'tasks' | 'notes' | 'reminders' | 'settings';

const NAV: { key: Tab; label: string }[] = [
  { key: 'birthdays', label: '生日纪念' },
  { key: 'courses', label: '课程表' },
  { key: 'tasks', label: '任务打卡' },
  { key: 'notes', label: '笔记' },
  { key: 'reminders', label: '提醒' },
  { key: 'settings', label: '设置' },
];

const TITLES: Record<Tab, string> = {
  birthdays: '生日与纪念日',
  courses: '课程表',
  tasks: '任务打卡',
  notes: '笔记',
  reminders: '提醒中心',
  settings: '设置',
};

interface Props {
  user: User;
  onUserChange: (user: User) => void;
  onSignOut: () => void;
}

export default function WorkspaceLayout({ user, onUserChange, onSignOut }: Props) {
  const [tab, setTab] = useState<Tab>('birthdays');

  return (
    <div className="admin-shell">
      <aside className="admin-side">
        <div className="admin-brand">
          <span className="admin-mark">羽</span>
          <div>
            <strong>羽鲸</strong>
            <small>PERSONAL TOOL</small>
          </div>
        </div>
        <nav className="admin-nav">
          {NAV.map((item) => (
            <button
              key={item.key}
              type="button"
              className={tab === item.key ? 'active' : ''}
              onClick={() => setTab(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>
        <div className="admin-side-foot">
          <a href="/">查看前台</a>
          <button type="button" onClick={onSignOut}>
            退出登录
          </button>
        </div>
      </aside>
      <main className="admin-main">
        <header className="admin-top">
          <h1>{TITLES[tab]}</h1>
          <span className="admin-user">
            {user.displayName ?? user.username}
            {user.role === 'admin' && <em>管理员</em>}
          </span>
        </header>
        <div className="admin-body ws-body">
          {tab === 'birthdays' && <BirthdaysPage />}
          {tab === 'courses' && <CoursesPage />}
          {tab === 'tasks' && <TasksPage />}
          {tab === 'notes' && <NotesPage />}
          {tab === 'reminders' && <RemindersPage />}
          {tab === 'settings' && (
            <SettingsPage user={user} onUserChange={onUserChange} onSignOut={onSignOut} />
          )}
        </div>
      </main>
    </div>
  );
}
