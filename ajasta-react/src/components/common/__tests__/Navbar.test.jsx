import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Mock react-router-dom as virtual to avoid real dependency resolution
jest.mock('react-router-dom', () => ({
  __esModule: true,
  MemoryRouter: ({ children }) => children,
  Link: ({ to, children, ...rest }) => <a href={to} {...rest}>{children}</a>,
  useNavigate: () => () => {},
}), { virtual: true });

// Mock i18n BEFORE importing Navbar
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

// Mock ApiService roles BEFORE importing Navbar
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

// Import Navbar and ApiService using require after mocks to satisfy import/first
const Navbar = require('../Navbar').default;
const ApiService = require('../../../services/ApiService').default;

describe('Navbar links', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Ensure mocks are set up correctly
    ApiService.isAuthenticated.mockReturnValue(true);
    ApiService.isCustomer.mockReturnValue(true);
    ApiService.isAdmin.mockReturnValue(false);
    ApiService.isDeliveryPerson.mockReturnValue(false);
  });

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
