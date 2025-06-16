import React, { useEffect, useState } from 'react';

interface TicketStats {
  total: number;
  open: number;
  assigned: number;
  closed: number;
}

export default function DashboardView() {
  const [stats, setStats] = useState<TicketStats>({
    total: 0,
    open: 0,
    assigned: 0,
    closed: 0,
  });

  useEffect(() => {
    // TODO: Load statistics from Hilla endpoint
    // For now, using placeholder data
    setStats({
      total: 0,
      open: 0,
      assigned: 0,
      closed: 0,
    });
  }, []);

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Ticket Management Dashboard</h2>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
          <h3 className="text-lg font-semibold text-blue-800">Total Tickets</h3>
          <p className="text-3xl font-bold text-blue-600">{stats.total}</p>
        </div>

        <div className="bg-yellow-50 p-4 rounded-lg border border-yellow-200">
          <h3 className="text-lg font-semibold text-yellow-800">Open</h3>
          <p className="text-3xl font-bold text-yellow-600">{stats.open}</p>
        </div>

        <div className="bg-orange-50 p-4 rounded-lg border border-orange-200">
          <h3 className="text-lg font-semibold text-orange-800">Assigned</h3>
          <p className="text-3xl font-bold text-orange-600">{stats.assigned}</p>
        </div>

        <div className="bg-green-50 p-4 rounded-lg border border-green-200">
          <h3 className="text-lg font-semibold text-green-800">Closed</h3>
          <p className="text-3xl font-bold text-green-600">{stats.closed}</p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-lg shadow border">
        <h3 className="text-xl font-semibold mb-4">Quick Actions</h3>
        <div className="space-y-2">
          <a
            href="/tickets/create"
            className="inline-block bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
          >
            Create New Ticket
          </a>
          <br />
          <a
            href="/tickets"
            className="inline-block bg-gray-600 text-white px-4 py-2 rounded hover:bg-gray-700"
          >
            View All Tickets
          </a>
        </div>
      </div>
    </div>
  );
}
