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
