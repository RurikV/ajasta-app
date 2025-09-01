import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AdminResourceFormPage from '../AdminResourceFormPage';

// Mock navigate and params
const mockedNavigate = jest.fn();
jest.mock('react-router-dom', () => {
  // Provide a minimal mock that works in Jest (CJS) without importing the actual ESM module
  return {
    __esModule: true,
    MemoryRouter: ({ children }) => children,
    useNavigate: () => mockedNavigate,
    useParams: () => ({}), // no id for create mode
    Link: ({ children }) => children,
    Outlet: () => null
  };
}, { virtual: true });

// Mock ApiService
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    addResource: jest.fn(),
    updateResource: jest.fn(),
    getResourceById: jest.fn()
  }
}));

import ApiService from '../../../services/ApiService';

const setup = () => {
  return render(
    <MemoryRouter>
      <AdminResourceFormPage />
    </MemoryRouter>
  );
};

describe('AdminResourceFormPage (Add Resource)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders Back and Save buttons with correct labels', () => {
    setup();

    expect(screen.getByRole('button', { name: /Back to Resource Items/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Save Resource Item/i })).toBeInTheDocument();
  });

  it('navigates back to resources list when Back is clicked', async () => {
    setup();

    await userEvent.click(screen.getByRole('button', { name: /Back to Resource Items/i }));
    expect(mockedNavigate).toHaveBeenCalledWith('/admin/resources');
  });

  it('submits form, calls addResource, and navigates to list on success', async () => {
    ApiService.addResource.mockResolvedValue({ statusCode: 200 });

    setup();

    await userEvent.type(screen.getByLabelText(/Name \*/i), 'Test Court');
    await userEvent.selectOptions(screen.getByLabelText(/Type \*/i), 'TURF_COURT');
    await userEvent.type(screen.getByLabelText(/Location/i), 'Downtown');

    const file = new File(['image-bytes'], 'photo.png', { type: 'image/png' });
    const fileInput = screen.getByLabelText(/Image \*/i);
    await userEvent.upload(fileInput, file);

    await userEvent.click(screen.getByRole('button', { name: /Save Resource Item/i }));

    await waitFor(() => {
      expect(ApiService.addResource).toHaveBeenCalledTimes(1);
      expect(ApiService.addResource).toHaveBeenCalledWith(expect.any(FormData));
      expect(mockedNavigate).toHaveBeenCalledWith('/admin/resources');
    });
  });

  it('shows error when addResource fails', async () => {
    ApiService.addResource.mockRejectedValue(new Error('Failed to save'));

    setup();

    await userEvent.type(screen.getByLabelText(/Name \*/i), 'Test Court');
    await userEvent.selectOptions(screen.getByLabelText(/Type \*/i), 'TURF_COURT');
    const file = new File(['image-bytes'], 'photo.png', { type: 'image/png' });
    await userEvent.upload(screen.getByLabelText(/Image \*/i), file);

    await userEvent.click(screen.getByRole('button', { name: /Save Resource Item/i }));

    // Error banner is rendered by ErrorDisplay
    expect(await screen.findByText(/Failed to save/i)).toBeInTheDocument();
  });
});
