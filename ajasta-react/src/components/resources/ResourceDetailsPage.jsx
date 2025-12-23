import { useEffect, useState, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';

const ResourceDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [resource, setResource] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [averageRating, setAverageRating] = useState(null);
  const [canReview, setCanReview] = useState(false);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { ErrorDisplay, showError } = useError();

  const numericId = useMemo(() => Number(id), [id]);

  const loadResource = async () => {
    try {
      const response = await ApiService.getResourceById(numericId);
      if (response.statusCode === 200) {
        setResource(response.data);
      } else {
        showError(response.message || 'Failed to load resource');
      }
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  const loadReviews = async () => {
    try {
      const [listResp, avgResp] = await Promise.all([
        ApiService.getResourceReviews(numericId),
        ApiService.getResourceAverageRating(numericId)
      ]);
      if (listResp.statusCode === 200) setReviews(listResp.data || []);
      if (avgResp.statusCode === 200) setAverageRating(avgResp.data ?? 0);
    } catch (e) {
      // non-fatal for page
    }
  };

  const loadEligibility = async () => {
    try {
      if (!ApiService.isAuthenticated()) { setCanReview(false); return; }
      const resp = await ApiService.getReviewEligibility(numericId);
      if (resp.statusCode === 200) setCanReview(!!resp.data);
    } catch (e) {
      setCanReview(false);
    }
  };

  useEffect(() => {
    loadResource();
    loadReviews();
    loadEligibility();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numericId]);

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (submitting) return;
    try {
      setSubmitting(true);
      const payload = { resourceId: numericId, rating: Number(rating), comment: comment?.trim() || undefined };
      const resp = await ApiService.createReview(payload);
      if (resp.statusCode === 200) {
        setComment('');
        setRating(5);
        setCanReview(false); // prevent another review
        await loadReviews();
      } else {
        showError(resp.message || 'Failed to submit review');
      }
    } catch (e) {
      showError(e.response?.data?.message || e.message);
    } finally {
      setSubmitting(false);
    }
  };

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

          {/* Reviews Summary */}
          <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #eee' }}>
            <h2 style={{ marginBottom: 8 }}>Reviews</h2>
            <div style={{ marginBottom: 12, color: '#555' }}>
              <strong>Average rating:</strong>{' '}
              {averageRating != null ? `${averageRating.toFixed(1)} / 10` : 'No ratings yet'}
            </div>

            {/* Leave a review (only if eligible) */}
            {canReview && (
              <form onSubmit={handleSubmitReview} style={{ marginBottom: 20, background: '#fafafa', padding: 12, borderRadius: 6, border: '1px solid #eee' }}>
                <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 8 }}>
                  <label htmlFor="rating"><strong>Your rating:</strong></label>
                  <input
                    id="rating"
                    type="number"
                    min={1}
                    max={10}
                    value={rating}
                    onChange={(e) => setRating(Number(e.target.value))}
                    style={{ width: 80 }}
                    required
                  />
                  <span>/ 10</span>
                </div>
                <div style={{ marginBottom: 8 }}>
                  <textarea
                    placeholder="Write your comment (optional)"
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    rows={3}
                    style={{ width: '100%', resize: 'vertical' }}
                    maxLength={500}
                  />
                </div>
                <button className="menu-search-button" type="submit" disabled={submitting}>
                  {submitting ? 'Submitting...' : 'Submit review'}
                </button>
              </form>
            )}

            {/* Reviews list */}
            {reviews && reviews.length > 0 ? (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {reviews.map(r => (
                  <li key={r.id} style={{ border: '1px solid #eee', borderRadius: 6, padding: 12, marginBottom: 10 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                      <strong>{r.userName || 'Anonymous'}</strong>
                      <span style={{ color: '#777' }}>{r.createdAt ? new Date(r.createdAt).toLocaleString() : ''}</span>
                    </div>
                    <div style={{ marginBottom: 6 }}>Rating: <strong>{r.rating}</strong> / 10</div>
                    {r.comment && <div style={{ whiteSpace: 'pre-wrap' }}>{r.comment}</div>}
                  </li>
                ))}
              </ul>
            ) : (
              <div style={{ color: '#777' }}>No reviews yet.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResourceDetailsPage;
