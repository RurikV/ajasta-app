import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEdit, faTrash, faPlus } from '@fortawesome/free-solid-svg-icons';

const AdminResourcesPage = () => {
  const [resources, setResources] = useState([]);
  const { ErrorDisplay, showError } = useError();
  const navigate = useNavigate();
  const isAdmin = ApiService.isAdmin();

  const fetchResources = useCallback(async () => {
    try {
      const [resListResp, profileResp] = await Promise.all([
        ApiService.getAllResources(),
        ApiService.myProfile()
      ]);
      const all = resListResp?.statusCode === 200 ? (resListResp.data || []) : [];
      if (isAdmin) {
        setResources(all);
      } else {
        const userId = profileResp?.data?.id;
        const filtered = (userId != null)
          ? all.filter(r => Array.isArray(r.managerIds) && r.managerIds.includes(userId))
          : [];
        setResources(filtered);
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  }, [showError, isAdmin]);

  useEffect(() => {
    fetchResources();
  }, [fetchResources]);

  const handleAdd = () => {
    navigate('/admin/resources/new');
  };

  const handleEdit = (id) => {
    navigate(`/admin/resources/edit/${id}`);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this resource?')) {
      try {
        const response = await ApiService.deleteResource(id);
        if (response.statusCode === 200) {
          fetchResources();
        }
      } catch (error) {
        showError(error.response?.data?.message || error.message);
      }
    }
  };

  return (
    <div className="admin-menu-items">
      <ErrorDisplay />
      <div className="content-header">
        <h1>Resources Management</h1>
        {isAdmin && (
          <button className="add-btn" onClick={handleAdd}>
            <FontAwesomeIcon icon={faPlus} /> Add Resource
          </button>
        )}
      </div>

      <div className="menu-items-grid">
        {resources.map(item => (
          <div className="menu-item-card" key={item.id}>
            <div className="manu-item-image">
              {item.imageUrl && <img src={item.imageUrl} alt={item.name} />}
            </div>
            <div className="item-details">
              <h3>{item.name}</h3>
              <p className="item-description">{item.location}</p>
              <p className="item-description">{item.type?.replaceAll('_', ' ')}</p>
              <div className="item-footer">
                <span className="reviews-count">
                  {item.active ? 'Active' : 'Inactive'}
                </span>
                <div className="item-actions">
                  <button
                    className="edit-btn"
                    onClick={() => handleEdit(item.id)}
                  >
                    <FontAwesomeIcon icon={faEdit} />
                  </button>
                  <button
                    className="delete-btn"
                    onClick={() => handleDelete(item.id)}
                  >
                    <FontAwesomeIcon icon={faTrash} />
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AdminResourcesPage;
