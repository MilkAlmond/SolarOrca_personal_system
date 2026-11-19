import type {
  AdminHome,
  AdminUser,
  BirthdayItem,
  CourseItem,
  CourseSaveResult,
  DeletionStatus,
  DeviceItem,
  ExportJob,
  NoteItem,
  NoteSummaryItem,
  PageData,
  PublicHome,
  ReminderItem,
  Settings,
  TaskCheckinResult,
  TaskItem,
  User,
} from './types';

const API_ORIGIN = (import.meta.env.VITE_API_BASE ?? '').replace(/\/+$/, '');
const BASE = `${API_ORIGIN}/api/v1`;

export function currentDeviceId(): string {
  const key = 'yujing.deviceId';
  let value = window.localStorage.getItem(key);
  if (!value) {
    value = typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    window.localStorage.setItem(key, value);
  }
  return value;
}

const TOKEN_KEY = 'yujing.token';

function isDesktop(): boolean {
  return typeof window !== 'undefined' && '__TAURI_INTERNALS__' in window;
}

function desktopPlatform(): 'web' | 'windows' | 'linux' {
  if (!isDesktop()) {
    return 'web';
  }
  return navigator.userAgent.includes('Windows') ? 'windows' : 'linux';
}

function clientHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'X-Client-Offset-Minutes': String(-new Date().getTimezoneOffset()),
    'X-Device-Id': currentDeviceId(),
    'X-Client-Version': '0.1.0',
  };
  const token = isDesktop() ? window.localStorage.getItem(TOKEN_KEY) : null;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

interface Envelope<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: number;
  readonly fields: Record<string, string>;

  constructor(status: number, code: number, message: string, fields: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.code = code;
    this.fields = fields;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  Object.entries(clientHeaders()).forEach(([key, value]) => headers.set(key, value));
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  const response = await fetch(BASE + path, { ...init, headers, credentials: 'include' });
  let payload: Envelope<T> | null = null;
  try {
    payload = (await response.json()) as Envelope<T>;
  } catch {
    // Non-JSON responses only happen outside the API contract.
  }
  if (!response.ok || !payload || payload.code !== 0) {
    const data = payload?.data as { fields?: Record<string, string> } | undefined;
    throw new ApiError(
      response.status,
      payload?.code ?? -1,
      payload?.message ?? '请求失败，请稍后重试',
      data?.fields ?? {},
    );
  }
  return payload.data;
}

interface AuthResult {
  token: string | null;
  user: User;
}

function keepToken(result: AuthResult): AuthResult {
  if (isDesktop() && result.token) {
    window.localStorage.setItem(TOKEN_KEY, result.token);
  }
  return result;
}

export function login(username: string, password: string): Promise<AuthResult> {
  return request<AuthResult>('/auth/login', {
    method: 'POST',
    headers: { 'X-Client-Platform': desktopPlatform() },
    body: JSON.stringify({ username, password }),
  }).then(keepToken);
}

export function logout(): Promise<null> {
  return request<null>('/auth/logout', { method: 'POST' }).then((result) => {
    window.localStorage.removeItem(TOKEN_KEY);
    return result;
  });
}

export function me(): Promise<User> {
  return request('/auth/me');
}

export function publicHome(): Promise<PublicHome> {
  return request('/public/home');
}

export function adminHome(): Promise<AdminHome> {
  return request('/admin/home');
}

export function saveProfile(body: {
  avatarFileId: number | null;
  title: string | null;
  subtitle: string | null;
  bio: string | null;
  imageFileIds: number[];
  version: number;
}): Promise<AdminHome> {
  return request('/admin/home/profile', { method: 'PUT', body: JSON.stringify(body) });
}

export function saveInfoLines(lines: { label: string | null; value: string }[]): Promise<AdminHome> {
  return request('/admin/home/info-lines', { method: 'PUT', body: JSON.stringify({ lines }) });
}

export function saveHonors(items: { name: string; awardedOn: string }[]): Promise<AdminHome> {
  return request('/admin/home/honors', { method: 'PUT', body: JSON.stringify({ items }) });
}

export function saveContacts(
  items: { type: string; displayText: string; target: string | null; publicVisible: boolean }[],
): Promise<AdminHome> {
  return request('/admin/home/contacts', { method: 'PUT', body: JSON.stringify({ items }) });
}

export function uploadPng(
  file: File,
  purpose: 'avatar' | 'bio',
): Promise<{ id: number; url: string; sizeBytes: number }> {
  const form = new FormData();
  form.append('purpose', purpose);
  form.append('file', file);
  return request('/admin/files', { method: 'POST', body: form });
}

export function listUsers(page: number, keyword: string): Promise<PageData<AdminUser>> {
  const params = new URLSearchParams({ page: String(page), size: '50' });
  if (keyword.trim()) {
    params.set('keyword', keyword.trim());
  }
  return request(`/admin/users?${params.toString()}`);
}

