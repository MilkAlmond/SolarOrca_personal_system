export const WEEKDAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

export function formatDateTime(value: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

export function formatDate(value: string | null): string {
  if (!value) {
    return '—';
  }
  return value.slice(0, 10);
}

export function formatClock(value: string): string {
  return value?.slice(0, 5) ?? '';
}

export function today(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

export function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}
