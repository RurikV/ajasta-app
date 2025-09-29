import { useState, useEffect } from 'react';
import ApiService from '../../services/ApiService';
import { useNavigate } from 'react-router-dom';
import { useError } from '../common/ErrorDisplay';

const ResourcesPage = () => {
  const [resources, setResources] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [activeOnly, setActiveOnly] = useState(true);
  const navigate = useNavigate();
  const { ErrorDisplay, showError } = useError();

  const fetchResources = async () => {
    try {
      const params = {};
      if (typeFilter) params.type = typeFilter;
      if (activeOnly !== null) params.active = activeOnly;
      const response = await ApiService.getAllResources(params);
      if (response.statusCode === 200) {
        setResources(response.data || []);
      } else {
        showError(response.message || 'Failed to load resources');
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  useEffect(() => {
    fetchResources();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [typeFilter, activeOnly]);

  const handleSearch = async () => {
    try {
      const params = { search: searchTerm };
      if (typeFilter) params.type = typeFilter;
      if (activeOnly !== null) params.active = activeOnly;
      const response = await ApiService.getAllResources(params);
      if (response.statusCode === 200) {
        setResources(response.data || []);
      } else {
        showError(response.message || 'Failed to search resources');
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const filtered = resources.filter((r) =>
    (r.name || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const navigateToBookResource = (id) => {
    navigate(`/resources/${id}/book`);
  };

  return (
    <div className="menu-page">
      <ErrorDisplay />
      <h1 className="menu-title">Resources</h1>

      <div className="menu-search" style={{ gap: '10px', display: 'flex', flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Search resources ..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="menu-search-input"
        />
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="menu-search-input"
          style={{ maxWidth: '220px' }}
        >
          <option value="">All Types</option>
          <option value="TURF_COURT">Turf Court</option>
          <option value="VOLLEYBALL_COURT">Volleyball Court</option>
          <option value="PLAYGROUND">Playground</option>
          <option value="HAIRDRESSING_CHAIR">Hairdressing Chair</option>
          <option value="OTHER">Other</option>
        </select>
        <label style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <input type="checkbox" checked={!!activeOnly} onChange={(e) => setActiveOnly(e.target.checked ? true : null)} />
          Active Only
        </label>
        <button onClick={handleSearch} className="menu-search-button">Search</button>
      </div>

      <div className="menu-grid">
        {filtered.map((item) => (
          <div
            className="menu-item-card"
            onClick={() => navigateToBookResource(item.id)}
            key={item.id}
          >
            {item.imageUrl && (
              <img src={item.imageUrl} alt={item.name} className="menu-item-image" />
            )}
            <div className="menu-item-content">
              <h2 className="menu-item-name">{item.name}</h2>
              <p className="menu-item-description">{item.location}</p>
              <p className="menu-item-description">{item.description}</p>
              <p className="menu-item-price" style={{ fontSize: '12px' }}>{item.type?.replaceAll('_', ' ')}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ResourcesPage;
