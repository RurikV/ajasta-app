import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
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
    bookResourceMulti: jest.fn(),
  }
}));

// Import component and mocked ApiService after mocks are set up
const ResourceBookingPage = require('../ResourceBookingPage').default;
const ApiService = require('../../../services/ApiService').default;

const resource = {
  id: 1,
  name: 'Court A',
  unitsCount: 1,
  openTime: '09:00',
  closeTime: '10:00',
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

// Timers not required for new hold countdown behavior

describe('ResourceBookingPage booking flows', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure no residual holds between tests
    localStorage.clear();
    // Ensure authentication mocks are properly set up
    ApiService.isAuthenticated.mockReturnValue(true);
    ApiService.isCustomer.mockReturnValue(true);
    ApiService.isAdmin.mockReturnValue(false);
    ApiService.getRoles.mockReturnValue(['CUSTOMER']);
  });

  it('single-day selection calls bookResourceBatch and holds slots with countdown', async () => {
    await setup();
    // Set a known future date to avoid past-date disabling
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2099-01-10' } });

    // Select two slots: 09:00 and 09:30
    fireEvent.click(screen.getByTestId('slot-09:00-1'));
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceBatch.mockResolvedValueOnce({ statusCode: 200, message: "Your booking has been received for 2 slot(s). We've sent a secure payment link to your email." });

    const bookBtn = screen.getByRole('button', { name: /Book Slots/i });
    await waitFor(() => expect(bookBtn).toBeEnabled());
    fireEvent.click(bookBtn);

    // Debug print
    // eslint-disable-next-line no-console
    console.log('[DEBUG_LOG] bookResourceBatch calls:', ApiService.bookResourceBatch.mock.calls.length);

    // Ensure API called with correct body
    expect(ApiService.bookResourceBatch).toHaveBeenCalledTimes(1);
    const [id, body] = ApiService.bookResourceBatch.mock.calls[0];
    expect(id).toBe(1);
    expect(body.date).toBe('2099-01-10');
    expect(body.slots).toEqual([
      { startTime: '09:00', endTime: '09:30', unit: 1 },
      { startTime: '09:30', endTime: '10:00', unit: 1 },
    ]);

    // Success message appears (should mention email sent and include slot count)
    expect(await screen.findByText(/2 slot\(s\)/i)).toBeInTheDocument();
    expect(screen.getByText(/email/i)).toBeInTheDocument();

    // Cells should now be held (yellow) and countdown visible
    const firstCell = screen.getByTestId('slot-09:00-1');
    const secondCell = screen.getByTestId('slot-09:30-1');
    expect(firstCell).toHaveStyle('background-color: yellow');
    expect(secondCell).toHaveStyle('background-color: yellow');
    expect(screen.getByText(/Reservation hold active/i)).toBeInTheDocument();
  });

  it('multi-day selection calls bookResourceMulti with grouped days and holds slots across days', async () => {
    await setup();

    // Day 1 (future)
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2099-01-11' } });
    fireEvent.click(screen.getByTestId('slot-09:00-1'));

    // Day 2 (future)
    fireEvent.change(dateInput, { target: { value: '2099-01-12' } });
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceMulti.mockResolvedValueOnce({ statusCode: 200, message: "Your booking has been received for 2 slot(s) across 2 day(s). We've sent a secure payment link to your email." });

    const bookBtn2 = screen.getByRole('button', { name: /Book Slots/i });
    await waitFor(() => expect(bookBtn2).toBeEnabled());
    fireEvent.click(bookBtn2);

    expect(ApiService.bookResourceMulti).toHaveBeenCalledTimes(1);
    const [id, body] = ApiService.bookResourceMulti.mock.calls[0];
    expect(id).toBe(1);
    expect(body.days).toEqual([
      { date: '2099-01-11', slots: [{ startTime: '09:00', endTime: '09:30', unit: 1 }] },
      { date: '2099-01-12', slots: [{ startTime: '09:30', endTime: '10:00', unit: 1 }] },
    ]);

    expect(await screen.findByText(/across 2 day/)).toBeInTheDocument();

    // Verify holds across days: switch between dates and check yellow
    fireEvent.change(dateInput, { target: { value: '2099-01-11' } });
    expect(screen.getByTestId('slot-09:00-1')).toHaveStyle('background-color: yellow');

    fireEvent.change(dateInput, { target: { value: '2099-01-12' } });
    expect(screen.getByTestId('slot-09:30-1')).toHaveStyle('background-color: yellow');

    expect(screen.getByText(/Reservation hold active/i)).toBeInTheDocument();
  });
});
