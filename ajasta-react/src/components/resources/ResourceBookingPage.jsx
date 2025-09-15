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
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));

  // Selection state
  const [selected, setSelected] = useState(() => new Set());
  const [dragging, setDragging] = useState(false);

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

  // Stop dragging on mouse up anywhere
  useEffect(() => {
    const onUp = () => setDragging(false);
    window.addEventListener('mouseup', onUp);
    return () => window.removeEventListener('mouseup', onUp);
  }, []);

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
    if (isDateUnavailable(date)) return true;
    // Daily time ranges
    if (resource.dailyUnavailableRanges && isTimeInRanges(timeHHMM, resource.dailyUnavailableRanges)) return true;
    return false;
  };

  const toggleKey = (key) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  const handleMouseDown = (key, disabled) => {
    if (disabled) return;
    setDragging(true);
    toggleKey(key);
  };

  const handleMouseEnter = (key, disabled) => {
    if (!dragging || disabled) return;
    setSelected(prev => {
      const next = new Set(prev);
      next.add(key);
      return next;
    });
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
                        onMouseDown={() => handleMouseDown(key, disabled)}
                        onMouseEnter={() => handleMouseEnter(key, disabled)}
                        onClick={() => !dragging && handleMouseDown(key, disabled)}
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
                        {isSelected ? 'Selected' : ''}
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
