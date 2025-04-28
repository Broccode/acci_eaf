/* eslint-disable */
export default {
  displayName: 'testing',
  preset: '../../jest.preset.js',
  testEnvironment: 'node',
  transform: {
    '^.+\\.[tj]s$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.spec.json' }],
  },
  moduleFileExtensions: ['ts', 'js', 'html'],
  coverageDirectory: '../../coverage/libs/testing',
  // Remove roots added as workaround
  // roots: [
  //   '<rootDir>/src', 
  //   '<rootDir>/../../apps' 
  // ],
  moduleNameMapper: {
    '^infrastructure': '<rootDir>/../../libs/infrastructure/src',
    '^tenancy': '<rootDir>/../../libs/tenancy/src',
    '^core': '<rootDir>/../../libs/core/src',
    '^shared': '<rootDir>/../../libs/shared/src',
    '^mikro-orm.config': '<rootDir>/../../mikro-orm.config',
  },
};
