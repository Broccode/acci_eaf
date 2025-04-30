import { ExampleProject } from './example-project.entity';
import { v4 as uuidV4, validate as uuidValidate } from 'uuid';

describe('ExampleProject', () => {
  it('should generate a valid uuid for id and set tenantId and name', () => {
    const tenantId = uuidV4();
    const project = new ExampleProject(tenantId, 'My Project');

    expect(uuidValidate(project.id)).toBe(true);
    expect(project.tenantId).toBe(tenantId);
    expect(project.name).toBe('My Project');
  });

  it('should have undefined description by default and allow setting it', () => {
    const tenantId = uuidV4();
    const project = new ExampleProject(tenantId, 'Test');

    expect(project.description).toBeUndefined();
    project.description = 'A description';
    expect(project.description).toBe('A description');
  });
}); 