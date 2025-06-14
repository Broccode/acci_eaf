---
sidebar_position: 6
title: Frontend Integration
---

# Frontend Integration

Now let's create a React frontend component that interacts with your user profile backend service.
You'll learn how to consume REST APIs, handle async operations, and manage state in a modern React
application.

## 🎯 What You'll Build

A **User Profile Management UI** with:

- **Profile List**: Display all user profiles
- **Profile Form**: Create and edit user profiles
- **Profile Details**: View individual profile information
- **Error Handling**: User-friendly error states
- **Loading States**: Proper async operation feedback
- **Type Safety**: Full TypeScript integration

## 📋 Prerequisites

Ensure you have:

- ✅ Completed [Hello World Example](./hello-world-example.md)
- ✅ User profile service running on `http://localhost:8080`
- ✅ Node.js and npm installed
- ✅ Basic React and TypeScript knowledge

## 🚀 Setting Up the Frontend

### Create a Simple React App

Since this is focused on EAF integration, we'll create a minimal React setup:

```bash
# Create a simple React component in the existing project
mkdir -p apps/user-profile-ui/src/components
mkdir -p apps/user-profile-ui/src/services
mkdir -p apps/user-profile-ui/src/types
```

### TypeScript Types

First, let's define our TypeScript interfaces:

```typescript
// apps/user-profile-ui/src/types/UserProfile.ts
export interface UserProfile {
  id: string;
  name: string;
  email: string;
  bio: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserProfileRequest {
  name: string;
  email: string;
  bio: string;
}

export interface UpdateUserProfileRequest {
  name: string;
  email: string;
  bio: string;
}

export interface ApiError {
  message: string;
  status: number;
}
```

### API Service Layer

Create a service layer to handle all API communications:

```typescript
// apps/user-profile-ui/src/services/userProfileService.ts
import {
  UserProfile,
  CreateUserProfileRequest,
  UpdateUserProfileRequest,
  ApiError,
} from '../types/UserProfile';

const API_BASE_URL = 'http://localhost:8080/api/v1';

class UserProfileService {
  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      const error: ApiError = {
        message: `HTTP error! status: ${response.status}`,
        status: response.status,
      };
      throw error;
    }
    return response.json();
  }

  async getAllProfiles(): Promise<UserProfile[]> {
    const response = await fetch(`${API_BASE_URL}/user-profiles`);
    return this.handleResponse<UserProfile[]>(response);
  }

  async getProfileById(id: string): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/user-profiles/${id}`);
    return this.handleResponse<UserProfile>(response);
  }

  async createProfile(data: CreateUserProfileRequest): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/user-profiles`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return this.handleResponse<UserProfile>(response);
  }

  async updateProfile(id: string, data: UpdateUserProfileRequest): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/user-profiles/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    return this.handleResponse<UserProfile>(response);
  }

  async deactivateProfile(id: string): Promise<UserProfile> {
    const response = await fetch(`${API_BASE_URL}/user-profiles/${id}`, {
      method: 'DELETE',
    });
    return this.handleResponse<UserProfile>(response);
  }

  async checkHealth(): Promise<{ status: string }> {
    const response = await fetch(`${API_BASE_URL}/user-profiles/health`);
    return this.handleResponse<{ status: string }>(response);
  }
}

export const userProfileService = new UserProfileService();
```

## 🧩 React Components

### User Profile List Component

```typescript
// apps/user-profile-ui/src/components/UserProfileList.tsx
import React, { useState, useEffect } from 'react';
import { UserProfile, ApiError } from '../types/UserProfile';
import { userProfileService } from '../services/userProfileService';

interface UserProfileListProps {
  onSelectProfile: (profile: UserProfile) => void;
  onEditProfile: (profile: UserProfile) => void;
  refreshTrigger: number;
}

export const UserProfileList: React.FC<UserProfileListProps> = ({
  onSelectProfile,
  onEditProfile,
  refreshTrigger,
}) => {
  const [profiles, setProfiles] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadProfiles = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userProfileService.getAllProfiles();
      setProfiles(data);
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message || 'Failed to load profiles');
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivate = async (id: string) => {
    try {
      await userProfileService.deactivateProfile(id);
      await loadProfiles(); // Refresh the list
    } catch (err) {
      const apiError = err as ApiError;
      alert(`Failed to deactivate profile: ${apiError.message}`);
    }
  };

  useEffect(() => {
    loadProfiles();
  }, [refreshTrigger]);

  if (loading) {
    return (
      <div className="user-profile-list">
        <h2>User Profiles</h2>
        <div className="loading">Loading profiles...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="user-profile-list">
        <h2>User Profiles</h2>
        <div className="error">
          <p>Error: {error}</p>
          <button onClick={loadProfiles}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="user-profile-list">
      <h2>User Profiles ({profiles.length})</h2>
      {profiles.length === 0 ? (
        <p>No profiles found. Create your first profile!</p>
      ) : (
        <div className="profile-grid">
          {profiles.map((profile) => (
            <div
              key={profile.id}
              className={`profile-card ${!profile.isActive ? 'inactive' : ''}`}
            >
              <div className="profile-header">
                <h3>{profile.name}</h3>
                <span className={`status ${profile.isActive ? 'active' : 'inactive'}`}>
                  {profile.isActive ? 'Active' : 'Inactive'}
                </span>
              </div>
              <p className="email">{profile.email}</p>
              <p className="bio">{profile.bio}</p>
              <div className="profile-actions">
                <button
                  onClick={() => onSelectProfile(profile)}
                  className="btn-secondary"
                >
                  View Details
                </button>
                <button
                  onClick={() => onEditProfile(profile)}
                  className="btn-primary"
                  disabled={!profile.isActive}
                >
                  Edit
                </button>
                {profile.isActive && (
                  <button
                    onClick={() => {
                      if (confirm(`Are you sure you want to deactivate ${profile.name}?`)) {
                        handleDeactivate(profile.id);
                      }
                    }}
                    className="btn-danger"
                  >
                    Deactivate
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
```

