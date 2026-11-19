import { Fragment, useState } from 'react';
import type { PageModel } from '../types';

interface Props {
  model: PageModel | null;
  published: boolean;
}

const CONTACT_LABELS: Record<string, string> = {
  email: '邮箱',
  social: '社交账号',
  link: '链接',
};

export default function PageContent({ model, published }: Props) {
  const [copied, setCopied] = useState<string | null>(null);

  const profile = model?.profile ?? null;
  const headerVisible = Boolean(profile && (profile.avatarUrl || profile.title || profile.subtitle));
  const bioVisible = Boolean(profile && (profile.bio || profile.images.length > 0));
  const infoVisible = Boolean(model && model.infoLines.length > 0);
  const honorsVisible = Boolean(model && model.honors.length > 0);
  const contactsVisible = Boolean(model && model.contacts.length > 0);
  const profileVisible = headerVisible || bioVisible;

  const nav = [
    profileVisible ? { id: 'profile', label: '个人简介' } : null,
    infoVisible ? { id: 'info', label: '个人信息' } : null,
    honorsVisible ? { id: 'honors', label: '曾获荣誉' } : null,
    contactsVisible ? { id: 'contacts', label: '联系方式' } : null,
  ].filter((item): item is { id: string; label: string } => item !== null);

  if (!published || nav.length === 0) {
    return (
      <div className="empty-page">
        <div className="empty-mark" aria-hidden="true">
          羽
        </div>
        <p>内容尚未发布</p>
      </div>
    );
  }

  async function copy(text: string, key: string) {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      const area = document.createElement('textarea');
      area.value = text;
      document.body.appendChild(area);
      area.select();
      document.execCommand('copy');
      document.body.removeChild(area);
    }
    setCopied(key);
    window.setTimeout(() => setCopied((current) => (current === key ? null : current)), 1600);
  }

  return (
    <div className="page-shell">
      {nav.length >= 2 && (
        <nav className="site-nav" aria-label="页面导航">
          {nav.map((item) => (
            <a key={item.id} href={`#${item.id}`}>
              {item.label}
            </a>
          ))}
        </nav>
      )}

      <main className="page-content">
        {profileVisible && (
          <section id="profile" className="block block-profile">
            {headerVisible && (
              <header className="profile-head">
                {profile?.avatarUrl && (
                  <img
                    className="avatar"
                    src={profile.avatarUrl}
                    alt={profile.title ?? '头像'}
                    onError={(event) => {
                      event.currentTarget.style.display = 'none';
                    }}
                  />
                )}
                <div className="profile-title">
                  {profile?.title && <h1>{profile.title}</h1>}
                  {profile?.title && profile?.subtitle && <p>{profile.subtitle}</p>}
                </div>
              </header>
            )}

            {bioVisible && (
              <div className="bio">
                {profile?.bio &&
                  profile.bio
                    .split(/\n\s*\n/)
                    .map((paragraph) => paragraph.trim())
                    .filter(Boolean)
                    .map((paragraph, index) => (
                      <p key={index}>
                        {paragraph.split('\n').map((line, lineIndex) => (
                          <Fragment key={lineIndex}>
                            {lineIndex > 0 && <br />}
                            {line}
                          </Fragment>
                        ))}
                      </p>
                    ))}
                {profile && profile.images.length > 0 && (
                  <div className="gallery">
                    {profile.images.map((image) => (
                      <img
                        key={image.url}
                        src={image.url}
                        alt=""
                        loading="lazy"
                        onError={(event) => {
                          event.currentTarget.style.display = 'none';
                        }}
                      />
                    ))}
                  </div>
                )}
              </div>
            )}
          </section>
        )}

        {infoVisible && (
          <section id="info" className="block">
            <h2 className="block-title">个人信息</h2>
            <ul className="info-list">
              {model?.infoLines.map((line, index) => (
                <li key={index}>
                  {line.label && (
                    <>
                      <span className="info-label">{line.label}</span>
                      <span className="info-colon">：</span>
                    </>
                  )}
                  <span>{line.value}</span>
                </li>
              ))}
            </ul>
          </section>
        )}

        {honorsVisible && (
          <section id="honors" className="block">
            <h2 className="block-title">曾获荣誉</h2>
            <ul className="honor-list">
              {model?.honors.map((honor, index) => (
                <li key={index}>
                  <span>{honor.name}</span>
                  <time dateTime={honor.awardedOn}>{honor.awardedOn}</time>
                </li>
              ))}
            </ul>
          </section>
        )}

        {contactsVisible && (
          <section id="contacts" className="block">
            <h2 className="block-title">联系方式</h2>
            <ul className="contact-list">
              {model?.contacts.map((contact, index) => {
                const key = `${contact.type}-${index}`;
                return (
                  <li key={key}>
                    <span className="contact-type">{CONTACT_LABELS[contact.type] ?? contact.type}</span>
                    {contact.type === 'email' && contact.target ? (
                      <a href={`mailto:${contact.target}`}>{contact.displayText}</a>
                    ) : contact.type === 'link' && contact.target ? (
                      <a href={contact.target} target="_blank" rel="noreferrer">
                        {contact.displayText}
                      </a>
                    ) : (
                      <button type="button" onClick={() => copy(contact.displayText, key)}>
                        {contact.displayText}
                      </button>
                    )}
                    {copied === key && <span className="copy-tip">已复制</span>}
                  </li>
                );
              })}
            </ul>
          </section>
        )}
      </main>
    </div>
  );
}
