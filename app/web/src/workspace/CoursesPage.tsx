import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createCourse, deleteCourse, listCourses, updateCourse } from '../api';
import type { CourseItem } from '../types';
import { WEEKDAYS, errorText, formatClock } from './format';

interface FormState {
  name: string;
  teacher: string;
  location: string;
  weekday: string;
  startTime: string;
  endTime: string;
  repeatType: 'weekly' | 'once';
  courseDate: string;
  effectiveFrom: string;
  effectiveTo: string;
  remindMinutes: string;
}

const EMPTY: FormState = {
  name: '',
  teacher: '',
  location: '',
  weekday: '1',
  startTime: '08:00',
  endTime: '09:40',
  repeatType: 'weekly',
  courseDate: '',
  effectiveFrom: '',
  effectiveTo: '',
  remindMinutes: '10',
};

export default function CoursesPage() {
  const [items, setItems] = useState<CourseItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<CourseItem | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setItems(await listCourses());
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
    setForm(EMPTY);
    setOpen(true);
    setError(null);
    setNotice(null);
  }

  function startEdit(item: CourseItem) {
    setEditing(item);
    setForm({
      name: item.name,
      teacher: item.teacher ?? '',
      location: item.location ?? '',
      weekday: String(item.weekday),
      startTime: formatClock(item.startTime),
      endTime: formatClock(item.endTime),
      repeatType: item.repeatType,
      courseDate: item.courseDate ?? '',
      effectiveFrom: item.effectiveFrom ?? '',
      effectiveTo: item.effectiveTo ?? '',
      remindMinutes: String(item.remindMinutes),
    });
    setOpen(true);
    setError(null);
    setNotice(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);
    const body = {
      name: form.name,
      teacher: form.teacher || null,
      location: form.location || null,
      weekday: Number(form.weekday),
      startTime: form.startTime,
      endTime: form.endTime,
      repeatType: form.repeatType,
      courseDate: form.repeatType === 'once' ? form.courseDate || null : null,
      effectiveFrom: form.repeatType === 'weekly' ? form.effectiveFrom || null : null,
      effectiveTo: form.repeatType === 'weekly' ? form.effectiveTo || null : null,
      remindMinutes: Number(form.remindMinutes),
      version: editing?.version ?? null,
    };
    try {
      const result = editing ? await updateCourse(editing.id, body) : await createCourse(body);
      setOpen(false);
      await load();
      if (result.conflicts.length > 0) {
        setNotice(`已保存，但与 ${result.conflicts.map((item) => item.name).join('、')} 时间重叠。`);
      } else if (result.reminderNotice) {
        setNotice(result.reminderNotice);
      }
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setSaving(false);
    }
  }

  async function remove(item: CourseItem) {
    if (!window.confirm(`确定删除「${item.name}」吗？`)) {
      return;
    }
    try {
      await deleteCourse(item.id, item.version);
      await load();
    } catch (ex) {
      setError(errorText(ex, '删除失败'));
    }
  }

  const sorted = [...items].sort(
    (a, b) => a.weekday - b.weekday || a.startTime.localeCompare(b.startTime),
  );

  return (
    <section className="ws-section">
      <div className="ws-toolbar">
        <p className="hint">课程开始前按设定分钟数提醒；时间冲突只提示，不阻止保存。</p>
        <button className="primary-button" type="button" onClick={startCreate}>
          新增课程
        </button>
      </div>
      {error && <div className="banner-error">{error}</div>}
      {notice && <div className="banner-notice">{notice}</div>}

      {open && (
        <form className="edit-card" onSubmit={submit}>
          <div className="card-head">
            <div>
              <h2>{editing ? '编辑课程' : '新增课程'}</h2>
              <p>每周课程按星期与时间段重复，单次课程只提醒一次。</p>
            </div>
            <button className="text-button" type="button" onClick={() => setOpen(false)}>
              取消
            </button>
          </div>
          <div className="ws-form-grid">
            <label className="field">
              <span>课程名称</span>
              <input
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                maxLength={40}
                required
              />
            </label>
            <label className="field">
              <span>星期</span>
              <select
                value={form.weekday}
                onChange={(event) => setForm({ ...form, weekday: event.target.value })}
              >
                {WEEKDAYS.map((label, index) => (
                  <option key={label} value={index + 1}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>开始时间</span>
              <input
                type="time"
                value={form.startTime}
                onChange={(event) => setForm({ ...form, startTime: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>结束时间</span>
              <input
                type="time"
                value={form.endTime}
                onChange={(event) => setForm({ ...form, endTime: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>地点</span>
              <input
                value={form.location}
                onChange={(event) => setForm({ ...form, location: event.target.value })}
                maxLength={40}
              />
            </label>
            <label className="field">
              <span>教师</span>
              <input
                value={form.teacher}
                onChange={(event) => setForm({ ...form, teacher: event.target.value })}
                maxLength={20}
              />
            </label>
            <label className="field">
              <span>重复方式</span>
              <select
                value={form.repeatType}
                onChange={(event) =>
                  setForm({ ...form, repeatType: event.target.value as FormState['repeatType'] })
                }
              >
                <option value="weekly">每周重复</option>
                <option value="once">单次课程</option>
              </select>
            </label>
            <label className="field">
              <span>提前提醒</span>
              <select
                value={form.remindMinutes}
                onChange={(event) => setForm({ ...form, remindMinutes: event.target.value })}
              >
                {[0, 5, 10, 15, 30].map((value) => (
                  <option key={value} value={value}>
                    {value === 0 ? '开始时' : `提前 ${value} 分钟`}
                  </option>
                ))}
              </select>
            </label>
            {form.repeatType === 'once' ? (
              <label className="field">
                <span>上课日期</span>
                <input
                  type="date"
                  value={form.courseDate}
                  onChange={(event) => setForm({ ...form, courseDate: event.target.value })}
                  required
                />
              </label>
            ) : (
              <>
                <label className="field">
                  <span>生效开始（可选）</span>
                  <input
                    type="date"
                    value={form.effectiveFrom}
                    onChange={(event) => setForm({ ...form, effectiveFrom: event.target.value })}
                  />
                </label>
                <label className="field">
                  <span>生效结束（可选）</span>
                  <input
                    type="date"
                    value={form.effectiveTo}
                    onChange={(event) => setForm({ ...form, effectiveTo: event.target.value })}
                  />
                </label>
              </>
            )}
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
      ) : sorted.length === 0 ? (
        <p className="table-empty">还没有课程，点击右上角新增。</p>
      ) : (
        <div className="ws-list">
          {sorted.map((item) => (
            <article key={item.id} className="ws-card">
              <div className="ws-card-main">
                <div className="ws-card-title">
                  <strong>{item.name}</strong>
                  <span className="status-badge active">{WEEKDAYS[item.weekday - 1]}</span>
                  <span className="ws-chip">
                    {formatClock(item.startTime)}–{formatClock(item.endTime)}
                  </span>
                  <span className="ws-chip">
                    {item.repeatType === 'weekly' ? '每周' : item.courseDate}
                  </span>
                </div>
                <p className="ws-meta">
                  {[item.location, item.teacher].filter(Boolean).join(' · ') || '未填写地点与教师'}
                  {' · '}
                  {item.remindMinutes === 0 ? '开始时提醒' : `提前 ${item.remindMinutes} 分钟提醒`}
                  {item.repeatType === 'weekly' && (item.effectiveFrom || item.effectiveTo) && (
                    <>
                      {' · '}
                      {item.effectiveFrom ?? '不限'} 至 {item.effectiveTo ?? '不限'}
                    </>
                  )}
                </p>
              </div>
              <div className="ws-card-actions">
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
