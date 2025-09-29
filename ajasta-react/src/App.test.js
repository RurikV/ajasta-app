import { render, screen } from '@testing-library/react';

// Mock axios (ESM-only in node_modules) to avoid Jest parsing ESM from node_modules
jest.mock('axios', () => {
  const mock = {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
    create: jest.fn(() => ({ get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() })),
  };
  return {
    __esModule: true,
    default: mock,
    ...mock,
  };
});

// Mock react-router-dom (RRD v7 is ESM-only; provide CJS-friendly test doubles)
jest.mock('react-router-dom', () => {
  const React = require('react');
  return {
    __esModule: true,
    BrowserRouter: ({ children }) => React.createElement(React.Fragment, null, children),
    // Do not render any routed elements in tests to avoid mounting pages with side-effects
    Routes: () => null,
    Route: () => null,
    Navigate: () => null,
    Link: ({ children }) => React.createElement('a', null, children),
    Outlet: () => null,
    useNavigate: () => jest.fn(),
    useParams: () => ({}),
    useLocation: () => ({ state: null })
  };
}, { virtual: true });

// Import App after mocking RRD and axios
const App = require('./App').default;

// Basic smoke test ensuring the app renders Navbar links
// We expect common navigation items to be present

test('renders Navbar with Home link', () => {
  render(<App />);
  expect(screen.getByText(/Home/i)).toBeInTheDocument();
});
