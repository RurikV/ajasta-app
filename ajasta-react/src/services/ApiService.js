import axios from "axios";
axios.defaults.withCredentials = true;

// Determine API base URL at runtime. Prefer explicit env/config, with smart localhost defaults.
const __RUNTIME_API_BASE__ = (() => {
    try {
        // 1) Build-time env or runtime globals take precedence
        const envUrl = (typeof process !== 'undefined' && process.env && (process.env.REACT_APP_API_BASE_URL || process.env.API_BASE_URL))
            || (typeof window !== 'undefined' && (window.__API_BASE_URL || window.API_BASE_URL));
        if (envUrl && typeof envUrl === 'string') {
            return envUrl.replace(/\/$/, '');
        }

        // 2) Smart defaults depending on environment
        if (typeof window !== 'undefined' && window.location && window.location.origin) {
            const origin = window.location.origin;
            const port = window.location.port;
            const hostname = window.location.hostname;
            const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';

            // CRA dev server: rely on dev proxy to avoid CORS
            if (isLocalhost && typeof process !== 'undefined' && process.env && process.env.NODE_ENV === 'development') {
                return `${origin}/api`;
            }

            // Static/production build served at localhost:3000 (e.g., docker-compose nginx)
            // There is no CRA proxy in this case; direct to backend port 8090 by default.
            if (isLocalhost && port === '3000' && (typeof process === 'undefined' || !process.env || process.env.NODE_ENV === 'production')) {
                return 'http://localhost:8090/api';
            }

            // Generic default: same-origin /api (assumes reverse proxy/Ingress present)
            return `${origin}/api`;
        }
    } catch (_) {}
    // 3) Safe default: relative path via Ingress
    return '/api';
})();

// Dedicated CMS base URL with per-service override support
const __RUNTIME_CMS_API_BASE__ = (() => {
    try {
        const envUrl = (typeof process !== 'undefined' && process.env && (process.env.REACT_APP_CMS_API_BASE_URL || process.env.CMS_API_BASE_URL))
            || (typeof window !== 'undefined' && (window.__CMS_API_BASE_URL || window.CMS_API_BASE_URL));
        if (envUrl && typeof envUrl === 'string') {
            return envUrl.replace(/\/$/, '');
        }

        // Localhost static build at port 3000 (no dev proxy) -> default to CMS on 8091
        if (typeof window !== 'undefined' && window.location) {
            const port = window.location.port;
            const hostname = window.location.hostname;
            const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';
            if (isLocalhost && port === '3000' && (typeof process === 'undefined' || !process.env || process.env.NODE_ENV === 'production')) {
                return 'http://localhost:8091/api/cms';
            }
        }

        // Fallback to primary API base + '/cms'
        const base = (__RUNTIME_API_BASE__ || '').replace(/\/$/, '');
        if (base) return `${base}/cms`;
    } catch (_) {}
    return '/api/cms';
})();

export default class ApiService {


    static BASE_URL = __RUNTIME_API_BASE__;
    static CMS_BASE_URL = __RUNTIME_CMS_API_BASE__;
    // static BASE_URL = "http://18.221.120.102:8090/api"; //production base url

    // In-memory cache for roles (never persisted to localStorage)
    static cachedRoles = null;

    // Simple listeners to notify components when roles change
    static roleListeners = new Set();

    static onRolesChange(callback) {
        if (typeof callback === 'function') {
            this.roleListeners.add(callback);
            return () => this.roleListeners.delete(callback);
        }
        return () => {};
    }

    static emitRolesChange() {
        try {
            const roles = this.getRoles();
            this.roleListeners.forEach(cb => {
                try { cb(roles); } catch {}
            });
        } catch {}
    }

    static saveToken(token) {
        localStorage.setItem("token", token);
    }

    static getToken() {
        return localStorage.getItem("token");
    }

    // Save roles: keep an in-memory cache for fast checks (no localStorage persistence)
    static saveRole(roles) {
        try {
            if (!roles) {
                this.cachedRoles = null;
                this.emitRolesChange();
                return;
            }
            const arr = Array.isArray(roles) ? roles : [roles];
            this.cachedRoles = Array.from(new Set(arr.map(r => String(r).replace(/^ROLE_/,'').toUpperCase())));
            this.emitRolesChange();
        } catch {
            this.cachedRoles = null;
            this.emitRolesChange();
        }
    }

