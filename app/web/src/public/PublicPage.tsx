import { useCallback, useEffect, useState } from 'react';
import { publicHome } from '../api';
import type { PublicHome } from '../types';
import PageContent from './PageContent';

export default function PublicPage() {
  const [data, setData] = useState<PublicHome | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');

  const load = useCallback(() => {
    setStatus('loading');
    publicHome()
      .then((home) => {
        setData(home);
        setStatus('ready');
      })
      .catch(() => setStatus('error'));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (status === 'loading') {
    return (
      <div className="page-shell">
        <div className="empty-page">
          <p className="muted">加载中…</p>
        </div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="page-shell">
        <div className="empty-page">
          <p>内容加载失败，请稍后重试</p>
          <button type="button" className="ghost-button" onClick={load}>
            重新加载
          </button>
        </div>
      </div>
    );
  }

  const model = data
    ? {
        profile: data.profile
          ? {
              avatarUrl: data.profile.avatarUrl,
              title: data.profile.title,
              subtitle: data.profile.subtitle,
              bio: data.profile.bio,
              images: data.profile.images.map((image) => ({ url: image.url })),
            }
          : null,
        infoLines: data.infoLines,
        honors: data.honors,
        contacts: data.contacts,
      }
    : null;

  return <PageContent model={model} published={data?.published ?? false} />;
}
