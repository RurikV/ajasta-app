import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Mock react-router-dom as virtual to avoid real dependency resolution
jest.mock('react-router-dom', () => ({
  __esModule: true,
  MemoryRouter: ({ children }) => children,
  useNavigate: () => () => {},
}), { virtual: true });
const OrderHistoryPage = require('../OrderHistoryPage').default;
const ApiService = require('../../../services/ApiService').default;

jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getMyOrders: jest.fn(),
    getOrderItemById: jest.fn(),
    deleteOrder: jest.fn(),
  }
}));

// Minimal ErrorDisplay hook consumer is used within, no special mocks needed

describe('OrderHistoryPage booking metadata rendering', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows bookingTitle and bookingDetails when no orderItems are present', async () => {
    const ordersResponse = {
      statusCode: 200,
      data: [
        {
          id: 101,
          orderDate: new Date().toISOString(),
          orderStatus: 'INITIALIZED',
          totalAmount: 30.0,
          orderItems: [],
          booking: true,
          bookingTitle: 'Booking: Court A (2 slot(s))',
          bookingDetails: 'Date: 2025-01-10\n- 09:00 - 09:30 | Unit 1\n- 09:30 - 10:00 | Unit 1\nTotal slots: 2\nPrice per slot: 15.00\nTotal: 30.00',
        }
      ],
    };
    ApiService.getMyOrders.mockResolvedValueOnce(ordersResponse);

    render(
      <MemoryRouter>
        <OrderHistoryPage />
      </MemoryRouter>
    );

    // Wait for data to load
    await waitFor(() => expect(ApiService.getMyOrders).toHaveBeenCalled());

    expect(await screen.findByText(/Your Order History/i)).toBeInTheDocument();
    expect(screen.getByText('Booking: Court A (2 slot(s))')).toBeInTheDocument();
    expect(screen.getAllByText(/Total: \$/i).length).toBeGreaterThan(0);
    // The details are rendered inside a <pre> block
    expect(screen.getByText(/09:00 - 09:30/)).toBeInTheDocument();
    expect(screen.getByText(/09:30 - 10:00/)).toBeInTheDocument();
  });
});
