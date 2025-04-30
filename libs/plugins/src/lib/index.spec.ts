import * as Plugins from './index';

describe('plugins index', () => {
  it('should export plugin constructors and functions', () => {
    expect(Plugins.ExamplePlugin).toBeDefined();
    expect(Plugins.AuthorizationPlugin).toBeDefined();
    expect(Plugins.demoPluginUsage).toBeDefined();
  });
}); 