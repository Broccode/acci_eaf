import '@vaadin/button';
import '@vaadin/card';
import '@vaadin/grid';
import '@vaadin/icon';
import '@vaadin/icons';
import '@vaadin/progress-bar';
import { LitElement, css, html } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface SystemMetrics {
  totalTenants: number;
  activeTenants: number;
  totalUsers: number;
  activeUsers: number;
  systemHealth: 'healthy' | 'warning' | 'error';
  lastUpdated: string;
}

interface RecentActivity {
  id: string;
  type: 'tenant_created' | 'user_created' | 'login' | 'system_alert';
  description: string;
  timestamp: string;
  severity: 'info' | 'warning' | 'error';
}

@customElement('dashboard-view')
export class DashboardView extends LitElement {
  @state()
  private metrics: SystemMetrics = {
    totalTenants: 0,
    activeTenants: 0,
    totalUsers: 0,
    activeUsers: 0,
    systemHealth: 'healthy',
    lastUpdated: new Date().toISOString(),
  };

  @state()
  private recentActivity: RecentActivity[] = [];

  @state()
  private isLoading = true;

  static styles = css`
    :host {
      display: block;
      padding: var(--lumo-space-m);
    }

    .dashboard-header {
      margin-bottom: var(--lumo-space-l);
    }

    .dashboard-header h1 {
      margin: 0 0 var(--lumo-space-s) 0;
      color: var(--lumo-header-text-color);
    }

    .dashboard-header p {
      margin: 0;
      color: var(--lumo-secondary-text-color);
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: var(--lumo-space-m);
      margin-bottom: var(--lumo-space-xl);
    }

    .metric-card {
      padding: var(--lumo-space-l);
    }

    .metric-value {
      font-size: var(--lumo-font-size-xxl);
      font-weight: var(--lumo-font-weight-bold);
      color: var(--lumo-primary-color);
      margin: var(--lumo-space-s) 0;
    }

    .metric-label {
      color: var(--lumo-secondary-text-color);
      font-size: var(--lumo-font-size-s);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .metric-change {
      font-size: var(--lumo-font-size-s);
      margin-top: var(--lumo-space-xs);
    }

    .metric-change.positive {
      color: var(--lumo-success-color);
    }

    .metric-change.negative {
      color: var(--lumo-error-color);
    }

    .health-indicator {
      display: flex;
      align-items: center;
      gap: var(--lumo-space-xs);
    }

    .health-indicator.healthy {
      color: var(--lumo-success-color);
    }

    .health-indicator.warning {
      color: var(--lumo-warning-color);
    }

    .health-indicator.error {
      color: var(--lumo-error-color);
    }

    .activity-section {
      margin-top: var(--lumo-space-xl);
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--lumo-space-m);
    }

    .section-title {
      font-size: var(--lumo-font-size-l);
      font-weight: var(--lumo-font-weight-bold);
      margin: 0;
    }

    .activity-item {
      display: flex;
      align-items: center;
      padding: var(--lumo-space-s) 0;
      border-bottom: 1px solid var(--lumo-contrast-10pct);
    }

    .activity-icon {
      margin-right: var(--lumo-space-m);
      width: 24px;
      height: 24px;
    }

    .activity-content {
      flex: 1;
    }

    .activity-description {
      font-weight: var(--lumo-font-weight-medium);
    }

    .activity-timestamp {
      color: var(--lumo-secondary-text-color);
      font-size: var(--lumo-font-size-s);
    }

    .loading-state {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 200px;
    }

    .quick-actions {
      display: flex;
      gap: var(--lumo-space-m);
      margin-top: var(--lumo-space-l);
    }
  `;

  connectedCallback() {
    super.connectedCallback();
    this.loadDashboardData();
  }

  private async loadDashboardData() {
    try {
      // TODO: Replace with actual API calls to EAF services
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1500));

      // Mock data - in real implementation, this would come from backend endpoints
      this.metrics = {
        totalTenants: 12,
        activeTenants: 11,
        totalUsers: 847,
        activeUsers: 234,
        systemHealth: 'healthy',
        lastUpdated: new Date().toISOString(),
      };

      this.recentActivity = [
        {
          id: '1',
          type: 'tenant_created',
          description: 'New tenant "Acme Corp" created by admin@eaf.com',
          timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(), // 30 min ago
          severity: 'info',
        },
        {
          id: '2',
          type: 'user_created',
          description: 'User john.doe@acme.com added to Acme Corp tenant',
          timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), // 2 hours ago
          severity: 'info',
        },
        {
          id: '3',
          type: 'login',
          description: 'Successful admin login from 192.168.1.100',
          timestamp: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(), // 4 hours ago
          severity: 'info',
        },
      ];
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      this.isLoading = false;
    }
  }

  private formatTimestamp(isoString: string): string {
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    return `${diffDays} days ago`;
  }

  private getActivityIcon(type: string): string {
    switch (type) {
      case 'tenant_created':
        return 'vaadin:building';
      case 'user_created':
        return 'vaadin:user-plus';
      case 'login':
        return 'vaadin:sign-in';
      case 'system_alert':
        return 'vaadin:warning';
      default:
        return 'vaadin:info-circle';
    }
  }

  render() {
    if (this.isLoading) {
      return html`
        <div class="loading-state">
          <vaadin-progress-bar indeterminate></vaadin-progress-bar>
        </div>
      `;
    }

    return html`
      <div class="dashboard-header">
        <h1>Dashboard</h1>
        <p>EAF Control Plane System Overview</p>
      </div>

      <div class="metrics-grid">
        <vaadin-card class="metric-card">
          <div class="metric-label">Total Tenants</div>
          <div class="metric-value">${this.metrics.totalTenants}</div>
          <div class="metric-change positive">+2 this month</div>
        </vaadin-card>

        <vaadin-card class="metric-card">
          <div class="metric-label">Active Tenants</div>
          <div class="metric-value">${this.metrics.activeTenants}</div>
          <div class="metric-change positive">91.7% active rate</div>
        </vaadin-card>

        <vaadin-card class="metric-card">
          <div class="metric-label">Total Users</div>
          <div class="metric-value">${this.metrics.totalUsers}</div>
          <div class="metric-change positive">+23 this week</div>
        </vaadin-card>

        <vaadin-card class="metric-card">
          <div class="metric-label">System Health</div>
          <div class="health-indicator ${this.metrics.systemHealth}">
            <vaadin-icon icon="vaadin:check-circle"></vaadin-icon>
            <span
              class="metric-value"
              style="font-size: var(--lumo-font-size-l);"
            >
              ${this.metrics.systemHealth.toUpperCase()}
            </span>
          </div>
        </vaadin-card>
      </div>

      <div class="quick-actions">
        <vaadin-button theme="primary">Create New Tenant</vaadin-button>
        <vaadin-button>Manage Users</vaadin-button>
        <vaadin-button>System Settings</vaadin-button>
      </div>

      <div class="activity-section">
        <div class="section-header">
          <h2 class="section-title">Recent Activity</h2>
          <vaadin-button theme="tertiary small">View All</vaadin-button>
        </div>

        <vaadin-card>
          ${this.recentActivity.map(
            activity => html`
              <div class="activity-item">
                <vaadin-icon
                  class="activity-icon"
                  icon="${this.getActivityIcon(activity.type)}"
                >
                </vaadin-icon>
                <div class="activity-content">
                  <div class="activity-description">
                    ${activity.description}
                  </div>
                  <div class="activity-timestamp">
                    ${this.formatTimestamp(activity.timestamp)}
                  </div>
                </div>
              </div>
            `
          )}
        </vaadin-card>
      </div>
    `;
  }
}
