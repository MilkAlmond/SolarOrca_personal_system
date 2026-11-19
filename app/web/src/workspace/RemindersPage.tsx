import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  createReminder,
  deleteReminder,
  listReminders,
  setReminderEnabled,
} from '../api';
import type { ReminderItem } from '../types';
import { errorText, formatDateTime } from './format';

const SOURCE: Record<ReminderItem['source'], string> = {
  birthday: '生日纪念',
  course: '课程',
  task: '任务',
  manual: '手动',
};

const STATUS: Record<ReminderItem['deliveryStatus'], string> = {
  pending: '待发送',
  sent: '已发送',
  failed: '发送失败',
};

type Filter = 'all' | 'pending' | 'sent' | 'failed';

export default function RemindersPage() {
  const [items, setItems] = useState<ReminderItem[]>([]);
  const [filter, setFilter] = useState<Filter>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [triggerAt, setTriggerAt] = useState('');
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setItems(await listReminders());
      setError(null);
    } catch (ex) {
      setError(errorText(ex, '加载失败'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await createReminder(title, new Date(triggerAt).toISOString());
      setTitle('');
      setTriggerAt('');
      await load();
    } catch (ex) {
      setError(errorText(ex, '创建失败'));
    } finally {
      setSaving(false);
    }
  }

  async function toggle(item: ReminderItem) {
    try {
      await setReminderEnabled(item.id, !item.enabled, item.version);
      await load();
    } catch (ex) {
      setError(errorText(ex, '操作失败'));
    }
  }

  async function remove(item: ReminderItem) {
    if (!window.confirm(`确定删除提醒「${item.title}」吗？`)) {
      return;
    }
    try {
      await deleteReminder(item.id, item.version);
      await load();
    } catch (ex) {
      setError(errorText(ex, '删除失败'));
    }
  }

  const visible = items.filter((item) => filter === 'all' || item.deliveryStatus === filter);

  return (
    <section className="ws-section">
      <form className="edit-card" onSubmit={submit}>
        <div className="card-head">
          <div>
            <h2>新建一次性提醒</h2>
            <p>生日、课程、任务会自动生成提醒；这里可以补充一条手动提醒。</p>
          </div>
        </div>
        <div className="ws-form-grid">
          <label className="field">
            <span>标题</span>
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={40}
              required
            />
          </label>
          <label className="field">
            <span>触发时间</span>
            <input
              type="datetime-local"
              value={triggerAt}
              onChange={(event) => setTriggerAt(event.target.value)}
              required
            />
          </label>
        </div>
        <div className="ws-actions">
          <button className="primary-button" type="submit" disabled={saving}>
            {saving ? '创建中…' : '创建提醒'}
          </button>
        </div>
      </form>

      <div className="ws-toolbar">
        <div className="ws-tabs">
          {(['all', 'pending', 'sent', 'failed'] as Filter[]).map((key) => (
            <button
              key={key}
              type="button"
              className={filter === key ? 'active' : ''}
              onClick={() => setFilter(key)}
            >
              {key === 'all' ? '全部' : STATUS[key]}
            </button>
          ))}
        </div>
        <span className="hint">关闭的提醒仍保留在列表中，但不会触发。</span>
      </div>
      {error && <div className="banner-error">{error}</div>}

      {loading ? (
        <p className="muted">加载中…</p>
      ) : visible.length === 0 ? (
        <p className="table-empty">没有符合条件的提醒。</p>
      ) : (
        <div className="ws-list">
          {visible.map((item) => (
            <article key={item.id} className="ws-card">
              <div className="ws-card-main">
                <div className="ws-card-title">
                  <strong>{item.title}</strong>
                  <span className="ws-chip">{SOURCE[item.source]}</span>
                  <span
                    className={
                      item.deliveryStatus === 'failed'
                        ? 'status-badge disabled'
                        : 'status-badge active'
                    }
                  >
                    {STATUS[item.deliveryStatus]}
                  </span>
                  {!item.enabled && <span className="ws-chip">已关闭</span>}
                </div>
                <p className="ws-meta">
                  触发 {formatDateTime(item.triggerAt)}
                  {item.advanceMinutes > 0 && ` · 提前 ${item.advanceMinutes} 分钟`}
                  {item.sentAt && ` · 发送于 ${formatDateTime(item.sentAt)}`}
                  {item.failedAt && ` · 失败于 ${formatDateTime(item.failedAt)}`}
                </p>
              </div>
              <div className="ws-card-actions">
                <button className="text-button" type="button" onClick={() => void toggle(item)}>
                  {item.enabled ? '关闭' : '开启'}
                </button>
                <button className="text-button danger" type="button" onClick={() => void remove(item)}>
                  删除
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
