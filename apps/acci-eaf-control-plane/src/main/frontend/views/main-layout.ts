import '@vaadin/app-layout';
import '@vaadin/app-layout/vaadin-drawer-toggle';
import '@vaadin/button';
import '@vaadin/icon';
import { Router } from '@vaadin/router';
import '@vaadin/tab';
import '@vaadin/tabs';
import { LitElement, css, html } from 'lit';
import { customElement, state } from 'lit/decorators.js';

@customElement('main-layout')
export class MainLayout extends LitElement {
  @state()
  private currentPath = '';

  static styles = css`
    :host {
      display: block;
      height: 100vh;
    }

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 var(--lumo-space-m);
      background: var(--lumo-primary-color);
      color: var(--lumo-primary-contrast-color);
    }

    .header h1 {
      margin: 0;
      font-size: var(--lumo-font-size-l);
      font-weight: var(--lumo-font-weight-bold);
    }

    .nav-tabs {
      background: var(--lumo-contrast-5pct);
    }

    .user-menu {
      display: flex;
      align-items: center;
      gap: var(--lumo-space-s);
    }

    .main-content {
      padding: var(--lumo-space-m);
      height: calc(100vh - 120px);
      overflow: auto;
    }
  `;

  connectedCallback() {
    super.connectedCallback();
    // Listen to router events to update current path
    window.addEventListener('vaadin-router-location-changed', this.updatePath);
    this.updatePath();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener(
      'vaadin-router-location-changed',
      this.updatePath
    );
  }

  private updatePath = () => {
    this.currentPath = window.location.pathname;
  };

  private navigate(path: string) {
    Router.go(path);
  }

  private handleLogout() {
    // TODO: Implement proper logout with EAF IAM
    console.log('Logout requested');
    this.navigate('/login');
  }

  render() {
    // Show minimal layout for login page
    if (this.currentPath === '/login') {
      return html`
        <div class="login-layout">
          <slot></slot>
        </div>
      `;
    }

    return html`
      <vaadin-app-layout>
        <vaadin-drawer-toggle
          slot="navbar touch-optimized"
        ></vaadin-drawer-toggle>

        <div slot="navbar" class="header">
          <h1>EAF Control Plane</h1>
          <div class="user-menu">
            <span>Admin User</span>
            <vaadin-button
              theme="tertiary-inline"
              @click="${this.handleLogout}"
            >
              Logout
            </vaadin-button>
          </div>
        </div>

        <vaadin-tabs slot="drawer" orientation="vertical" class="nav-tabs">
          <vaadin-tab>
            <vaadin-button
              theme="tertiary-inline"
              @click="${() => this.navigate('/dashboard')}"
              ?pressed="${this.currentPath === '/' ||
              this.currentPath === '/dashboard'}"
            >
              Dashboard
            </vaadin-button>
          </vaadin-tab>

          <vaadin-tab>
            <vaadin-button
              theme="tertiary-inline"
              @click="${() => this.navigate('/tenants')}"
              ?pressed="${this.currentPath === '/tenants'}"
            >
              Tenant Management
            </vaadin-button>
          </vaadin-tab>

          <vaadin-tab>
            <vaadin-button
              theme="tertiary-inline"
              @click="${() => this.navigate('/users')}"
              ?pressed="${this.currentPath === '/users'}"
            >
              User Management
            </vaadin-button>
          </vaadin-tab>
        </vaadin-tabs>

        <div slot="drawer-toggle">
          <vaadin-drawer-toggle></vaadin-drawer-toggle>
        </div>

        <main class="main-content">
          <slot></slot>
        </main>
      </vaadin-app-layout>
    `;
  }
}
