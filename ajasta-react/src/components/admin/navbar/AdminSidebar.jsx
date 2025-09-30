import { NavLink, useLocation } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { 
  faChartLine, 
  faList, 
  faShoppingBag, 
  faCreditCard 
} from '@fortawesome/free-solid-svg-icons';


 

const AdminSidebar = () => {
  const location = useLocation();

  

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
          <li>
            <NavLink 
              to="/admin/orders" 
              className={location.pathname.includes('/admin/orders') ? 'active' : ''}
            >
              <FontAwesomeIcon icon={faShoppingBag} />
              <span>Orders</span>
            </NavLink>
          </li>
          <li>
            <NavLink 
              to="/admin/payments" 
              className={location.pathname.includes('/admin/payments') ? 'active' : ''}
            >
              <FontAwesomeIcon icon={faCreditCard} />
              <span>Payments</span>
            </NavLink>
          </li>
        </ul>
      </div>
    </div>
  );
};

export default AdminSidebar;