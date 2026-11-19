import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  ApiError,
  adminHome,
  saveContacts,
  saveHonors,
  saveInfoLines,
  saveProfile,
  uploadPng,
} from '../api';
import type { AdminHome, ContactType, PageModel } from '../types';
import PageContent from '../public/PageContent';
import { moveItem, useDragSort } from './useDragSort';

interface ProfileDraft {
  avatarFileId: number | null;
  avatarUrl: string | null;
  title: string;
  subtitle: string;
  bio: string;
  images: { fileId: number; url: string }[];
  version: number;
}

interface LineDraft {
  key: number;
  label: string;
  value: string;
}

interface HonorDraft {
  key: number;
  name: string;
  awardedOn: string;
}

interface ContactDraft {
  key: number;
  type: ContactType;
  displayText: string;
  target: string;
  publicVisible: boolean;
}

type SectionKey = 'profile' | 'lines' | 'honors' | 'contacts';

let keySeq = 1;
const nextKey = () => keySeq++;

const CONTACT_OPTIONS: { value: ContactType; label: string }[] = [
  { value: 'email', label: '邮箱' },
  { value: 'social', label: '社交账号' },
  { value: 'link', label: '链接' },
];

function visibleChars(value: string): number {
  let count = 0;
  for (let i = 0; i < value.length; ) {
    const codePoint = value.codePointAt(i) ?? 0;
    if (codePoint !== 10 && codePoint !== 13) {
      count++;
    }
    i += codePoint > 0xffff ? 2 : 1;
  }
  return count;
}

function ErrorList({ errors }: { errors: Record<string, string> }) {
  const entries = Object.entries(errors);
  if (entries.length === 0) {
    return null;
  }
  return (
    <ul className="field-errors">
      {entries.map(([key, message]) => (
        <li key={key}>{message}</li>
      ))}
    </ul>
  );
}

