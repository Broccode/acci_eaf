import { Flow } from '@vaadin/flow-frontend';
import { Router } from '@vaadin/router';
import './views/dashboard-view';
import './views/endpoint-test-view';
import './views/login-view';
import './views/main-layout';

const { serverSideRoutes } = new Flow({
  imports: () => import('../../../target/frontend/generated-flow-imports.js'),
});

const routes = [
  // Main layout wraps all views
  {
    path: '',
    component: 'main-layout',
    children: [
      // Public routes
      { path: '/login', component: 'login-view' },

      // Protected routes (will be secured via Spring Security)
      { path: '/', component: 'dashboard-view' },
      { path: '/dashboard', component: 'dashboard-view' },
      { path: '/tenants', component: 'tenant-management-view' },
      { path: '/users', component: 'user-management-view' },
      { path: '/endpoint-test', component: 'endpoint-test-view' },

      // Server-side routes (Hilla endpoints)
      ...serverSideRoutes,
    ],
  },
];

export const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);

// Global error handling for async operations
window.addEventListener('unhandledrejection', event => {
  console.error('Unhandled promise rejection:', event.reason);
  // TODO: Integrate with proper error reporting when available
});

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
  console.log('Control Plane Frontend initialized');
});
