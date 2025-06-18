import { AuthProvider } from 'Frontend/hooks/useAuth';
import { router } from 'Frontend/routes';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';

const container = document.getElementById('outlet');
if (container) {
  const root = createRoot(container);
  root.render(
    <React.StrictMode>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </React.StrictMode>
  );
} else {
  console.error('Could not find outlet container');
}