### User Profile Form Component

```typescript
// apps/user-profile-ui/src/components/UserProfileForm.tsx
import React, { useState, useEffect } from 'react';
import { UserProfile, CreateUserProfileRequest, UpdateUserProfileRequest, ApiError } from '../types/UserProfile';
import { userProfileService } from '../services/userProfileService';

interface UserProfileFormProps {
  profile?: UserProfile;
  onSuccess: (profile: UserProfile) => void;
  onCancel: () => void;
}

export const UserProfileForm: React.FC<UserProfileFormProps> = ({
  profile,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState({
    name: profile?.name || '',
    email: profile?.email || '',
    bio: profile?.bio || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const isEdit = !!profile;

  useEffect(() => {
    if (profile) {
      setFormData({
        name: profile.name,
        email: profile.email,
        bio: profile.bio,
      });
    }
  }, [profile]);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.name.trim()) {
      errors.name = 'Name is required';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    }

    if (!formData.bio.trim()) {
      errors.bio = 'Bio is required';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
    // Clear validation error when user starts typing
    if (validationErrors[name]) {
      setValidationErrors(prev => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      let result: UserProfile;

      if (isEdit && profile) {
        const updateData: UpdateUserProfileRequest = {
          name: formData.name.trim(),
          email: formData.email.trim(),
          bio: formData.bio.trim(),
        };
        result = await userProfileService.updateProfile(profile.id, updateData);
      } else {
        const createData: CreateUserProfileRequest = {
          name: formData.name.trim(),
          email: formData.email.trim(),
          bio: formData.bio.trim(),
        };
        result = await userProfileService.createProfile(createData);
      }

      onSuccess(result);
    } catch (err) {
      const apiError = err as ApiError;
      setError(apiError.message || `Failed to ${isEdit ? 'update' : 'create'} profile`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="user-profile-form">
      <h2>{isEdit ? 'Edit Profile' : 'Create New Profile'}</h2>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="name">Name *</label>
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            className={validationErrors.name ? 'error' : ''}
            placeholder="Enter full name"
          />
          {validationErrors.name && (
            <span className="validation-error">{validationErrors.name}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="email">Email *</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleInputChange}
            className={validationErrors.email ? 'error' : ''}
            placeholder="Enter email address"
          />
          {validationErrors.email && (
            <span className="validation-error">{validationErrors.email}</span>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="bio">Bio *</label>
          <textarea
            id="bio"
            name="bio"
            value={formData.bio}
            onChange={handleInputChange}
            className={validationErrors.bio ? 'error' : ''}
            placeholder="Tell us about yourself"
            rows={4}
          />
          {validationErrors.bio && (
            <span className="validation-error">{validationErrors.bio}</span>
          )}
        </div>

        <div className="form-actions">
          <button
            type="button"
            onClick={onCancel}
            className="btn-secondary"
            disabled={loading}
          >
            Cancel
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={loading}
          >
            {loading ? 'Saving...' : (isEdit ? 'Update Profile' : 'Create Profile')}
          </button>
        </div>
      </form>
    </div>
  );
};
```

### User Profile Details Component