export function setUserStatus(id: number, status: 'active' | 'disabled'): Promise<AdminUser> {
  return request(`/admin/users/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

/* ---------- Workspace ---------- */

export function register(username: string, password: string): Promise<AuthResult> {
  return request<AuthResult>('/auth/register', {
    method: 'POST',
    headers: { 'X-Client-Platform': desktopPlatform() },
    body: JSON.stringify({ username, password }),
  }).then(keepToken);
}

export function updateProfile(displayName: string | null): Promise<User> {
  return request('/auth/me', {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  });
}

export function changePassword(currentPassword: string, newPassword: string): Promise<null> {
  return request('/auth/password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function listBirthdays(): Promise<BirthdayItem[]> {
  return request('/birthdays');
}

export function createBirthday(body: Record<string, unknown>): Promise<BirthdayItem> {
  return request('/birthdays', { method: 'POST', body: JSON.stringify(body) });
}

export function updateBirthday(id: number, body: Record<string, unknown>): Promise<BirthdayItem> {
  return request(`/birthdays/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteBirthday(id: number, version: number): Promise<null> {
  return request(`/birthdays/${id}?version=${version}`, { method: 'DELETE' });
}

export function listCourses(): Promise<CourseItem[]> {
  return request('/courses');
}

export function createCourse(body: Record<string, unknown>): Promise<CourseSaveResult> {
  return request('/courses', { method: 'POST', body: JSON.stringify(body) });
}

export function updateCourse(id: number, body: Record<string, unknown>): Promise<CourseSaveResult> {
  return request(`/courses/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteCourse(id: number, version: number): Promise<null> {
  return request(`/courses/${id}?version=${version}`, { method: 'DELETE' });
}

export function listTasks(): Promise<TaskItem[]> {
  return request('/tasks');
}

export function createTask(body: Record<string, unknown>): Promise<TaskItem> {
  return request('/tasks', { method: 'POST', body: JSON.stringify(body) });
}

export function updateTask(id: number, body: Record<string, unknown>): Promise<TaskItem> {
  return request(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteTask(id: number, version: number): Promise<null> {
  return request(`/tasks/${id}?version=${version}`, { method: 'DELETE' });
}

export function checkinTask(id: number): Promise<TaskCheckinResult> {
  const now = new Date();
  const date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  return request(`/tasks/${id}/checkins`, { method: 'POST', body: JSON.stringify({ date }) });
}

export function undoCheckin(id: number, date: string): Promise<TaskItem> {
  return request(`/tasks/${id}/checkins?date=${date}`, { method: 'DELETE' });
}

export function listNotes(page = 1, keyword = ''): Promise<PageData<NoteSummaryItem>> {
  const params = new URLSearchParams({ page: String(page), size: '50' });
  if (keyword.trim()) {
    params.set('keyword', keyword.trim());
  }
  return request(`/notes?${params.toString()}`);
}

export function getNote(id: number): Promise<NoteItem> {
  return request(`/notes/${id}`);
}

export function createNote(body: Record<string, unknown>): Promise<NoteItem> {
  return request('/notes', { method: 'POST', body: JSON.stringify(body) });
}

export function updateNote(id: number, body: Record<string, unknown>): Promise<NoteItem> {
  return request(`/notes/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function deleteNote(id: number, version: number): Promise<null> {
  return request(`/notes/${id}?version=${version}`, { method: 'DELETE' });
}

export function listReminders(): Promise<ReminderItem[]> {
  return request('/reminders');
}

export function createReminder(title: string, triggerAt: string): Promise<ReminderItem> {
  return request('/reminders', {
    method: 'POST',
    body: JSON.stringify({ title, triggerAt }),
  });
}

export function setReminderEnabled(id: number, enabled: boolean, version: number): Promise<ReminderItem> {
  return request(`/reminders/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ enabled, version }),
  });
}

export function deleteReminder(id: number, version: number): Promise<null> {
  return request(`/reminders/${id}?version=${version}`, { method: 'DELETE' });
}

export function getSettings(): Promise<Settings> {
  return request('/settings');
}

export function updateSettings(remindersEnabled: boolean): Promise<Settings> {
  return request('/settings', {
    method: 'PATCH',
    body: JSON.stringify({ remindersEnabled }),
  });
}

export function createExport(): Promise<ExportJob> {
  return request('/export', { method: 'POST' });
}

export function latestExport(): Promise<ExportJob | null> {
  return request('/export/latest');
}

export async function downloadExport(id: number): Promise<void> {
  const headers = new Headers(clientHeaders());
  const response = await fetch(`${BASE}/export/${id}/download`, { headers, credentials: 'include' });
  if (!response.ok) {
    throw new ApiError(response.status, -1, '下载失败，请重新生成导出');
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `yujing-export-${id}.zip`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export function listDevices(): Promise<DeviceItem[]> {
  return request('/devices');
}

export function registerDevice(appVersion = '0.1.0'): Promise<DeviceItem> {
  return request('/devices', {
    method: 'POST',
    body: JSON.stringify({
      deviceId: currentDeviceId(),
      platform: desktopPlatform(),
      appVersion,
    }),
  });
}

export function setDeviceNotifications(
  deviceId: string,
  notificationsEnabled: boolean,
  version: number,
): Promise<DeviceItem> {
  return request(`/devices/${encodeURIComponent(deviceId)}`, {
    method: 'PATCH',
    body: JSON.stringify({ notificationsEnabled, version }),
  });
}

export function deletionStatus(): Promise<DeletionStatus> {
  return request('/account/deletion');
}

export function requestDeletion(password: string): Promise<DeletionStatus> {
  return request('/account/deletion', {
    method: 'POST',
    body: JSON.stringify({ password }),
  });
}

export function cancelDeletion(username: string, password: string): Promise<null> {
  return request('/account/deletion/cancel', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}
