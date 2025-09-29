import React, { useState, useEffect, useCallback } from 'react';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';

const DeliveriesPage = () => {
    const { ErrorDisplay, showError } = useError();
    const [deliveries, setDeliveries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState('');

    const fetchDeliveries = useCallback(async () => {
        try {
            setLoading(true);
            // Fetch orders visible to delivery personnel
            const response = await ApiService.getDeliveryOrders(statusFilter || 'CONFIRMED');
            if (response.statusCode === 200) {
                setDeliveries(response.data.content || response.data);
            } else if (response.statusCode === 403) {
                // Graceful handling: no error banner; show empty state instead
                setDeliveries([]);
            } else {
                showError(response.message || 'Failed to fetch deliveries');
            }
        } catch (error) {
            showError(error.response?.data?.message || error.message || 'Failed to fetch deliveries');
        } finally {
            setLoading(false);
        }
    }, [statusFilter, showError]);

    useEffect(() => {
        fetchDeliveries();
    }, [fetchDeliveries]);

    const updateOrderStatus = async (orderId, newStatus) => {
        try {
            const response = await ApiService.updateOrderStatus({
                id: orderId,
                orderStatus: newStatus
            });
            if (response.statusCode === 200) {
                // Refresh the deliveries list
                fetchDeliveries();
            } else {
                showError(response.message || 'Failed to update order status');
            }
        } catch (error) {
            showError(error.response?.data?.message || error.message || 'Failed to update order status');
        }
    };

    const handleStatusChange = (orderId, newStatus) => {
        const confirmMessage = newStatus === 'DELIVERED' 
            ? 'Confirm delivery completion?' 
            : `Update order status to ${newStatus}?`;
        
        if (window.confirm(confirmMessage)) {
            updateOrderStatus(orderId, newStatus);
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'CONFIRMED':
                return '#ffa500'; // orange
            case 'ON_THE_WAY':
                return '#007bff'; // blue
            case 'DELIVERED':
                return '#28a745'; // green
            default:
                return '#6c757d'; // gray
        }
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleDateString() + ' ' + 
               new Date(dateString).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    };

    if (loading) {
        return (
            <div className="deliveries-page">
                <div className="loading-container">
                    <p>Loading deliveries...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="deliveries-page">
            <ErrorDisplay />
            
            <div className="deliveries-header">
                <h1>My Deliveries</h1>
                <div className="filter-controls">
                    <label htmlFor="statusFilter">Filter by status:</label>
                    <select 
                        id="statusFilter" 
                        value={statusFilter} 
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="status-filter"
                    >
                        <option value="">All Deliveries</option>
                        <option value="CONFIRMED">Confirmed</option>
                        <option value="ON_THE_WAY">On the way</option>
                        <option value="DELIVERED">Delivered</option>
                    </select>
                </div>
            </div>

            {deliveries.length === 0 ? (
                <div className="no-deliveries">
                    <p>No deliveries found{statusFilter ? ` with status "${statusFilter}"` : ''}.</p>
                </div>
            ) : (
                <div className="deliveries-list">
                    {deliveries.map((order) => (
                        <div key={order.id} className="delivery-card">
                            <div className="delivery-header">
                                <h3>Order #{order.id}</h3>
                                <span 
                                    className="status-badge" 
                                    style={{ backgroundColor: getStatusColor(order.orderStatus || order.status), color: 'white' }}
                                >
                                    {(order.orderStatus || order.status)?.replace(/_/g, ' ')}
                                </span>
                            </div>
                            
                            <div className="delivery-details">
                                <div className="customer-info">
                                    <h4>Customer Information</h4>
                                    <p><strong>Name:</strong> {order.user?.name || 'N/A'}</p>
                                    <p><strong>Email:</strong> {order.user?.email || 'N/A'}</p>
                                    <p><strong>Phone:</strong> {order.user?.phoneNumber || 'N/A'}</p>
                                    <p><strong>Address:</strong> {order.user?.address || 'N/A'}</p>
                                </div>
                                
                                <div className="order-info">
                                    <h4>Order Information</h4>
                                    <p><strong>Order Date:</strong> {formatDate(order.createdAt)}</p>
                                    <p><strong>Total Amount:</strong> ${order.totalAmount?.toFixed(2) || '0.00'}</p>
                                    
                                    {order.orderItems && order.orderItems.length > 0 && (
                                        <div className="order-items">
                                            <h5>Items:</h5>
                                            <ul>
                                                {order.orderItems.map((item, index) => (
                                                    <li key={index}>
                                                        {item.quantity}x {item.menu?.name || 'Unknown Item'} 
                                                        (${(item.price * item.quantity).toFixed(2)})
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                </div>
                            </div>
                            
                            <div className="delivery-actions">
                                {(order.orderStatus || order.status) === 'CONFIRMED' && (
                                    <button 
                                        className="btn btn-primary"
                                        onClick={() => handleStatusChange(order.id, 'ON_THE_WAY')}
                                    >
                                        Start Delivery
                                    </button>
                                )}
                                {(order.orderStatus || order.status) === 'ON_THE_WAY' && (
                                    <button 
                                        className="btn btn-success"
                                        onClick={() => handleStatusChange(order.id, 'DELIVERED')}
                                    >
                                        Mark as Delivered
                                    </button>
                                )}
                                {(order.orderStatus || order.status) === 'DELIVERED' && (
                                    <span className="delivered-status">✓ Delivered</span>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default DeliveriesPage;