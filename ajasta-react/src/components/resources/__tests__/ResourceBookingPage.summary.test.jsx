import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResourceBookingPage from '../ResourceBookingPage';
import ApiService from '../../../services/ApiService';

// Mock react-router-dom minimal pieces
jest.mock('react-router-dom', () => ({
  __esModule: true,
  MemoryRouter: ({ children }) => children,
  useParams: () => ({ id: '1' }),
}), { virtual: true });

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

const baseResource = {
  id: 1,
  name: 'Court A',
  unitsCount: 2,
  openTime: '09:00',
  closeTime: '10:00', // 09:00 and 09:30 rows
  unavailableWeekdays: '',
  unavailableDates: '',
  dailyUnavailableRanges: '',
  pricePerSlot: 12.5,
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
  resolveGet({ statusCode: 200, data: baseResource });
  await screen.findByText(/Unit 1/i);
};

describe('ResourceBookingPage summary and pricing', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure authentication mocks are properly set up
    ApiService.isAuthenticated.mockReturnValue(true);
  });

  it('shows booking summary with price per slot and disabled button when nothing selected', async () => {
    await setup();
    // Summary header exists
    expect(screen.getByText(/Booking Summary/i)).toBeInTheDocument();
    // Shows price per slot with numeric value
    expect(screen.getByText(/Price per slot/i)).toBeInTheDocument();
    // Don't assert currency symbol to avoid locale flakiness, check number
    expect(screen.getByText(/12\.50|12,50/)).toBeInTheDocument();

    const btn = screen.getByRole('button', { name: /Book Slots/i });
    expect(btn).toBeDisabled();
    // Total should be 0.00 initially
    expect(screen.getByText(/Total:/i).textContent).toMatch(/0(\.00|,00)/);
  });

  it('updates total and enables button after selecting slots', async () => {
    await setup();
    // Set a future date to avoid time-based disabling
    const dateInput = screen.getByLabelText(/Select date:/i);
    fireEvent.change(dateInput, { target: { value: '2099-01-15' } });
    
    const first = screen.getByTestId('slot-09:00-1');
    const second = screen.getByTestId('slot-09:30-1');

    fireEvent.click(first);
    let totalEl = screen.getByText(/Total:/i);
    // 1 x 12.50 = 12.50
    expect(totalEl.textContent).toMatch(/(12\.50|12,50)/);
    const btn = screen.getByRole('button', { name: /Book Slots/i });
    expect(btn).toBeEnabled();

    // Add second slot: 2 x 12.50 = 25.00
    fireEvent.click(second);
    totalEl = screen.getByText(/Total:/i);
    expect(totalEl.textContent).toMatch(/(25\.00|25,00)/);
  });
});
