import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';

const AdminResourceFormPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { ErrorDisplay, showError } = useError();

  const [resource, setResource] = useState({
    name: '',
    type: '',
    location: '',
    description: '',
    imageFile: null,
    active: true
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (id) {
      fetchResource();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const fetchResource = async () => {
    try {
      const response = await ApiService.getResourceById(id);
      if (response.statusCode === 200) {
        setResource({
          ...response.data,
          type: response.data.type || '',
          active: !!response.data.active,
          imageFile: null
        });
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setResource(prev => ({ ...prev, [name]: value }));
  };

  const handleCheckboxChange = (e) => {
    const { name, checked } = e.target;
    setResource(prev => ({ ...prev, [name]: checked }));
  };

  const handleFileChange = (e) => {
    setResource(prev => ({ ...prev, imageFile: e.target.files[0] }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);

    try {
      const formData = new FormData();
      if (resource.name) formData.append('name', resource.name);
      if (resource.type) formData.append('type', resource.type);
      if (resource.location) formData.append('location', resource.location);
      if (resource.description) formData.append('description', resource.description);
      if (resource.active !== undefined && resource.active !== null) formData.append('active', resource.active);
      if (resource.imageFile) formData.append('imageFile', resource.imageFile);

      let response;
      if (id) {
        formData.append('id', id);
        response = await ApiService.updateResource(formData);
      } else {
        response = await ApiService.addResource(formData);
      }

      if (response.statusCode === 200) {
        navigate('/admin/resources');
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="admin-form-page">
      <ErrorDisplay />
      <h1>{id ? 'Edit Resource' : 'Add Resource'}</h1>

      <form onSubmit={handleSubmit} className="admin-form">
        <div className="form-group">
          <label htmlFor="name">Name</label>
          <input id="name" type="text" name="name" value={resource.name} onChange={handleInputChange} required />
        </div>

        <div className="form-group">
          <label htmlFor="type">Type</label>
          <select id="type" name="type" value={resource.type} onChange={handleInputChange} required>
            <option value="">Select type</option>
            <option value="TURF_COURT">Turf Court</option>
            <option value="VOLLEYBALL_COURT">Volleyball Court</option>
            <option value="PLAYGROUND">Playground</option>
            <option value="HAIRDRESSING_CHAIR">Hairdressing Chair</option>
            <option value="OTHER">Other</option>
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="location">Location</label>
          <input id="location" type="text" name="location" value={resource.location} onChange={handleInputChange} />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description</label>
          <textarea id="description" name="description" value={resource.description} onChange={handleInputChange} rows={4} />
        </div>

        <div className="form-group">
          <label htmlFor="imageFile">Image</label>
          <input id="imageFile" type="file" accept="image/*" onChange={handleFileChange} />
        </div>

        <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label htmlFor="active">Active</label>
          <input id="active" type="checkbox" name="active" checked={!!resource.active} onChange={handleCheckboxChange} />
        </div>

        <button type="submit" className="submit-btn" disabled={isSubmitting}>
          {isSubmitting ? 'Saving...' : id ? 'Update' : 'Create'}
        </button>
      </form>
    </div>
  );
};

export default AdminResourceFormPage;
