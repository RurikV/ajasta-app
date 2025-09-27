import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Navbar from '../Navbar';

// Mock i18n
jest.mock('react-i18next', () => ({
  __esModule: true,
  useTranslation: () => ({ t: (k) => ({
    app_title: 'Ajasta',
    home: 'Home',
    menu: 'Menu',
    resources: 'Resources',
    categories: 'Categories',
    language: 'Language',
    orders: 'Orders',
    cart: 'Cart',
    admin: 'Admin',
    profile: 'Profile',
    login: 'Login',
    register: 'Register',
    logout: 'Logout',
  }[k] || k), i18n: { language: 'en' } })
}));

// Mock ApiService roles
jest.mock('../../../services/ApiService', () => ({
  __esModule: true,
  default: {
    isAuthenticated: jest.fn(() => true),
    isAdmin: jest.fn(() => false),
    isCustomer: jest.fn(() => true),
    isDeliveryPerson: jest.fn(() => false),
    logout: jest.fn(),
  }
}));

describe('Navbar links', () => {
  it('Orders link points to /my-order-history', () => {
    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    const ordersLink = screen.getByRole('link', { name: /Orders/i });
    expect(ordersLink).toHaveAttribute('href', '/my-order-history');
  });
});
