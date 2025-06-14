import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  // ACCI EAF Documentation Sidebar
  tutorialSidebar: [
    'index',
    'getting-started',
    {
      type: 'category',
      label: 'Architectural Principles',
      items: [
        'architectural-principles/index',
        'architectural-principles/ddd',
        'architectural-principles/hexagonal',
        'architectural-principles/hexagonal-architecture-guide',
        'architectural-principles/cqrs-es',
        'architectural-principles/eaf-eventsourcing-sdk',
        'architectural-principles/tdd',
        'architectural-principles/testing-strategy',
      ],
    },
    {
      type: 'category',
      label: 'Core Services',
      items: [
        'core-services/index',
        'core-services/security-context-access',
        'core-services/context-propagation',
        'core-services/eaf-iam-client-sdk',
        'core-services/nats-event-publishing',
        'core-services/nats-event-consumption',
        'core-services/nats-integration-testing',
        'core-services/spring-boot-integration-testing',
        'core-services/idempotent-projectors',
      ],
    },
    {
      type: 'category',
      label: 'UI Foundation Kit',
      items: ['ui-foundation-kit/index'],
    },
  ],
};

export default sidebars;
