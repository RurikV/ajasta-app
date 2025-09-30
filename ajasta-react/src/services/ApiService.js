import axios from "axios";

export default class ApiService {


    static BASE_URL = "http://localhost:8090/api";
    // static BASE_URL = "http://18.221.120.102:8090/api"; //production base url

    // In-memory cache for roles (never persisted to localStorage)
    static cachedRoles = null;

    static saveToken(token) {
        localStorage.setItem("token", token);
    }

    static getToken() {
        return localStorage.getItem("token");
    }

    // Save roles in-memory only (do NOT store in localStorage)
    static saveRole(roles) {
        try {
            if (!roles) { this.cachedRoles = null; return; }
            const arr = Array.isArray(roles) ? roles : [roles];
            this.cachedRoles = Array.from(new Set(arr.map(r => String(r).replace(/^ROLE_/,'').toUpperCase())));
        } catch {
            this.cachedRoles = null;
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

    // Get roles from token (preferred) or from in-memory cache
    static getRoles() {
        const fromToken = this.parseRolesFromToken();
        if (fromToken && fromToken.length) return fromToken;
        return this.cachedRoles || [];
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

    // Check if the user is a delivery person
    static isDeliveryPerson() {
        return this.hasRole('DELIVERY');
    }


    static logout() {
        localStorage.removeItem("token");
        // Clear any legacy roles key and reset in-memory cache
        try { localStorage.removeItem("roles"); } catch {}
        this.cachedRoles = null;
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

    static async initiateDelivery(body) {
        const resp = await axios.post(`${this.BASE_URL}/orders/initiate-delivery`, body, {
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


    static async getAllOrders(orderStatus, page = 0, size = 200) {

        let url = `${this.BASE_URL}/orders/all`;

        if (orderStatus) {
            url = `${this.BASE_URL}/orders/all?orderStatus=${encodeURIComponent(orderStatus)}&page=${page}&size=${size}`
        }

        const resp = await axios.get(url, {
            headers: this.getHeader()
        })
        return resp.data;

    }

    static async getDeliveryOrders(orderStatus, page = 0, size = 200) {
        // Try a set of likely delivery endpoints and query variants; return the first successful response
        const candidates = [
            `${this.BASE_URL}/orders/for-delivery`,
            `${this.BASE_URL}/orders/ready-for-delivery`,
            `${this.BASE_URL}/orders/available-for-delivery`,
            `${this.BASE_URL}/orders/available`,
            `${this.BASE_URL}/orders/deliveries`,
            `${this.BASE_URL}/orders/delivery`,
            `${this.BASE_URL}/orders/my-deliveries`,
            `${this.BASE_URL}/orders/assigned`,
            `${this.BASE_URL}/orders/assigned-to-me`,
            `${this.BASE_URL}/delivery/orders`,
            `${this.BASE_URL}/orders/driver`,
            `${this.BASE_URL}/orders/courier`,
            `${this.BASE_URL}/orders/rider`,
            `${this.BASE_URL}/orders/ready`
        ];

        // Build query string variants to support different backends
        const qsVariants = [];
        const encoded = encodeURIComponent(orderStatus || '');
        // with orderStatus
        qsVariants.push(`?orderStatus=${encoded}&page=${page}&size=${size}`);
        // with status
        qsVariants.push(`?status=${encoded}&page=${page}&size=${size}`);
        // no status filter
        qsVariants.push(`?page=${page}&size=${size}`);
        // bare (no params) as last resort
        qsVariants.push('');

        for (const base of candidates) {
            for (const qs of qsVariants) {
                try {
                    const url = `${base}${qs}`;
                    const resp = await axios.get(url, {
                        headers: this.getHeader()
                    });
                    return resp.data;
                } catch (e) {
                    const status = e.response?.status;
                    const msg = e.response?.data?.message || '';
                    // Continue probing on typical discovery errors or framework messages
                    if (
                        status === 404 ||
                        status === 403 ||
                        status === 400 ||
                        status === 405 ||
                        msg.includes('Failed to convert value of type') ||
                        msg.includes('No static resource')
                    ) {
                        continue;
                    }
                    // Bubble up 401 (unauthorized) and other unexpected errors
                    if (status === 401) throw e;
                    throw e;
                }
            }
        }
        // As a last resort, if admin, reuse admin listing
        if (this.isAdmin()) {
            return await this.getAllOrders(orderStatus, page, size);
        }
        // Graceful fallback for delivery users when endpoint is unavailable
        return { statusCode: 403, message: 'Access Denied: Delivery orders endpoint is not accessible for your role.' };
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


    static async getOrderItemById(id) {
        const resp = await axios.get(`${this.BASE_URL}/orders/order-item/${id}`, {
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






}