import { TicketEndpoint } from 'Frontend/generated/endpoints';
import { useAuth } from 'Frontend/hooks/useAuth';
import { useEffect, useState } from 'react';

interface TicketStats {
  openTickets: number;
  assignedToMe: number;
  closedToday: number;
}

export default function DashboardView() {
  const { user } = useAuth();
  const [debugResults, setDebugResults] = useState('');
  const [ticketStats, setTicketStats] = useState<TicketStats>({
    openTickets: 0,
    assignedToMe: 0,
    closedToday: 0,
  });
  const [loading, setLoading] = useState(true);

  const loadTicketStats = async () => {
    try {
      setLoading(true);
      const tickets = await TicketEndpoint.listTickets();

      if (tickets) {
        const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format

        const stats = {
          openTickets: tickets.filter(
            ticket =>
              ticket &&
              ticket.status &&
              !['CLOSED', 'RESOLVED'].includes(ticket.status)
          ).length,
          assignedToMe: tickets.filter(
            ticket => ticket && ticket.assigneeId === user?.username
          ).length,
          closedToday: tickets.filter(
            ticket =>
              ticket &&
              ticket.status &&
              ['CLOSED', 'RESOLVED'].includes(ticket.status) &&
              ticket.closedAt &&
              ticket.closedAt.startsWith(today)
          ).length,
        };

        setTicketStats(stats);
      }
    } catch (error) {
      console.error('Failed to load ticket stats:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTicketStats();
  }, [user]);

  const testAuth = async () => {
    try {
      const result = await TicketEndpoint.testAuth();
      setDebugResults(`testAuth: ${result}`);
    } catch (error: any) {
      setDebugResults(`testAuth error: ${error.message}`);
    }
  };

  const testHealthCheck = async () => {
    try {
      const result = await TicketEndpoint.healthCheck();
      setDebugResults(`healthCheck: ${result}`);
    } catch (error: any) {
      setDebugResults(`healthCheck error: ${error.message}`);
    }
  };

  const testCreateTicket = async () => {
    try {
      const result = await TicketEndpoint.createTicket({
        title: 'Test Ticket',
        description: 'Test Description',
        priority: 'MEDIUM',
        assigneeId: undefined,
      });
      setDebugResults(`createTicket: ${JSON.stringify(result)}`);
    } catch (error: any) {
      setDebugResults(`createTicket error: ${error.message}`);
    }
  };

  const testNonSuspend = async () => {
    setDebugResults(
      'testNonSuspend: Method not generated yet - try health check instead'
    );
  };

  const testSuspend = async () => {
    setDebugResults(
      'testSuspend: Method not generated yet - try create ticket instead'
    );
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold mb-4">Dashboard</h2>
        <p className="text-gray-600">
          Welcome to the ACCI EAF Ticket Management System
        </p>
      </div>

      {/* Authentication Debug Section */}
      <div
        style={{
          marginBottom: '20px',
          padding: '20px',
          border: '1px solid #ddd',
          borderRadius: '8px',
        }}
      >
        <h3>🔧 Debug Authentication & Endpoints</h3>
        <div
          style={{
            display: 'flex',
            gap: '10px',
            flexWrap: 'wrap',
            marginBottom: '10px',
          }}
        >
          <button onClick={testAuth}>Test Auth Endpoint</button>
          <button onClick={testHealthCheck}>Test Health Check</button>
          <button onClick={testCreateTicket}>Test Create Ticket</button>
          <button onClick={testNonSuspend}>Test Non-Suspend</button>
          <button onClick={testSuspend}>Test Suspend</button>
        </div>
        <div
          style={{
            minHeight: '100px',
            padding: '10px',
            backgroundColor: '#f5f5f5',
            borderRadius: '4px',
            whiteSpace: 'pre-wrap',
            fontFamily: 'monospace',
            fontSize: '12px',
          }}
        >
          {debugResults || 'Click buttons above to test endpoints...'}
        </div>
      </div>

      {/* Regular Dashboard Content */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-lg font-semibold mb-2">Open Tickets</h3>
          <p className="text-3xl font-bold text-blue-600">
            {loading ? '...' : ticketStats.openTickets}
          </p>
          <p className="text-sm text-gray-500">
            Active tickets requiring attention
          </p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-lg font-semibold mb-2">Assigned to Me</h3>
          <p className="text-3xl font-bold text-green-600">
            {loading ? '...' : ticketStats.assignedToMe}
          </p>
          <p className="text-sm text-gray-500">Tickets assigned to you</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-lg font-semibold mb-2">Closed Today</h3>
          <p className="text-3xl font-bold text-gray-600">
            {loading ? '...' : ticketStats.closedToday}
          </p>
          <p className="text-sm text-gray-500">Tickets resolved today</p>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="text-xl font-semibold">Quick Actions</h3>
        <div className="flex space-x-4">
          <a
            href="/tickets/create"
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Create New Ticket
          </a>
          <a
            href="/tickets"
            className="bg-gray-600 text-white px-4 py-2 rounded hover:bg-gray-700"
          >
            View All Tickets
          </a>
        </div>
      </div>
    </div>
  );
}
