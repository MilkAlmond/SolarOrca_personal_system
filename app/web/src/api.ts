import type { AdminHome, AdminUser, PageData, PublicHome, User } from './types';

const BASE = '/api/v1';

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

export function login(username: string, password: string): Promise<{ user: User }> {
  return request('/auth/login', {
    method: 'POST',
    headers: { 'X-Client-Platform': 'web' },
    body: JSON.stringify({ username, password }),
  });
}

export function logout(): Promise<null> {
  return request('/auth/logout', { method: 'POST' });
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
