import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createNote, deleteNote, getNote, listNotes, updateNote } from '../api';
import type { NoteItem, NoteSummaryItem } from '../types';
import { errorText, formatDateTime } from './format';

interface EditorState {
  id: number | null;
  title: string;
  category: string;
  body: string;
  version: number | null;
}

const EMPTY: EditorState = { id: null, title: '', category: '', body: '', version: null };

export default function NotesPage() {
  const [items, setItems] = useState<NoteSummaryItem[]>([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editor, setEditor] = useState<EditorState>(EMPTY);
  const [saving, setSaving] = useState(false);
  const [savedText, setSavedText] = useState('');
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved'>('idle');

  const signature = JSON.stringify([editor.title, editor.category, editor.body]);

  async function load(search = keyword) {
    setLoading(true);
    try {
      const page = await listNotes(1, search);
      setItems(page.items);
      setError(null);
    } catch (ex) {
      setError(errorText(ex, '加载失败'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load('');
  }, []);

  useEffect(() => {
    if (!editor.id || editor.version === null || signature === savedText) {
      return undefined;
    }
    const timer = window.setTimeout(() => {
      void autosave(signature);
    }, 2000);
    return () => window.clearTimeout(timer);
  }, [signature, savedText, editor.id, editor.version]);

  async function autosave(snapshot: string) {
    if (!editor.id || editor.version === null) {
      return;
    }
    setSaveState('saving');
    try {
      const note = await updateNote(editor.id, {
        title: editor.title,
        category: editor.category || null,
        body: editor.body,
        version: editor.version,
      });
      setEditor((current) => (current.id === note.id ? { ...current, version: note.version } : current));
      setSavedText(snapshot);
      setSaveState('saved');
      await load();
    } catch (ex) {
      setSaveState('idle');
      setError(errorText(ex, '自动保存失败'));
    }
  }

  async function openNote(id: number) {
    try {
      const note = await getNote(id);
      setEditor({
        id: note.id,
        title: note.title,
        category: note.category ?? '',
        body: note.body,
        version: note.version,
      });
      setSavedText(JSON.stringify([note.title, note.category ?? '', note.body]));
      setSaveState('idle');
      setError(null);
    } catch (ex) {
      setError(errorText(ex, '读取失败'));
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    const body = {
      title: editor.title,
      category: editor.category || null,
      body: editor.body,
      version: editor.version,
    };
    try {
      const note: NoteItem = editor.id
        ? await updateNote(editor.id, body)
        : await createNote(body);
      setEditor({
        id: note.id,
        title: note.title,
        category: note.category ?? '',
        body: note.body,
        version: note.version,
      });
      setSavedText(JSON.stringify([note.title, note.category ?? '', note.body]));
      setSaveState('saved');
      await load();
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!editor.id || editor.version === null) {
      return;
    }
    if (!window.confirm(`确定删除「${editor.title}」吗？`)) {
      return;
    }
    try {
      await deleteNote(editor.id, editor.version);
      setEditor(EMPTY);
      setSavedText('');
      setSaveState('idle');
      await load();
    } catch (ex) {
      setError(errorText(ex, '删除失败'));
    }
  }

  return (
    <section className="ws-section">
      <div className="ws-toolbar">
        <form
          className="ws-search"
          onSubmit={(event) => {
            event.preventDefault();
            void load();
          }}
        >
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="按标题搜索"
          />
          <button className="ghost-button" type="submit">
            搜索
          </button>
        </form>
        <button
          className="primary-button"
          type="button"
          onClick={() => {
            setEditor(EMPTY);
            setSavedText('');
            setSaveState('idle');
          }}
        >
          新建笔记
        </button>
      </div>
      {error && <div className="banner-error">{error}</div>}

      <div className="ws-notes">
        <div className="ws-note-list">
          {loading ? (
            <p className="muted">加载中…</p>
          ) : items.length === 0 ? (
            <p className="table-empty">没有找到笔记。</p>
          ) : (
            items.map((item) => (
              <button
                key={item.id}
                type="button"
                className={editor.id === item.id ? 'ws-note-item active' : 'ws-note-item'}
                onClick={() => void openNote(item.id)}
              >
                <strong>{item.title}</strong>
                <span>
                  {item.category || '未分类'} · {formatDateTime(item.updatedAt)}
                </span>
              </button>
            ))
          )}
        </div>

        <form className="edit-card ws-note-editor" onSubmit={submit}>
          <div className="card-head">
            <div>
              <h2>{editor.id ? '编辑笔记' : '新建笔记'}</h2>
              <p>
                标题 60 字以内，正文 10000 字以内，纯文本保存。
                {editor.id && saveState === 'saving' && ' · 保存中…'}
                {editor.id && saveState === 'saved' && ' · 已保存'}
              </p>
            </div>
            {editor.id && (
              <button className="text-button danger" type="button" onClick={() => void remove()}>
                删除
              </button>
            )}
          </div>
          <div className="ws-form-grid">
            <label className="field">
              <span>
                标题
                <em>{editor.title.length}/60</em>
              </span>
              <input
                value={editor.title}
                onChange={(event) => setEditor({ ...editor, title: event.target.value })}
                maxLength={60}
                required
              />
            </label>
            <label className="field">
              <span>分类</span>
              <input
                value={editor.category}
                onChange={(event) => setEditor({ ...editor, category: event.target.value })}
                maxLength={20}
                placeholder="未分类"
              />
            </label>
            <label className="field ws-span-2">
              <span>
                正文
                <em>{editor.body.length}/10000</em>
              </span>
              <textarea
                value={editor.body}
                onChange={(event) => setEditor({ ...editor, body: event.target.value })}
                maxLength={10000}
                rows={14}
                required
              />
            </label>
          </div>
          <div className="ws-actions">
            <button className="primary-button" type="submit" disabled={saving}>
              {saving ? '保存中…' : '保存笔记'}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
