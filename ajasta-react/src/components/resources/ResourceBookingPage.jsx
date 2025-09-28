import { useEffect, useMemo, useState, useRef, useCallback } from 'react';
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

  // Holds: reservation holds for 30 minutes after booking (per slot key)
  const [holds, setHolds] = useState({}); // { [key]: { expiresAt: number, owner: string } }
  const [holdsOwner, setHoldsOwner] = useState(null);
  const tickRef = useRef(null);
  const [, setTick] = useState(0); // trigger re-render each second

  const isAuthenticated = ApiService.isAuthenticated();
    // eslint-disable-next-line no-console
    console.log('[DEBUG_LOG] render isAuthenticated=', isAuthenticated);
      // eslint-disable-next-line no-console
      console.log('[DEBUG_LOG] ApiService keys', Object.keys(ApiService || {}));
        // eslint-disable-next-line no-console
        console.log('[DEBUG_LOG] typeof ApiService.isAuthenticated =', typeof (ApiService && ApiService.isAuthenticated));
        try {
          // eslint-disable-next-line no-console
          console.log('[DEBUG_LOG] ApiService.isAuthenticated() ->', typeof ApiService.isAuthenticated === 'function' ? ApiService.isAuthenticated() : 'not-a-function');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log('[DEBUG_LOG] ApiService.isAuthenticated call error', e && e.message);
        }

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

  // Get current user key from JWT token for user-scoped localStorage
  const getCurrentUserKey = useCallback(() => {
    try {
      const token = typeof ApiService.getToken === 'function' ? ApiService.getToken() : localStorage.getItem('token');
      if (!token) return 'anon';
      
      // Decode JWT token to extract email as persistent user identifier
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || 'anon'; // 'sub' typically contains the email/username
    } catch {
      // Fallback to token if decoding fails, but this should be rare
      const token = localStorage.getItem('token');
      return token || 'anon';
    }
  }, []);

  // Local storage key for holds - now resource-scoped so all users can see all reserved slots
  const getHoldsKey = useCallback(() => {
    if (!resource) return null;
    return `resourceHolds_${resource.id}`;
  }, [resource]);
  
  // Legacy: Still maintain individual owner keys for backward compatibility during transition
  const getOwnerKey = useCallback(() => {
    if (!resource) return null;
    const userKey = getCurrentUserKey();
    return `resourceHolds_${resource.id}_user_${userKey}_owner`;
  }, [resource, getCurrentUserKey]);

  // Load holds from localStorage when resource changes or authentication state changes
  useEffect(() => {
    if (!resource) return;
    try {
      const k = getHoldsKey();
      const raw = k ? localStorage.getItem(k) : null;
      let parsed = raw ? JSON.parse(raw) : {};
      const now = Date.now();
      
      // Handle both old format (number) and new format ({ expiresAt, owner })
      const cleaned = {};
      const currentUser = getCurrentUserKey();
      let hasOwnHolds = false;
      
      Object.entries(parsed).forEach(([key, value]) => {
        if (typeof value === 'number') {
          // Old format: just expiry time, assume anonymous owner
          if (value > now) {
            cleaned[key] = { expiresAt: value, owner: 'legacy' };
          }
        } else if (value && typeof value === 'object' && typeof value.expiresAt === 'number') {
          // New format: { expiresAt, owner }
          if (value.expiresAt > now) {
            cleaned[key] = value;
            if (value.owner === currentUser) {
              hasOwnHolds = true;
            }
          }
        }
      });
      
      setHolds(cleaned);
      if (k) localStorage.setItem(k, JSON.stringify(cleaned));
      
      // Set holdsOwner based on whether current user has any active holds
      setHoldsOwner(hasOwnHolds ? currentUser : null);
    } catch (e) {
      // ignore parse errors
      setHolds({});
      setHoldsOwner(null);
    }
  }, [resource, getHoldsKey, getOwnerKey, isAuthenticated, getCurrentUserKey]);

  // Start interval to tick each second and prune expired holds
  useEffect(() => {
    if (!resource) return;
    if (tickRef.current) clearInterval(tickRef.current);
    tickRef.current = setInterval(() => {
      setTick((t) => t + 1);
      const now = Date.now();
      const currentUser = getCurrentUserKey();
      setHolds((prev) => {
        const cleaned = {};
        let hasOwnHolds = false;
        
        Object.entries(prev).forEach(([key, value]) => {
          if (typeof value === 'number') {
            // Old format: just expiry time
            if (value > now) {
              cleaned[key] = { expiresAt: value, owner: 'legacy' };
            }
          } else if (value && typeof value === 'object' && typeof value.expiresAt === 'number') {
            // New format: { expiresAt, owner }
            if (value.expiresAt > now) {
              cleaned[key] = value;
              if (value.owner === currentUser) {
                hasOwnHolds = true;
              }
            }
          }
        });
        
        const holdsKey = getHoldsKey();
        try {
          if (holdsKey) {
            if (Object.keys(cleaned).length === 0) {
              // No active holds left: clear storage
              localStorage.removeItem(holdsKey);
            } else {
              localStorage.setItem(holdsKey, JSON.stringify(cleaned));
            }
          }
        } catch {}
        
        // Update holdsOwner state based on whether current user has active holds
        setHoldsOwner(hasOwnHolds ? currentUser : null);
        
        return cleaned;
      });
    }, 1000);
    return () => {
      if (tickRef.current) clearInterval(tickRef.current);
    };
  }, [resource, getHoldsKey, getCurrentUserKey]);

  const isHeld = (key) => {
    const hold = holds[key];
    if (!hold) return false;
    
    // Handle both old format (number) and new format ({ expiresAt, owner })
    if (typeof hold === 'number') {
      return hold > Date.now();
    } else if (hold && typeof hold === 'object' && typeof hold.expiresAt === 'number') {
      return hold.expiresAt > Date.now();
    }
    
    return false;
  };

  // Helper to check if current user owns a specific hold
  const ownsHold = (key) => {
    const hold = holds[key];
    if (!hold) return false;
    
    const currentUser = getCurrentUserKey();
    if (typeof hold === 'number') {
      // Old format: assume legacy ownership
      return false;
    } else if (hold && typeof hold === 'object' && hold.owner) {
      return hold.owner === currentUser;
    }
    
    return false;
  };

  const addHolds = (keys) => {
    if (!resource || !keys || keys.length === 0) return;
    const expiry = Date.now() + 30 * 60 * 1000; // 30 minutes
    const owner = getCurrentUserKey();
    const next = { ...holds };
    
    keys.forEach((k) => {
      const current = next[k];
      const newHold = { expiresAt: expiry, owner };
      
      // If there's an existing hold, only update if the new expiry is later
      if (current) {
        if (typeof current === 'number') {
          // Old format: replace with new format if expiry is later
          next[k] = current < expiry ? newHold : { expiresAt: current, owner: 'legacy' };
        } else if (current.expiresAt < expiry) {
          // New format: update if expiry is later
          next[k] = newHold;
        }
      } else {
        next[k] = newHold;
      }
    });
    
    setHolds(next);
    setHoldsOwner(owner);
    
    const k = getHoldsKey();
    if (k) {
      try { localStorage.setItem(k, JSON.stringify(next)); } catch {}
    }
  };

  const handleCancelHold = (key) => {
    if (!ownsHold(key)) return; // Only allow cancelling own holds
    
    const next = { ...holds };
    delete next[key];
    setHolds(next);
    
    // Check if user still has any holds after this cancellation
    const currentUser = getCurrentUserKey();
    const hasOwnHolds = Object.values(next).some(hold => {
      return typeof hold === 'object' && hold.owner === currentUser;

    });
    setHoldsOwner(hasOwnHolds ? currentUser : null);
    
    const holdsKey = getHoldsKey();
    try {
      if (holdsKey) {
        if (Object.keys(next).length === 0) {
          // No holds left for any user: clear storage
          localStorage.removeItem(holdsKey);
        } else {
          localStorage.setItem(holdsKey, JSON.stringify(next));
        }
      }
    } catch {}
  };

  const formatSeconds = (total) => {
    if (total == null) return '';
    const m = Math.floor(total / 60);
    const s = total % 60;
    return `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
  };

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


  const isSlotUnavailable = useCallback((timeHHMM) => {
    if (!resource) return false;

    // Date unavailability checks (weekday and specific dates)
    if (resource.unavailableWeekdays) {
      const list = resource.unavailableWeekdays.split(',').map(s => s.trim()).filter(Boolean);
      if (weekdayIndex != null && list.includes(String(weekdayIndex))) return true;
    }
    if (resource.unavailableDates) {
      const dates = resource.unavailableDates.split(',').map(s => s.trim()).filter(Boolean);
      if (dates.includes(date)) return true;
    }

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
  }, [resource, date, weekdayIndex]);

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
    // Check if this is a held slot that belongs to the current user
    if (isHeld(key) && ownsHold(key)) {
      handleCancelHold(key);
      return;
    }
    
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
      // eslint-disable-next-line no-console
      console.log('[DEBUG_LOG] handleBookResource invoked', { isAuthenticated, selectedSize: selected.size });
    
    if (!isAuthenticated) {
      showError("Please login to continue, If you don't have an account do well to register");
      setTimeout(() => {
        // Navigate to login if needed
      }, 5000);
      return;
    }

    // Check if user has required role for booking
    const isCustomer = ApiService.isCustomer();
    const isAdmin = ApiService.isAdmin();
    const roles = ApiService.getRoles();
    
    // eslint-disable-next-line no-console
    console.log('[DEBUG_LOG] User roles check', { isCustomer, isAdmin, roles });
    
    if (!isCustomer && !isAdmin) {
      showError("Access Denied: You need CUSTOMER or ADMIN role to book resources. Please contact support if you believe this is an error.");
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
      // Add reservation holds for booked slots (30 minutes)
      addHolds(keys);
      setSelected(new Set());
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

  // Compute earliest hold expiry countdown (only visible to the user who owns the holds)
  const nowMs = Date.now();
  const currentUserKey = getCurrentUserKey();
  
  // Filter active holds (both old and new format)
  const activeHolds = Object.entries(holds).filter(([, hold]) => {
    if (typeof hold === 'number') {
      return hold > nowMs;
    } else if (hold && typeof hold === 'object' && typeof hold.expiresAt === 'number') {
      return hold.expiresAt > nowMs;
    }
    return false;
  });
  
  // Filter holds owned by current user
  const ownActiveHolds = activeHolds.filter(([, hold]) => {
    if (typeof hold === 'number') {
      return false; // Old format, assume not owned by current user
    } else if (hold && typeof hold === 'object' && hold.owner) {
      return hold.owner === currentUserKey;
    }
    return false;
  });
  
  const hasOwnActiveHolds = ownActiveHolds.length > 0;
  const hasAnyActiveHolds = activeHolds.length > 0;
  
  // Calculate earliest expiry for current user's holds
  const earliestExp = ownActiveHolds.length > 0 ? Math.min(...ownActiveHolds.map(([, hold]) => {
    return typeof hold === 'number' ? hold : hold.expiresAt;
  })) : null;
  
  const secondsLeft = hasOwnActiveHolds && earliestExp != null ? Math.max(0, Math.floor((earliestExp - nowMs) / 1000)) : null;
 
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
        backgroundColor: selected.size > 0 && !hasOwnActiveHolds ? '#f9f9f9' : '#f5f5f5',
        opacity: selected.size > 0 && !hasOwnActiveHolds ? 1 : 0.6
      }}>
        <h3>Booking Summary</h3>
        <div className="booking-details">
          <p>Booked slots: {selected.size}</p>
          <p>Price per slot: {formatMoney(pricePerSlot)}</p>
          <div className="total-price" style={{ fontSize: 18, fontWeight: 'bold', marginTop: 8 }}>
            Total: {formatMoney(selected.size * pricePerSlot)}
          </div>
          {hasOwnActiveHolds && (
            <div className="hold-warning" style={{ marginTop: 8, color: '#b36b00', fontWeight: 600 }}>
              You have {ownActiveHolds.length} reserved slot(s) awaiting payment. You cannot reserve additional slots until you complete payment or the hold expires. You can cancel your reserved slots by clicking on them.
            </div>
          )}
          {secondsLeft != null && (
            <div className="hold-countdown" style={{ marginTop: 8, color: '#856404' }}>
              Reservation hold active. Expires in {formatSeconds(secondsLeft)}
            </div>
          )}
        </div>
        <button
          onClick={handleBookResource}
          className="add-to-cart-btn"
          disabled={selected.size === 0 || hasOwnActiveHolds}
          style={{ 
            marginTop: 12, 
            padding: '12px 24px', 
            backgroundColor: selected.size > 0 && !hasOwnActiveHolds ? '#007bff' : '#cccccc', 
            color: selected.size > 0 && !hasOwnActiveHolds ? 'white' : '#666666', 
            border: 'none', 
            borderRadius: 4, 
            cursor: selected.size > 0 && !hasOwnActiveHolds ? 'pointer' : 'not-allowed' 
          }}
          title={hasOwnActiveHolds ? 'You have an active reservation hold. Finish payment or wait until it expires to reserve more.' : undefined}
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
                    const held = isHeld(key);
                    const ownHold = held && ownsHold(key);
                    const otherUserHold = held && !ownHold;
                    const canCancelHold = ownHold;
                    const disabled = disabledRow || (held && !canCancelHold) || (hasOwnActiveHolds && !held);
                    
                    // Background colors: own hold (yellow), other user hold (orange), selected (lightgreen), unavailable (grey)
                    let backgroundColor = 'transparent';
                    if (disabledRow) {
                      backgroundColor = '#f0f0f0';
                    } else if (ownHold) {
                      backgroundColor = 'yellow';
                    } else if (otherUserHold) {
                      backgroundColor = '#ffb366'; // Light orange for other users' holds
                    } else if (isSelected) {
                      backgroundColor = 'lightgreen';
                    }
                    
                    return (
                      <td
                        key={`${time}-${n}`}
                        data-testid={`slot-${time}-${n}`}
                        onClick={() => handleSelectClick(key, disabled)}
                        style={{
                          padding: 8,
                          textAlign: 'center',
                          border: '1px solid #eee',
                          backgroundColor,
                          color: (disabledRow ? '#888' : 'inherit'),
                          cursor: (disabled ? 'not-allowed' : 'pointer'),
                          userSelect: 'none'
                        }}
                        title={
                          disabledRow ? 'Unavailable' : 
                          canCancelHold ? 'Reserved by you - Click to cancel' : 
                          otherUserHold ? 'Reserved by another user' : 
                          'Available'
                        }
                        aria-disabled={disabled}
                      >
                        {/* visual slot */}
                        {held ? (
                          <span style={{ fontSize: 12, color: '#333', fontWeight: 600 }}>
                            {canCancelHold ? 'reserved (click to cancel)' : 'reserved'}
                          </span>
                        ) : (isSelected ? (
                          <span style={{ fontSize: 12, color: '#555' }}>
                            {time} - {minutesToHHMM(parseTimeToMinutes(time) + 30)}
                          </span>
                        ) : '')}
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
