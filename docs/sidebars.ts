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
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/index',
        'getting-started/prerequisites',
        'getting-started/local-setup',
        'getting-started/first-service',
        'getting-started/hello-world-example',
        'getting-started/frontend-integration',
        'getting-started/development-workflow',
        'getting-started/troubleshooting',
      ],
    },
    {
      type: 'category',
      label: 'Core Architecture',
      items: [
        'architecture/index',
        'architecture/eaf-overview',
        'architecture/domain-driven-design',
        'architecture/hexagonal-architecture-guide',
        'architecture/cqrs-event-sourcing',
        'architecture/test-driven-development',
        'architecture/testing-strategy',
        'architecture/architectural-decisions',
        'architecture/patterns-and-practices',
      ],
    },
    {
      type: 'category',
      label: 'SDK Reference',
      items: [
        'sdk-reference/index',
        {
          type: 'category',
          label: 'Eventing SDK',
          items: [
            'sdk-reference/eventing-sdk/index',
            'sdk-reference/eventing-sdk/getting-started',
            'sdk-reference/eventing-sdk/api-reference',
            'sdk-reference/eventing-sdk/configuration',
            'sdk-reference/eventing-sdk/patterns',
            'sdk-reference/eventing-sdk/troubleshooting',
          ],
        },
        {
          type: 'category',
          label: 'Event Sourcing SDK',
          items: [
            'sdk-reference/eventsourcing-sdk/index',
            'sdk-reference/eventsourcing-sdk/getting-started',
            'sdk-reference/eventsourcing-sdk/api-reference',
            'sdk-reference/eventsourcing-sdk/configuration',
            'sdk-reference/eventsourcing-sdk/patterns',
            'sdk-reference/eventsourcing-sdk/troubleshooting',
          ],
        },
        {
          type: 'category',
          label: 'IAM Client SDK',
          items: [
            'sdk-reference/iam-client-sdk/index',
            'sdk-reference/iam-client-sdk/getting-started',
            'sdk-reference/iam-client-sdk/api-reference',
            'sdk-reference/iam-client-sdk/configuration',
            'sdk-reference/iam-client-sdk/patterns',
            'sdk-reference/iam-client-sdk/troubleshooting',
          ],
        },
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
        {
          type: 'category',
          label: 'Axon Framework',
          items: [
            'core-services/axon-framework/index',
            'core-services/axon-framework/core-concepts',
            'core-services/axon-framework/eaf-integration',
            'core-services/axon-framework/aggregates-commands',
            'core-services/axon-framework/event-handlers-projections',
            'core-services/axon-framework/eaf-patterns',
            'core-services/axon-framework/sagas',
            'core-services/axon-framework/testing',
            'core-services/axon-framework/performance-operations',
            'core-services/axon-framework/lab-01-order-management',
            'core-services/axon-framework/lab-02-user-management',
            'core-services/axon-framework/faq',
            'core-services/axon-framework/troubleshooting',
            'core-services/axon-framework/decision-trees',
          ],
        },
      ],
    },
    {
      type: 'category',
      label: 'Developer Tools',
      items: ['developer-tools/index', 'developer-tools/acci-eaf-cli'],
    },
    {
      type: 'category',
      label: 'UI Foundation Kit',
      items: ['ui-foundation-kit/index'],
    },
  ],
};

export default sidebars;
