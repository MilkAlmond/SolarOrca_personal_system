import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  checkinTask,
  createTask,
  deleteTask,
  listTasks,
  undoCheckin,
  updateTask,
} from '../api';
import type { TaskItem } from '../types';
import { errorText, formatClock, today } from './format';

interface FormState {
  name: string;
  period: 'daily' | 'weekly';
  weeklyTarget: string;
  startDate: string;
  remindTime: string;
  enabled: boolean;
}

function emptyForm(): FormState {
  return {
    name: '',
    period: 'daily',
    weeklyTarget: '3',
    startDate: today(),
    remindTime: '09:00',
    enabled: true,
  };
}

export default function TasksPage() {
  const [items, setItems] = useState<TaskItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TaskItem | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setItems(await listTasks());
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

  function startCreate() {
    setEditing(null);
    setForm(emptyForm());
    setOpen(true);
    setError(null);
    setNotice(null);
  }

  function startEdit(item: TaskItem) {
    setEditing(item);
    setForm({
      name: item.name,
      period: item.period,
      weeklyTarget: String(item.weeklyTarget ?? 3),
      startDate: item.startDate,
      remindTime: formatClock(item.remindTime),
      enabled: item.enabled,
    });
    setOpen(true);
    setError(null);
    setNotice(null);
  }

  function bodyOf(state: FormState, version: number | null) {
    return {
      name: state.name,
      period: state.period,
      weeklyTarget: state.period === 'weekly' ? Number(state.weeklyTarget) : null,
      startDate: state.startDate,
      remindTime: state.remindTime,
      enabled: state.enabled,
      version,
    };
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await updateTask(editing.id, bodyOf(form, editing.version));
      } else {
        await createTask(bodyOf(form, null));
      }
      setOpen(false);
      await load();
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setSaving(false);
    }
  }

  async function checkin(item: TaskItem) {
    setError(null);
    setNotice(null);
    try {
      const result = await checkinTask(item.id);
      await load();
      if (result.newMilestone) {
        setNotice(`连续 ${result.newMilestone} 天，新的里程碑达成！`);
      }
    } catch (ex) {
      setError(errorText(ex, '打卡失败'));
    }
  }

  async function undo(item: TaskItem) {
    try {
      await undoCheckin(item.id, today());
      await load();
    } catch (ex) {
      setError(errorText(ex, '撤销失败'));
    }
  }

  async function toggle(item: TaskItem) {
    try {
      await updateTask(item.id, bodyOf({
        name: item.name,
        period: item.period,
        weeklyTarget: String(item.weeklyTarget ?? 3),
        startDate: item.startDate,
        remindTime: formatClock(item.remindTime),
        enabled: !item.enabled,
      }, item.version));
      await load();
    } catch (ex) {
      setError(errorText(ex, '操作失败'));
    }
  }

  async function remove(item: TaskItem) {
    if (!window.confirm(`确定删除「${item.name}」吗？打卡记录会一并删除。`)) {
      return;
    }
    try {
      await deleteTask(item.id, item.version);
      await load();
    } catch (ex) {
      setError(errorText(ex, '删除失败'));
    }
  }

  return (
    <section className="ws-section">
      <div className="ws-toolbar">
        <p className="hint">每日或每周达成目标即可打卡，连续 7 / 30 / 100 天会记录里程碑。</p>
        <button className="primary-button" type="button" onClick={startCreate}>
          新增任务
        </button>
      </div>
      {error && <div className="banner-error">{error}</div>}
      {notice && <div className="banner-notice">{notice}</div>}

      {open && (
        <form className="edit-card" onSubmit={submit}>
          <div className="card-head">
            <div>
              <h2>{editing ? '编辑任务' : '新增任务'}</h2>
              <p>提醒时间使用本地时区，任务关闭后不再生成新的提醒。</p>
            </div>
            <button className="text-button" type="button" onClick={() => setOpen(false)}>
              取消
            </button>
          </div>
          <div className="ws-form-grid">
            <label className="field ws-span-2">
              <span>任务名称</span>
              <input
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                maxLength={40}
                required
              />
            </label>
            <label className="field">
              <span>周期</span>
              <select
                value={form.period}
                onChange={(event) =>
                  setForm({ ...form, period: event.target.value as FormState['period'] })
                }
              >
                <option value="daily">每日</option>
                <option value="weekly">每周</option>
              </select>
            </label>
            {form.period === 'weekly' && (
              <label className="field">
                <span>每周目标次数</span>
                <input
                  type="number"
                  min={1}
                  max={7}
                  value={form.weeklyTarget}
                  onChange={(event) => setForm({ ...form, weeklyTarget: event.target.value })}
                  required
                />
              </label>
            )}
            <label className="field">
              <span>开始日期</span>
              <input
                type="date"
                value={form.startDate}
                onChange={(event) => setForm({ ...form, startDate: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>提醒时间</span>
              <input
                type="time"
                value={form.remindTime}
                onChange={(event) => setForm({ ...form, remindTime: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>启用</span>
              <span className="ws-check">
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(event) => setForm({ ...form, enabled: event.target.checked })}
                />
                启用提醒与打卡
              </span>
            </label>
          </div>
          <div className="ws-actions">
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? '保存中…' : '保存'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="muted">加载中…</p>
      ) : items.length === 0 ? (
        <p className="table-empty">还没有任务，点击右上角新增。</p>
      ) : (
        <div className="ws-list">
          {items.map((item) => (
            <article key={item.id} className="ws-card">
              <div className="ws-card-main">
                <div className="ws-card-title">
                  <strong>{item.name}</strong>
                  <span className={item.enabled ? 'status-badge active' : 'status-badge disabled'}>
                    {item.enabled ? '进行中' : '已暂停'}
                  </span>
                  <span className="ws-chip">
                    {item.period === 'daily' ? '每日' : `每周 ${item.weeklyTarget} 次`}
                  </span>
                  <span className="ws-chip">提醒 {formatClock(item.remindTime)}</span>
                </div>
                <p className="ws-meta">
                  连续 {item.streakDays} 天
                  {item.period === 'weekly' && ` · 本周 ${item.weekProgress}/${item.weeklyTarget}`}
                  {item.milestones.length > 0 && ` · 里程碑 ${item.milestones.join(' / ')} 天`}
                  {item.checkedToday ? ' · 今天已打卡' : ' · 今天未打卡'}
                </p>
              </div>
              <div className="ws-card-actions">
                {item.checkedToday ? (
                  <button className="ghost-button" type="button" onClick={() => void undo(item)}>
                    撤销今日
                  </button>
                ) : (
                  <button
                    className="primary-button"
                    type="button"
                    onClick={() => void checkin(item)}
                    disabled={!item.enabled}
                  >
                    打卡
                  </button>
                )}
                <button className="text-button" type="button" onClick={() => void toggle(item)}>
                  {item.enabled ? '暂停' : '启用'}
                </button>
                <button className="text-button" type="button" onClick={() => startEdit(item)}>
                  编辑
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
