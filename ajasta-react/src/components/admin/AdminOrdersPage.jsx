import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import ApiService from '../../services/ApiService';
import { useError } from '../common/ErrorDisplay';




const AdminOrdersPage = () => {

    const [orders, setOrders] = useState([]);
    const [filter, setFilter] = useState('all');
    const [search, setSearch] = useState('');

    const { ErrorDisplay, showError } = useError();
    const navigate = useNavigate();


    const fetchOrders = useCallback(async () => {
        try {
            const response = await ApiService.getAllOrders(filter === 'all' ? null : filter, 0, 200, search);

            if (response.statusCode === 200) {
                setOrders(response.data.content);
            }

        } catch (error) {
            showError(error.response?.data?.message || error.message);
        }
    }, [filter, search, showError]);

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);



    const handleViewOrder = (id) => {
        navigate(`/admin/orders/${id}`);
    };




    return (
        <div className="admin-orders">
            <ErrorDisplay />
            <div className="content-header">
                <h1>Orders Management</h1>
                <div className="order-filters" style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                    <select value={filter} onChange={(e) => setFilter(e.target.value)}>
                        <option value="all">All Orders</option>
                        <option value="INITIALIZED">Initialized</option>
                        <option value="CONFIRMED">Confirmed</option>
                        <option value="CANCELLED">Cancelled</option>
                        <option value="FAILED">Failed</option>
                    </select>
                    <input
                        type="text"
                        placeholder="Search by resource name"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ padding: '8px 10px', border: '1px solid #ccc', borderRadius: 4 }}
                    />
                </div>
            </div>

            <div className="orders-table">
                <table>
                    <thead>
                        <tr>
                            <th>Order ID</th>
                            <th>Date</th>
                            <th>Items</th>
                            <th>Total</th>
                            <th>Status</th>
                            <th>Payment</th>
                            <th>Details</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {orders.map(order => (
                            <tr key={order.id}>
                                <td>#{order.id}</td>
                                <td>{new Date(order.orderDate).toLocaleDateString()}</td>
                                <td>{order.orderItems.reduce((sum, item) => sum + item.quantity, 0)}</td>
                                <td>${order.totalAmount.toFixed(2)}</td>
                                <td>
                                    <span className={`status ${order.orderStatus.toLowerCase()}`}>
                                        {order.orderStatus}
                                    </span>
                                </td>
                                <td>
                                    <span className={`payment-status ${order.paymentStatus?.toLowerCase() || 'pending'}`}>
                                        {order.paymentStatus || 'PENDING'}
                                    </span>
                                </td>
                                <td>
                                    {order.orderItems && order.orderItems.length > 0 ? (
                                        <span>{order.orderItems.map(i => i.itemName).filter(Boolean).slice(0,2).join(', ')}{order.orderItems.length > 2 ? '…' : ''}</span>
                                    ) : (
                                        (order.booking || order.bookingTitle) ? (
                                            <span title={order.bookingDetails || ''}>{order.bookingTitle || 'Booking'}</span>
                                        ) : (
                                            <span>-</span>
                                        )
                                    )}
                                </td>
                                <td className="actions">
                                    <button
                                        className="view-btn"
                                        onClick={() => handleViewOrder(order.id)}
                                    >
                                        View
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );




}
export default AdminOrdersPage;