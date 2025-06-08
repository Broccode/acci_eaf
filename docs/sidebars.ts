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
    'intro',
    'getting-started',
    {
      type: 'category',
      label: 'Architectural Principles',
      items: [
        'architectural-principles/index',
        'architectural-principles/ddd',
        'architectural-principles/hexagonal',
        'architectural-principles/cqrs-es',
        'architectural-principles/tdd',
      ],
    },
    {
      type: 'category',
      label: 'Core Services',
      items: [
        'core-services/index',
        'core-services/nats-event-publishing',
        'core-services/nats-event-consumption',
        'core-services/nats-integration-testing',
      ],
    },
    {
      type: 'category',
      label: 'UI Foundation Kit',
      items: ['ui-foundation-kit/index'],
    },
    {
      type: 'category',
      label: 'Tutorial Examples',
      items: [
        'tutorial-basics/running-nats',
        'tutorial-basics/create-a-document',
        'tutorial-basics/create-a-page',
        'tutorial-basics/create-a-blog-post',
        'tutorial-basics/markdown-features',
        'tutorial-basics/deploy-your-site',
        'tutorial-basics/congratulations',
      ],
    },
    {
      type: 'category',
      label: 'Advanced Guides',
      items: [
        'tutorial-extras/manage-docs-versions',
        'tutorial-extras/translate-your-site',
      ],
    },
  ],
};

export default sidebars;
