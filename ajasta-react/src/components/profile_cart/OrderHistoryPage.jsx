import { useState, useEffect } from 'react';
import ApiService from '../../services/ApiService';
import { useNavigate } from 'react-router-dom';
import { useError } from '../common/ErrorDisplay';


const OrderHistoryPage = () => {

    const [orders, setOrders] = useState(null);
    const [message, setMessage] = useState(null);
    const navigate = useNavigate();
    const { ErrorDisplay, showError } = useError();


    useEffect(() => {
        const fetchOrders = async () => {
            try {
                const response = await ApiService.getMyOrders();
                if (response.statusCode === 200) {
                    setOrders(response.data || []);
                }
            } catch (error) {
                showError(error.response?.data?.message || error.message);
            }
        };
        fetchOrders();
    }, [showError]);


    const formatDate = (dateString) => {
        const date = new Date(dateString);
        const options = {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        };
        return date.toLocaleDateString(undefined, options);
    };


    const handleLeaveReview = (orderId, menuId) => {
        navigate(`/leave-review?orderId=${orderId}&menuId=${menuId}`);
    };

    const handleDeleteOrder = async (orderId) => {
        const confirm = window.confirm('Are you sure you want to delete this order? This action cannot be undone.');
        if (!confirm) return;
        try {
            const resp = await ApiService.deleteOrder(orderId);
            if (resp.statusCode === 200) {
                setOrders((prev) => (prev || []).filter((o) => o.id !== orderId));
                setMessage('Order deleted successfully');
                setTimeout(() => setMessage(null), 3000);
            } else {
                showError(resp.message || 'Failed to delete order');
            }
        } catch (e) {
            showError(e.response?.data?.message || e.message);
        }
    };


    if (!orders || orders.length === 0) {
        return (
            <div className="order-history-container">
                <div className="no-orders-message">
                    <p>You have no previous orders.</p>
                </div>
            </div>
        );
    }


    return (
        <div className="order-history-container">
            {/* Render the ErrorDisplay component */}
            <ErrorDisplay />
            {message && (
                <p className="success">{message}</p>
            )}
            <h1 className="order-history-title">Your Order History</h1>
            <div className="order-list">
                {orders.map((order) => (
                    <div key={order.id} className="order-card">
                        <div className="order-header">
                            <span className="order-id">Order ID: {order.id}</span>
                            <span className="order-date">
                                Date: {formatDate(order.orderDate)}
                            </span>
                            <span className="order-status">
                                Status: <span className={`status-${order.orderStatus.toLowerCase()}`}>{order.orderStatus}</span>
                            </span>
                            <span className="order-total">
                                Total: ${order.totalAmount.toFixed(2)}
                            </span>
                            <button
                                className="remove-btn"
                                onClick={() => handleDeleteOrder(order.id)}
                                style={{ marginLeft: 'auto' }}
                            >
                                Delete
                            </button>
                        </div>
                        <div className="order-items">
                            <h2 className="order-items-title">Order Items:</h2>
                            {order.orderItems && order.orderItems.length > 0 ? (
                                order.orderItems.map((item) => (
                                    <div key={item.id} className="order-item">
                                        <div className="item-details">
                                            <span className="item-name">{item.itemName || 'Item'}</span>
                                            <span className="item-quantity">Quantity: {item.quantity}</span>
                                            <span className="item-price">
                                                Price: ${item.pricePerUnit.toFixed(2)}
                                            </span>
                                            <span className="subtotal">
                                                Subtotal: ${item.subtotal.toFixed(2)}
                                            </span>
                                        </div>
                                        <div className="item-image-container">
                                            {item.itemImageUrl && (
                                                <img src={item.itemImageUrl} alt={item.itemName || 'Item'} className="item-image" />
                                            )}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                (order.booking || order.bookingDetails) && (
                                    <div className="order-item">
                                        <div className="item-details">
                                            <span className="item-name">{order.bookingTitle || 'Booking'}</span>
                                            <pre className="item-description" style={{ whiteSpace: 'pre-wrap', margin: '8px 0' }}>
{order.bookingDetails || 'Booking details not available'}
                                            </pre>
                                            <span className="item-price">Total: ${order.totalAmount.toFixed(2)}</span>
                                        </div>
                                    </div>
                                )
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );

}
export default OrderHistoryPage;