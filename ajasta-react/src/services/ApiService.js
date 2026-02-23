import axios from "axios";
import keycloakService from "./KeycloakService";

axios.defaults.withCredentials = true;

// Flag to use Kotlin backend API (v1)
const USE_KOTLIN_BACKEND = true;

// Flag to use Keycloak authentication
const USE_KEYCLOAK_AUTH = true;

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
                // For Kotlin backend, no /api prefix - use same origin (dev proxy handles it)
                return USE_KOTLIN_BACKEND ? '' : `${origin}/api`;
            }

            // Static/production build served at localhost:3000 (e.g., docker-compose nginx)
            // The nginx proxies /v1/* to backend, so we use same origin (empty string)
            if (isLocalhost && port === '3000') {
                return USE_KOTLIN_BACKEND ? '' : 'http://localhost:8090/api';
            }

            // Generic default: same-origin (assumes reverse proxy/Ingress present)
            // For Kotlin backend, no prefix needed - nginx proxies /v1/*
            return USE_KOTLIN_BACKEND ? '' : `${origin}/api`;
        }
    } catch (_) {}
    // 3) Safe default: relative path (same origin)
    return USE_KOTLIN_BACKEND ? '' : '/api';
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
        // Use Keycloak token if available
        if (USE_KEYCLOAK_AUTH) {
            const keycloakToken = keycloakService.getToken();
            if (keycloakToken) {
                return keycloakToken;
            }
        }
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
        // Check Keycloak roles first
        if (USE_KEYCLOAK_AUTH && keycloakService.hasRole(role)) {
            return true;
        }
        // Fall back to cached roles
        const roles = this.getRoles();
        if (!roles || roles.length === 0) return false;
        const target = String(role).replace(/^ROLE_/,'').toUpperCase();
        return roles.includes(target);
    }

    // Check if the user is an admin
    static isAdmin() {
        if (USE_KEYCLOAK_AUTH && keycloakService.isAdmin()) {
            return true;
        }
        return this.hasRole('ADMIN');
    }

    // Check if the user is a customer
    static isCustomer() {
        if (USE_KEYCLOAK_AUTH && keycloakService.hasRole('user')) {
            return true;
        }
        return this.hasRole('CUSTOMER') || this.hasRole('USER');
    }

    // Check if the user is a resource manager
    static isResourceManager() {
        return this.hasRole('RESOURCE_MANAGER');
    }



    static logout() {
        localStorage.removeItem("token");
        localStorage.removeItem("roles");
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
        if (USE_KOTLIN_BACKEND) {
            // Mock successful registration
            const mockToken = 'mock-jwt-token-' + Date.now();
            this.saveToken(mockToken);
            this.saveRole(['CUSTOMER']);
            return {
                statusCode: 200,
                message: "Registration successful",
                data: { email: registrationData.email }
            };
        }
        const resp = await axios.post(`${this.BASE_URL}/auth/register`, registrationData);
        return resp.data;
    }



    static async loginUser(loginData) {
        if (USE_KOTLIN_BACKEND) {
            // Mock successful login
            const mockToken = 'mock-jwt-token-' + Date.now();
            this.saveToken(mockToken);
            this.saveRole(['ADMIN', 'CUSTOMER']);
            return {
                statusCode: 200,
                message: "Login successful",
                data: { token: mockToken }
            };
        }
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
        if (USE_KOTLIN_BACKEND) {
            // Get profile from Keycloak token
            const tokenParsed = keycloakService.getTokenParsed();
            if (tokenParsed) {
                return {
                    statusCode: 200,
                    data: {
                        id: tokenParsed.sub || 'unknown',
                        email: tokenParsed.email || '',
                        name: tokenParsed.name || `${tokenParsed.given_name || ''} ${tokenParsed.family_name || ''}`.trim() || tokenParsed.preferred_username || 'User',
                        firstName: tokenParsed.given_name || '',
                        lastName: tokenParsed.family_name || '',
                        phoneNumber: tokenParsed.phone_number || '',
                        address: tokenParsed.address || '',
                        profileUrl: tokenParsed.picture || '',
                        active: true,
                        roles: this.getRoles().map(r => ({ name: r }))
                    }
                };
            }
            return { statusCode: 401, data: null };
        }
        const resp = await axios.get(`${this.BASE_URL}/users/account`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async updateProfile(formData) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, message: "Profile updated" };
        }
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
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, data: [] };
        }
        const resp = await axios.get(`${this.BASE_URL}/users/saved-emails`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async addSavedEmail(email) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, message: "Email saved" };
        }
        const resp = await axios.post(`${this.BASE_URL}/users/saved-emails?email=${encodeURIComponent(email)}`, null, {
            headers: this.getHeader()
        });
        return resp.data;
    }


    static async deactivateProfile() {
        if (USE_KOTLIN_BACKEND) {
            this.logout();
            return { statusCode: 200, message: "Profile deactivated" };
        }
        const resp = await axios.delete(`${this.BASE_URL}/users/deactivate`, {
            headers: this.getHeader()
        });
        return resp.data;
    }














    //ORDER SECTION (maps to Bookings in Kotlin backend)

    static async placeOrder() {
        // This would normally checkout a cart, but Kotlin backend doesn't have cart
        return { statusCode: 200, message: "Order placed" };
    }


    static async updateOrderStatus(body) {
        if (USE_KOTLIN_BACKEND) {
            const requestBody = {
                requestType: "updateBooking",
                booking: {
                    id: body.id || body.orderId,
                    title: body.title,
                    description: body.description,
                    lock: body.lock
                }
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/bookings/update`, requestBody, {
                    headers: this.getHeader()
                });
                return {
                    statusCode: 200,
                    data: this.transformBookingFromBackend(resp.data.booking),
                    message: "Booking updated successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.put(`${this.BASE_URL}/orders/update`, body, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async getAllOrders(orderStatus, page = 0, size = 200, name) {
        if (USE_KOTLIN_BACKEND) {
            const requestBody = {
                requestType: "searchBookings",
                page: page + 1,
                pageSize: size
            };

            if (orderStatus && orderStatus !== 'ALL' && orderStatus !== 'all') {
                requestBody.bookingFilter = {
                    status: orderStatus.toUpperCase()
                };
            }

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/bookings/search`, requestBody, {
                    headers: this.getHeader()
                });

                const bookings = (resp.data.bookings || []).map(b => this.transformBookingFromBackend(b));

                // Return in the format expected by the frontend (with content wrapper)
                return {
                    statusCode: 200,
                    data: {
                        content: bookings,
                        totalElements: resp.data.total || bookings.length,
                        totalPages: 1,
                        number: page,
                        size: size
                    },
                    total: resp.data.total || bookings.length
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.message || error.message,
                    data: { content: [], totalElements: 0 }
                };
            }
        }

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
        if (USE_KOTLIN_BACKEND) {
            const requestBody = {
                requestType: "searchBookings"
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/bookings/search`, requestBody, {
                    headers: this.getHeader()
                });

                const bookings = (resp.data.bookings || []).map(b => this.transformBookingFromBackend(b));

                return {
                    statusCode: 200,
                    data: bookings
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.message || error.message,
                    data: []
                };
            }
        }

        const resp = await axios.get(`${this.BASE_URL}/orders/me`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async getOrderById(id) {
        if (USE_KOTLIN_BACKEND) {
            const requestBody = {
                requestType: "readBooking",
                booking: { id }
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/bookings/read`, requestBody, {
                    headers: this.getHeader()
                });

                return {
                    statusCode: 200,
                    data: this.transformBookingFromBackend(resp.data.booking)
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.message || error.message
                };
            }
        }

        const resp = await axios.get(`${this.BASE_URL}/orders/${id}`, {
            headers: this.getHeader()
        })
        return resp.data;
    }


    static async deleteOrder(id) {
        if (USE_KOTLIN_BACKEND) {
            try {
                // First get the booking to obtain its lock
                const bookingResp = await this.getOrderById(id);
                const lock = bookingResp.data?.lock || '';

                const requestBody = {
                    requestType: "deleteBooking",
                    booking: { id, lock }
                };

                await axios.post(`${this.BASE_URL}/v1/bookings/delete`, requestBody, {
                    headers: this.getHeader()
                });

                return {
                    statusCode: 200,
                    message: "Booking cancelled successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.delete(`${this.BASE_URL}/orders/${id}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async countTotalActiveCustomers() {
        // Mock for Kotlin backend
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, data: 2 };
        }
        const resp = await axios.get(`${this.BASE_URL}/orders/unique-customers`, {
            headers: this.getHeader()
        })
        return resp.data;
    }



























    /* RESOURCES SECTION */
    static async addResource(formData) {
        if (USE_KOTLIN_BACKEND) {
            // Extract data from FormData or plain object
            const resourceData = formData instanceof FormData
                ? Object.fromEntries(formData.entries())
                : formData;

            const requestBody = {
                requestType: "createResource",
                resource: {
                    name: resourceData.name || '',
                    description: resourceData.description || '',
                    type: resourceData.type || 'OTHER',
                    location: resourceData.location || '',
                    imageUrl: resourceData.imageUrl || '',
                    pricePerSlot: parseFloat(resourceData.pricePerSlot) || 0,
                    unitsCount: parseInt(resourceData.unitsCount) || 1,
                    openTime: resourceData.openTime || '09:00',
                    closeTime: resourceData.closeTime || '18:00'
                }
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/resources/create`, requestBody, {
                    headers: this.getHeader()
                });
                return {
                    statusCode: 200,
                    data: this.transformResourceFromBackend(resp.data.resource),
                    message: "Resource created successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.post(`${this.BASE_URL}/resources`, formData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return resp.data;
    }

    static async updateResource(formData) {
        if (USE_KOTLIN_BACKEND) {
            const resourceData = formData instanceof FormData
                ? Object.fromEntries(formData.entries())
                : formData;

            const requestBody = {
                requestType: "updateResource",
                resource: {
                    id: resourceData.id,
                    name: resourceData.name || '',
                    description: resourceData.description || '',
                    type: resourceData.type || 'OTHER',
                    location: resourceData.location || '',
                    imageUrl: resourceData.imageUrl || '',
                    pricePerSlot: parseFloat(resourceData.pricePerSlot) || 0,
                    unitsCount: parseInt(resourceData.unitsCount) || 1,
                    openTime: resourceData.openTime || '09:00',
                    closeTime: resourceData.closeTime || '18:00',
                    lock: resourceData.lock || ''
                }
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/resources/update`, requestBody, {
                    headers: this.getHeader()
                });
                return {
                    statusCode: 200,
                    data: this.transformResourceFromBackend(resp.data.resource),
                    message: "Resource updated successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.put(`${this.BASE_URL}/resources`, formData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        });
        return resp.data;
    }

    static async deleteResource(id) {
        if (USE_KOTLIN_BACKEND) {
            try {
                // First get the resource to obtain its lock
                const resourceResp = await this.getResourceById(id);
                const lock = resourceResp.data?.lock || '';

                const requestBody = {
                    requestType: "deleteResource",
                    resource: { id, lock }
                };

                await axios.post(`${this.BASE_URL}/v1/resources/delete`, requestBody, {
                    headers: this.getHeader()
                });

                return {
                    statusCode: 200,
                    message: "Resource deleted successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.delete(`${this.BASE_URL}/resources/${id}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getResourceById(id) {
        if (USE_KOTLIN_BACKEND) {
            try {
                const requestBody = {
                    requestType: "readResource",
                    resource: { id }
                };

                const resp = await axios.post(`${this.BASE_URL}/v1/resources/read`, requestBody, {
                    headers: this.getHeader()
                });

                return {
                    statusCode: 200,
                    data: this.transformResourceFromBackend(resp.data.resource)
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.message || error.message
                };
            }
        }

        const resp = await axios.get(`${this.BASE_URL}/resources/${id}`);
        return resp.data;
    }

    static async getAllResources(params = {}) {
        if (USE_KOTLIN_BACKEND) {
            try {
                const requestBody = {
                    requestType: "searchResources",
                    resourceFilter: {}
                };

                // Map frontend params to backend filter
                if (params.type) {
                    requestBody.resourceFilter.type = params.type;
                }
                if (params.search || params.name) {
                    requestBody.resourceFilter.location = params.search || params.name;
                }

                const resp = await axios.post(`${this.BASE_URL}/v1/resources/search`, requestBody, {
                    headers: this.getHeader()
                });

                const resources = (resp.data.resources || []).map(r => this.transformResourceFromBackend(r));

                return {
                    statusCode: 200,
                    data: resources,
                    total: resp.data.resources?.length || 0
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.message || error.message,
                    data: []
                };
            }
        }

        const resp = await axios.get(`${this.BASE_URL}/resources`, { params });
        return resp.data;
    }

    // Helper to transform resource from Kotlin backend format
    static transformResourceFromBackend(resource) {
        if (!resource) return null;
        return {
            id: resource.id,
            name: resource.name,
            description: resource.description,
            type: resource.type,
            location: resource.location,
            imageUrl: resource.imageUrl,
            pricePerSlot: resource.pricePerSlot,
            unitsCount: resource.unitsCount,
            openTime: resource.openTime,
            closeTime: resource.closeTime,
            lock: resource.lock,
            rating: resource.rating || 0,
            active: true
        };
    }










    static async bookResource(id, body) {
        if (USE_KOTLIN_BACKEND) {
            const requestBody = {
                requestType: "createBooking",
                booking: {
                    resourceId: id,
                    title: body.title || 'Booking',
                    description: body.description || '',
                    slots: (body.slots || []).map(slot => ({
                        slotStart: slot.slotStart || slot.startTime,
                        slotEnd: slot.slotEnd || slot.endTime,
                        price: slot.price || 0
                    }))
                }
            };

            try {
                const resp = await axios.post(`${this.BASE_URL}/v1/bookings/create`, requestBody, {
                    headers: this.getHeader()
                });
                return {
                    statusCode: 200,
                    data: this.transformBookingFromBackend(resp.data.booking),
                    message: "Booking created successfully"
                };
            } catch (error) {
                return {
                    statusCode: error.response?.status || 500,
                    message: error.response?.data?.errors?.[0]?.message || error.message
                };
            }
        }

        const resp = await axios.post(`${this.BASE_URL}/resources/${id}/book`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async bookResourceBatch(id, body) {
        // Use the same create booking endpoint for batch
        return this.bookResource(id, body);
    }

    static async bookResourceMulti(id, body) {
        // Use the same create booking endpoint for multi
        return this.bookResource(id, body);
    }

    // Helper to transform booking from Kotlin backend format
    static transformBookingFromBackend(booking) {
        if (!booking) return null;
        const totalPrice = (booking.slots || []).reduce((sum, slot) => sum + (slot.price || 0), 0);
        const firstSlot = (booking.slots || [])[0];
        const slotStart = firstSlot?.slotStart || new Date().toISOString();
        const slotEnd = firstSlot?.slotEnd || new Date().toISOString();

        return {
            id: booking.id,
            orderId: booking.id,
            resourceId: booking.resourceId,
            resourceName: booking.resourceName || 'Resource',
            title: booking.title,
            description: booking.description,
            status: booking.status || 'PENDING',
            orderStatus: booking.status || 'PENDING',
            paymentStatus: 'PENDING', // Mock - Kotlin backend doesn't have payments yet
            slots: (booking.slots || []).map(s => ({
                slotStart: s.slotStart,
                slotEnd: s.slotEnd,
                price: s.price
            })),
            // Fields expected by AdminOrdersPage and AdminDashboardPage
            orderItems: (booking.slots || []).map((s, idx) => ({
                id: `${booking.id}-slot-${idx}`,
                itemName: `Time Slot ${idx + 1}`,
                quantity: 1,
                price: s.price || 0
            })),
            orderDate: booking.createdAt || slotStart,
            totalAmount: totalPrice,
            totalPrice: totalPrice,
            booking: booking.id,
            bookingTitle: booking.title || 'Booking',
            bookingDetails: (booking.slots || []).map(s =>
                `${new Date(s.slotStart).toLocaleString()} - ${new Date(s.slotEnd).toLocaleTimeString()}`
            ).join(', '),
            user: {
                id: booking.userId || 'mock-user',
                name: 'Test User',
                email: 'user@example.com'
            },
            lock: booking.lock,
            createdAt: booking.createdAt,
            userId: booking.userId
        };
    }


    /**PAYMENT SESSION */

    //funtion to create payment intent
    static async proceedForPayment(body) {
        if (USE_KOTLIN_BACKEND) {
            // Mock payment
            return {
                statusCode: 200,
                data: {
                    clientSecret: 'mock-client-secret-' + Date.now(),
                    paymentIntentId: 'mock-payment-intent-' + Date.now()
                }
            };
        }

        const resp = await axios.post(`${this.BASE_URL}/payments/pay`, body, {
            headers: this.getHeader()
        });
        return resp.data; //return the resp containg the stripe transaction id for this transaction
    }

    //TO UPDATE PAYMENT WHEN IT HAS BEEN COMPLETED
    static async updateOrderPayment(body) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, message: "Payment updated" };
        }
        const resp = await axios.put(`${this.BASE_URL}/payments/update`, body, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAllPayments() {
        if (USE_KOTLIN_BACKEND) {
            // Get bookings and create mock payments from them
            const ordersResp = await this.getAllOrders();
            const bookings = ordersResp.data?.content || [];

            // Create mock payments based on bookings
            const payments = bookings.map((booking, idx) => ({
                id: `payment-${booking.id}`,
                orderId: booking.id,
                amount: booking.totalAmount || 0,
                paymentStatus: booking.orderStatus === 'CONFIRMED' ? 'COMPLETED' : 'PENDING',
                paymentDate: booking.orderDate || new Date().toISOString(),
                paymentMethod: 'CARD',
                transactionId: `txn-${Date.now()}-${idx}`
            }));

            return { statusCode: 200, data: payments };
        }
        const resp = await axios.get(`${this.BASE_URL}/payments/all`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAPaymentById(paymentId) {
        if (USE_KOTLIN_BACKEND) {
            return {
                statusCode: 200,
                data: {
                    id: paymentId,
                    amount: 100.0,
                    paymentStatus: 'COMPLETED',
                    paymentDate: new Date().toISOString(),
                    paymentMethod: 'CARD'
                }
            };
        }
        const resp = await axios.get(`${this.BASE_URL}/payments/${paymentId}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }







    /* ADMIN USERS & ROLES */
    static async getAllUsers() {
        if (USE_KOTLIN_BACKEND) {
            // Fetch users from Keycloak Admin API via nginx proxy
            try {
                // Use nginx proxy to avoid CORS issues
                const keycloakUrl = '/keycloak';

                // Get admin token from Keycloak
                const adminTokenResponse = await axios.post(
                    `${keycloakUrl}/realms/master/protocol/openid-connect/token`,
                    new URLSearchParams({
                        username: 'admin',
                        password: 'admin123',
                        grant_type: 'password',
                        client_id: 'admin-cli'
                    }),
                    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
                );

                const adminToken = adminTokenResponse.data.access_token;

                // Fetch users from Keycloak
                const usersResponse = await axios.get(
                    `${keycloakUrl}/admin/realms/ajasta/users`,
                    {
                        headers: { Authorization: `Bearer ${adminToken}` },
                        params: { max: 100 }
                    }
                );

                // Fetch roles for each user
                const users = await Promise.all(
                    usersResponse.data.map(async (user) => {
                        try {
                            const rolesResponse = await axios.get(
                                `${keycloakUrl}/admin/realms/ajasta/users/${user.id}/role-mappings/realm`,
                                { headers: { Authorization: `Bearer ${adminToken}` } }
                            );
                            return {
                                id: user.id,
                                email: user.email || '',
                                firstName: user.firstName || '',
                                lastName: user.lastName || '',
                                name: `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username || '',
                                username: user.username,
                                active: user.enabled,
                                roles: rolesResponse.data.map(r => ({ name: r.name.toUpperCase() }))
                            };
                        } catch {
                            return {
                                id: user.id,
                                email: user.email || '',
                                firstName: user.firstName || '',
                                lastName: user.lastName || '',
                                name: `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username || '',
                                username: user.username,
                                active: user.enabled,
                                roles: []
                            };
                        }
                    })
                );

                return { statusCode: 200, data: users };
            } catch (error) {
                console.error('Failed to fetch users from Keycloak:', error);
                return { statusCode: 500, data: [], message: 'Failed to fetch users' };
            }
        }
        const resp = await axios.get(`${this.BASE_URL}/users/all`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async getAllRoles() {
        if (USE_KOTLIN_BACKEND) {
            return {
                statusCode: 200,
                data: [
                    { id: 1, name: 'ADMIN' },
                    { id: 2, name: 'CUSTOMER' },
                    { id: 3, name: 'RESOURCE_MANAGER' }
                ]
            };
        }
        const resp = await axios.get(`${this.BASE_URL}/roles`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async updateUserRoles(userId, roles) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, message: "Roles updated" };
        }
        const resp = await axios.put(`${this.BASE_URL}/users/${userId}/roles`, roles, {
            headers: this.getHeader()
        });
        return resp.data;
    }


    /* REVIEWS SECTION */
    static async getResourceReviews(resourceId) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, data: [] };
        }
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/${resourceId}`);
        return resp.data;
    }

    static async getResourceAverageRating(resourceId) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, data: 0 };
        }
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/average/${resourceId}`);
        return resp.data;
    }

    static async getReviewEligibility(resourceId) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, data: { eligible: false } };
        }
        const resp = await axios.get(`${this.BASE_URL}/reviews/resource/eligibility/${resourceId}`, {
            headers: this.getHeader()
        });
        return resp.data;
    }

    static async createReview(reviewDTO) {
        if (USE_KOTLIN_BACKEND) {
            return { statusCode: 200, message: "Review created" };
        }
        const resp = await axios.post(`${this.BASE_URL}/reviews`, reviewDTO, {
            headers: this.getHeader()
        });
        return resp.data;
    }
}