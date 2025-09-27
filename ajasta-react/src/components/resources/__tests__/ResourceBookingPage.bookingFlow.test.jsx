import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResourceBookingPage from '../ResourceBookingPage';
import ApiService from '../../../services/ApiService';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
  __esModule: true,
  MemoryRouter: ({ children }) => children,
  useParams: () => ({ id: '1' }),
}));

// Mock ApiService
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getResourceById: jest.fn(),
    isAuthenticated: jest.fn(() => true),
    bookResourceBatch: jest.fn(),
    bookResourceMulti: jest.fn(),
  }
}));

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
  });

  it('single-day selection calls bookResourceBatch and holds slots with countdown', async () => {
    await setup();
    // Set a known date
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2025-01-10' } });

    // Select two slots: 09:00 and 09:30
    fireEvent.click(screen.getByTestId('slot-09:00-1'));
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceBatch.mockResolvedValueOnce({ statusCode: 200, message: 'Booking request accepted for 2 slot(s)' });

    fireEvent.click(screen.getByRole('button', { name: /Book Slots/i }));

    // Ensure API called with correct body
    expect(ApiService.bookResourceBatch).toHaveBeenCalledTimes(1);
    const [id, body] = ApiService.bookResourceBatch.mock.calls[0];
    expect(id).toBe(1);
    expect(body.date).toBe('2025-01-10');
    expect(body.slots).toEqual([
      { startTime: '09:00', endTime: '09:30', unit: 1 },
      { startTime: '09:30', endTime: '10:00', unit: 1 },
    ]);

    // Success message appears
    expect(await screen.findByText(/Booking request accepted for 2 slot/)).toBeInTheDocument();

    // Cells should now be held (yellow) and countdown visible
    const firstCell = screen.getByTestId('slot-09:00-1');
    const secondCell = screen.getByTestId('slot-09:30-1');
    expect(firstCell).toHaveStyle('background-color: yellow');
    expect(secondCell).toHaveStyle('background-color: yellow');
    expect(screen.getByText(/Reservation hold active/i)).toBeInTheDocument();
  });

  it('multi-day selection calls bookResourceMulti with grouped days and holds slots across days', async () => {
    await setup();

    // Day 1
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2025-01-11' } });
    fireEvent.click(screen.getByTestId('slot-09:00-1'));

    // Day 2
    fireEvent.change(dateInput, { target: { value: '2025-01-12' } });
    fireEvent.click(screen.getByTestId('slot-09:30-1'));

    ApiService.bookResourceMulti.mockResolvedValueOnce({ statusCode: 200, message: 'Booking request accepted for 2 slot(s) across 2 day(s)' });

    fireEvent.click(screen.getByRole('button', { name: /Book Slots/i }));

    expect(ApiService.bookResourceMulti).toHaveBeenCalledTimes(1);
    const [id, body] = ApiService.bookResourceMulti.mock.calls[0];
    expect(id).toBe(1);
    expect(body.days).toEqual([
      { date: '2025-01-11', slots: [{ startTime: '09:00', endTime: '09:30', unit: 1 }] },
      { date: '2025-01-12', slots: [{ startTime: '09:30', endTime: '10:00', unit: 1 }] },
    ]);

    expect(await screen.findByText(/across 2 day/)).toBeInTheDocument();

    // Verify holds across days: switch between dates and check yellow
    fireEvent.change(dateInput, { target: { value: '2025-01-11' } });
    expect(screen.getByTestId('slot-09:00-1')).toHaveStyle('background-color: yellow');

    fireEvent.change(dateInput, { target: { value: '2025-01-12' } });
    expect(screen.getByTestId('slot-09:30-1')).toHaveStyle('background-color: yellow');

    expect(screen.getByText(/Reservation hold active/i)).toBeInTheDocument();
  });
});
