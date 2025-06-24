import '@vaadin/button';
import '@vaadin/login-form';
import '@vaadin/notification';
import { Router } from '@vaadin/router';
import { LitElement, css, html } from 'lit';
import { customElement, state } from 'lit/decorators.js';

@customElement('login-view')
export class LoginView extends LitElement {
  @state()
  private isLoading = false;

  @state()
  private errorMessage = '';

  static styles = css`
    :host {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: var(--lumo-contrast-5pct);
    }

    .login-container {
      background: var(--lumo-base-color);
      padding: var(--lumo-space-xl);
      border-radius: var(--lumo-border-radius-l);
      box-shadow: var(--lumo-box-shadow-s);
      max-width: 400px;
      width: 100%;
    }

    .login-header {
      text-align: center;
      margin-bottom: var(--lumo-space-l);
    }

    .login-header h1 {
      margin: 0 0 var(--lumo-space-s) 0;
      color: var(--lumo-primary-color);
      font-size: var(--lumo-font-size-xl);
    }

    .login-header p {
      margin: 0;
      color: var(--lumo-secondary-text-color);
      font-size: var(--lumo-font-size-s);
    }

    .error-message {
      color: var(--lumo-error-color);
      font-size: var(--lumo-font-size-s);
      margin-top: var(--lumo-space-s);
      text-align: center;
    }

    vaadin-login-form {
      --vaadin-login-form-background: transparent;
      --vaadin-login-form-form-background: transparent;
    }
  `;

  private async handleLogin(event: CustomEvent) {
    this.isLoading = true;
    this.errorMessage = '';

    const { username, password } = event.detail;

    try {
      // TODO: Replace with actual EAF IAM authentication
      console.log('Attempting login with:', { username });

      // Mock authentication for now
      if (username === 'admin@eaf.com' && password === 'admin123') {
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, 1000));

        // Store authentication state (in real app, this would be handled by EAF IAM)
        sessionStorage.setItem('eaf-authenticated', 'true');
        sessionStorage.setItem(
          'eaf-user',
          JSON.stringify({
            username,
            roles: ['SUPER_ADMIN'],
            tenantId: 'system',
          })
        );

        // Navigate to dashboard
        Router.go('/dashboard');
      } else {
        this.errorMessage = 'Invalid credentials. Please try again.';
      }
    } catch (error) {
      console.error('Login error:', error);
      this.errorMessage = 'Login failed. Please try again.';
    } finally {
      this.isLoading = false;
    }
  }

  render() {
    return html`
      <div class="login-container">
        <div class="login-header">
          <h1>EAF Control Plane</h1>
          <p>Administrative Access Portal</p>
        </div>

        <vaadin-login-form
          .disabled="${this.isLoading}"
          @login="${this.handleLogin}"
        >
        </vaadin-login-form>

        ${this.errorMessage
          ? html`<div class="error-message">${this.errorMessage}</div>`
          : ''}
      </div>
    `;
  }
}
