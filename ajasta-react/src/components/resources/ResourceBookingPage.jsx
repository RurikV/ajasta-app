import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';

// Utility: parse HH:mm to minutes from midnight
const parseTimeToMinutes = (hhmm) => {
  if (!hhmm) return null;
  const [h, m] = hhmm.split(':').map(Number);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  return h * 60 + m;
};

// Utility: format minutes from midnight to HH:mm
const minutesToHHMM = (mins) => {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
};

// Utility: get local date as yyyy-MM-dd (not UTC-based)
const getLocalDateStr = (d = new Date()) => {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const isTimeInRanges = (timeHHMM, rangesStr) => {
  if (!rangesStr || !timeHHMM) return false;
  const timeMins = parseTimeToMinutes(timeHHMM);
  return rangesStr
    .split(';')
    .map(s => s.trim())
    .filter(Boolean)
    .some(r => {
      const [from, to] = r.split('-').map(x => x?.trim());
      const fromM = parseTimeToMinutes(from);
      const toM = parseTimeToMinutes(to);
      if (fromM == null || toM == null) return false;
      return timeMins >= fromM && timeMins < toM;
    });
};

const ResourceBookingPage = () => {
  const { id } = useParams();
  const { ErrorDisplay, showError } = useError();
  const [resource, setResource] = useState(null);
  const [date, setDate] = useState(() => getLocalDateStr());
  const [bookingSuccess, setBookingSuccess] = useState(false);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state (single selection)
  const [selected, setSelected] = useState(() => new Set());

  const isAuthenticated = ApiService.isAuthenticated();

  useEffect(() => {
    const fetchResource = async () => {
      try {
        const resp = await ApiService.getResourceById(id);
        if (resp.statusCode === 200) {
          setResource(resp.data);
        } else {
          showError(resp.message || 'Failed to load resource');
        }
      } catch (e) {
        showError(e.response?.data?.message || e.message);
      }
    };
    fetchResource();
  }, [id, showError]);

  const slots = useMemo(() => {
    if (!resource) return [];
    const open = resource.openTime || '08:00';
    const close = resource.closeTime || '20:00';
    const start = parseTimeToMinutes(open);
    const end = parseTimeToMinutes(close);
    if (start == null || end == null || end <= start) return [];
    const arr = [];
    for (let t = start; t < end; t += 30) {
      arr.push(minutesToHHMM(t));
    }
    return arr;
  }, [resource]);

  const weekdayIndex = useMemo(() => {
    try {
      const dt = new Date(date + 'T00:00:00');
      return dt.getDay(); // 0=Sun..6=Sat
    } catch {
      return null;
    }
  }, [date]);

  const isDateUnavailable = (dateStr) => {
    if (!resource) return false;
    // Weekday unavailability
    if (resource.unavailableWeekdays) {
      const list = resource.unavailableWeekdays.split(',').map(s => s.trim()).filter(Boolean);
      if (weekdayIndex != null && list.includes(String(weekdayIndex))) return true;
    }
    // Specific dates
    if (resource.unavailableDates) {
      const dates = resource.unavailableDates.split(',').map(s => s.trim()).filter(Boolean);
      if (dates.includes(dateStr)) return true;
    }
    return false;
  };

  const isSlotUnavailable = (timeHHMM) => {
    if (!resource) return false;

    // If selected date is unavailable by config
    if (isDateUnavailable(date)) return true;

    // Disable all slots for past dates
    const todayStr = getLocalDateStr();
    if (date < todayStr) return true;

    // For today, disable slots that start before current local time
    if (date === todayStr) {
      const now = new Date();
      const nowMinutes = now.getHours() * 60 + now.getMinutes();
      const slotStart = parseTimeToMinutes(timeHHMM);
      if (slotStart != null && slotStart < nowMinutes) return true;
    }

    // Daily unavailable time ranges
    return !!(resource.dailyUnavailableRanges && isTimeInRanges(timeHHMM, resource.dailyUnavailableRanges));
  };

  // If today has no available slots, automatically move to the next day
  useEffect(() => {
    if (!resource) return;
    const todayStr = getLocalDateStr();
    if (date !== todayStr) return;
    const anyAvailable = slots.some((time) => !isSlotUnavailable(time));
    if (!anyAvailable) {
      const next = new Date(todayStr + 'T00:00:00');
      next.setDate(next.getDate() + 1);
      setDate(getLocalDateStr(next));
    }
  }, [resource, date, slots, isSlotUnavailable]);

  const handleSelectClick = (key, disabled) => {
    if (disabled) return;
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key); // toggle off
      } else {
        next.add(key); // allow multi-select
      }
      return next;
    });
  };

  const handleBookResource = async () => {
    if (!isAuthenticated) {
      showError("Please login to continue, If you don't have an account do well to register");
      setTimeout(() => {
        // Navigate to login if needed
      }, 5000);
      return;
    }

    if (selected.size === 0) {
      showError("Please select at least one time slot to book");
      return;
    }

    const keys = Array.from(selected);
    const byDate = new Map();
    for (const key of keys) {
      const [d, time, unit] = key.split('_');
      const slot = {
        startTime: time,
        endTime: minutesToHHMM(parseTimeToMinutes(time) + 30),
        unit: parseInt(unit, 10)
      };
      if (!byDate.has(d)) byDate.set(d, []);
      byDate.get(d).push(slot);
    }

    setBookingSuccess(false);
    setSuccessMessage(null);
    try {
      let response;
      if (byDate.size === 1) {
        const [[onlyDate, slotsPayload]] = Array.from(byDate.entries());
        const body = { date: onlyDate, slots: slotsPayload };
        response = await ApiService.bookResourceBatch(resource.id, body);
      } else {
        const days = Array.from(byDate.entries()).map(([d, slots]) => ({ date: d, slots }));
        const body = { days };
        response = await ApiService.bookResourceMulti(resource.id, body);
      }

      if (response.statusCode !== 200) {
        showError(response.message || 'Failed to book selected slots');
        return;
      }

      const count = keys.length;
      setBookingSuccess(true);
      setSuccessMessage(response.message || `Booked ${count} slot(s) successfully!`);
      setSelected(new Set());
      setTimeout(() => {
        setBookingSuccess(false);
        setSuccessMessage(null);
        if (typeof window !== 'undefined' && window.location) {
          window.location.assign('/my-order-history');
        }
      }, 5000);
    } catch (error) {
      showError(error.response?.data?.message || error.message);
    }
  };

  if (!resource) {
    return (
      <div className="menu-page">
        <ErrorDisplay />
        <p>Loading scheduler...</p>
      </div>
    );
  }

  const units = Math.max(1, Number(resource.unitsCount || 1));
  const unitCols = Array.from({ length: units }, (_, i) => i + 1);

  // Pricing: price per 30-min slot comes from resource item defined in admin (EUR by default)
  const pricePerSlot = Number(resource?.pricePerSlot ?? 0);
  const currency = resource?.currency || 'EUR';
  const formatMoney = (amount) => {
    const value = Number(amount) || 0;
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(value);
    } catch (e) {
      // Fallback if Intl or currency code fails
      return `${value.toFixed(2)} ${currency}`;
    }
  };

  return (
    <div className="menu-page">
      <ErrorDisplay />
      <h1 className="menu-title">Book: {resource.name}</h1>

      <div className="menu-search" style={{ gap: '10px', display: 'flex', flexWrap: 'wrap' }}>
        <label>
          Select date:
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} style={{ marginLeft: 8 }} />
        </label>
      </div>

      {/* Summary Price and Booking Section - Always visible but inactive when no slots selected */}
      <div className="add-to-cart-section" style={{ 
        marginTop: 20, 
        padding: 16, 
        border: '1px solid #ddd', 
        borderRadius: 8, 
        backgroundColor: selected.size > 0 ? '#f9f9f9' : '#f5f5f5',
        opacity: selected.size > 0 ? 1 : 0.6
      }}>
        <h3>Booking Summary</h3>
        <div className="booking-details">
          <p>Booked slots: {selected.size}</p>
          <p>Price per slot: {formatMoney(pricePerSlot)}</p>
          <div className="total-price" style={{ fontSize: 18, fontWeight: 'bold', marginTop: 8 }}>
            Total: {formatMoney(selected.size * pricePerSlot)}
          </div>
        </div>
        <button
          onClick={handleBookResource}
          className="add-to-cart-btn"
          disabled={selected.size === 0}
          style={{ 
            marginTop: 12, 
            padding: '12px 24px', 
            backgroundColor: selected.size > 0 ? '#007bff' : '#cccccc', 
            color: selected.size > 0 ? 'white' : '#666666', 
            border: 'none', 
            borderRadius: 4, 
            cursor: selected.size > 0 ? 'pointer' : 'not-allowed' 
          }}
        >
          Book Slots
        </button>
        {bookingSuccess && (
          <div className="cart-success-message" style={{ marginTop: 12, padding: 8, backgroundColor: '#d4edda', color: '#155724', borderRadius: 4 }}>
            {successMessage || 'Booking confirmed successfully!'}
          </div>
        )}
      </div>

      <div className="scheduler-table-wrapper" style={{ overflowX: 'auto', marginTop: 16 }}>
        <table className="scheduler-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', padding: '8px' }}>Time</th>
              {unitCols.map((n) => (
                <th key={n} style={{ textAlign: 'center', padding: '8px' }}>Unit {n}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {slots.map((time) => {
              const disabledRow = isSlotUnavailable(time);
              return (
                <tr key={time}>
                  <td style={{ padding: '6px 8px', fontWeight: 500 }}>{time}</td>
                  {unitCols.map((n) => {
                    const key = `${date}_${time}_${n}`;
                    const isSelected = selected.has(key);
                    const disabled = disabledRow;
                    return (
                      <td
                        key={`${time}-${n}`}
                        data-testid={`slot-${time}-${n}`}
                        onClick={() => handleSelectClick(key, disabled)}
                        style={{
                          padding: 8,
                          textAlign: 'center',
                          border: '1px solid #eee',
                          backgroundColor: disabled ? '#f0f0f0' : (isSelected ? 'lightgreen' : 'transparent'),
                          color: disabled ? '#888' : 'inherit',
                          cursor: disabled ? 'not-allowed' : 'pointer',
                          userSelect: 'none'
                        }}
                        title={disabled ? 'Unavailable' : 'Available'}
                        aria-disabled={disabled}
                      >
                        {/* visual slot */}
                        {isSelected ? (
                          <span style={{ fontSize: 12, color: '#555' }}>
                            {time} - {minutesToHHMM(parseTimeToMinutes(time) + 30)}
                          </span>
                        ) : ''}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div style={{ marginTop: 12, fontSize: 12, color: '#666' }}>
        <p>
          Working hours: {resource.openTime || '08:00'} - {resource.closeTime || '20:00'} | Units: {units}
        </p>
      </div>
    </div>
  );
};

export default ResourceBookingPage;
