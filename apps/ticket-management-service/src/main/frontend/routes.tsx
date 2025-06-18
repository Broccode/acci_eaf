import ProtectedRoute from 'Frontend/components/ProtectedRoute';
import CreateTicketView from 'Frontend/views/CreateTicketView';
import DashboardView from 'Frontend/views/DashboardView';
import LoginView from 'Frontend/views/LoginView';
import MainLayout from 'Frontend/views/MainLayout';
import TicketDetailView from 'Frontend/views/TicketDetailView';
import TicketListView from 'Frontend/views/TicketListView';
import { createBrowserRouter, RouteObject } from 'react-router-dom';

// Define routes for the React application
export const routes: RouteObject[] = [
  {
    path: '/login',
    element: <LoginView />,
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <DashboardView /> },
      { path: 'tickets', element: <TicketListView /> },
      { path: 'tickets/create', element: <CreateTicketView /> },
      { path: 'tickets/:id', element: <TicketDetailView /> },
    ],
  },
];

export const router = createBrowserRouter(routes, {
  basename: new URL(document.baseURI).pathname,
});
