import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import AdminResourceFormPage from '../AdminResourceFormPage';

// Mock react-router-dom (CJS-friendly virtual mock)
jest.mock('react-router-dom', () => {
  return {
    __esModule: true,
    MemoryRouter: ({ children }) => children,
    useParams: () => ({ id: '1' }),
    useNavigate: () => jest.fn(),
    Link: ({ children }) => children
  };
}, { virtual: true });

// Mock ApiService
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    getResourceById: jest.fn(),
    updateResource: jest.fn()
  }
}));

// Mock useError hook
jest.mock('../../common/ErrorDisplay', () => ({
  useError: () => ({
    ErrorDisplay: () => null,
    showError: jest.fn()
  })
}));

describe('Price Precision Issue', () => {
  test('demonstrates JavaScript floating point precision loss', () => {
    console.log('[DEBUG_LOG] Testing JavaScript floating-point precision issue');
    
    // Simulate what happens with JavaScript numbers
    const originalValue = '5.00';
    const numberValue = parseFloat(originalValue); // 5
    const backToString = numberValue.toString(); // "5"
    
    console.log('[DEBUG_LOG] Original: ' + originalValue);
    console.log('[DEBUG_LOG] As number: ' + numberValue);
    console.log('[DEBUG_LOG] Back to string: ' + backToString);
    
    // This demonstrates the precision loss
    expect(backToString).not.toBe(originalValue);
    
    // Test with more problematic values
    const problematicValue = '5.00';
    const problemNumber = Number(problematicValue);
    const problemBack = problemNumber.toString();
    
    console.log('[DEBUG_LOG] Problematic original: ' + problematicValue);
    console.log('[DEBUG_LOG] As Number(): ' + problemNumber);
    console.log('[DEBUG_LOG] Back to string: ' + problemBack);
    
    // Test potential causes of 4.97
    const calculation1 = (5.0 * 0.994).toString();
    const calculation2 = (5.0 - 0.03).toString(); 
    const calculation3 = Number('4.970000000001').toString();
    
    console.log('[DEBUG_LOG] 5.0 * 0.994 = ' + calculation1);
    console.log('[DEBUG_LOG] 5.0 - 0.03 = ' + calculation2);
    console.log('[DEBUG_LOG] Number(4.970000000001) = ' + calculation3);
    
    // Check if any calculation results in 4.97
    if (calculation2 === '4.97') {
      console.log('[DEBUG_LOG] Found it! 5.0 - 0.03 produces 4.97');
    }
  });

  test('input value handling preserves precision', async () => {
    console.log('[DEBUG_LOG] Testing input value precision handling');
    
    // Mock the API response
    const ApiService = require('../../../services/ApiService').default;
    ApiService.getResourceById.mockResolvedValue({
      statusCode: 200,
      data: {
        id: 1,
        name: 'Test Resource',
        type: 'TURF_COURT',
        pricePerSlot: 5.00,
        active: true
      }
    });

    render(<AdminResourceFormPage />);

    // Wait for the form to load
    await screen.findByDisplayValue('Test Resource');

    // Find the price input
    const priceInput = screen.getByLabelText(/Price per 30-minute slot/i);
    
    // Test setting a precise decimal value
    fireEvent.change(priceInput, { target: { value: '5.00' } });
    
    // The value should be preserved as string
    expect(priceInput.value).toBe('5.00');
    
    console.log('[DEBUG_LOG] Input value after setting 5.00: ' + priceInput.value);
  });
});