import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResourceBookingPage from '../ResourceBookingPage';
import ApiService from '../../../services/ApiService';

// Mock react-router-dom (CJS-friendly virtual mock)
jest.mock('react-router-dom', () => {
  return {
    __esModule: true,
    MemoryRouter: ({ children }) => children,
    useParams: () => ({ id: '1' }),
    Link: ({ children }) => children,
    Outlet: () => null
  };
}, { virtual: true });

// Mock ApiService
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getResourceById: jest.fn()
  }
}));

const mockResource = {
  id: 1,
  name: 'City Turf Court A',
  unitsCount: 3,
  openTime: '09:00',
  closeTime: '10:00', // 2 slots: 09:00, 09:30
  unavailableWeekdays: '',
  unavailableDates: '',
  dailyUnavailableRanges: ''
};

const setup = async () => {
  let resolveGet;
  const promise = new Promise(res => { resolveGet = res; });
  ApiService.getResourceById.mockReturnValue(promise);
  render(
    <MemoryRouter>
      <ResourceBookingPage />
    </MemoryRouter>
  );
  // Resolve the API call to trigger state update; Testing Library will handle act internally via findBy*
  resolveGet({ statusCode: 200, data: mockResource });
  // Wait for header/unit columns to appear
  await screen.findByText(/Unit 1/i);
};

describe('ResourceBookingPage selection', () => {
  let originalError;
  beforeAll(() => {
    originalError = console.error;
    jest.spyOn(console, 'error').mockImplementation((...args) => {
      const first = args[0];
      if (typeof first === 'string' && first.includes('not wrapped in act')) {
        return; // suppress only act() warning noise
      }
      originalError(...args);
    });
  });
  afterAll(() => {
    console.error.mockRestore();
  });
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders 3 unit columns for City Turf Court A', async () => {
    await setup();
    expect(screen.getByText('Unit 1')).toBeInTheDocument();
    expect(screen.getByText('Unit 2')).toBeInTheDocument();
    expect(screen.getByText('Unit 3')).toBeInTheDocument();
  });

  it('allows multi-select and toggling a slot off', async () => {
    await setup();
    const first = screen.getByTestId('slot-09:00-1');
    const second = screen.getByTestId('slot-09:30-1');

    // Click first: selected
    fireEvent.click(first);
    expect(first).toHaveStyle('background-color: lightgreen');
    expect(second).not.toHaveStyle('background-color: lightgreen');

    // Click second: both selected (multi-select)
    fireEvent.click(second);
    expect(first).toHaveStyle('background-color: lightgreen');
    expect(second).toHaveStyle('background-color: lightgreen');

    // Click first again: toggles off first, second remains
    fireEvent.click(first);
    expect(first).not.toHaveStyle('background-color: lightgreen');
    expect(second).toHaveStyle('background-color: lightgreen');
  });

  it('hover/drag does not select additional cells, and no "Selected" label is shown', async () => {
    await setup();
    const first = screen.getByTestId('slot-09:00-1');
    const second = screen.getByTestId('slot-09:30-1');

    fireEvent.click(first);
    // simulate hover/drag behavior (should have no effect)
    fireEvent.mouseEnter(second);
    fireEvent.mouseOver(second);

    // Still only first selected
    expect(first).toHaveStyle('background-color: lightgreen');
    expect(second).not.toHaveStyle('background-color: lightgreen');

    // ensure the literal label 'Selected' is not rendered anywhere
    expect(screen.queryByText(/Selected/i)).toBeNull();
  });
});
