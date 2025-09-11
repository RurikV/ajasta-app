import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import ResourceBookingPage from '../ResourceBookingPage';

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

import ApiService from '../../../services/ApiService';

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
  ApiService.getResourceById.mockResolvedValue({ statusCode: 200, data: mockResource });
  render(
    <MemoryRouter>
      <ResourceBookingPage />
    </MemoryRouter>
  );
  // Wait for header/unit columns to appear
  await waitFor(() => expect(screen.getByText(/Unit 1/i)).toBeInTheDocument());
};

describe('ResourceBookingPage selection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders 3 unit columns for City Turf Court A', async () => {
    await setup();
    expect(screen.getByText('Unit 1')).toBeInTheDocument();
    expect(screen.getByText('Unit 2')).toBeInTheDocument();
    expect(screen.getByText('Unit 3')).toBeInTheDocument();
  });

  it('clicking a cell toggles light green highlight', async () => {
    await setup();
    const cell = screen.getByTestId('slot-09:00-1');

    // First click: select (mousedown handler toggles selection)
    fireEvent.mouseDown(cell);
    expect(cell).toHaveStyle('background-color: lightgreen');

    // Second click: deselect
    fireEvent.mouseDown(cell);
    expect(cell).not.toHaveStyle('background-color: lightgreen');
  });

  it('dragging selects multiple cells', async () => {
    await setup();
    const first = screen.getByTestId('slot-09:00-1');
    const second = screen.getByTestId('slot-09:30-1');

    fireEvent.mouseDown(first);
    // simulate drag across to second slot
    fireEvent.mouseEnter(second);
    // release drag
    fireEvent.mouseUp(window);

    expect(first).toHaveStyle('background-color: lightgreen');
    expect(second).toHaveStyle('background-color: lightgreen');
  });
});
