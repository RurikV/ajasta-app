import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  __esModule: true,
  MemoryRouter: ({ children }) => children,
  useParams: () => ({ id: '1' }),
}), { virtual: true });

// Mock ApiService BEFORE importing component
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getResourceById: jest.fn(),
    isAuthenticated: jest.fn(() => true),
    isCustomer: jest.fn(() => true),
    isAdmin: jest.fn(() => false),
    getRoles: jest.fn(() => ['CUSTOMER']),
    bookResourceBatch: jest.fn(),
  }
}));

// Import component and mocked ApiService after mocks are set up
import ResourceBookingPage from '../ResourceBookingPage';
import ApiService from '../../../services/ApiService';

const resource = {
  id: 1,
  name: 'Court A',
  unitsCount: 1,
  openTime: '09:00',
  closeTime: '10:30', // 09:00, 09:30, 10:00
  unavailableWeekdays: '',
  unavailableDates: '',
  dailyUnavailableRanges: '',
  pricePerSlot: 10,
};

const setup = async () => {
  let resolveGet;
  const p = new Promise(res => { resolveGet = res; });
  ApiService.getResourceById.mockReturnValueOnce(p);
  render(
    <MemoryRouter>
      <ResourceBookingPage />
    </MemoryRouter>
  );
  resolveGet({ statusCode: 200, data: resource });
  await screen.findByText(/Unit 1/i);
};

describe('Reservation blocking when user has active holds', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure clean localStorage between tests
    localStorage.clear();
    // Ensure authentication mocks are properly set up
    ApiService.isAuthenticated.mockReturnValue(true);
    ApiService.isCustomer.mockReturnValue(true);
    ApiService.isAdmin.mockReturnValue(false);
    ApiService.getRoles.mockReturnValue(['CUSTOMER']);
  });

  it('prevents selecting new slots and disables booking when user has active holds', async () => {
    await setup();

    // Pick a far-future date to avoid time-based disabling
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2099-01-15' } });

    // Select one slot and book it (creating a hold)
    const first = screen.getByTestId('slot-09:00-1');
    fireEvent.click(first);

    ApiService.bookResourceBatch.mockResolvedValueOnce({ statusCode: 200, message: "Your booking has been received for 1 slot(s). We've sent a secure payment link to your email." });

    fireEvent.click(screen.getByRole('button', { name: /Book Slots/i }));

    // Success banner appears
    expect(await screen.findByText(/booking has been received/i)).toBeInTheDocument();

    // A different free slot should not become selectable while hold is active
    const other = screen.getByTestId('slot-10:00-1');
    fireEvent.click(other);
    expect(other).not.toHaveStyle('background-color: lightgreen');

    // Book button remains disabled because user has an active hold
    const bookBtn = screen.getByRole('button', { name: /Book Slots/i });
    expect(bookBtn).toBeDisabled();

    // Countdown text appears
    expect(screen.getByText(/Reservation hold active/i)).toBeInTheDocument();
  });
});
