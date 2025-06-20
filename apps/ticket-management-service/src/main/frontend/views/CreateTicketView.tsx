import { TicketEndpoint } from 'Frontend/generated/endpoints';
import { useAuth } from 'Frontend/hooks/useAuth';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface CreateTicketForm {
  title: string;
  description: string;
  priority: string;
}

export default function CreateTicketView() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [form, setForm] = useState<CreateTicketForm>({
    title: '',
    description: '',
    priority: 'MEDIUM',
  });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrors({});

    // Validation
    const newErrors: Record<string, string> = {};
    if (!form.title.trim()) {
      newErrors.title = 'Title is required';
    }
    if (!form.description.trim()) {
      newErrors.description = 'Description is required';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      setLoading(false);
      return;
    }

    try {
      // Call the TicketEndpoint to create the ticket using the connect client directly
      console.log('Creating ticket with user:', user);
      console.log('Form data:', form);

      const requestData = {
        title: form.title.trim(),
        description: form.description.trim(),
        priority: form.priority,
        assigneeId: undefined,
      };

      console.log('Sending request:', requestData);

      const result = await TicketEndpoint.createTicket(requestData);

      console.log('Ticket created successfully:', result);

      // Navigate back to tickets list on success
      navigate('/tickets');
    } catch (error) {
      console.error('Error creating ticket:', error);

      // More specific error handling
      let errorMessage = 'Failed to create ticket. Please try again.';
      if (error && typeof error === 'object') {
        if ('message' in error) {
          errorMessage = `Error: ${error.message}`;
        } else if ('status' in error) {
          if (error.status === 401 || error.status === 403) {
            errorMessage = 'Authentication failed. Please login and try again.';
          } else {
            errorMessage = `Server error (${error.status}). Please try again.`;
          }
        }
      }

      setErrors({ general: errorMessage });
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field: keyof CreateTicketForm, value: string) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Create New Ticket</h2>
        <button
          onClick={() => navigate('/tickets')}
          className="text-gray-600 hover:text-gray-800"
        >
          ← Back to Tickets
        </button>
      </div>

      <div className="bg-white shadow rounded-lg p-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          {errors.general && (
            <div className="bg-red-50 border border-red-300 text-red-700 px-4 py-3 rounded">
              {errors.general}
            </div>
          )}

          <div>
            <label
              htmlFor="title"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Title *
            </label>
            <input
              type="text"
              id="title"
              value={form.title}
              onChange={e => handleChange('title', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                errors.title ? 'border-red-300' : 'border-gray-300'
              }`}
              placeholder="Enter ticket title"
            />
            {errors.title && (
              <p className="mt-1 text-sm text-red-600">{errors.title}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Description *
            </label>
            <textarea
              id="description"
              rows={4}
              value={form.description}
              onChange={e => handleChange('description', e.target.value)}
              className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 ${
                errors.description ? 'border-red-300' : 'border-gray-300'
              }`}
              placeholder="Describe the issue or request"
            />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description}</p>
            )}
          </div>

          <div>
            <label
              htmlFor="priority"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Priority
            </label>
            <select
              id="priority"
              value={form.priority}
              onChange={e => handleChange('priority', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>

          <div className="flex justify-end space-x-3 pt-4">
            <button
              type="button"
              onClick={() => navigate('/tickets')}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Ticket'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
