import { NavLink, useLocation } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faChartLine, 
  faList, 
  faShoppingBag, 
  faCreditCard, 
  faUsers
} from '@fortawesome/free-solid-svg-icons';
import ApiService from '../../../services/ApiService';
import { useEffect, useState } from 'react';

const AdminSidebar = () => {
  const location = useLocation();
  const [, setRoleTick] = useState(0);

  useEffect(() => {
    const unsubscribe = ApiService.onRolesChange(() => setRoleTick(t => t + 1));
    if (ApiService.isAuthenticated()) {
      ApiService.bootstrapRoles();
    }
    return unsubscribe;
  }, []);

  const isAdmin = ApiService.isAdmin();
  const isRM = ApiService.isResourceManager();

  return (
    <div className="admin-sidebar">
      <div className="sidebar-header">
        <h2>Pannel</h2>
      </div>
      
      <div className="sidebar-nav">
        <ul>
          <li>
            <NavLink 
              to="/admin" 
              className={location.pathname === '/admin' ? 'active' : ''}
              end
            >
              <FontAwesomeIcon icon={faChartLine} />
              <span>Dashboard</span>
            </NavLink>
          </li>
          <li>
            <NavLink 
              to="/admin/resources" 
              className={location.pathname.includes('/admin/resources') ? 'active' : ''}
            >
              <FontAwesomeIcon icon={faList} />
              <span>Resources</span>
            </NavLink>
          </li>
          {(isAdmin || isRM) && (
            <li>
              <NavLink 
                to="/admin/orders" 
                className={location.pathname.includes('/admin/orders') ? 'active' : ''}
              >
                <FontAwesomeIcon icon={faShoppingBag} />
                <span>Orders</span>
              </NavLink>
            </li>
          )}
          {isAdmin && (
            <li>
              <NavLink 
                to="/admin/payments" 
                className={location.pathname.includes('/admin/payments') ? 'active' : ''}
              >
                <FontAwesomeIcon icon={faCreditCard} />
                <span>Payments</span>
              </NavLink>
            </li>
          )}
          {isAdmin && (
            <li>
              <NavLink 
                to="/admin/users" 
                className={location.pathname.includes('/admin/users') ? 'active' : ''}
              >
                <FontAwesomeIcon icon={faUsers} />
                <span>Users</span>
              </NavLink>
            </li>
          )}
        </ul>
      </div>
    </div>
  );
};

export default AdminSidebar;