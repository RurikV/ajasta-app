import React, { useEffect, useState } from 'react';
import ApiService from '../../services/ApiService';

// Minimal renderer for CMS components: supports text and image
function CmsRenderer({ components }) {
  if (!Array.isArray(components)) return null;
  return (
    <div className="cms-page">
      {components.map((c, idx) => {
        switch (c.type) {
          case 'text': {
            const Tag = c.props?.tag || 'p';
            const style = c.props?.align ? { textAlign: c.props.align } : undefined;
            return <Tag key={idx} style={style}>{c.props?.text}</Tag>;
          }
          case 'image': {
            const { url, alt, width, height } = c.props || {};
            return <img key={idx} src={url} alt={alt || ''} width={width} height={height} style={{ maxWidth: '100%', height: 'auto' }} />;
          }
          default:
            return <div key={idx} className="cms-unknown">Unknown component: {String(c.type)}</div>;
        }
      })}
    </div>
  );
}

const CmsPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const resp = await fetch(`${ApiService.CMS_BASE_URL}/pages/home`);
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        const json = await resp.json();
        if (alive) setData(json);
      } catch (e) {
        if (alive) setError(e?.message || 'Failed to load');
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();
    return () => { alive = false; };
  }, []);

  if (loading) return <div className="container"><p>Loading CMS…</p></div>;
  if (error) return <div className="container"><p style={{ color: 'red' }}>CMS error: {error}</p></div>;

  return (
    <div className="container">
      <h1>{data?.title || 'CMS'}</h1>
      <CmsRenderer components={data?.components} />
    </div>
  );
};

export default CmsPage;
