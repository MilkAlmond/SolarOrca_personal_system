import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createBirthday, deleteBirthday, listBirthdays, updateBirthday } from '../api';
import type { BirthdayItem } from '../types';
import { errorText, formatDate } from './format';

interface FormState {
  name: string;
  category: 'birthday' | 'anniversary';
  calendarType: 'solar' | 'lunar';
  solarDate: string;
  lunarMonth: string;
  lunarDay: string;
  lunarLeap: boolean;
  relationTag: string;
  advanceDays: string;
  note: string;
}

const EMPTY: FormState = {
  name: '',
  category: 'birthday',
  calendarType: 'solar',
  solarDate: '',
  lunarMonth: '1',
  lunarDay: '1',
  lunarLeap: false,
  relationTag: '',
  advanceDays: '0',
  note: '',
};

export default function BirthdaysPage() {
  const [items, setItems] = useState<BirthdayItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<BirthdayItem | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    try {
      setItems(await listBirthdays());
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
  }

  function startEdit(item: BirthdayItem) {
    setEditing(item);
    setForm({
      name: item.name,
      category: item.category,
      calendarType: item.calendarType,
      solarDate: item.solarDate ?? '',
      lunarMonth: String(item.lunarMonth ?? 1),
      lunarDay: String(item.lunarDay ?? 1),
      lunarLeap: item.lunarLeap,
      relationTag: item.relationTag ?? '',
      advanceDays: String(item.advanceDays),
      note: item.note ?? '',
    });
    setOpen(true);
    setError(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    const body = {
      name: form.name,
      category: form.category,
      calendarType: form.calendarType,
      solarDate: form.calendarType === 'solar' ? form.solarDate || null : null,
      lunarMonth: form.calendarType === 'lunar' ? Number(form.lunarMonth) : null,
      lunarDay: form.calendarType === 'lunar' ? Number(form.lunarDay) : null,
      lunarLeap: form.calendarType === 'lunar' && form.lunarLeap,
      relationTag: form.relationTag || null,
      advanceDays: Number(form.advanceDays),
      note: form.note || null,
      version: editing?.version ?? null,
    };
    try {
      if (editing) {
        await updateBirthday(editing.id, body);
      } else {
        await createBirthday(body);
      }
      setOpen(false);
      await load();
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setSaving(false);
    }
  }

  async function remove(item: BirthdayItem) {
    if (!window.confirm(`确定删除「${item.name}」吗？`)) {
      return;
    }
    try {
      await deleteBirthday(item.id, item.version);
      await load();
    } catch (ex) {
      setError(errorText(ex, '删除失败'));
    }
  }

  return (
    <section className="ws-section">
      <div className="ws-toolbar">
        <p className="hint">支持公历与农历，农历闰月会自动换算；2 月 29 日在平年按 2 月 28 日提醒。</p>
        <button className="primary-button" type="button" onClick={startCreate}>
          新增生日 / 纪念日
        </button>
      </div>
      {error && <div className="banner-error">{error}</div>}

      {open && (
        <form className="edit-card" onSubmit={submit}>
          <div className="card-head">
            <div>
              <h2>{editing ? '编辑生日 / 纪念日' : '新增生日 / 纪念日'}</h2>
              <p>提前提醒天数可选 0–30 天，生日当天 09:00 还会再提醒一次。</p>
            </div>
            <button className="text-button" type="button" onClick={() => setOpen(false)}>
              取消
            </button>
          </div>
          <div className="ws-form-grid">
            <label className="field">
              <span>名称</span>
              <input
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                maxLength={32}
                required
              />
            </label>
            <label className="field">
              <span>类型</span>
              <select
                value={form.category}
                onChange={(event) =>
                  setForm({ ...form, category: event.target.value as FormState['category'] })
                }
              >
                <option value="birthday">生日</option>
                <option value="anniversary">纪念日</option>
              </select>
            </label>
            <label className="field">
              <span>历法</span>
              <select
                value={form.calendarType}
                onChange={(event) =>
                  setForm({ ...form, calendarType: event.target.value as FormState['calendarType'] })
                }
              >
                <option value="solar">公历</option>
                <option value="lunar">农历</option>
              </select>
            </label>
            {form.calendarType === 'solar' ? (
              <label className="field">
                <span>日期</span>
                <input
                  type="date"
                  value={form.solarDate}
                  onChange={(event) => setForm({ ...form, solarDate: event.target.value })}
                  required
                />
              </label>
            ) : (
              <>
                <label className="field">
                  <span>农历月份</span>
                  <input
                    type="number"
                    min={1}
                    max={12}
                    value={form.lunarMonth}
                    onChange={(event) => setForm({ ...form, lunarMonth: event.target.value })}
                    required
                  />
                </label>
                <label className="field">
                  <span>农历日期</span>
                  <input
                    type="number"
                    min={1}
                    max={30}
                    value={form.lunarDay}
                    onChange={(event) => setForm({ ...form, lunarDay: event.target.value })}
                    required
                  />
                </label>
                <label className="field">
                  <span>闰月</span>
                  <span className="ws-check">
                    <input
                      type="checkbox"
                      checked={form.lunarLeap}
                      onChange={(event) => setForm({ ...form, lunarLeap: event.target.checked })}
                    />
                    按闰月计算
                  </span>
                </label>
              </>
            )}
            <label className="field">
              <span>提前提醒（天）</span>
              <input
                type="number"
                min={0}
                max={30}
                value={form.advanceDays}
                onChange={(event) => setForm({ ...form, advanceDays: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>关系标签</span>
              <input
                value={form.relationTag}
                onChange={(event) => setForm({ ...form, relationTag: event.target.value })}
                maxLength={20}
                placeholder="家人、朋友…"
              />
            </label>
            <label className="field ws-span-2">
              <span>备注</span>
              <textarea
                value={form.note}
                onChange={(event) => setForm({ ...form, note: event.target.value })}
                rows={3}
                maxLength={200}
              />
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
        <p className="table-empty">还没有记录，点击右上角新增。</p>
      ) : (
        <div className="ws-list">
          {items.map((item) => (
            <article key={item.id} className="ws-card">
              <div className="ws-card-main">
                <div className="ws-card-title">
                  <strong>{item.name}</strong>
                  <span className="status-badge active">
                    {item.category === 'birthday' ? '生日' : '纪念日'}
                  </span>
                  {item.relationTag && <span className="ws-chip">{item.relationTag}</span>}
                  {item.leapAdjusted && <span className="ws-chip">闰月调整</span>}
                </div>
                <p className="ws-meta">
                  {item.calendarType === 'lunar'
                    ? `农历 ${item.lunarMonth} 月 ${item.lunarDay} 日${item.lunarLeap ? '（闰月）' : ''}`
                    : `公历 ${formatDate(item.solarDate)}`}
                  {' · '}下次 {formatDate(item.nextDate)}
                  {' · '}
                  {item.daysUntil === 0 ? '就是今天' : `还有 ${item.daysUntil} 天`}
                  {' · '}提前 {item.advanceDays} 天提醒
                </p>
                {item.note && <p className="ws-note">{item.note}</p>}
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
