import ApiService from '../ApiService';

// Helper to make a JWT token string with provided payload
function makeToken(payload) {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

describe('ApiService roles and authentication', () => {
  beforeEach(() => {
    localStorage.clear();
    ApiService.cachedRoles = null;
  });

  test('parseRolesFromToken extracts from roles array', () => {
    const token = makeToken({ roles: ['ROLE_ADMIN', 'CUSTOMER'] });
    localStorage.setItem('token', token);
    expect(ApiService.parseRolesFromToken().sort()).toEqual(['ADMIN', 'CUSTOMER']);
    expect(ApiService.isAdmin()).toBe(true);
    expect(ApiService.isCustomer()).toBe(true);
  });

  test('parseRolesFromToken extracts from authorities (strings)', () => {
    const token = makeToken({ authorities: ['ROLE_ADMIN', 'USER'] });
    localStorage.setItem('token', token);
    expect(ApiService.getRoles().sort()).toEqual(['ADMIN', 'USER']);
  });

  test('parseRolesFromToken extracts from authorities (objects)', () => {
    const token = makeToken({ authorities: [{ authority: 'ROLE_ADMIN' }, { role: 'customer' }] });
    localStorage.setItem('token', token);
    expect(ApiService.getRoles().sort()).toEqual(['ADMIN', 'CUSTOMER']);
  });

  test('parseRolesFromToken extracts from scope string', () => {
    const token = makeToken({ scope: 'ROLE_ADMIN customer' });
    localStorage.setItem('token', token);
    expect(ApiService.getRoles().sort()).toEqual(['ADMIN', 'CUSTOMER']);
  });

  test('getRoles falls back to cached in-memory roles when token lacks roles', () => {
    // Save a token with no roles
    const token = makeToken({ sub: 'user@example.com' });
    localStorage.setItem('token', token);

    // Set cached roles
    ApiService.saveRole(['admin', 'ADMIN', 'customer']);
    const roles = ApiService.getRoles().sort();
    expect(roles).toEqual(['ADMIN', 'CUSTOMER']);
  });

  test('logout clears token and cached roles, not localStorage roles', () => {
    localStorage.setItem('token', 'x');
    localStorage.setItem('roles', 'should-be-removed');
    ApiService.cachedRoles = ['ADMIN'];

    ApiService.logout();

    expect(localStorage.getItem('token')).toBeNull();
    expect(ApiService.cachedRoles).toBeNull();
    expect(localStorage.getItem('roles')).toBeNull();
  });

  test('isAuthenticated true when token exists', () => {
    expect(ApiService.isAuthenticated()).toBe(false);
    localStorage.setItem('token', 'x');
    expect(ApiService.isAuthenticated()).toBe(true);
  });
});
