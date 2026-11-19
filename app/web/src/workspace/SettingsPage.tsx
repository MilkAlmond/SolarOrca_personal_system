import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  changePassword,
  createExport,
  deletionStatus,
  downloadExport,
  getSettings,
  latestExport,
  listDevices,
  requestDeletion,
  setDeviceNotifications,
  updateProfile,
  updateSettings,
} from '../api';
import type { DeletionStatus, DeviceItem, ExportJob, Settings, User } from '../types';
import { errorText, formatDateTime } from './format';

interface Props {
  user: User;
  onUserChange: (user: User) => void;
  onSignOut: () => void;
}

const EXPORT_STATUS: Record<ExportJob['status'], string> = {
  pending: '正在生成…',
  ready: '可以下载',
  failed: '生成失败，可稍后重试',
  expired: '下载链接已过期',
};

export default function SettingsPage({ user, onUserChange, onSignOut }: Props) {
  const [settings, setSettings] = useState<Settings | null>(null);
  const [settingsBusy, setSettingsBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [displayName, setDisplayName] = useState(user.displayName ?? '');
  const [profileNotice, setProfileNotice] = useState<string | null>(null);
  const [profileBusy, setProfileBusy] = useState(false);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);
  const [passwordBusy, setPasswordBusy] = useState(false);

  const [job, setJob] = useState<ExportJob | null>(null);
  const [exportBusy, setExportBusy] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const [devices, setDevices] = useState<DeviceItem[]>([]);

  const [deletion, setDeletion] = useState<DeletionStatus | null>(null);
  const [deletionPassword, setDeletionPassword] = useState('');
  const [deletionAck, setDeletionAck] = useState(false);
  const [deletionBusy, setDeletionBusy] = useState(false);
  const [deletionError, setDeletionError] = useState<string | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const [loadedSettings, loadedJob, loadedDevices, loadedDeletion] = await Promise.all([
          getSettings(),
          latestExport(),
          listDevices(),
          deletionStatus(),
        ]);
        setSettings(loadedSettings);
        setJob(loadedJob);
        setDevices(loadedDevices);
        setDeletion(loadedDeletion);
      } catch (ex) {
        setError(errorText(ex, '加载设置失败'));
      }
    })();
  }, []);

  useEffect(() => {
    if (job?.status !== 'pending') {
      return undefined;
    }
    const timer = window.setInterval(() => {
      latestExport()
        .then((latest) => {
          if (latest) {
            setJob(latest);
          }
        })
        .catch(() => undefined);
    }, 3000);
    return () => window.clearInterval(timer);
  }, [job?.status]);

  async function toggleReminders() {
    if (!settings) {
      return;
    }
    setSettingsBusy(true);
    setError(null);
    try {
      setSettings(await updateSettings(!settings.remindersEnabled));
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setSettingsBusy(false);
    }
  }

  async function saveProfile(event: FormEvent) {
    event.preventDefault();
    setProfileBusy(true);
    setProfileNotice(null);
    setError(null);
    try {
      const updated = await updateProfile(displayName.trim() || null);
      onUserChange(updated);
      setDisplayName(updated.displayName ?? '');
      setProfileNotice('显示名已更新。');
    } catch (ex) {
      setError(errorText(ex, '保存失败'));
    } finally {
      setProfileBusy(false);
    }
  }

  async function savePassword(event: FormEvent) {
    event.preventDefault();
    setPasswordBusy(true);
    setPasswordNotice(null);
    setError(null);
    try {
      await changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setPasswordNotice('密码已更新，其他设备已退出登录。');
    } catch (ex) {
      setError(errorText(ex, '修改失败'));
    } finally {
      setPasswordBusy(false);
    }
  }

  async function startExport() {
    setExportBusy(true);
    setExportError(null);
    try {
      setJob(await createExport());
    } catch (ex) {
      setExportError(errorText(ex, '导出失败'));
    } finally {
      setExportBusy(false);
    }
  }

  async function saveArchive(id: number) {
    setExportError(null);
    try {
      await downloadExport(id);
    } catch (ex) {
      setExportError(errorText(ex, '下载失败'));
    }
  }

  async function toggleDevice(device: DeviceItem) {
    try {
      const updated = await setDeviceNotifications(
        device.deviceId,
        !device.notificationsEnabled,
        device.version,
      );
      setDevices((current) => current.map((item) => (item.deviceId === updated.deviceId ? updated : item)));
    } catch (ex) {
      setError(errorText(ex, '操作失败'));
    }
  }

  async function submitDeletion(event: FormEvent) {
    event.preventDefault();
    if (!deletionAck) {
      return;
    }
    setDeletionBusy(true);
    setDeletionError(null);
    try {
      const status = await requestDeletion(deletionPassword);
      setDeletion(status);
      setDeletionPassword('');
      window.alert(`注销申请已提交，账号将于 ${formatDateTime(status.scheduledAt)} 删除。`);
      onSignOut();
    } catch (ex) {
      setDeletionError(errorText(ex, '提交失败'));
    } finally {
      setDeletionBusy(false);
    }
  }

  return (
    <section className="ws-section ws-settings">
      {error && <div className="banner-error">{error}</div>}

      <div className="edit-card">
        <div className="card-head">
          <div>
            <h2>提醒总开关</h2>
            <p>关闭后所有客户端都不再收到提醒，重新打开即恢复。</p>
          </div>
          <button
            className={settings?.remindersEnabled ? 'primary-button' : 'ghost-button'}
            type="button"
            onClick={() => void toggleReminders()}
            disabled={!settings || settingsBusy}
          >
            {settings?.remindersEnabled ? '提醒已开启' : '提醒已关闭'}
          </button>
        </div>
      </div>

      <form className="edit-card" onSubmit={saveProfile}>
        <div className="card-head">
          <div>
            <h2>个人资料</h2>
            <p>显示名会出现在工作台右上角，最多 32 字。</p>
          </div>
        </div>
        <div className="ws-form-grid">
          <label className="field">
            <span>显示名</span>
            <input
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={32}
              placeholder={user.username}
            />
          </label>
        </div>
        {profileNotice && <p className="form-notice">{profileNotice}</p>}
        <div className="ws-actions">
          <button className="primary-button" type="submit" disabled={profileBusy}>
            {profileBusy ? '保存中…' : '保存资料'}
          </button>
        </div>
      </form>

      <form className="edit-card" onSubmit={savePassword}>
        <div className="card-head">
          <div>
            <h2>修改密码</h2>
            <p>修改成功后保留当前设备，其他设备会被退出登录。</p>
          </div>
        </div>
        <div className="ws-form-grid">
          <label className="field">
            <span>当前密码</span>
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          <label className="field">
            <span>新密码</span>
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              autoComplete="new-password"
              minLength={8}
              maxLength={64}
              required
            />
          </label>
        </div>
        {passwordNotice && <p className="form-notice">{passwordNotice}</p>}
        <div className="ws-actions">
          <button className="primary-button" type="submit" disabled={passwordBusy}>
            {passwordBusy ? '提交中…' : '修改密码'}
          </button>
        </div>
      </form>

      <div className="edit-card">
        <div className="card-head">
          <div>
            <h2>数据导出</h2>
            <p>导出五项数据 JSON 与全部图片，24 小时内只能导出一次，链接 7 天有效。</p>
          </div>
          <button
            className="ghost-button"
            type="button"
            onClick={() => void startExport()}
            disabled={exportBusy || job?.status === 'pending'}
          >
            {exportBusy ? '提交中…' : '生成导出'}
          </button>
        </div>
        {exportError && <p className="form-error">{exportError}</p>}
        {job ? (
          <p className="ws-meta">
            任务 #{job.id} · {EXPORT_STATUS[job.status]}
            {job.createdAt && ` · 创建于 ${formatDateTime(job.createdAt)}`}
            {job.expiresAt && ` · 有效至 ${formatDateTime(job.expiresAt)}`}
          </p>
        ) : (
          <p className="ws-meta">还没有导出记录。</p>
        )}
        {job?.status === 'ready' && (
          <div className="ws-actions">
            <button className="primary-button" type="button" onClick={() => void saveArchive(job.id)}>
              下载 ZIP
            </button>
          </div>
        )}
      </div>

      <div className="edit-card">
        <div className="card-head">
          <div>
            <h2>登录设备</h2>
            <p>用于区分提醒回执的设备；关闭通知后该设备不再弹出提醒。</p>
          </div>
        </div>
        {devices.length === 0 ? (
          <p className="ws-meta">暂无已登记设备。</p>
        ) : (
          <div className="ws-list">
            {devices.map((device) => (
              <div key={device.deviceId} className="ws-device">
                <div>
                  <strong>{device.platform}</strong>
                  <p className="ws-meta">
                    {device.deviceId} · {device.appVersion ?? '未知版本'} · 最近活跃{' '}
                    {formatDateTime(device.lastSeenAt)}
                  </p>
                </div>
                <button className="text-button" type="button" onClick={() => void toggleDevice(device)}>
                  {device.notificationsEnabled ? '关闭通知' : '开启通知'}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <form className="edit-card ws-danger" onSubmit={submitDeletion}>
        <div className="card-head">
          <div>
            <h2>注销账号</h2>
            <p>
              提交后账号进入 30 天冻结期，期间无法登录；到期后会永久删除全部数据。
              {deletion?.frozen && ` 当前计划删除时间：${formatDateTime(deletion.scheduledAt)}。`}
            </p>
          </div>
        </div>
        <div className="ws-form-grid">
          <label className="field">
            <span>确认密码</span>
            <input
              type="password"
              value={deletionPassword}
              onChange={(event) => setDeletionPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          <label className="field ws-span-2">
            <span className="ws-check">
              <input
                type="checkbox"
                checked={deletionAck}
                onChange={(event) => setDeletionAck(event.target.checked)}
              />
              我了解 30 天后数据将不可恢复
            </span>
          </label>
        </div>
        {deletionError && <p className="form-error">{deletionError}</p>}
        <div className="ws-actions">
          <button className="primary-button ws-delete-button" type="submit" disabled={!deletionAck || deletionBusy}>
            {deletionBusy ? '提交中…' : '申请注销账号'}
          </button>
        </div>
      </form>
    </section>
  );
}
