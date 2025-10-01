import { useEffect, useMemo, useState } from 'react';
import ApiService from '../../services/ApiService';

const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const roleOptions = useMemo(() => roles.map(r => ({ id: r.id, name: (r.name || '').toUpperCase() })), [roles]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [usersResp, rolesResp] = await Promise.all([
        ApiService.getAllUsers(),
        ApiService.getAllRoles()
      ]);
      const usersData = usersResp?.data || [];
      const rolesData = rolesResp?.data || [];
      setUsers(usersData);
      setRoles(rolesData);
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onRoleChange = (userId, selected) => {
    setUsers(prev => prev.map(u => u.id === userId ? { ...u, roles: selected } : u));
  };

  const handleSave = async (user) => {
    setSavingUserId(user.id);
    setMessage('');
    setError('');
    try {
      const roleNames = (user.roles || []).map(r => (r.name || '').toUpperCase());
      const resp = await ApiService.updateUserRoles(user.id, roleNames);
      const updated = resp?.data;
      if (updated) {
        setUsers(prev => prev.map(u => u.id === user.id ? updated : u));
      }
      setMessage('User roles updated');
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || 'Failed to update roles');
    } finally {
      setSavingUserId(null);
      setTimeout(() => setMessage(''), 2500);
    }
  };

  if (loading) {
    return <div className="container"><p>Loading users and roles...</p></div>;
  }

  return (
    <div className="container">
      <h2>Users & Roles</h2>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      {message && <div className="alert alert-success" role="alert">{message}</div>}

      <div className="table-responsive">
        <table className="table table-striped">
          <thead>
            <tr>
              <th>#</th>
              <th>Name</th>
              <th>Email</th>
              <th>Current Roles</th>
              <th>Change Roles</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u, idx) => {
              const currentRoleNames = (u.roles || []).map(r => (r.name || '').toUpperCase());
              return (
                <tr key={u.id}>
                  <td>{idx + 1}</td>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td>{currentRoleNames.join(', ') || '-'}</td>
                  <td style={{ minWidth: 240 }}>
                    <select
                      multiple
                      className="form-select"
                      value={(u.roles || []).map(r => (r.name || '').toUpperCase())}
                      onChange={(e) => {
                        const selectedNames = Array.from(e.target.selectedOptions).map(o => o.value);
                        const selectedRoles = roleOptions
                          .filter(ro => selectedNames.includes(ro.name))
                          .map(ro => ({ id: ro.id, name: ro.name }));
                        onRoleChange(u.id, selectedRoles);
                      }}
                    >
                      {roleOptions.map(ro => (
                        <option key={ro.id} value={ro.name}>{ro.name}</option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <button
                      className="btn btn-primary"
                      disabled={savingUserId === u.id}
                      onClick={() => handleSave(u)}
                    >
                      {savingUserId === u.id ? 'Saving...' : 'Save'}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AdminUsersPage;
