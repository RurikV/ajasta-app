import Keycloak from 'keycloak-js';

/**
 * Keycloak Configuration
 *
 * Configuration for connecting to the Keycloak authentication server.
 * The settings can be overridden via environment variables.
 */
const keycloakConfig = {
    url: process.env.REACT_APP_KEYCLOAK_URL || 'http://localhost:8180',
    realm: process.env.REACT_APP_KEYCLOAK_REALM || 'ajasta',
    clientId: process.env.REACT_APP_KEYCLOAK_CLIENT_ID || 'ajasta-frontend',
};

const keycloak = new Keycloak(keycloakConfig);

/**
 * Keycloak Service
 *
 * Provides authentication functionality using Keycloak with Google OAuth.
 */
class KeycloakService {
    constructor() {
        this.keycloak = keycloak;
        this.initialized = false;
        this.initPromise = null;
    }

    /**
     * Initialize Keycloak
     * @returns {Promise<boolean>} - Resolves when initialization is complete
     */
    async init() {
        if (this.initialized) {
            return this.initPromise;
        }

        this.initPromise = this.keycloak.init({
            onLoad: 'check-sso',
            silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
            pkceMethod: 'S256',
            checkLoginIframe: false,
            responseMode: 'query',
        }).then((authenticated) => {
            this.initialized = true;

            // Set up token refresh
            if (authenticated) {
                this.scheduleTokenRefresh();
            }

            return authenticated;
        }).catch((error) => {
            console.error('Failed to initialize Keycloak:', error);
            this.initialized = true;
            return false;
        });

        return this.initPromise;
    }

    /**
     * Schedule automatic token refresh
     */
    scheduleTokenRefresh() {
        // Refresh token 1 minute before it expires
        const refreshThreshold = 60;

        this.keycloak.onAuthRefresh = () => {
            console.log('Token refreshed');
        };

        // Check token expiry periodically
        setInterval(() => {
            if (this.keycloak.authenticated && this.keycloak.token) {
                const expiry = this.keycloak.tokenParsed?.exp;
                const now = Math.floor(Date.now() / 1000);

                if (expiry && expiry - now < refreshThreshold) {
                    this.updateToken(refreshThreshold);
                }
            }
        }, 30000); // Check every 30 seconds
    }

    /**
     * Login with Keycloak (redirects to Keycloak/Google login)
     */
    login() {
        if (!this.keycloak.authenticated) {
            this.keycloak.login({
                redirectUri: window.location.origin + '/dashboard',
            });
        }
    }

    /**
     * Login with Google directly
     */
    loginWithGoogle() {
        if (!this.keycloak.authenticated) {
            this.keycloak.login({
                redirectUri: window.location.origin + '/dashboard',
                idpHint: 'google',
            });
        }
    }

    /**
     * Logout from Keycloak
     */
    logout() {
        if (this.keycloak.authenticated) {
            this.keycloak.logout({
                redirectUri: window.location.origin,
            });
        }
    }

    /**
     * Check if user is authenticated
     * @returns {boolean}
     */
    isAuthenticated() {
        return this.keycloak.authenticated || false;
    }

    /**
     * Get the access token
     * @returns {string|null}
     */
    getToken() {
        return this.keycloak.token || null;
    }

    /**
     * Get the parsed token with user info
     * @returns {object|null}
     */
    getTokenParsed() {
        return this.keycloak.tokenParsed || null;
    }

    /**
     * Update the token if it's about to expire
     * @param {number} minValidity - Minimum validity in seconds
     * @returns {Promise<boolean>}
     */
    async updateToken(minValidity = 30) {
        try {
            const refreshed = await this.keycloak.updateToken(minValidity);
            return refreshed;
        } catch (error) {
            console.error('Failed to refresh token:', error);
            this.logout();
            return false;
        }
    }

    /**
     * Get user profile information
     * @returns {Promise<object>}
     */
    async getUserProfile() {
        if (!this.keycloak.authenticated) {
            return null;
        }

        try {
            const profile = await this.keycloak.loadUserProfile();
            return profile;
        } catch (error) {
            console.error('Failed to load user profile:', error);
            return this.getTokenParsed();
        }
    }

    /**
     * Get user's roles
     * @returns {string[]}
     */
    getRoles() {
        const tokenParsed = this.getTokenParsed();
        if (!tokenParsed) {
            return [];
        }

        // Get roles from the token (configured in Keycloak realm)
        return tokenParsed.roles || [];
    }

    /**
     * Check if user has a specific role
     * @param {string} role - Role name to check
     * @returns {boolean}
     */
    hasRole(role) {
        return this.getRoles().includes(role);
    }

    /**
     * Check if user is an admin
     * @returns {boolean}
     */
    isAdmin() {
        return this.hasRole('admin');
    }

    /**
     * Get user ID from token
     * @returns {string|null}
     */
    getUserId() {
        const tokenParsed = this.getTokenParsed();
        return tokenParsed?.sub || null;
    }

    /**
     * Get username from token
     * @returns {string|null}
     */
    getUsername() {
        const tokenParsed = this.getTokenParsed();
        return tokenParsed?.preferred_username || tokenParsed?.email || null;
    }

    /**
     * Get email from token
     * @returns {string|null}
     */
    getEmail() {
        const tokenParsed = this.getTokenParsed();
        return tokenParsed?.email || null;
    }

    /**
     * Get user's full name
     * @returns {string|null}
     */
    getFullName() {
        const tokenParsed = this.getTokenParsed();
        if (tokenParsed?.name) {
            return tokenParsed.name;
        }
        const firstName = tokenParsed?.given_name || '';
        const lastName = tokenParsed?.family_name || '';
        return `${firstName} ${lastName}`.trim() || null;
    }

    /**
     * Register a callback for authentication state changes
     * @param {function} callback - Function to call on auth change
     * @returns {function} - Unsubscribe function
     */
    onAuthChange(callback) {
        const onAuthSuccess = () => callback(true);
        const onAuthLogout = () => callback(false);
        const onAuthRefreshSuccess = () => callback(true);

        this.keycloak.onAuthSuccess = onAuthSuccess;
        this.keycloak.onAuthLogout = onAuthLogout;
        this.keycloak.onAuthRefreshSuccess = onAuthRefreshSuccess;

        // Return unsubscribe function
        return () => {
            this.keycloak.onAuthSuccess = undefined;
            this.keycloak.onAuthLogout = undefined;
            this.keycloak.onAuthRefreshSuccess = undefined;
        };
    }
}

// Export singleton instance
const keycloakService = new KeycloakService();
export default keycloakService;
