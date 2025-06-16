import React from 'react';
import { createBrowserRouter, RouteObject } from 'react-router-dom';
import CreateTicketView from './views/CreateTicketView.js';
import DashboardView from './views/DashboardView.js';
import MainLayout from './views/MainLayout.js';
import TicketDetailView from './views/TicketDetailView.js';
import TicketListView from './views/TicketListView.js';

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <DashboardView /> },
      { path: 'tickets', element: <TicketListView /> },
      { path: 'tickets/create', element: <CreateTicketView /> },
      { path: 'tickets/:id', element: <TicketDetailView /> },
    ],
  },
];

export const router = createBrowserRouter(routes);
