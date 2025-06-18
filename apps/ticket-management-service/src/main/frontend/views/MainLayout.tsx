import { AppLayout, Avatar } from '@vaadin/react-components';
import { useAuth } from 'Frontend/hooks/useAuth';
import { JSX, Suspense, useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';

const navLinkClasses = ({ isActive }: { isActive: boolean }): string => {
  return `flex items-center gap-s p-s rounded-m ${
    isActive ? 'bg-primary-10 text-primary' : 'text-secondary'
  } hover:bg-contrast-5 hover:text-body`;
};

const MainLayout = (): JSX.Element | null => {
  const { user, isAuthenticated, loading, logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, loading, navigate]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div>Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Return null or a minimal layout while redirecting
    return null;
  }

  return (
    <AppLayout primarySection="drawer">
      <div slot="drawer" className="flex flex-col justify-between h-full p-m">
        <header className="flex flex-col gap-m">
          <h1 className="text-l font-semibold">Ticket App</h1>
          <nav>
            <NavLink to="/" className={navLinkClasses} end>
              Dashboard
            </NavLink>
            <NavLink to="/tickets/create" className={navLinkClasses}>
              Create Ticket
            </NavLink>
            <NavLink to="/tickets" className={navLinkClasses}>
              My Tickets
            </NavLink>
            {/* Example of role-based navigation */}
            {user?.roles?.includes('ROLE_ADMIN') && (
              <NavLink to="/admin" className={navLinkClasses}>
                Admin View
              </NavLink>
            )}
          </nav>
        </header>
        <footer className="flex flex-col gap-s">
          <button onClick={logout} className="btn btn-secondary text-left">
            Sign Out
          </button>
          <div className="flex items-center gap-s">
            <Avatar name={user?.name} />
            <span>{user?.name}</span>
          </div>
        </footer>
      </div>

      <div className="p-m">
        <Suspense fallback={<div>Loading view...</div>}>
          <Outlet />
        </Suspense>
      </div>
    </AppLayout>
  );
};

export default MainLayout;
