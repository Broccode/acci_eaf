import UserInfo from 'Frontend/generated/com/axians/eaf/ticketmanagement/infrastructure/adapter/inbound/hilla/dto/UserInfo';
import { UserInfoService } from 'Frontend/generated/endpoints';
import React, {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useState,
} from 'react';

/**
 * Defines the shape of the authentication context, providing user information
 * and authentication status to the application.
 */
export interface AuthContextType {
  user: UserInfo | null;
  isAuthenticated: boolean;
  loading: boolean;
  logout: () => void;
}

/**
 * React Context for authentication. It will be used to provide the authentication
 * state to all components wrapped in the AuthProvider.
 */
export const AuthContext = createContext<AuthContextType | undefined>(
  undefined
);

/**
 * The properties for the AuthProvider component.
 */
interface AuthProviderProps {
  children: ReactNode;
}

/**
 * An AuthProvider component that wraps the application and provides authentication state.
 *
 * It fetches the current user's information on mount and makes it available through
 * the AuthContext. This is the central point for managing the application's auth state.
 */
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch user info when the provider mounts.
    // This determines if a user is already logged in.
    UserInfoService.getUserInfo().then(userInfo => {
      setUser(userInfo ?? null);
      setLoading(false);
    });
  }, []);

  const isAuthenticated = !!user && !user.anonymous;

  const logout = () => {
    // In a stateless JWT setup with cookies, the simplest "logout" is to
    // have the server clear the cookie. Spring Security's default /logout
    // endpoint handles this.
    window.location.href = '/logout';
  };

  const contextValue: AuthContextType = {
    user,
    isAuthenticated,
    loading,
    logout,
  };

  return (
    <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
  );
};

/**
 * Custom hook `useAuth` to easily access the authentication context.
 *
 * This hook simplifies consuming the AuthContext, providing a clean API
 * for components to get authentication state.
 *
 * @returns The authentication context.
 * @throws An error if used outside of an AuthProvider.
 */
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
