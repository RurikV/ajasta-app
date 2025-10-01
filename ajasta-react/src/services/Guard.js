import React from 'react';
import { Navigate, useLocation } from "react-router-dom"
import ApiService from "./ApiService"




export const CustomerRoute = ({element: Component}) => {
   
    const location = useLocation()
    const [checking, setChecking] = React.useState(true);
    const [allowed, setAllowed] = React.useState(false);

    React.useEffect(() => {
        let mounted = true;
        const run = async () => {
            if (!ApiService.isAuthenticated()) {
                setAllowed(false);
                setChecking(false);
                return;
            }
            // First quick check
            const ok = ApiService.isCustomer();
            if (ok) {
                setAllowed(true);
                setChecking(false);
                return;
            }
            // Bootstrap roles and re-check
            await ApiService.bootstrapRoles();
            if (!mounted) return;
            setAllowed(ApiService.isCustomer());
            setChecking(false);
        };
        run();
        return () => { mounted = false; };
    }, []);
    
    if (checking) return null;

    return allowed ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}

export const AdminRoute = ({element: Component}) => {
    
    const location = useLocation()
    const [checking, setChecking] = React.useState(true);
    const [allowed, setAllowed] = React.useState(false);

    React.useEffect(() => {
        let mounted = true;
        const run = async () => {
            if (!ApiService.isAuthenticated()) {
                setAllowed(false);
                setChecking(false);
                return;
            }
            const ok = (ApiService.isAdmin() || ApiService.isResourceManager());
            if (ok) {
                setAllowed(true);
                setChecking(false);
                return;
            }
            await ApiService.bootstrapRoles();
            if (!mounted) return;
            setAllowed(ApiService.isAdmin() || ApiService.isResourceManager());
            setChecking(false);
        };
        run();
        return () => { mounted = false; };
    }, []);

    if (checking) return null;

    return allowed ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}

export const ProtectedRoute = ({element: Component}) => {
    const location = useLocation()
    return ApiService.isAuthenticated() ? (
        Component
    ):(
        <Navigate to="/login" replace state={{from: location}}/>
    )
}
