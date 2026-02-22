import axios from "axios";

/**
 * Kotlin Backend API Service
 * Adapter layer to connect the React frontend to the Kotlin backend
 *
 * The Kotlin backend uses a POST-based API with request types in the body:
 * - /v1/resources/create, /v1/resources/search, etc.
 * - /v1/bookings/create, /v1/bookings/search, etc.
 */

axios.defaults.withCredentials = true;

// Determine API base URL at runtime
const getApiBaseUrl = () => {
    // Check for explicit env/config
    const envUrl = (typeof process !== 'undefined' && process.env && process.env.REACT_APP_API_BASE_URL)
        || (typeof window !== 'undefined' && window.__API_BASE_URL);
    if (envUrl) return envUrl.replace(/\/$/, '');

    // Smart defaults
    if (typeof window !== 'undefined' && window.location) {
        const origin = window.location.origin;
        const port = window.location.port;
        const hostname = window.location.hostname;
        const isLocalhost = hostname === 'localhost' || hostname === '127.0.0.1';

        // CRA dev server with proxy
        if (isLocalhost && typeof process !== 'undefined' && process.env && process.env.NODE_ENV === 'development') {
            return `${origin}`;
        }

        // Static build at localhost:3000 (docker-compose nginx)
        if (isLocalhost && port === '3000') {
            return 'http://localhost:8090';
        }

        // Production with reverse proxy
        return origin;
    }
    return '';
};

class KotlinApiService {
    static BASE_URL = getApiBaseUrl();

    // In-memory cache for roles
    static cachedRoles = null;
    static roleListeners = new Set();

    // ============================================
    // Authentication (Mock for now - Kotlin backend has no auth yet)
    // ============================================

    static saveToken(token) {
        localStorage.setItem("token", token);
    }

    static getToken() {
        return localStorage.getItem("token");
    }

    static saveRole(roles) {
        if (!roles) {
            this.cachedRoles = null;
            return;
        }
        const arr = Array.isArray(roles) ? roles : [roles];
        this.cachedRoles = Array.from(new Set(arr.map(r => String(r).replace(/^ROLE_/, '').toUpperCase())));
    }

    static getRoles() {
        if (this.cachedRoles && this.cachedRoles.length) return this.cachedRoles;
        // Mock: return admin role for development
        if (this.isAuthenticated()) {
            return ['ADMIN', 'CUSTOMER'];
        }
        return [];
    }

    static hasRole(role) {
        const roles = this.getRoles();
        if (!roles || roles.length === 0) return false;
        const target = String(role).replace(/^ROLE_/, '').toUpperCase();
        return roles.includes(target);
    }

    static isAdmin() {
        return this.hasRole('ADMIN');
    }

    static isCustomer() {
        return this.hasRole('CUSTOMER');
    }

    static logout() {
        localStorage.removeItem("token");
        this.cachedRoles = null;
    }

    static isAuthenticated() {
        return !!this.getToken();
    }

