import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import ErrorDisplay from '../ErrorDisplay';

describe('ErrorDisplay modal', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders message and allows closing', () => {
    const onDismiss = jest.fn();
    render(<ErrorDisplay message={'Something went wrong'} onDismiss={onDismiss} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Something went wrong/i)).toBeInTheDocument();

    fireEvent.click(screen.getByText(/Close/i));
    expect(onDismiss).toHaveBeenCalled();
  });

  it('copies message to clipboard and shows Copied!', async () => {
    const writeText = jest.fn().mockResolvedValue();
    Object.assign(navigator, { clipboard: { writeText } });
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });

    render(<ErrorDisplay message={'Copy me'} onDismiss={() => {}} />);

    const copyBtn = screen.getByRole('button', { name: /Copy/i });
    await act(async () => {
      fireEvent.click(copyBtn);
    });

    expect(writeText).toHaveBeenCalledWith('Copy me');
    expect(screen.getByRole('button', { name: /Copied!/i })).toBeInTheDocument();
  });
});
