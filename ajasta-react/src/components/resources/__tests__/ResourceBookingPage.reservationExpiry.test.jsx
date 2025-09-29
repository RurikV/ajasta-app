import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Mock react-router-dom minimal pieces
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

const makeResource = (overrides = {}) => ({
  id: 1,
  name: 'Court A',
  unitsCount: 1,
  openTime: '09:00',
  closeTime: '10:00', // rows: 09:00, 09:30
  unavailableWeekdays: '',
  unavailableDates: '',
  dailyUnavailableRanges: '',
  pricePerSlot: 10,
  ...overrides,
});

const setup = async (resourceOverrides = {}) => {
  // Resolve resource fetch
  let resolveGet;
  const p = new Promise(res => { resolveGet = res; });
  ApiService.getResourceById.mockReturnValueOnce(p);

  render(
    <MemoryRouter>
      <ResourceBookingPage />
    </MemoryRouter>
  );

  resolveGet({ statusCode: 200, data: makeResource(resourceOverrides) });
  await screen.findByText(/Unit 1/i);
};

// Helper to select date via input label
const setDate = (dateStr) => {
  const dateInput = screen.getByLabelText(/Select date:/i);
  fireEvent.change(dateInput, { target: { value: dateStr } });
};

// Ensure timers are modern to use setSystemTime
beforeAll(() => {
  jest.useFakeTimers({ legacyFakeTimers: false });
});

afterAll(() => {
  jest.useRealTimers();
});

describe('Reservation expiry and time-based unavailability', () => {
  beforeEach(() => {
    // Ensure clean mocks and a token so the test user owns the holds
    jest.clearAllMocks();
    localStorage.setItem('token', 'test-token');
    // Ensure authentication mocks are properly set up
    ApiService.isAuthenticated.mockReturnValue(true);
    ApiService.isCustomer.mockReturnValue(true);
    ApiService.isAdmin.mockReturnValue(false);
    ApiService.getRoles.mockReturnValue(['CUSTOMER']);
  });

  it('after hold expires on today, slot becomes unavailable (grey) because current time passed its start', async () => {
    // Start day at 09:00 local time
    jest.setSystemTime(new Date('2025-09-28T09:00:00'));

    await setup();

    // Work with today
    setDate('2025-09-28');

    // Select 09:30 slot and book
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceBatch.mockResolvedValueOnce({ statusCode: 200, message: "Your booking has been received for 1 slot(s). We've sent a secure payment link to your email." });
    fireEvent.click(screen.getByRole('button', { name: /Book Slots/i }));

    // Wait for success banner to ensure holds have been applied
    await screen.findByText(/booking has been received/i);

    // Initially: held (yellow)
    const cell = await screen.findByTestId('slot-09:30-1');
    expect(cell).toHaveStyle('background-color: yellow');

    // Advance time by 31 minutes -> 09:31
    await act(async () => {
      jest.setSystemTime(new Date('2025-09-28T09:31:00'));
      jest.advanceTimersByTime(31 * 60 * 1000);
    });

    // After expiry, since today and now > 09:30 start, it should be unavailable (grey), not yellow
    await waitFor(() => expect(screen.getByTestId('slot-09:30-1')).not.toHaveStyle('background-color: yellow'));
    await waitFor(() => expect(screen.getByTestId('slot-09:30-1')).toHaveStyle('background-color: #f0f0f0'));
  });

  it('after hold expires on a future date, slot becomes available again (transparent and selectable)', async () => {
    // Current time baseline
    jest.setSystemTime(new Date('2025-09-28T09:00:00'));

    await setup();

    // Choose a future date (tomorrow)
    setDate('2025-09-29');

    // Select 09:30 slot and book
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceBatch.mockResolvedValueOnce({ statusCode: 200, message: "Your booking has been received for 1 slot(s). We've sent a secure payment link to your email." });
    fireEvent.click(screen.getByRole('button', { name: /Book Slots/i }));

    // Wait for success message to ensure holds have been applied
    await screen.findByText(/booking has been received/i);

    const cell = await screen.findByTestId('slot-09:30-1');
    expect(cell).toHaveStyle('background-color: yellow');

    // Advance time beyond 30 minutes to expire the hold
    await act(async () => {
      jest.setSystemTime(new Date('2025-09-28T09:31:00'));
      jest.advanceTimersByTime(31 * 60 * 1000);
    });

    // Since the selected date is in the future, the slot should become available again (transparent)
    await waitFor(() => expect(screen.getByTestId('slot-09:30-1')).toHaveStyle('background-color: transparent'));
    const c = screen.getByTestId('slot-09:30-1');
    expect(c).not.toHaveStyle('background-color: yellow');
    expect(c).not.toHaveStyle('background-color: #f0f0f0');

    // And it should be selectable again
    fireEvent.click(screen.getByTestId('slot-09:30-1'));
    expect(screen.getByTestId('slot-09:30-1')).toHaveStyle('background-color: lightgreen');
  });
});