    // Extract roles from JWT token payload in a robust way
    static parseRolesFromToken() {
        const token = this.getToken();
        if (!token) return [];
        try {
            const payload = JSON.parse(atob(token.split('.')[1] || '')) || {};
            let roles = [];
            if (Array.isArray(payload.roles)) {
                roles = payload.roles;
            } else if (Array.isArray(payload.authorities)) {
                const first = payload.authorities[0];
                if (typeof first === 'string') roles = payload.authorities;
                else roles = payload.authorities.map(a => a && (a.authority || a.role)).filter(Boolean);
            } else if (typeof payload.scope === 'string') {
                roles = payload.scope.split(/[ ,]+/).filter(Boolean);
            } else if (typeof payload.scopes === 'string') {
                roles = payload.scopes.split(/[ ,]+/).filter(Boolean);
            }
            roles = roles.map(r => String(r).replace(/^ROLE_/,'').toUpperCase());
            return Array.from(new Set(roles));
        } catch {
            return [];
        }
    }

    // Get roles from token (preferred) or in-memory cache
    static getRoles() {
        // Prefer parsed roles from token if present
        const fromToken = this.parseRolesFromToken();
        if (fromToken && fromToken.length) {
            this.cachedRoles = fromToken;
            return fromToken;
        }
        // Fall back to in-memory cached roles
        if (this.cachedRoles && this.cachedRoles.length) return this.cachedRoles;
        return [];
    }

    // Check if the user has a specific role
    static hasRole(role) {
        const roles = this.getRoles();
        if (!roles || roles.length === 0) return false;
        const target = String(role).replace(/^ROLE_/,'').toUpperCase();
        return roles.includes(target);
    }

    // Check if the user is an admin
    static isAdmin() {
        return this.hasRole('ADMIN');
    }

    // Check if the user is a customer
    static isCustomer() {
        return this.hasRole('CUSTOMER');
    }

    // Check if the user is a resource manager
    static isResourceManager() {
        return this.hasRole('RESOURCE_MANAGER');
    }



    static logout() {
        localStorage.removeItem("token");
        // Reset in-memory cache and notify listeners
        this.cachedRoles = null;
        this.emitRolesChange();
    }

    static isAuthenticated() {
        const token = this.getToken();
        return !!token;
    }

    static getHeader() {
        const token = this.getToken();
        return {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
        }
    }






    


    // REGISTER USER
    static async registerUser(registrationData) {
        const resp = await axios.post(`${this.BASE_URL}/auth/register`, registrationData);
        return resp.data;
    }



    static async loginUser(loginData) {
        const resp = await axios.post(`${this.BASE_URL}/auth/login`, loginData);
        return resp.data;
    }

    // Bootstrap roles securely from backend (in-memory only)
    static async bootstrapRoles(force = false) {
        if (!this.isAuthenticated()) {
            this.saveRole(null);
            return [];
        }
        if (!force) {
            const existing = this.getRoles();
            if (existing && existing.length) return existing;
        }
        try {
            const profile = await this.myProfile();
            const list = (profile?.data?.roles || []).map(r => (r?.name || '').toUpperCase()).filter(Boolean);
            this.saveRole(list);
            return list;
        } catch (e) {
            this.saveRole([]);
            return [];
        }
    }











