import { createElement } from 'react';
import { createRoot } from 'react-dom/client';
import { router } from './routes.js';

const container = document.getElementById('outlet');
if (container) {
  const root = createRoot(container);
  root.render(createElement(router.component));
} else {
  console.error('Could not find outlet element');
}