```typescript
// apps/user-profile-ui/src/components/UserProfileDetails.tsx
import React from 'react';
import { UserProfile } from '../types/UserProfile';

interface UserProfileDetailsProps {
  profile: UserProfile;
  onEdit: () => void;
  onBack: () => void;
}

export const UserProfileDetails: React.FC<UserProfileDetailsProps> = ({
  profile,
  onEdit,
  onBack,
}) => {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="user-profile-details">
      <div className="details-header">
        <button onClick={onBack} className="btn-secondary">
          ← Back to List
        </button>
        <h2>Profile Details</h2>
      </div>

      <div className="profile-info">
        <div className="info-section">
          <h3>Basic Information</h3>
          <div className="info-grid">
            <div className="info-item">
              <label>Name</label>
              <p>{profile.name}</p>
            </div>
            <div className="info-item">
              <label>Email</label>
              <p>{profile.email}</p>
            </div>
            <div className="info-item">
              <label>Status</label>
              <p className={`status ${profile.isActive ? 'active' : 'inactive'}`}>
                {profile.isActive ? 'Active' : 'Inactive'}
              </p>
            </div>
          </div>
        </div>

        <div className="info-section">
          <h3>Bio</h3>
          <p className="bio-text">{profile.bio}</p>
        </div>

        <div className="info-section">
          <h3>Metadata</h3>
          <div className="info-grid">
            <div className="info-item">
              <label>Profile ID</label>
              <p className="monospace">{profile.id}</p>
            </div>
            <div className="info-item">
              <label>Created</label>
              <p>{formatDate(profile.createdAt)}</p>
            </div>
            <div className="info-item">
              <label>Last Updated</label>
              <p>{formatDate(profile.updatedAt)}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="details-actions">
        {profile.isActive && (
          <button onClick={onEdit} className="btn-primary">
            Edit Profile
          </button>
        )}
      </div>
    </div>
  );
};
```

### Main Application Component

```typescript
// apps/user-profile-ui/src/components/UserProfileApp.tsx
import React, { useState, useEffect } from 'react';
import { UserProfile } from '../types/UserProfile';
import { UserProfileList } from './UserProfileList';
import { UserProfileForm } from './UserProfileForm';
import { UserProfileDetails } from './UserProfileDetails';
import { userProfileService } from '../services/userProfileService';

type ViewMode = 'list' | 'create' | 'edit' | 'details';

export const UserProfileApp: React.FC = () => {
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [selectedProfile, setSelectedProfile] = useState<UserProfile | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [connectionStatus, setConnectionStatus] = useState<'checking' | 'connected' | 'error'>('checking');

  useEffect(() => {
    // Check backend connection on startup
    checkBackendConnection();
  }, []);

  const checkBackendConnection = async () => {
    try {
      await userProfileService.checkHealth();
      setConnectionStatus('connected');
    } catch (error) {
      setConnectionStatus('error');
    }
  };

  const handleCreateNew = () => {
    setSelectedProfile(null);
    setViewMode('create');
  };

  const handleSelectProfile = (profile: UserProfile) => {
    setSelectedProfile(profile);
    setViewMode('details');
  };

  const handleEditProfile = (profile: UserProfile) => {
    setSelectedProfile(profile);
    setViewMode('edit');
  };

  const handleFormSuccess = () => {
    setViewMode('list');
    setSelectedProfile(null);
    setRefreshTrigger(prev => prev + 1); // Trigger list refresh
  };

  const handleFormCancel = () => {
    setViewMode('list');
    setSelectedProfile(null);
  };

  const handleBackToList = () => {
    setViewMode('list');
    setSelectedProfile(null);
  };

  if (connectionStatus === 'checking') {
    return (
      <div className="app-container">
        <div className="loading-screen">
          <h2>Connecting to User Profile Service...</h2>
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  if (connectionStatus === 'error') {
    return (
      <div className="app-container">
        <div className="error-screen">
          <h2>Connection Error</h2>
          <p>Unable to connect to the user profile service.</p>
          <p>Please ensure the backend service is running on http://localhost:8080</p>
          <button onClick={checkBackendConnection} className="btn-primary">
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>User Profile Management</h1>
        <div className="connection-status">
          <span className="status-indicator connected"></span>
          Connected to EAF Backend
        </div>
      </header>

      <main className="app-main">
        {viewMode === 'list' && (
          <div>
            <div className="list-header">
              <button onClick={handleCreateNew} className="btn-primary">
                Create New Profile
              </button>
            </div>
            <UserProfileList
              onSelectProfile={handleSelectProfile}
              onEditProfile={handleEditProfile}
              refreshTrigger={refreshTrigger}
            />
          </div>
        )}

        {(viewMode === 'create' || viewMode === 'edit') && (
          <UserProfileForm
            profile={selectedProfile || undefined}
            onSuccess={handleFormSuccess}
            onCancel={handleFormCancel}
          />
        )}

        {viewMode === 'details' && selectedProfile && (
          <UserProfileDetails
            profile={selectedProfile}
            onEdit={() => handleEditProfile(selectedProfile)}
            onBack={handleBackToList}
          />
        )}
      </main>
    </div>
  );
};
```

## 🎨 Basic CSS Styling

Create basic CSS for a clean, functional interface:

