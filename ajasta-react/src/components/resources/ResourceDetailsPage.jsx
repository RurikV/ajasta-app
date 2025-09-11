import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';

const ResourceDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [resource, setResource] = useState(null);
  const { ErrorDisplay, showError } = useError();

  useEffect(() => {
    const fetchResource = async () => {
      try {
        const response = await ApiService.getResourceById(id);
        if (response.statusCode === 200) {
          setResource(response.data);
        } else {
          showError(response.message || 'Failed to load resource');
        }
      } catch (error) {
        showError(error.response?.data?.message || error.message);
      }
    };
    fetchResource();
  }, [id]);

  if (!resource) {
    return (
      <div className="menu-page">
        <ErrorDisplay />
        <p>Loading resource...</p>
      </div>
    );
  }

  return (
    <div className="menu-details-page">
      <ErrorDisplay />

      <div className="menu-details-container">
        {resource.imageUrl && (
          <div className="menu-details-image">
            <img src={resource.imageUrl} alt={resource.name} />
          </div>
        )}
        <div className="menu-details-content">
          <h1>{resource.name}</h1>
          <p className="menu-details-description">{resource.description}</p>
          <p><strong>Type:</strong> {resource.type?.replaceAll('_', ' ')}</p>
          {resource.location && <p><strong>Location:</strong> {resource.location}</p>}
          <p><strong>Status:</strong> {resource.active ? 'Active' : 'Inactive'}</p>

          <div style={{ marginTop: '16px' }}>
            <button className="menu-search-button" onClick={() => navigate(`/resources/${id}/book`)}>
              Book this resource
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResourceDetailsPage;
