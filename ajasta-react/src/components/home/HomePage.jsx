import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ApiService from '../../services/ApiService';

const TYPES = [
  { key: 'TURF_COURT', label: 'Turf Court' },
  { key: 'VOLLEYBALL_COURT', label: 'Volleyball Court' },
  { key: 'PLAYGROUND', label: 'Playground' },
  { key: 'HAIRDRESSING_CHAIR', label: 'Hairdressing Chair' },
  { key: 'OTHER', label: 'Other' },
];

const HomePage = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const [featured, setFeatured] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const resp = await ApiService.getAllResources({ active: true });
        if (mounted && resp.statusCode === 200) {
          // Take first 6 items as featured
          setFeatured((resp.data || []).slice(0, 6));
        }
      } catch (_) {
        // ignore on homepage
      } finally {
        mounted && setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const goSearch = () => {
    const params = new URLSearchParams();
    if (search) params.set('search', search);
    if (type) params.set('type', type);
    params.set('active', 'true');
    navigate(`/resources?${params.toString()}`);
  };

  const heroBg = useMemo(() => ({
    background: 'linear-gradient(135deg, rgba(0,0,0,0.55), rgba(0,0,0,0.25)), url(https://images.unsplash.com/photo-1520975657283-cd98dfd912e7?q=80&w=2070&auto=format&fit=crop) center/cover no-repeat',
  }), []);

  return (
    <div className="home-page" style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>
      {/* Hero section */}
      <section className="hero" style={{ ...heroBg, color: 'white', padding: '80px 16px' }}>
        <div style={{ maxWidth: 1100, margin: '0 auto', textAlign: 'center' }}>
          <h1 style={{ fontSize: 42, lineHeight: 1.2, margin: 0 }}>Find and book resources with ease</h1>
          <p style={{ opacity: 0.95, marginTop: 12, fontSize: 18 }}>
            Explore courts, chairs, playgrounds and more. Check availability and book in minutes.
          </p>

          {/* Search bar */}
          <div className="menu-search home-search" style={{ marginTop: 24, display: 'flex', gap: 10, flexWrap: 'wrap', justifyContent: 'center' }}>
            <input
              type="text"
              placeholder={t('search_resources') || 'Search resources ...'}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="menu-search-input"
              style={{ minWidth: 260, background: 'rgba(255,255,255,0.95)' }}
            />
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="menu-search-input"
              style={{ minWidth: 220, background: 'rgba(255,255,255,0.95)' }}
            >
              <option value="">All Types</option>
              {TYPES.map(ti => (
                <option key={ti.key} value={ti.key}>{ti.label}</option>
              ))}
            </select>
            <button className="menu-search-button btn-cta" onClick={goSearch}>
              Browse Resources
            </button>
          </div>
        </div>
      </section>

      {/* Quick types */}
      <section style={{ maxWidth: 1100, margin: '0 auto', padding: '0 16px' }}>
        <h2 style={{ marginBottom: 12 }}>Explore by type</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
          {TYPES.slice(0,4).map(ti => (
            <div
              key={ti.key}
              onClick={() => navigate(`/resources?type=${encodeURIComponent(ti.key)}&active=true`)}
              style={{ cursor: 'pointer', background: '#f7f7f7', padding: 16, borderRadius: 8, border: '1px solid #eee', display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 80 }}
              className="quick-type-card"
            >
              <span style={{ fontWeight: 600 }}>{ti.label}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Featured resources */}
      <section style={{ maxWidth: 1100, margin: '0 auto', padding: '0 16px 24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
          <h2 style={{ margin: 0 }}>Featured resources</h2>
          <button className="menu-search-button" onClick={() => navigate('/resources')}>View all</button>
        </div>

        {loading ? (
          <p style={{ color: '#777', marginTop: 16 }}>Loading resources...</p>
        ) : featured.length === 0 ? (
          <p style={{ color: '#777', marginTop: 16 }}>No resources available yet.</p>
        ) : (
          <div className="menu-grid" style={{ marginTop: 16 }}>
            {featured.map((r) => (
              <div className="menu-item-card" key={r.id} onClick={() => navigate(`/resources/${r.id}`)}>
                {r.imageUrl ? (
                  <img src={r.imageUrl} alt={r.name} className="menu-item-image" />
                ) : (
                  <div style={{ height: 140, background: '#eaeaea', borderTopLeftRadius: 8, borderTopRightRadius: 8 }} />
                )}
                <div className="menu-item-content">
                  <h3 className="menu-item-name" style={{ marginBottom: 4 }}>{r.name}</h3>
                  <p className="menu-item-description" style={{ marginBottom: 4 }}>{r.location}</p>
                  <p className="menu-item-price" style={{ fontSize: 12 }}>{r.type?.replaceAll('_',' ')}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default HomePage;