```css
/* apps/user-profile-ui/src/styles/app.css */

.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.status-indicator.connected {
  background-color: #4caf50;
}

/* Buttons */
.btn-primary,
.btn-secondary,
.btn-danger {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary {
  background-color: #2196f3;
  color: white;
}

.btn-primary:hover {
  background-color: #1976d2;
}

.btn-secondary {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}

.btn-danger {
  background-color: #f44336;
  color: white;
}

.btn-danger:hover {
  background-color: #d32f2f;
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Profile Grid */
.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.profile-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.profile-card.inactive {
  opacity: 0.6;
  background-color: #f9f9f9;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.status.active {
  color: #4caf50;
  font-weight: 500;
}

.status.inactive {
  color: #f44336;
  font-weight: 500;
}

.profile-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.profile-actions button {
  flex: 1;
  padding: 8px 12px;
  font-size: 12px;
}

/* Forms */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input.error,
.form-group textarea.error {
  border-color: #f44336;
}

.validation-error {
  display: block;
  color: #f44336;
  font-size: 12px;
  margin-top: 5px;
}

.form-actions {
  display: flex;
  gap: 15px;
  justify-content: flex-end;
  margin-top: 30px;
}

/* Loading and Error States */
.loading,
.error-message {
  text-align: center;
  padding: 40px;
  color: #666;
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
  border: 1px solid #ffcdd2;
  border-radius: 4px;
  margin-bottom: 20px;
}

.loading-screen,
.error-screen {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #2196f3;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 20px auto;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Details View */
.details-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.info-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 15px;
}

.info-item label {
  display: block;
  font-weight: 500;
  color: #666;
  margin-bottom: 5px;
}

.monospace {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background-color: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
}

.bio-text {
  line-height: 1.6;
  color: #333;
  margin-top: 10px;
}
```

## 🧪 Testing Your Frontend

### 1. Start Your Backend Service

```bash
# Ensure infrastructure is running
cd infra/docker-compose && docker compose up -d && cd ../..

# Start your user profile service
nx run user-profile:run
```

### 2. Create a Simple HTML Test Page

```html
<!-- apps/user-profile-ui/index.html -->
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>User Profile Management</title>
    <link rel="stylesheet" href="src/styles/app.css" />
  </head>
  <body>
    <div id="app"></div>

    <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>

    <script type="text/babel">
      // Import your components here and render the app
      // For a full setup, you'd typically use a build tool like Vite or Create React App

      const App = () => {
        return React.createElement(
          'div',
          { className: 'app-container' },
          React.createElement('h1', null, 'User Profile Management'),
          React.createElement(
            'p',
            null,
            'Frontend integration example - see the component files for full implementation!'
          )
        );
      };

      ReactDOM.render(React.createElement(App), document.getElementById('app'));
    </script>
  </body>
</html>
```

### 3. Test API Integration

You can test the API service independently:

```javascript
// Test in browser console or create a separate test file
import { userProfileService } from './services/userProfileService';

// Test creating a profile
const testProfile = {
  name: 'Jane Doe',
  email: 'jane.doe@example.com',
  bio: 'Frontend Developer',
};

userProfileService
  .createProfile(testProfile)
  .then(profile => console.log('Created:', profile))
  .catch(error => console.error('Error:', error));

// Test getting all profiles
userProfileService
  .getAllProfiles()
  .then(profiles => console.log('All profiles:', profiles))
  .catch(error => console.error('Error:', error));
```

## 🎯 Key Frontend Patterns

### Error Handling Strategy

```typescript
// Centralized error handling
const handleApiError = (error: unknown) => {
  if (error instanceof ApiError) {
    // Handle known API errors
    return error.message;
  }
  // Handle unexpected errors
  return 'An unexpected error occurred';
};
```

### Loading State Management

```typescript
// Consistent loading patterns
const [loading, setLoading] = useState(false);

const performAsyncOperation = async () => {
  try {
    setLoading(true);
    setError(null);
    // Perform operation
    const result = await apiCall();
    // Handle success
  } catch (error) {
    // Handle error
  } finally {
    setLoading(false);
  }
};
```

### Type-Safe API Calls

```typescript
// Always use TypeScript interfaces
const createProfile = async (data: CreateUserProfileRequest): Promise<UserProfile> => {
  // Implementation with proper typing
};
```

## ✅ Success Criteria

You've successfully integrated the frontend if:

- ✅ Components render without errors
- ✅ API calls work correctly with proper error handling
- ✅ Loading states provide good user feedback
- ✅ Form validation works client-side
- ✅ CRUD operations work end-to-end
- ✅ TypeScript provides full type safety

## 🚀 Next Steps

Great job! You now have a complete full-stack application. Continue to
[Development Workflow](./development-workflow.md) to learn about day-to-day development practices
and tooling.

---

**Fantastic!** You've successfully integrated a React frontend with your EAF backend service! 🎉
