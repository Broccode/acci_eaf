import type {
  EchoRequest,
  EchoResponse,
  HealthResponse,
  SystemInfoResponse,
  UpdatePreferencesRequest,
  UserInfoResponse,
  UserPreferencesResponse,
} from 'Frontend/generated/endpoints';
import {
  ControlPlaneHealthEndpoint,
  UserInfoEndpoint,
} from 'Frontend/generated/endpoints';
import { css, html, LitElement } from 'lit';
import { customElement, state } from 'lit/decorators.js';

/**
 * TypeScript component demonstrating type-safe communication with Hilla endpoints.
 * Validates TypeScript client generation and type safety features.
 */
@customElement('endpoint-test-view')
export class EndpointTestView extends LitElement {
  @state()
  private healthData: HealthResponse | null = null;

  @state()
  private systemInfo: SystemInfoResponse | null = null;

  @state()
  private echoResult: EchoResponse | null = null;

  @state()
  private userInfo: UserInfoResponse | null = null;

  @state()
  private userPreferences: UserPreferencesResponse | null = null;

  @state()
  private loading = false;

  @state()
  private error: string | null = null;

  static styles = css`
    :host {
      display: block;
      padding: 20px;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .container {
      max-width: 1200px;
      margin: 0 auto;
    }

    .section {
      background: #f5f5f5;
      border-radius: 8px;
      padding: 20px;
      margin-bottom: 20px;
      border-left: 4px solid #007acc;
    }

    .section h3 {
      margin-top: 0;
      color: #333;
    }

    .button-group {
      margin-bottom: 15px;
    }

    button {
      background: #007acc;
      color: white;
      border: none;
      padding: 10px 15px;
      border-radius: 4px;
      cursor: pointer;
      margin-right: 10px;
      margin-bottom: 10px;
    }

    button:hover {
      background: #005a99;
    }

    button:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    .result {
      background: white;
      border: 1px solid #ddd;
      border-radius: 4px;
      padding: 15px;
      margin-top: 10px;
      font-family: 'Courier New', monospace;
      white-space: pre-wrap;
      max-height: 300px;
      overflow-y: auto;
    }

    .error {
      background: #ffe6e6;
      border-color: #ff9999;
      color: #cc0000;
    }

    .success {
      background: #e6ffe6;
      border-color: #99ff99;
      color: #006600;
    }

    .input-group {
      margin-bottom: 15px;
    }

    label {
      display: block;
      margin-bottom: 5px;
      font-weight: bold;
    }

    input,
    textarea,
    select {
      width: 100%;
      padding: 8px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-family: inherit;
    }

    textarea {
      height: 80px;
      resize: vertical;
    }

    .type-info {
      background: #e8f4f8;
      border: 1px solid #bee5eb;
      border-radius: 4px;
      padding: 10px;
      margin-top: 10px;
      font-size: 0.9em;
      color: #0c5460;
    }
  `;

