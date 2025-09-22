import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import AdminResourceFormPage from '../AdminResourceFormPage';

// Mock react-router-dom (CJS-friendly virtual mock)
jest.mock('react-router-dom', () => {
  return {
    __esModule: true,
    MemoryRouter: ({ children }) => children,
    useParams: () => ({ id: null }),
    useNavigate: () => jest.fn(),
    Link: ({ children }) => children
  };
}, { virtual: true });

// Mock ApiService
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getResourceById: jest.fn(),
    addResource: jest.fn(),
  }
}));

// Mock useError hook
jest.mock('../../common/ErrorDisplay', () => ({
  useError: () => ({
    ErrorDisplay: () => null,
    showError: jest.fn()
  })
}));

describe('Decimal Input Test', () => {
  test('allows comma as decimal separator', () => {
    render(<AdminResourceFormPage />);
    
    const priceInput = screen.getByLabelText(/Price per 30-minute slot/i);
    
    // Test comma input
    fireEvent.change(priceInput, { target: { value: '5,50' } });
    expect(priceInput.value).toBe('5,50');
    
    console.log('[DEBUG_LOG] Comma input test: ' + priceInput.value);
  });

  test('allows dot as decimal separator', () => {
    render(<AdminResourceFormPage />);
    
    const priceInput = screen.getByLabelText(/Price per 30-minute slot/i);
    
    // Test dot input
    fireEvent.change(priceInput, { target: { value: '5.50' } });
    expect(priceInput.value).toBe('5.50');
    
    console.log('[DEBUG_LOG] Dot input test: ' + priceInput.value);
  });

  test('prevents invalid input formats', () => {
    render(<AdminResourceFormPage />);
    
    const priceInput = screen.getByLabelText(/Price per 30-minute slot/i);
    
    // Test invalid inputs that should be rejected
    fireEvent.change(priceInput, { target: { value: '5,5,5' } });
    expect(priceInput.value).toBe('');
    
    fireEvent.change(priceInput, { target: { value: '5.5.5' } });
    expect(priceInput.value).toBe('');
    
    fireEvent.change(priceInput, { target: { value: 'abc' } });
    expect(priceInput.value).toBe('');
    
    console.log('[DEBUG_LOG] Invalid input rejection test passed');
  });

  test('allows valid decimal formats', () => {
    render(<AdminResourceFormPage />);
    
    const priceInput = screen.getByLabelText(/Price per 30-minute slot/i);
    
    // Test various valid formats
    const validInputs = ['5', '5.0', '5.50', '5,0', '5,50', '25.99', '25,99'];
    
    validInputs.forEach(input => {
      fireEvent.change(priceInput, { target: { value: '' } }); // Clear
      fireEvent.change(priceInput, { target: { value: input } });
      expect(priceInput.value).toBe(input);
      console.log('[DEBUG_LOG] Valid input "' + input + '" accepted');
    });
  });
});