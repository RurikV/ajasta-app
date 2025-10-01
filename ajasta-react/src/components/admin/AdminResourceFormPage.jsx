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
    pricePerSlot: '', // Price per 30-minute slot
    active: true,
    unitsCount: 1,
    openTime: '08:00',
    closeTime: '20:00',
    unavailableWeekdays: '', // CSV of 0-6 (0=Sun)
    unavailableDates: '', // CSV yyyy-MM-dd
    dailyUnavailableRanges: '', // e.g., 12:00-13:00;16:00-17:00
    managerIds: []
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [managers, setManagers] = useState([]);

  const loadManagers = async () => {
    try {
      const resp = await ApiService.getAllUsers();
      const users = resp?.data || [];
      const mgrs = users.filter(u => Array.isArray(u.roles) && u.roles.some(r => (r.name || '').toUpperCase() === 'RESOURCE_MANAGER'));
      setManagers(mgrs);
    } catch (e) {
      // silently ignore; errors will be surfaced when saving anyway
    }
  };

  useEffect(() => {
    loadManagers();
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
          pricePerSlot: response.data.pricePerSlot || '',
          imageFile: null,
          managerIds: Array.isArray(response.data.managerIds) ? response.data.managerIds : []
        });
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // For pricePerSlot, validate and preserve decimal precision
    if (name === 'pricePerSlot') {
      // Allow digits, dots, and commas with up to 2 decimal places
      if (value === '' || /^\d*[.,]?\d{0,2}$/.test(value)) {
        setResource(prev => ({ ...prev, [name]: value }));
      }
    } else {
      setResource(prev => ({ ...prev, [name]: value }));
    }
  };

  const handleCheckboxChange = (e) => {
    const { name, checked } = e.target;
    setResource(prev => ({ ...prev, [name]: checked }));
  };

  // Weekday helpers for unavailableWeekdays CSV (0=Sun..6=Sat)
  const isWeekdaySelected = (idx) => {
    if (!resource.unavailableWeekdays) return false;
    return resource.unavailableWeekdays.split(',').map(s => s.trim()).filter(Boolean).includes(String(idx));
  };

  const toggleWeekday = (idx) => {
    setResource(prev => {
      const list = (prev.unavailableWeekdays || '')
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);
      const strIdx = String(idx);
      const exists = list.includes(strIdx);
      const next = exists ? list.filter(x => x !== strIdx) : [...list, strIdx];
      return { ...prev, unavailableWeekdays: next.join(',') };
    });
  };

  const handleManagerToggle = (userId) => {
    setResource(prev => {
      const current = new Set(prev.managerIds || []);
      if (current.has(userId)) current.delete(userId); else current.add(userId);
      return { ...prev, managerIds: Array.from(current) };
    });
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
      if (resource.pricePerSlot && resource.pricePerSlot.toString().trim()) {
        // Convert comma to dot for backend compatibility
        const normalizedPrice = resource.pricePerSlot.toString().replace(',', '.');
        formData.append('pricePerSlot', normalizedPrice);
      }
      if (resource.active !== undefined && resource.active !== null) formData.append('active', resource.active);
      if (resource.imageFile) formData.append('imageFile', resource.imageFile);
      if (resource.unitsCount !== undefined && resource.unitsCount !== null) formData.append('unitsCount', String(resource.unitsCount));
      if (resource.openTime) formData.append('openTime', resource.openTime);
      if (resource.closeTime) formData.append('closeTime', resource.closeTime);
      if (resource.unavailableWeekdays !== undefined) formData.append('unavailableWeekdays', resource.unavailableWeekdays);
      if (resource.unavailableDates !== undefined) formData.append('unavailableDates', resource.unavailableDates);
      if (resource.dailyUnavailableRanges !== undefined) formData.append('dailyUnavailableRanges', resource.dailyUnavailableRanges);
      if (Array.isArray(resource.managerIds)) {
        resource.managerIds.forEach(mId => formData.append('managerIds', String(mId)));
      }

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
    <div className="admin-menu-item-form">
      <ErrorDisplay />
      <div className="content-header">
        <h1>{id ? 'Edit Resource Item' : 'Add New Resource Item'}</h1>
        <button
          className="back-btn"
          type="button"
          onClick={() => navigate('/admin/resources')}
        >
          Back to Resource Items
        </button>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Name *</label>
          <input
            type="text"
            id="name"
            name="name"
            value={resource.name}
            onChange={handleInputChange}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="type">Type *</label>
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
          <input
            type="text"
            id="location"
            name="location"
            value={resource.location}
            onChange={handleInputChange}
          />
        </div>

        <div className="form-group">
          <label htmlFor="description">Description</label>
          <textarea
            id="description"
            name="description"
            value={resource.description}
            onChange={handleInputChange}
            rows="4"
          />
        </div>

        <div className="form-group">
          <label htmlFor="pricePerSlot">Price per 30-minute slot (€)</label>
          <input
            type="text"
            id="pricePerSlot"
            name="pricePerSlot"
            value={resource.pricePerSlot}
            onChange={handleInputChange}
            placeholder="Enter price (e.g., 25.00)"
            pattern="^\d*[.,]?\d{0,2}$"
            title="Please enter a valid price with up to 2 decimal places (use . or , as decimal separator)"
          />
        </div>

        <div className="form-group">
          <label htmlFor="imageFile">{id ? 'Change Image (Leave blank to keep current)' : 'Image *'}</label>
          <input
            type="file"
            id="imageFile"
            name="imageFile"
            onChange={handleFileChange}
            accept="image/*"
            required={!id}
          />
          {id && resource.imageUrl && (
            <div className="current-image-preview">
              <p>Current Image:</p>
              <img src={resource.imageUrl} alt="Current resource" className="preview-image" />
            </div>
          )}
        </div>

        <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label htmlFor="active">Active</label>
          <input id="active" type="checkbox" name="active" checked={!!resource.active} onChange={handleCheckboxChange} />
        </div>

        <div className="form-row">
          <div className="form-group">
            <label htmlFor="unitsCount">Units Count</label>
            <input
              type="number"
              id="unitsCount"
              name="unitsCount"
              min="1"
              value={resource.unitsCount}
              onChange={handleInputChange}
            />
          </div>

          <div className="form-group">
            <label htmlFor="openTime">Open Time</label>
            <input
              type="time"
              id="openTime"
              name="openTime"
              value={resource.openTime || ''}
              onChange={handleInputChange}
            />
          </div>

          <div className="form-group">
            <label htmlFor="closeTime">Close Time</label>
            <input
              type="time"
              id="closeTime"
              name="closeTime"
              value={resource.closeTime || ''}
              onChange={handleInputChange}
            />
          </div>
        </div>

        <div className="form-group">
          <label>Unavailable Weekdays</label>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {['Sun','Mon','Tue','Wed','Thu','Fri','Sat'].map((d, idx) => (
              <label key={idx} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <input
                  type="checkbox"
                  checked={isWeekdaySelected(idx)}
                  onChange={() => toggleWeekday(idx)}
                />
                {d}
              </label>
            ))}
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="unavailableDates">Unavailable Dates (CSV yyyy-MM-dd)</label>
          <textarea
            id="unavailableDates"
            name="unavailableDates"
            placeholder="2025-12-24,2025-12-25"
            rows="2"
            value={resource.unavailableDates}
            onChange={handleInputChange}
          />
        </div>

        <div className="form-group">
          <label htmlFor="dailyUnavailableRanges">Daily Unavailable Time Ranges (semicolon separated HH:mm-HH:mm)</label>
          <textarea
            id="dailyUnavailableRanges"
            name="dailyUnavailableRanges"
            placeholder="12:00-13:00;16:00-17:00"
            rows="2"
            value={resource.dailyUnavailableRanges}
            onChange={handleInputChange}
          />
        </div>
        
        <div className="form-group">
          <label>Resource Managers</label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {managers.length === 0 ? (
              <small style={{ color: '#666' }}>No resource managers available</small>
            ) : (
              managers.map(m => (
                <label key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 6, border: '1px solid #eee', borderRadius: 8, padding: '6px 10px' }}>
                  <input
                    type="checkbox"
                    checked={(resource.managerIds || []).includes(m.id)}
                    onChange={() => handleManagerToggle(m.id)}
                  />
                  <span>{m.name || m.email}</span>
                  {m.email && <span style={{ color: '#999', fontSize: 12 }}>({m.email})</span>}
                </label>
              ))
            )}
          </div>
        </div>
        
        <div className="form-actions">
          <button
            type="submit"
            className="save-btn"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Saving...' : 'Save Resource Item'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AdminResourceFormPage;
