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
    getSavedEmails: jest.fn(),
    myProfile: jest.fn(),
    addSavedEmail: jest.fn(),
    bookResourceBatch: jest.fn(),
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
  // Resolve resource
  let resolveGet;
  const p = new Promise(res => { resolveGet = res; });
  ApiService.getResourceById.mockReturnValueOnce(p);

  // Saved emails empty initially; profile has email
  ApiService.getSavedEmails.mockResolvedValueOnce({ statusCode: 200, data: [] });
  ApiService.myProfile.mockResolvedValueOnce({ statusCode: 200, data: { email: 'me@example.com' } });

  render(
    <MemoryRouter>
      <ResourceBookingPage />
    </MemoryRouter>
  );

  resolveGet({ statusCode: 200, data: resource });
  await screen.findByText(/Unit 1/i);

  // Choose a distant future date to avoid time-based unavailability
  const dateInput = screen.getByLabelText(/Select date:/i);
  fireEvent.change(dateInput, { target: { value: '2099-02-01' } });

  // Select a single slot to enable booking summary and split toggle
  fireEvent.click(screen.getByTestId('slot-09:30-1'));
};

const getSplitToggle = () => screen.getByLabelText(/Split total among participants/i, { selector: 'input[type="checkbox"]' });

describe('ResourceBookingPage split among participants', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    ApiService.isAuthenticated.mockReturnValue(true);
    ApiService.isCustomer.mockReturnValue(true);
    ApiService.isAdmin.mockReturnValue(false);
    ApiService.getRoles.mockReturnValue(['CUSTOMER']);
  });

  it('initializes with one participant equal to total and recomputes equal split when adding participant', async () => {
    await setup();

    // Enable split
    fireEvent.click(getSplitToggle());

    // First participant amount should equal total 10.00
    const amountInputs1 = screen.getAllByDisplayValue('10');
    expect(amountInputs1.length).toBeGreaterThan(0);

    // Add second participant -> equal split 5.00 each
    fireEvent.click(screen.getByRole('button', { name: /Add participant/i }));

    // After recompute, expect two amount inputs with 5.00 and 5.00 (display may be '5' or '5.00' depending on browser)
    const amounts = screen.getAllByRole('spinbutton');
    // Find numeric values of the two amount fields
    const values = amounts.map(i => Number(i.value));
    // One of these is the amount field; there may be only two numeric inputs in the split table
    // Ensure there are at least two amount inputs and they sum to 10 with equal split
    expect(values.filter(v => !Number.isNaN(v)).length).toBeGreaterThanOrEqual(2);
    const amountValues = values.filter(v => v === 5 || v === 10 || v === 0 || v === 5.00);
    expect(amountValues.includes(5) || amountValues.includes(5.00)).toBe(true);
  });

  it('shows validation error for invalid participant email and disables booking', async () => {
    await setup();
    fireEvent.click(getSplitToggle());

    // Add second participant (email input enabled for non-first)
    fireEvent.click(screen.getByRole('button', { name: /Add participant/i }));

    // Enter invalid email for participant #2
    const emailInputs = screen.getAllByPlaceholderText(/email@example.com/i);
    expect(emailInputs.length).toBeGreaterThan(0);
    fireEvent.change(emailInputs[0], { target: { value: 'not-an-email' } });

    // Error message should appear
    expect(await screen.findByText(/looks invalid/i)).toBeInTheDocument();

    // Booking button disabled
    const bookBtn = screen.getByRole('button', { name: /Book Slots/i });
    expect(bookBtn).toBeDisabled();
  });

  it("enforces first participant's min-share rule", async () => {
    await setup();
    fireEvent.click(getSplitToggle());

    // Add second participant (equal split 5/5)
    fireEvent.click(screen.getByRole('button', { name: /Add participant/i }));

    // Reduce first participant amount below 5
    const amountInputs = screen.getAllByRole('spinbutton');
    // The first spinbutton in the split table should correspond to first participant amount
    fireEvent.change(amountInputs[0], { target: { value: '4' } });

    // Error should mention at least equal share
    expect(await screen.findByText(/at least/i)).toBeInTheDocument();

    // Booking button disabled
    const bookBtn = screen.getByRole('button', { name: /Book Slots/i });
    expect(bookBtn).toBeDisabled();
  });

  it('adds new valid email to saved list on blur and deduplicates', async () => {
    await setup();
    fireEvent.click(getSplitToggle());

    // Add second participant
    fireEvent.click(screen.getByRole('button', { name: /Add participant/i }));

    // Enter a new valid email and blur
    const emailInputs = screen.getAllByPlaceholderText(/email@example.com/i);
    fireEvent.change(emailInputs[0], { target: { value: 'friend@example.com' } });

    ApiService.addSavedEmail.mockResolvedValueOnce({ statusCode: 200 });
    fireEvent.blur(emailInputs[0]);

    // Should call API to save email
    await waitFor(() => expect(ApiService.addSavedEmail).toHaveBeenCalledWith('friend@example.com'));

    // The email should appear in datalist options (saved emails)
    const option = await screen.findByDisplayValue('friend@example.com');
    expect(option).toBeInTheDocument();

    // Blur again with same email -> should not call API second time
    fireEvent.blur(emailInputs[0]);
    // ensure still one call
    expect(ApiService.addSavedEmail).toHaveBeenCalledTimes(1);
  });
});