  render() {
    return html`
      <div class="container">
        <h2>🔧 EAF Control Plane Endpoint Testing</h2>
        <p>
          This view demonstrates
          <strong>type-safe TypeScript</strong> communication with Hilla
          endpoints.
        </p>

        <!-- Health Endpoint Testing -->
        <div class="section">
          <h3>🏥 Health Endpoint</h3>
          <div class="button-group">
            <button @click=${this.checkHealth} ?disabled=${this.loading}>
              Check Health
            </button>
            <button @click=${this.getSystemInfo} ?disabled=${this.loading}>
              Get System Info
            </button>
          </div>

          ${this.healthData
            ? html`
                <div class="result success">
                  <strong>Health Status:</strong> ${this.healthData.status}
                  <br /><strong>Version:</strong> ${this.healthData.version}
                  <br /><strong>Services:</strong>
                  ${Object.entries(this.healthData.services).map(
                    ([name, service]) => html`
                      <br />
                      • ${name}: ${service.status} (${service.responseTime})
                    `
                  )}
                  <br /><strong>Check Duration:</strong> ${this.healthData
                    .metadata.checkDuration}
                </div>
                <div class="type-info">
                  ✅ TypeScript types: HealthResponse, ServiceHealth,
                  HealthMetadata
                </div>
              `
            : ''}
          ${this.systemInfo
            ? html`
                <div class="result success">
                  <strong>System Information:</strong>
                  <br />Application:
                  ${this.systemInfo.systemInfo?.applicationName} <br />Version:
                  ${this.systemInfo.systemInfo?.version} <br />Java:
                  ${this.systemInfo.systemInfo?.javaVersion} <br />Spring Boot:
                  ${this.systemInfo.systemInfo?.springBootVersion} <br />Hilla:
                  ${this.systemInfo.systemInfo?.hillaVersion} <br />Uptime:
                  ${this.systemInfo.systemInfo?.uptime}
                </div>
                <div class="type-info">
                  ✅ TypeScript types: SystemInfoResponse, SystemInfo,
                  ResponseMetadata
                </div>
              `
            : ''}
        </div>

        <!-- Echo Endpoint Testing -->
        <div class="section">
          <h3>📡 Echo Endpoint (Input Validation)</h3>
          <div class="input-group">
            <label>Message to Echo:</label>
            <textarea
              id="echoMessage"
              placeholder="Enter a message to test echo functionality and validation..."
            ></textarea>
          </div>
          <div class="button-group">
            <button @click=${this.testEcho} ?disabled=${this.loading}>
              Send Echo
            </button>
            <button @click=${this.testEchoValidation} ?disabled=${this.loading}>
              Test Validation (Long Message)
            </button>
          </div>

          ${this.echoResult
            ? html`
                <div class="result success">
                  <strong>Echo Response:</strong>
                  <br />Original: "${this.echoResult.originalMessage}"
                  <br />Server Response: "${this.echoResult.serverResponse}"
                  <br />Message Length: ${this.echoResult.messageLength}
                  characters <br />Request ID:
                  ${this.echoResult.metadata.requestId}
                </div>
                <div class="type-info">
                  ✅ TypeScript types: EchoRequest, EchoResponse with validation
                </div>
              `
            : ''}
        </div>

        <!-- User Info Endpoint Testing -->
        <div class="section">
          <h3>👤 User Info Endpoint (EAF Integration)</h3>
          <div class="button-group">
            <button @click=${this.getCurrentUser} ?disabled=${this.loading}>
              Get Current User
            </button>
            <button @click=${this.getUserPreferences} ?disabled=${this.loading}>
              Get Preferences
            </button>
          </div>

          ${this.userInfo
            ? html`
                <div class="result success">
                  <strong>User Information:</strong>
                  <br />Name: ${this.userInfo.user?.firstName}
                  ${this.userInfo.user?.lastName} <br />Email:
                  ${this.userInfo.user?.email} <br />Tenant:
                  ${this.userInfo.user?.tenantName}
                  (${this.userInfo.user?.tenantId}) <br />Roles:
                  ${this.userInfo.user?.roles.join(', ')} <br />Status:
                  ${this.userInfo.user?.accountStatus} <br /><strong
                    >Permissions:</strong
                  >
                  <br />
                  • Create Tenants:
                  ${this.userInfo.permissions?.canCreateTenants} <br />
                  • Manage Users: ${this.userInfo.permissions?.canManageUsers}
                  <br />
                  • View Audit Logs:
                  ${this.userInfo.permissions?.canViewAuditLogs}
                </div>
                <div class="type-info">
                  ✅ TypeScript types: UserInfoResponse, UserInfo,
                  UserPermissions
                </div>
              `
            : ''}
          ${this.userPreferences
            ? html`
                <div class="result success">
                  <strong>User Preferences:</strong>
                  <br />Theme: ${this.userPreferences.preferences?.theme}
                  <br />Language: ${this.userPreferences.preferences?.language}
                  <br />Timezone: ${this.userPreferences.preferences?.timezone}
                  <br />Date Format:
                  ${this.userPreferences.preferences?.dateFormat} <br />Items
                  per Page: ${this.userPreferences.preferences?.itemsPerPage}
                  <br />Notifications:
                  ${this.userPreferences.preferences?.enableNotifications}
                </div>
                <div class="type-info">
                  ✅ TypeScript types: UserPreferencesResponse, UserPreferences
                </div>
              `
            : ''}
        </div>

        <!-- Preference Update Testing -->
        <div class="section">
          <h3>⚙️ Update Preferences (Validation Testing)</h3>
          <div class="input-group">
            <label>Theme:</label>
            <select id="themeSelect">
              <option value="light">Light</option>
              <option value="dark">Dark</option>
              <option value="auto">Auto</option>
              <option value="invalid">Invalid (for testing)</option>
            </select>
          </div>
          <div class="input-group">
            <label>Items per Page:</label>
            <input
              type="number"
              id="itemsPerPage"
              value="25"
              min="1"
              max="200"
            />
          </div>
          <div class="button-group">
            <button @click=${this.updatePreferences} ?disabled=${this.loading}>
              Update Preferences
            </button>
            <button
              @click=${this.testInvalidPreferences}
              ?disabled=${this.loading}
            >
              Test Invalid Data
            </button>
          </div>
        </div>

        <!-- Error Display -->
        ${this.error
          ? html`
              <div class="result error">
                <strong>Error:</strong> ${this.error}
                <div class="type-info">
                  ✅ TypeScript error handling with EndpointException
                </div>
              </div>
            `
          : ''}

        <!-- Loading Indicator -->
        ${this.loading ? html` <div class="result">⏳ Loading...</div> ` : ''}
      </div>
    `;
  }