    static getHeader() {
        const token = this.getToken();
        const headers = { "Content-Type": "application/json" };
        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }
        return headers;
    }

    // Mock auth for development
    static async registerUser(registrationData) {
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

    static async loginUser(loginData) {
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

    static async myProfile() {
        // Mock profile
        return {
            statusCode: 200,
            data: {
                id: "mock-user-id",
                email: "user@example.com",
                firstName: "Test",
                lastName: "User",
                roles: [{ name: 'ADMIN' }, { name: 'CUSTOMER' }]
            }
        };
    }

    static async updateProfile(formData) {
        return { statusCode: 200, message: "Profile updated" };
    }

    static async deactivateProfile() {
        this.logout();
        return { statusCode: 200, message: "Profile deactivated" };
    }

    // ============================================
    // Resources API
    // ============================================

    /**
     * Search resources - maps to Kotlin backend /v1/resources/search
     */
    static async getAllResources(params = {}) {
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

            const response = await axios.post(`${this.BASE_URL}/v1/resources/search`, requestBody, {
                headers: this.getHeader()
            });

            // Transform Kotlin backend response to frontend format
            const resources = (response.data.resources || []).map(r => this.transformResourceFromBackend(r));

            return {
                statusCode: 200,
                data: resources,
                total: response.data.resources?.length || 0
            };
        } catch (error) {
            console.error('Error fetching resources:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message,
                data: []
            };
        }
    }

    /**
     * Get resource by ID - maps to Kotlin backend /v1/resources/read
     */
    static async getResourceById(id) {
        try {
            const requestBody = {
                requestType: "readResource",
                resource: { id }
            };

            const response = await axios.post(`${this.BASE_URL}/v1/resources/read`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformResourceFromBackend(response.data.resource)
            };
        } catch (error) {
            console.error('Error fetching resource:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message
            };
        }
    }

    /**
     * Create resource - maps to Kotlin backend /v1/resources/create
     */
    static async addResource(formData) {
        try {
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

            const response = await axios.post(`${this.BASE_URL}/v1/resources/create`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformResourceFromBackend(response.data.resource),
                message: "Resource created successfully"
            };
        } catch (error) {
            console.error('Error creating resource:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Update resource - maps to Kotlin backend /v1/resources/update
     */
    static async updateResource(formData) {
        try {
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

            const response = await axios.post(`${this.BASE_URL}/v1/resources/update`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformResourceFromBackend(response.data.resource),
                message: "Resource updated successfully"
            };
        } catch (error) {
            console.error('Error updating resource:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Delete resource - maps to Kotlin backend /v1/resources/delete
     */
    static async deleteResource(id) {
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
            console.error('Error deleting resource:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Transform resource from Kotlin backend format to frontend format
     */
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
            active: true // Kotlin backend doesn't have active field yet
        };
    }

    // ============================================
    // Bookings API (maps to frontend's "orders" concept)
    // ============================================

    /**
     * Book a resource - maps to Kotlin backend /v1/bookings/create
     */
    static async bookResource(resourceId, bookingData) {
        try {
            const requestBody = {
                requestType: "createBooking",
                booking: {
                    resourceId: resourceId,
                    title: bookingData.title || 'Booking',
                    description: bookingData.description || '',
                    slots: (bookingData.slots || []).map(slot => ({
                        slotStart: slot.slotStart || slot.startTime,
                        slotEnd: slot.slotEnd || slot.endTime,
                        price: slot.price || 0
                    }))
                }
            };

            const response = await axios.post(`${this.BASE_URL}/v1/bookings/create`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformBookingFromBackend(response.data.booking),
                message: "Booking created successfully"
            };
        } catch (error) {
            console.error('Error creating booking:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Get all bookings (admin) - maps to Kotlin backend /v1/bookings/search
     */
    static async getAllOrders(status, page = 0, size = 200) {
        try {
            const requestBody = {
                requestType: "searchBookings",
                page: page + 1, // Kotlin backend uses 1-based pages
                pageSize: size
            };

            if (status && status !== 'ALL') {
                requestBody.bookingFilter = {
                    status: status
                };
            }

            const response = await axios.post(`${this.BASE_URL}/v1/bookings/search`, requestBody, {
                headers: this.getHeader()
            });

            const bookings = (response.data.bookings || []).map(b => this.transformBookingFromBackend(b));

            return {
                statusCode: 200,
                data: bookings,
                total: response.data.total || bookings.length
            };
        } catch (error) {
            console.error('Error fetching bookings:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message,
                data: []
            };
        }
    }

    /**
     * Get user's bookings - maps to Kotlin backend /v1/bookings/search
     */
    static async getMyOrders() {
        try {
            const requestBody = {
                requestType: "searchBookings"
            };

            const response = await axios.post(`${this.BASE_URL}/v1/bookings/search`, requestBody, {
                headers: this.getHeader()
            });

            const bookings = (response.data.bookings || []).map(b => this.transformBookingFromBackend(b));

            return {
                statusCode: 200,
                data: bookings
            };
        } catch (error) {
            console.error('Error fetching my bookings:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message,
                data: []
            };
        }
    }

    /**
     * Get booking by ID - maps to Kotlin backend /v1/bookings/read
     */
    static async getOrderById(id) {
        try {
            const requestBody = {
                requestType: "readBooking",
                booking: { id }
            };

            const response = await axios.post(`${this.BASE_URL}/v1/bookings/read`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformBookingFromBackend(response.data.booking)
            };
        } catch (error) {
            console.error('Error fetching booking:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message
            };
        }
    }

    /**
     * Update booking status - maps to Kotlin backend /v1/bookings/update
     */
    static async updateOrderStatus(body) {
        try {
            const requestBody = {
                requestType: "updateBooking",
                booking: {
                    id: body.id || body.orderId,
                    title: body.title,
                    description: body.description,
                    lock: body.lock
                }
            };

            const response = await axios.post(`${this.BASE_URL}/v1/bookings/update`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: this.transformBookingFromBackend(response.data.booking),
                message: "Booking updated successfully"
            };
        } catch (error) {
            console.error('Error updating booking:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Delete/cancel booking - maps to Kotlin backend /v1/bookings/delete
     */
    static async deleteOrder(id) {
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
            console.error('Error deleting booking:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.errors?.[0]?.message || error.message
            };
        }
    }

    /**
     * Transform booking from Kotlin backend format to frontend "order" format
     */
    static transformBookingFromBackend(booking) {
        if (!booking) return null;

        // Calculate total price from slots
        const totalPrice = (booking.slots || []).reduce((sum, slot) => sum + (slot.price || 0), 0);

        return {
            id: booking.id,
            orderId: booking.id, // Frontend uses orderId
            resourceId: booking.resourceId,
            resourceName: booking.resourceName || 'Resource',
            title: booking.title,
            description: booking.description,
            status: booking.status || 'PENDING',
            orderStatus: booking.status || 'PENDING', // Frontend uses orderStatus
            slots: (booking.slots || []).map(s => ({
                slotStart: s.slotStart,
                slotEnd: s.slotEnd,
                price: s.price
            })),
            totalPrice: totalPrice,
            totalAmount: totalPrice,
            lock: booking.lock,
            createdAt: booking.createdAt,
            userId: booking.userId
        };
    }

    // ============================================
    // Availability API
    // ============================================

    static async getResourceAvailability(resourceId, dateFrom, dateTo) {
        try {
            const requestBody = {
                requestType: "getAvailability",
                resourceId: resourceId,
                dateFrom: dateFrom,
                dateTo: dateTo
            };

            const response = await axios.post(`${this.BASE_URL}/v1/resources/availability`, requestBody, {
                headers: this.getHeader()
            });

            return {
                statusCode: 200,
                data: {
                    resourceId: response.data.resourceId,
                    availableSlots: response.data.availableSlots || []
                }
            };
        } catch (error) {
            console.error('Error fetching availability:', error);
            return {
                statusCode: error.response?.status || 500,
                message: error.response?.data?.message || error.message,
                data: { availableSlots: [] }
            };
        }
    }

    // ============================================
    // Payments API (Mock - Kotlin backend has no payment integration yet)
    // ============================================

    static async proceedForPayment(body) {
        // Mock payment
        return {
            statusCode: 200,
            data: {
                clientSecret: 'mock-client-secret-' + Date.now(),
                paymentIntentId: 'mock-payment-intent-' + Date.now()
            }
        };
    }

    static async updateOrderPayment(body) {
        return { statusCode: 200, message: "Payment updated" };
    }

    static async getAllPayments() {
        return { statusCode: 200, data: [] };
    }

    static async getAPaymentById(paymentId) {
        return { statusCode: 200, data: {} };
    }

    // ============================================
    // Users API (Mock)
    // ============================================

    static async getAllUsers() {
        return {
            statusCode: 200,
            data: [
                {
                    id: "mock-user-1",
                    email: "admin@example.com",
                    firstName: "Admin",
                    lastName: "User",
                    roles: [{ name: 'ADMIN' }]
                },
                {
                    id: "mock-user-2",
                    email: "customer@example.com",
                    firstName: "Customer",
                    lastName: "User",
                    roles: [{ name: 'CUSTOMER' }]
                }
            ]
        };
    }

    static async getAllRoles() {
        return {
            statusCode: 200,
            data: [
                { id: 1, name: 'ADMIN' },
                { id: 2, name: 'CUSTOMER' },
                { id: 3, name: 'RESOURCE_MANAGER' }
            ]
        };
    }

    static async updateUserRoles(userId, roles) {
        return { statusCode: 200, message: "Roles updated" };
    }

    static async countTotalActiveCustomers() {
        return { statusCode: 200, data: 2 };
    }

    static async placeOrder() {
        return { statusCode: 200, message: "Order placed" };
    }

    // ============================================
    // Reviews API (Mock)
    // ============================================

    static async getResourceReviews(resourceId) {
        return { statusCode: 200, data: [] };
    }

    static async getResourceAverageRating(resourceId) {
        return { statusCode: 200, data: 0 };
    }

    static async getReviewEligibility(resourceId) {
        return { statusCode: 200, data: { eligible: false } };
    }

    static async createReview(reviewDTO) {
        return { statusCode: 200, message: "Review created" };
    }

    // Saved emails (mock)
    static async getSavedEmails() {
        return { statusCode: 200, data: [] };
    }

    static async addSavedEmail(email) {
        return { statusCode: 200, message: "Email saved" };
    }
}

export default KotlinApiService;
