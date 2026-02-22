import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import keycloakService from '../services/KeycloakService';

/**
 * Authentication Context
 *
 * Provides authentication state and methods throughout the React application.
 */
const AuthContext = createContext(null);

/**
 * Authentication Provider Component
 */
export const AuthProvider = ({ children }) => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [user, setUser] = useState(null);

    /**
     * Load user information from Keycloak
     */
    const loadUserInfo = useCallback(async () => {
        if (keycloakService.isAuthenticated()) {
            const profile = await keycloakService.getUserProfile();
            setUser({
                id: keycloakService.getUserId(),
                username: keycloakService.getUsername(),
                email: keycloakService.getEmail(),
                fullName: keycloakService.getFullName(),
                roles: keycloakService.getRoles(),
                isAdmin: keycloakService.isAdmin(),
                ...profile,
            });
        } else {
            setUser(null);
        }
    }, []);

    /**
     * Initialize Keycloak on mount
     */
    useEffect(() => {
        const initAuth = async () => {
            try {
                const authenticated = await keycloakService.init();
                setIsAuthenticated(authenticated);

                if (authenticated) {
                    await loadUserInfo();
                }

                // Subscribe to auth changes
                const unsubscribe = keycloakService.onAuthChange(async (authenticated) => {
                    setIsAuthenticated(authenticated);
                    if (authenticated) {
                        await loadUserInfo();
                    } else {
                        setUser(null);
                    }
                });

                return unsubscribe;
            } catch (error) {
                console.error('Failed to initialize authentication:', error);
            } finally {
                setIsLoading(false);
            }
        };

        initAuth();
    }, [loadUserInfo]);

    /**
     * Login with Keycloak
     */
    const login = useCallback(() => {
        keycloakService.login();
    }, []);

    /**
     * Login with Google
     */
    const loginWithGoogle = useCallback(() => {
        keycloakService.loginWithGoogle();
    }, []);

    /**
     * Logout
     */
    const logout = useCallback(() => {
        keycloakService.logout();
        setIsAuthenticated(false);
        setUser(null);
    }, []);

    /**
     * Get authorization header for API requests
     */
    const getAuthHeader = useCallback(() => {
        const token = keycloakService.getToken();
        if (token) {
            return { Authorization: `Bearer ${token}` };
        }
        return {};
    }, []);

    /**
     * Check if user has a specific role
     */
    const hasRole = useCallback((role) => {
        return keycloakService.hasRole(role);
    }, []);

    const value = {
        isAuthenticated,
        isLoading,
        user,
        login,
        loginWithGoogle,
        logout,
        getAuthHeader,
        hasRole,
        isAdmin: user?.isAdmin || false,
        getToken: () => keycloakService.getToken(),
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

/**
 * Custom hook to use authentication context
 */
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

/**
 * Higher-Order Component for requiring authentication
 */
export const withAuth = (Component) => {
    return function AuthenticatedComponent(props) {
        const { isAuthenticated, isLoading, login } = useAuth();

        if (isLoading) {
            return (
                <div className="loading-container">
                    <div className="spinner-border text-primary" role="status">
                        <span className="sr-only">Loading...</span>
                    </div>
                </div>
            );
        }

        if (!isAuthenticated) {
            login();
            return null;
        }

        return <Component {...props} />;
    };
};

/**
 * Higher-Order Component for requiring admin role
 */
export const withAdminRole = (Component) => {
    return function AdminComponent(props) {
        const { isAuthenticated, isLoading, isAdmin, login } = useAuth();

        if (isLoading) {
            return (
                <div className="loading-container">
                    <div className="spinner-border text-primary" role="status">
                        <span className="sr-only">Loading...</span>
                    </div>
                </div>
            );
        }

        if (!isAuthenticated) {
            login();
            return null;
        }

        if (!isAdmin) {
            return (
                <div className="access-denied">
                    <h2>Access Denied</h2>
                    <p>You need administrator privileges to access this page.</p>
                </div>
            );
        }

        return <Component {...props} />;
    };
};

export default AuthContext;
