export type ContactType = 'email' | 'social' | 'link';

export interface User {
  id: number;
  username: string;
  displayName: string | null;
  role: 'user' | 'admin';
}

export interface PublicImage {
  id: number;
  url: string;
}

export interface PublicProfile {
  avatarUrl: string | null;
  title: string | null;
  subtitle: string | null;
  bio: string | null;
  images: PublicImage[];
}

export interface PublicHome {
  published: boolean;
  profile: PublicProfile | null;
  infoLines: { label: string | null; value: string }[];
  honors: { name: string; awardedOn: string }[];
  contacts: { type: ContactType; displayText: string; target: string | null }[];
}

export interface AdminImage {
  fileId: number;
  url: string;
}

export interface AdminProfile {
  avatarFileId: number | null;
  avatarUrl: string | null;
  title: string | null;
  subtitle: string | null;
  bio: string | null;
  images: AdminImage[];
}

export interface AdminHome {
  published: boolean;
  version: number;
  profile: AdminProfile | null;
  infoLines: { id: number; label: string | null; value: string }[];
  honors: { id: number; name: string; awardedOn: string }[];
  contacts: {
    id: number;
    type: ContactType;
    displayText: string;
    target: string | null;
    publicVisible: boolean;
  }[];
}

export interface AdminUser {
  id: number;
  username: string;
  displayName: string | null;
  role: 'user' | 'admin';
  status: 'active' | 'disabled';
  lastLoginAt: string | null;
  createdAt: string;
}

export interface PageData<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface PageModel {
  profile: {
    avatarUrl: string | null;
    title: string | null;
    subtitle: string | null;
    bio: string | null;
    images: { url: string }[];
  } | null;
  infoLines: { label: string | null; value: string }[];
  honors: { name: string; awardedOn: string }[];
  contacts: { type: ContactType; displayText: string; target: string | null }[];
}

/* ---------- Workspace (M3/M4) ---------- */

export interface BirthdayItem {
  id: number;
  name: string;
  category: 'birthday' | 'anniversary';
  calendarType: 'solar' | 'lunar';
  solarDate: string | null;
  lunarMonth: number | null;
  lunarDay: number | null;
  lunarLeap: boolean;
  relationTag: string | null;
  advanceDays: number;
  note: string | null;
  nextDate: string;
  daysUntil: number;
  leapAdjusted: boolean;
  version: number;
  updatedAt: string;
}

export interface CourseItem {
  id: number;
  name: string;
  teacher: string | null;
  location: string | null;
  weekday: number;
  startTime: string;
  endTime: string;
  repeatType: 'weekly' | 'once';
  courseDate: string | null;
  effectiveFrom: string | null;
  effectiveTo: string | null;
  remindMinutes: number;
  version: number;
  updatedAt: string;
}

export interface CourseSaveResult {
  course: CourseItem;
  conflicts: CourseItem[];
  reminderNotice: string | null;
}

export interface TaskItem {
  id: number;
  name: string;
  period: 'daily' | 'weekly';
  weeklyTarget: number | null;
  startDate: string;
  remindTime: string;
  enabled: boolean;
  streakDays: number;
  checkedToday: boolean;
  weekProgress: number;
  weekCompleted: boolean;
  milestones: number[];
  version: number;
  updatedAt: string;
}

export interface TaskCheckinResult {
  task: TaskItem;
  newMilestone: number | null;
}

export interface NoteItem {
  id: number;
  title: string;
  body: string;
  category: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface NoteSummaryItem {
  id: number;
  title: string;
  category: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderItem {
  id: number;
  source: 'birthday' | 'course' | 'task' | 'manual';
  sourceId: number | null;
  title: string;
  triggerAt: string;
  advanceMinutes: number;
  enabled: boolean;
  deliveryStatus: 'pending' | 'sent' | 'failed';
  sentAt: string | null;
  failedAt: string | null;
  version: number;
  updatedAt: string;
}

export interface Settings {
  remindersEnabled: boolean;
  version: number;
}

export interface ExportJob {
  id: number;
  status: 'pending' | 'ready' | 'failed' | 'expired';
  createdAt: string;
  completedAt: string | null;
  expiresAt: string | null;
  downloadUrl: string | null;
}

export interface DeviceItem {
  deviceId: string;
  platform: string;
  appVersion: string | null;
  notificationsEnabled: boolean;
  lastSeenAt: string;
  version: number;
}

export interface DeletionStatus {
  frozen: boolean;
  requestedAt: string | null;
  scheduledAt: string | null;
}
