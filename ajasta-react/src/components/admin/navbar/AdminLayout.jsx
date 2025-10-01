import { Outlet, useLocation, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import AdminSidebar from './AdminSidebar';
import AdminTopbar from './AdminTopbar';
import ApiService from '../../../services/ApiService';

const AdminLayout = () => {
    const location = useLocation();
    const [, setRoleTick] = useState(0);

    useEffect(() => {
        const unsubscribe = ApiService.onRolesChange(() => setRoleTick(t => t + 1));
        if (ApiService.isAuthenticated()) {
            ApiService.bootstrapRoles();
        }
        return unsubscribe;
    }, []);

    const isRM = ApiService.isResourceManager();

    if (isRM && location.pathname === '/admin') {
        return <Navigate to="/admin/resources" replace />;
    }

    return (
        <div className="admin-layout">
            <AdminSidebar />
            <div className="admin-main">
                <AdminTopbar />
                <div className="admin-content">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default AdminLayout;