  private async checkHealth() {
    this.loading = true;
    this.error = null;
    try {
      // TypeScript ensures type safety at compile time
      this.healthData = await ControlPlaneHealthEndpoint.checkHealth();
    } catch (error: any) {
      this.error = `Health check failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async getSystemInfo() {
    this.loading = true;
    this.error = null;
    try {
      this.systemInfo = await ControlPlaneHealthEndpoint.getSystemInfo();
    } catch (error: any) {
      this.error = `System info failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async testEcho() {
    this.loading = true;
    this.error = null;
    try {
      const messageInput = this.shadowRoot?.querySelector(
        '#echoMessage'
      ) as HTMLTextAreaElement;
      const message = messageInput?.value || 'Default test message';

      // TypeScript ensures request structure is correct
      const request: EchoRequest = {
        message: message,
        includeTimestamp: true,
      };

      this.echoResult = await ControlPlaneHealthEndpoint.echo(request);
    } catch (error: any) {
      this.error = `Echo failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async testEchoValidation() {
    this.loading = true;
    this.error = null;
    try {
      // Test validation with message > 1000 characters
      const longMessage = 'A'.repeat(1001);
      const request: EchoRequest = {
        message: longMessage,
        includeTimestamp: true,
      };

      this.echoResult = await ControlPlaneHealthEndpoint.echo(request);
    } catch (error: any) {
      this.error = `Validation test (expected): ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async getCurrentUser() {
    this.loading = true;
    this.error = null;
    try {
      this.userInfo = await UserInfoEndpoint.getCurrentUser();
    } catch (error: any) {
      this.error = `Get user failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async getUserPreferences() {
    this.loading = true;
    this.error = null;
    try {
      this.userPreferences = await UserInfoEndpoint.getUserPreferences();
    } catch (error: any) {
      this.error = `Get preferences failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async updatePreferences() {
    this.loading = true;
    this.error = null;
    try {
      const themeSelect = this.shadowRoot?.querySelector(
        '#themeSelect'
      ) as HTMLSelectElement;
      const itemsInput = this.shadowRoot?.querySelector(
        '#itemsPerPage'
      ) as HTMLInputElement;

      // TypeScript ensures request structure is correct
      const request: UpdatePreferencesRequest = {
        theme: themeSelect?.value,
        itemsPerPage: parseInt(itemsInput?.value || '25'),
      };

      const result = await UserInfoEndpoint.updateUserPreferences(request);
      this.userPreferences = result;
    } catch (error: any) {
      this.error = `Update preferences failed: ${error.message}`;
    } finally {
      this.loading = false;
    }
  }

  private async testInvalidPreferences() {
    this.loading = true;
    this.error = null;
    try {
      // Test validation with invalid data
      const request: UpdatePreferencesRequest = {
        theme: 'invalid-theme',
        itemsPerPage: 999, // Outside valid range
      };

      const result = await UserInfoEndpoint.updateUserPreferences(request);
      this.userPreferences = result;
    } catch (error: any) {
      this.error = `Validation test (expected): ${error.message}`;
    } finally {
      this.loading = false;
    }
  }
}