     /**USERS PROFILE MANAGEMENT SESSION */
    static async myProfile() {
        const resp = await axios.get(`${this.BASE_URL}/users/account`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async updateProfile(formData) {
        const resp = await axios.put(`${this.BASE_URL}/users/update`, formData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return resp.data;
    }

    // Saved emails (user profile)
    static async getSavedEmails() {
        const resp = await axios.get(`${this.BASE_URL}/users/saved-emails`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async addSavedEmail(email) {
        const resp = await axios.post(`${this.BASE_URL}/users/saved-emails?email=${encodeURIComponent(email)}`, null, {
            headers: this.getHeader()
        });
        return resp.data;
    }


    static async deactivateProfile() {
        const resp = await axios.delete(`${this.BASE_URL}/users/deactivate`, {
            headers: this.getHeader()
        });
        return resp.data;
    }














    //ORDER SECTION

    static async placeOrder() {
        const resp = await axios.post(`${this.BASE_URL}/orders/checkout`, {}, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async updateOrderStatus(body) {
        const resp = await axios.put(`${this.BASE_URL}/orders/update`, body, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async getAllOrders(orderStatus, page = 0, size = 200, name) {

        let params = new URLSearchParams();
        if (orderStatus) params.set('orderStatus', orderStatus);
        if (page != null) params.set('page', String(page));
        if (size != null) params.set('size', String(size));
        if (name && String(name).trim()) params.set('name', String(name).trim());
        const qs = params.toString();
        const url = `${this.BASE_URL}/orders/all${qs ? ('?' + qs) : ''}`;

        const resp = await axios.get(url, {
            headers: this.getHeader()
        })
        return resp.data;

    }


    static async getMyOrders() {
        const resp = await axios.get(`${this.BASE_URL}/orders/me`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async getOrderById(id) {
        const resp = await axios.get(`${this.BASE_URL}/orders/${id}`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async deleteOrder(id) {
        const resp = await axios.delete(`${this.BASE_URL}/orders/${id}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async countTotalActiveCustomers() {
        const resp = await axios.get(`${this.BASE_URL}/orders/unique-customers`, {
            headers: this.getHeader()
        })
        return resp.data;
    }



























    /* RESOURCES SECTION */
    static async addResource(formData) {
        const resp = await axios.post(`${this.BASE_URL}/resources`, formData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return resp.data;
    }

    static async updateResource(formData) {
        const resp = await axios.put(`${this.BASE_URL}/resources`, formData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return resp.data;
    }

    static async deleteResource(id) {
        const resp = await axios.delete(`${this.BASE_URL}/resources/${id}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getResourceById(id) {
        const resp = await axios.get(`${this.BASE_URL}/resources/${id}`);
        return resp.data;
    }

    static async getAllResources(params = {}) {
        const resp = await axios.get(`${this.BASE_URL}/resources`, { params });
        return resp.data;
    }










    static async bookResource(id, body) {
        const resp = await axios.post(`${this.BASE_URL}/resources/${id}/book`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async bookResourceBatch(id, body) {
        const resp = await axios.post(`${this.BASE_URL}/resources/${id}/book-batch`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async bookResourceMulti(id, body) {
        const resp = await axios.post(`${this.BASE_URL}/resources/${id}/book-multi`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }


    /**PAYMENT SESSION */

    //funtion to create payment intent
    static async proceedForPayment(body) {


        const resp = await axios.post(`${this.BASE_URL}/payments/pay`, body, {
            headers: this.getHeader()
        });
        return resp.data; //return the resp containg the stripe transaction id for this transaction
    }

    //TO UPDATE PAYMENT WHEN IT HAS BEEN COMPLETED
    static async updateOrderPayment(body) {
        const resp = await axios.put(`${this.BASE_URL}/payments/update`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAllPayments() {
        const resp = await axios.get(`${this.BASE_URL}/payments/all`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAPaymentById(paymentId) {
        const resp = await axios.get(`${this.BASE_URL}/payments/${paymentId}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }







    /* ADMIN USERS & ROLES */
    static async getAllUsers() {
        const resp = await axios.get(`${this.BASE_URL}/users/all`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAllRoles() {
        const resp = await axios.get(`${this.BASE_URL}/roles`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async updateUserRoles(userId, roles) {
        const resp = await axios.put(`${this.BASE_URL}/users/${userId}/roles`, roles, {
            headers: this.getHeader()
        });
        return resp.data;
    }


    /* REVIEWS SECTION */
    static async getResourceReviews(resourceId) {
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/${resourceId}`);
        return resp.data;
    }

    static async getResourceAverageRating(resourceId) {
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/average/${resourceId}`);
        return resp.data;
    }

    static async getReviewEligibility(resourceId) {
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/eligibility/${resourceId}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async createReview(reviewDTO) {
        const resp = await axios.post(`${this.BASE_URL}/reviews`, reviewDTO, {
            headers: this.getHeader()
        });
        return resp.data;
    }
}