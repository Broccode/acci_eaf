import { AppLayout } from '@vaadin/react-components/AppLayout.js';
import { DrawerToggle } from '@vaadin/react-components/DrawerToggle.js';
import { Header } from '@vaadin/react-components/Header.js';
import { SideNav } from '@vaadin/react-components/SideNav.js';
import { SideNavItem } from '@vaadin/react-components/SideNavItem.js';
import React from 'react';
import { Outlet, useNavigate } from 'react-router-dom';

export default function MainLayout() {
  const navigate = useNavigate();

  return (
    <AppLayout>
      <Header slot="navbar" className="bg-primary text-white">
        <DrawerToggle />
        <h1 className="text-xl font-bold ml-4">EAF Ticket Management</h1>
      </Header>

      <SideNav slot="drawer">
        <SideNavItem path="/" onClick={() => navigate('/')}>
          Dashboard
        </SideNavItem>
        <SideNavItem path="/tickets" onClick={() => navigate('/tickets')}>
          All Tickets
        </SideNavItem>
        <SideNavItem
          path="/tickets/create"
          onClick={() => navigate('/tickets/create')}
        >
          Create Ticket
        </SideNavItem>
      </SideNav>

      <div className="p-6">
        <Outlet />
      </div>
    </AppLayout>
  );
}