export default function ContentEditor() {
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [profile, setProfile] = useState<ProfileDraft>({
    avatarFileId: null,
    avatarUrl: null,
    title: '',
    subtitle: '',
    bio: '',
    images: [],
    version: 0,
  });
  const [lines, setLines] = useState<LineDraft[]>([]);
  const [honors, setHonors] = useState<HonorDraft[]>([]);
  const [contacts, setContacts] = useState<ContactDraft[]>([]);
  const [dirty, setDirty] = useState<Record<SectionKey, boolean>>({
    profile: false,
    lines: false,
    honors: false,
    contacts: false,
  });
  const [errors, setErrors] = useState<Record<SectionKey, Record<string, string>>>({
    profile: {},
    lines: {},
    honors: {},
    contacts: {},
  });
  const [saving, setSaving] = useState<SectionKey | null>(null);
  const [uploading, setUploading] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [preview, setPreview] = useState(false);

  const avatarInput = useRef<HTMLInputElement>(null);
  const galleryInput = useRef<HTMLInputElement>(null);
  const toastTimer = useRef<number | undefined>(undefined);

  function showToast(message: string) {
    setToast(message);
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setToast(null), 2000);
  }

  function markDirty(section: SectionKey) {
    setDirty((current) => ({ ...current, [section]: true }));
  }

  const applyHome = useCallback((home: AdminHome) => {
    const saved = home.profile;
    setProfile({
      avatarFileId: saved?.avatarFileId ?? null,
      avatarUrl: saved?.avatarUrl ?? null,
      title: saved?.title ?? '',
      subtitle: saved?.subtitle ?? '',
      bio: saved?.bio ?? '',
      images: saved?.images ?? [],
      version: home.version,
    });
    setLines(
      home.infoLines.map((line) => ({ key: nextKey(), label: line.label ?? '', value: line.value })),
    );
    setHonors(
      home.honors.map((honor) => ({ key: nextKey(), name: honor.name, awardedOn: honor.awardedOn })),
    );
    setContacts(
      home.contacts.map((contact) => ({
        key: nextKey(),
        type: contact.type,
        displayText: contact.displayText,
        target: contact.target ?? '',
        publicVisible: contact.publicVisible,
      })),
    );
    setDirty({ profile: false, lines: false, honors: false, contacts: false });
    setErrors({ profile: {}, lines: {}, honors: {}, contacts: {} });
  }, []);

  const load = useCallback(async () => {
    setStatus('loading');
    try {
      const home = await adminHome();
      applyHome(home);
      setStatus('ready');
    } catch {
      setStatus('error');
    }
  }, [applyHome]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    function guard(event: BeforeUnloadEvent) {
      if (Object.values(dirty).some(Boolean)) {
        event.preventDefault();
        event.returnValue = '';
      }
    }
    window.addEventListener('beforeunload', guard);
    return () => window.removeEventListener('beforeunload', guard);
  }, [dirty]);

  function handleApiError(ex: unknown, section: SectionKey) {
    if (ex instanceof ApiError) {
      setErrors((current) => ({ ...current, [section]: ex.fields }));
      setGlobalError(ex.message);
    } else {
      setGlobalError('操作失败，请稍后重试');
    }
  }

  async function persistProfile() {
    setSaving('profile');
    setGlobalError(null);
    setErrors((current) => ({ ...current, profile: {} }));
    try {
      const home = await saveProfile({
        avatarFileId: profile.avatarFileId,
        title: profile.title.trim() || null,
        subtitle: profile.subtitle.trim() || null,
        bio: profile.bio.trim() ? profile.bio : null,
        imageFileIds: profile.images.map((image) => image.fileId),
        version: profile.version,
      });
      const saved = home.profile;
      setProfile({
        avatarFileId: saved?.avatarFileId ?? null,
        avatarUrl: saved?.avatarUrl ?? null,
        title: saved?.title ?? '',
        subtitle: saved?.subtitle ?? '',
        bio: saved?.bio ?? '',
        images: saved?.images ?? [],
        version: home.version,
      });
      setDirty((current) => ({ ...current, profile: false }));
      showToast('已保存');
    } catch (ex) {
      handleApiError(ex, 'profile');
    } finally {
      setSaving(null);
    }
  }

  async function persistLines() {
    setSaving('lines');
    setGlobalError(null);
    setErrors((current) => ({ ...current, lines: {} }));
    try {
      const home = await saveInfoLines(
        lines.map((line) => ({ label: line.label.trim() || null, value: line.value })),
      );
      setLines(
        home.infoLines.map((line) => ({
          key: nextKey(),
          label: line.label ?? '',
          value: line.value,
        })),
      );
      setDirty((current) => ({ ...current, lines: false }));
      showToast('已保存');
    } catch (ex) {
      handleApiError(ex, 'lines');
    } finally {
      setSaving(null);
    }
  }

  async function persistHonors() {
    setSaving('honors');
    setGlobalError(null);
    setErrors((current) => ({ ...current, honors: {} }));
    try {
      const home = await saveHonors(
        honors.map((honor) => ({ name: honor.name, awardedOn: honor.awardedOn })),
      );
      setHonors(
        home.honors.map((honor) => ({
          key: nextKey(),
          name: honor.name,
          awardedOn: honor.awardedOn,
        })),
      );
      setDirty((current) => ({ ...current, honors: false }));
      showToast('已保存');
    } catch (ex) {
      handleApiError(ex, 'honors');
    } finally {
      setSaving(null);
    }
  }

  async function persistContacts() {
    setSaving('contacts');
    setGlobalError(null);
    setErrors((current) => ({ ...current, contacts: {} }));
    try {
      const home = await saveContacts(
        contacts.map((contact) => ({
          type: contact.type,
          displayText: contact.displayText,
          target: contact.target.trim() || null,
          publicVisible: contact.publicVisible,
        })),
      );
      setContacts(
        home.contacts.map((contact) => ({
          key: nextKey(),
          type: contact.type,
          displayText: contact.displayText,
          target: contact.target ?? '',
          publicVisible: contact.publicVisible,
        })),
      );
      setDirty((current) => ({ ...current, contacts: false }));
      showToast('已保存');
    } catch (ex) {
      handleApiError(ex, 'contacts');
    } finally {
      setSaving(null);
    }
  }

  function checkPng(file: File): string | null {
    if (file.type && file.type !== 'image/png') {
      return '仅支持 PNG 图片';
    }
    if (file.size > 5 * 1024 * 1024) {
      return '单张图片不能超过 5 MB';
    }
    return null;
  }

  async function onAvatar(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    const problem = checkPng(file);
    if (problem) {
      setErrors((current) => ({ ...current, profile: { avatarFileId: problem } }));
      return;
    }
    setUploading(true);
    setGlobalError(null);
    try {
      const uploaded = await uploadPng(file, 'avatar');
      setProfile((current) => ({
        ...current,
        avatarFileId: uploaded.id,
        avatarUrl: uploaded.url,
      }));
      markDirty('profile');
    } catch (ex) {
      handleApiError(ex, 'profile');
    } finally {
      setUploading(false);
    }
  }

  async function onGallery(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = '';
    if (files.length === 0) {
      return;
    }
    if (profile.images.length + files.length > 6) {
      setErrors((current) => ({ ...current, profile: { imageFileIds: '配图最多 6 张' } }));
      return;
    }
    for (const file of files) {
      const problem = checkPng(file);
      if (problem) {
        setErrors((current) => ({ ...current, profile: { imageFileIds: problem } }));
        return;
      }
    }
    setUploading(true);
    setGlobalError(null);
    try {
      for (const file of files) {
        const uploaded = await uploadPng(file, 'bio');
        setProfile((current) => ({
          ...current,
          images: [...current.images, { fileId: uploaded.id, url: uploaded.url }],
        }));
      }
      markDirty('profile');
    } catch (ex) {
      handleApiError(ex, 'profile');
    } finally {
      setUploading(false);
    }
  }

  const gallerySort = useDragSort(profile.images, (images) => {
    setProfile((current) => ({ ...current, images }));
    markDirty('profile');
  });
  const lineSort = useDragSort(lines, (next) => {
    setLines(next);
    markDirty('lines');
  });
  const honorSort = useDragSort(honors, (next) => {
    setHonors(next);
    markDirty('honors');
  });
  const contactSort = useDragSort(contacts, (next) => {
    setContacts(next);
    markDirty('contacts');
  });

  if (status === 'loading') {
    return <p className="muted">加载中…</p>;
  }

  if (status === 'error') {
    return (
      <div>
        <p>内容加载失败。</p>
        <button type="button" className="ghost-button" onClick={load}>
          重试
        </button>
      </div>
    );
  }

  const previewProfile =
    profile.title || profile.subtitle || profile.avatarUrl || profile.bio || profile.images.length > 0
      ? {
          avatarUrl: profile.avatarUrl,
          title: profile.title.trim() || null,
          subtitle: profile.subtitle.trim() || null,
          bio: profile.bio.trim() || null,
          images: profile.images.map((image) => ({ url: image.url })),
        }
      : null;
  const previewModel: PageModel = {
    profile: previewProfile,
    infoLines: lines
      .filter((line) => line.value.trim())
      .map((line) => ({ label: line.label.trim() || null, value: line.value })),
    honors: honors
      .filter((honor) => honor.name.trim() && honor.awardedOn)
      .map((honor) => ({ name: honor.name, awardedOn: honor.awardedOn })),
    contacts: contacts
      .filter((contact) => contact.publicVisible && contact.displayText.trim())
      .map((contact) => ({
        type: contact.type,
        displayText: contact.displayText,
        target: contact.target.trim() || null,
      })),
  };
  const previewPublished = Boolean(
    previewModel.profile ||
      previewModel.infoLines.length ||
      previewModel.honors.length ||
      previewModel.contacts.length,
  );

  const filledLines = lines.filter((line) => line.value.trim()).length;

  return (
    <div className="editor">
      {globalError && <div className="banner-error">{globalError}</div>}

      <div className="editor-toolbar">
        <p>修改在点击保存前不会生效；预览只读取当前草稿。</p>
        <button type="button" className="ghost-button" onClick={() => setPreview(true)}>
          预览
        </button>
      </div>

      <section className="edit-card">
        <div className="card-head">
          <div>
            <h2>个人简介</h2>
            <p>头像、标题、副标题、正文与配图，前台按编辑顺序展示。</p>
          </div>
          <button
            type="button"
            className="primary-button"
            onClick={persistProfile}
            disabled={saving === 'profile' || uploading}
          >
            {saving === 'profile' ? '保存中…' : '保存'}
          </button>
        </div>
        <ErrorList errors={errors.profile} />

        <div className="avatar-row">
          {profile.avatarUrl ? (
            <img className="avatar-preview" src={profile.avatarUrl} alt="头像预览" />
          ) : (
            <div className="avatar-preview placeholder">无头像</div>
          )}
          <div>
            <button
              type="button"
              className="ghost-button"
              onClick={() => avatarInput.current?.click()}
              disabled={uploading}
            >
              选择头像 PNG
            </button>
            {profile.avatarUrl && (
              <button
                type="button"
                className="text-button danger"
                onClick={() => {
                  setProfile((current) => ({ ...current, avatarFileId: null, avatarUrl: null }));
                  markDirty('profile');
                }}
              >
                移除头像
              </button>
            )}
            <p className="hint">正方形裁切，仅 PNG，不超过 5 MB</p>
          </div>
          <input ref={avatarInput} type="file" accept="image/png" hidden onChange={onAvatar} />
        </div>

        <label className="field">
          <span>标题</span>
          <input
            value={profile.title}
            maxLength={40}
            onChange={(event) => {
              setProfile((current) => ({ ...current, title: event.target.value }));
              markDirty('profile');
            }}
          />
        </label>
        <label className="field">
          <span>副标题</span>
          <input
            value={profile.subtitle}
            maxLength={60}
            onChange={(event) => {
              setProfile((current) => ({ ...current, subtitle: event.target.value }));
              markDirty('profile');
            }}
          />
        </label>
        <label className="field">
          <span>
            正文 <em>{visibleChars(profile.bio)} / 2000</em>
          </span>
          <textarea
            rows={8}
            value={profile.bio}
            onChange={(event) => {
              setProfile((current) => ({ ...current, bio: event.target.value }));
              markDirty('profile');
            }}
          />
        </label>

        <div className="gallery-edit">
          <div className="gallery-head">
            <span>配图（{profile.images.length} / 6）</span>
            <button
              type="button"
              className="ghost-button"
              onClick={() => galleryInput.current?.click()}
              disabled={uploading || profile.images.length >= 6}
            >
              上传 PNG
            </button>
          </div>
          {profile.images.length === 0 ? (
            <p className="hint">尚无配图</p>
          ) : (
            <div className="gallery-edit-list">
              {profile.images.map((image, index) => (
                <div className="gallery-item" key={image.fileId} {...gallerySort.handlers(index)}>
                  <img src={image.url} alt="" />
                  <div className="gallery-item-actions">
                    <button
                      type="button"
                      onClick={() => {
                        setProfile((current) => ({
                          ...current,
                          images: moveItem(current.images, index, -1),
                        }));
                        markDirty('profile');
                      }}
                      disabled={index === 0}
                      aria-label="上移"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setProfile((current) => ({
                          ...current,
                          images: moveItem(current.images, index, 1),
                        }));
                        markDirty('profile');
                      }}
                      disabled={index === profile.images.length - 1}
                      aria-label="下移"
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setProfile((current) => ({
                          ...current,
                          images: current.images.filter((item) => item.fileId !== image.fileId),
                        }));
                        markDirty('profile');
                      }}
                    >
                      移除
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
          <input
            ref={galleryInput}
            type="file"
            accept="image/png"
            multiple
            hidden
            onChange={onGallery}
          />
        </div>
      </section>

      <section className="edit-card">
        <div className="card-head">
          <div>
            <h2>个人信息</h2>
            <p>每行“标签：取值”或仅取值；拖动调整顺序，空行保存时忽略。</p>
          </div>
          <button
            type="button"
            className="primary-button"
            onClick={persistLines}
            disabled={saving === 'lines'}
          >
            {saving === 'lines' ? '保存中…' : '保存'}
          </button>
        </div>
        <ErrorList errors={errors.lines} />
        <div className="row-list">
          {lines.map((line, index) => (
            <div className="edit-row" key={line.key} {...lineSort.handlers(index)}>
              <span className="drag-handle" aria-hidden="true">
                ⋮⋮
              </span>
              <input
                className="row-label"
                placeholder="标签（可空）"
                maxLength={40}
                value={line.label}
                onChange={(event) => {
                  const value = event.target.value;
                  setLines((current) =>
                    current.map((item) => (item.key === line.key ? { ...item, label: value } : item)),
                  );
                  markDirty('lines');
                }}
              />
              <input
                className="row-value"
                placeholder="取值"
                maxLength={200}
                value={line.value}
                onChange={(event) => {
                  const value = event.target.value;
                  setLines((current) =>
                    current.map((item) => (item.key === line.key ? { ...item, value } : item)),
                  );
                  markDirty('lines');
                }}
              />
              <div className="row-actions">
                <button
                  type="button"
                  onClick={() => {
                    setLines(moveItem(lines, index, -1));
                    markDirty('lines');
                  }}
                  disabled={index === 0}
                  aria-label="上移"
                >
                  ↑
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setLines(moveItem(lines, index, 1));
                    markDirty('lines');
                  }}
                  disabled={index === lines.length - 1}
                  aria-label="下移"
                >
                  ↓
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setLines(lines.filter((item) => item.key !== line.key));
                    markDirty('lines');
                  }}
                  aria-label="删除"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
        </div>
        <div className="row-foot">
          <button
            type="button"
            className="ghost-button"
            disabled={lines.length >= 20}
            onClick={() => {
              setLines([...lines, { key: nextKey(), label: '', value: '' }]);
              markDirty('lines');
            }}
          >
            添加一行
          </button>
          <span className="hint">
            有效 {filledLines} 行 / 20 行
          </span>
        </div>
      </section>

      <section className="edit-card">
        <div className="card-head">
          <div>
            <h2>曾获荣誉</h2>
            <p>默认按时间倒序，新条目插入首位；可拖动调整顺序。</p>
          </div>
          <button
            type="button"
            className="primary-button"
            onClick={persistHonors}
            disabled={saving === 'honors'}
          >
            {saving === 'honors' ? '保存中…' : '保存'}
          </button>
        </div>
        <ErrorList errors={errors.honors} />
        <div className="row-list">
          {honors.map((honor, index) => (
            <div className="edit-row" key={honor.key} {...honorSort.handlers(index)}>
              <span className="drag-handle" aria-hidden="true">
                ⋮⋮
              </span>
              <input
                className="row-name"
                placeholder="荣誉名称"
                maxLength={60}
                value={honor.name}
                onChange={(event) => {
                  const value = event.target.value;
                  setHonors((current) =>
                    current.map((item) => (item.key === honor.key ? { ...item, name: value } : item)),
                  );
                  markDirty('honors');
                }}
              />
              <input
                className="row-date"
                type="date"
                value={honor.awardedOn}
                onChange={(event) => {
                  const value = event.target.value;
                  setHonors((current) =>
                    current.map((item) =>
                      item.key === honor.key ? { ...item, awardedOn: value } : item,
                    ),
                  );
                  markDirty('honors');
                }}
              />
              <div className="row-actions">
                <button
                  type="button"
                  onClick={() => {
                    setHonors(moveItem(honors, index, -1));
                    markDirty('honors');
                  }}
                  disabled={index === 0}
                  aria-label="上移"
                >
                  ↑
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setHonors(moveItem(honors, index, 1));
                    markDirty('honors');
                  }}
                  disabled={index === honors.length - 1}
                  aria-label="下移"
                >
                  ↓
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setHonors(honors.filter((item) => item.key !== honor.key));
                    markDirty('honors');
                  }}
                  aria-label="删除"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
        </div>
        <div className="row-foot">
          <button
            type="button"
            className="ghost-button"
            onClick={() => {
              setHonors([{ key: nextKey(), name: '', awardedOn: '' }, ...honors]);
              markDirty('honors');
            }}
          >
            添加荣誉
          </button>
        </div>
      </section>

      <section className="edit-card">
        <div className="card-head">
          <div>
            <h2>联系方式</h2>
            <p>仅“公开”条目出现在前台；社交账号点击后复制显示文本。</p>
          </div>
          <button
            type="button"
            className="primary-button"
            onClick={persistContacts}
            disabled={saving === 'contacts'}
          >
            {saving === 'contacts' ? '保存中…' : '保存'}
          </button>
        </div>
        <ErrorList errors={errors.contacts} />
        <div className="row-list">
          {contacts.map((contact, index) => (
            <div className="edit-row contact-row" key={contact.key} {...contactSort.handlers(index)}>
              <span className="drag-handle" aria-hidden="true">
                ⋮⋮
              </span>
              <select
                value={contact.type}
                onChange={(event) => {
                  const value = event.target.value as ContactType;
                  setContacts((current) =>
                    current.map((item) => (item.key === contact.key ? { ...item, type: value } : item)),
                  );
                  markDirty('contacts');
                }}
              >
                {CONTACT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <input
                className="row-name"
                placeholder="显示文字"
                maxLength={60}
                value={contact.displayText}
                onChange={(event) => {
                  const value = event.target.value;
                  setContacts((current) =>
                    current.map((item) =>
                      item.key === contact.key ? { ...item, displayText: value } : item,
                    ),
                  );
                  markDirty('contacts');
                }}
              />
              <input
                className="row-value"
                placeholder={
                  contact.type === 'email'
                    ? '邮箱地址'
                    : contact.type === 'link'
                      ? 'http/https 链接'
                      : '跳转地址（可空）'
                }
                maxLength={300}
                value={contact.target}
                onChange={(event) => {
                  const value = event.target.value;
                  setContacts((current) =>
                    current.map((item) =>
                      item.key === contact.key ? { ...item, target: value } : item,
                    ),
                  );
                  markDirty('contacts');
                }}
              />
              <label className="switch">
                <input
                  type="checkbox"
                  checked={contact.publicVisible}
                  onChange={(event) => {
                    const value = event.target.checked;
                    setContacts((current) =>
                      current.map((item) =>
                        item.key === contact.key ? { ...item, publicVisible: value } : item,
                      ),
                    );
                    markDirty('contacts');
                  }}
                />
                <span>公开</span>
              </label>
              <div className="row-actions">
                <button
                  type="button"
                  onClick={() => {
                    setContacts(moveItem(contacts, index, -1));
                    markDirty('contacts');
                  }}
                  disabled={index === 0}
                  aria-label="上移"
                >
                  ↑
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setContacts(moveItem(contacts, index, 1));
                    markDirty('contacts');
                  }}
                  disabled={index === contacts.length - 1}
                  aria-label="下移"
                >
                  ↓
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setContacts(contacts.filter((item) => item.key !== contact.key));
                    markDirty('contacts');
                  }}
                  aria-label="删除"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
        </div>
        <div className="row-foot">
          <button
            type="button"
            className="ghost-button"
            onClick={() => {
              setContacts([
                ...contacts,
                {
                  key: nextKey(),
                  type: 'email',
                  displayText: '',
                  target: '',
                  publicVisible: true,
                },
              ]);
              markDirty('contacts');
            }}
          >
            添加联系方式
          </button>
        </div>
      </section>

      {preview && (
        <div className="preview-overlay">
          <div className="preview-bar">
            <span>前台预览（未保存）</span>
            <button type="button" className="ghost-button" onClick={() => setPreview(false)}>
              关闭预览
            </button>
          </div>
          <div className="preview-body">
            <PageContent model={previewModel} published={previewPublished} />
          </div>
        </div>
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  );
}
