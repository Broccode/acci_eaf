import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

interface TicketDetail {
  id: string;
  title: string;
  description: string;
  status: string;
  priority: string;
  assigneeId?: string;
  createdAt: string;
  closedAt?: string;
}

interface AssignTicketForm {
  assigneeId: string;
}

export default function TicketDetailView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [assignForm, setAssignForm] = useState<AssignTicketForm>({
    assigneeId: '',
  });
  const [showAssignForm, setShowAssignForm] = useState(false);

  useEffect(() => {
    if (id) {
      loadTicket(id);
    }
  }, [id]);

  const loadTicket = async () => {
    try {
      // TODO: Load ticket from Hilla endpoint
      // For now, using placeholder data
      setTicket(null);
    } catch (error) {
      console.error('Error loading ticket:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAssignTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticket || !assignForm.assigneeId.trim()) return;

    try {
      // TODO: Assign ticket via Hilla endpoint
      console.log('Assigning ticket:', ticket.id, 'to:', assignForm.assigneeId);

      // Update local state
      setTicket(prev =>
        prev
          ? { ...prev, status: 'ASSIGNED', assigneeId: assignForm.assigneeId }
          : null
      );
      setShowAssignForm(false);
    } catch (error) {
      console.error('Error assigning ticket:', error);
    }
  };

  const handleCloseTicket = async () => {
    if (!ticket) return;

    try {
      // TODO: Close ticket via Hilla endpoint
      console.log('Closing ticket:', ticket.id);

      // Update local state
      setTicket(prev =>
        prev
          ? { ...prev, status: 'CLOSED', closedAt: new Date().toISOString() }
          : null
      );
    } catch (error) {
      console.error('Error closing ticket:', error);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN':
        return 'bg-yellow-100 text-yellow-800';
      case 'ASSIGNED':
        return 'bg-orange-100 text-orange-800';
      case 'CLOSED':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-red-100 text-red-800';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-800';
      case 'LOW':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return <div>Loading ticket...</div>;
  }

  if (!ticket) {
    return (
      <div className="text-center py-8">
        <h2 className="text-xl font-semibold text-gray-700">
          Ticket not found
        </h2>
        <button
          onClick={() => navigate('/tickets')}
          className="mt-4 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Back to Tickets
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Ticket Details</h2>
        <button
          onClick={() => navigate('/tickets')}
          className="text-gray-600 hover:text-gray-800"
        >
          ← Back to Tickets
        </button>
      </div>

      <div className="bg-white shadow rounded-lg p-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-4">
            <div>
              <h3 className="text-lg font-semibold text-gray-900">
                {ticket.title}
              </h3>
              <p className="text-gray-600 mt-2">{ticket.description}</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Status
              </label>
              <span
                className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(ticket.status)}`}
              >
                {ticket.status}
              </span>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Priority
              </label>
              <span
                className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getPriorityColor(ticket.priority)}`}
              >
                {ticket.priority}
              </span>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Assignee
              </label>
              <p className="text-sm text-gray-900">
                {ticket.assigneeId || 'Unassigned'}
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700">
                Created
              </label>
              <p className="text-sm text-gray-900">
                {new Date(ticket.createdAt).toLocaleString()}
              </p>
            </div>

            {ticket.closedAt && (
              <div>
                <label className="block text-sm font-medium text-gray-700">
                  Closed
                </label>
                <p className="text-sm text-gray-900">
                  {new Date(ticket.closedAt).toLocaleString()}
                </p>
              </div>
            )}
          </div>
        </div>

        {ticket.status !== 'CLOSED' && (
          <div className="mt-6 pt-6 border-t border-gray-200">
            <h4 className="text-lg font-medium text-gray-900 mb-4">Actions</h4>
            <div className="flex space-x-3">
              {ticket.status === 'OPEN' && (
                <button
                  onClick={() => setShowAssignForm(!showAssignForm)}
                  className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                >
                  Assign Ticket
                </button>
              )}

              <button
                onClick={handleCloseTicket}
                className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
              >
                Close Ticket
              </button>
            </div>

            {showAssignForm && (
              <form
                onSubmit={handleAssignTicket}
                className="mt-4 p-4 bg-gray-50 rounded"
              >
                <div>
                  <label
                    htmlFor="assigneeId"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Assign to User ID
                  </label>
                  <input
                    type="text"
                    id="assigneeId"
                    value={assignForm.assigneeId}
                    onChange={e =>
                      setAssignForm({ assigneeId: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-gray-300 rounded-md"
                    placeholder="Enter user ID"
                  />
                </div>
                <div className="mt-3 flex space-x-2">
                  <button
                    type="submit"
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                  >
                    Assign
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowAssignForm(false)}
                    className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
