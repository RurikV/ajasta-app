import React from 'react';
import { Navigate, useLocation } from "react-router-dom"
import { useAuth } from "../context/AuthContext"

/**
 * Loading component for route guards
 */
const LoadingSpinner = () => (
    <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
        <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
        </div>
    </div>
);

/**
 * Customer Route Guard
 * Requires authentication with 'user' role
 */
export const CustomerRoute = ({ element: Component }) => {
    const { isAuthenticated, isLoading, hasRole, login } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!isAuthenticated) {
        login();
        return null;
    }

    // Check if user has 'user' role (basic authenticated user)
    const isCustomer = hasRole('user') || hasRole('customer') || hasRole('admin');

    return isCustomer ? (
        Component
    ) : (
        <Navigate to="/login" replace state={{ from: location }} />
    )
}

/**
 * Admin Route Guard
 * Requires authentication with 'admin' role
 */
export const AdminRoute = ({ element: Component }) => {
    const { isAuthenticated, isLoading, isAdmin, login } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!isAuthenticated) {
        login();
        return null;
    }

    return isAdmin ? (
        Component
    ) : (
        <div className="container mt-5">
            <div className="alert alert-danger">
                <h4>Access Denied</h4>
                <p>You need administrator privileges to access this page.</p>
            </div>
        </div>
    )
}

/**
 * Protected Route Guard
 * Requires authentication (any role)
 */
export const ProtectedRoute = ({ element: Component }) => {
    const { isAuthenticated, isLoading, login } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!isAuthenticated) {
        login();
        return null;
    }

    return Component;
}

/**
 * Resource Manager Route Guard
 * Requires 'admin' or 'resource_manager' role
 */
export const ResourceManagerRoute = ({ element: Component }) => {
    const { isAuthenticated, isLoading, hasRole, login } = useAuth();
    const location = useLocation();

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!isAuthenticated) {
        login();
        return null;
    }

    const isResourceManager = hasRole('admin') || hasRole('resource_manager');

    return isResourceManager ? (
        Component
    ) : (
        <Navigate to="/login" replace state={{ from: location }} />
    )
}
