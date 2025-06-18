import { LoginI18n, LoginOverlay } from '@vaadin/react-components';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

const loginI18n: LoginI18n = {
  header: {
    title: 'ACCI EAF Ticket Management',
    description: 'Login using user/password',
  },
  form: {
    title: 'Login',
    username: 'Username',
    password: 'Password',
    submit: 'Login',
    forgotPassword: 'Forgot password',
  },
  errorMessage: {
    title: 'Incorrect username or password',
    message:
      'Check that you have entered the correct username and password and try again.',
  },
};

export default function LoginView() {
  const [error, setError] = useState(false);
  const location = useLocation();

  // The 'error' query parameter is set by Spring Security on failed login attempts.
  // We use it to display the error message.
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    setError(params.has('error'));
  }, [location]);

  return (
    <LoginOverlay
      opened
      error={error}
      i18n={loginI18n}
      // The action attribute points to the Spring Security login processing URL.
      action="/login"
      noForgotPassword
    />
  );
}
