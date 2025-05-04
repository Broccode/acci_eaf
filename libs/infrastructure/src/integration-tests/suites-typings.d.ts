declare module '@suites/unit' {
  export class TestBed {
    static create<T>(token: any): any;
    static solitary<T>(token: any): any;
    static sociable<T>(token: any): any;
    compile(): Promise<{ unit: T; unitRef: any }>;
    expose(token: any): any;
  }
  
  export type Mocked<T> = {
    [P in keyof T]: T[P] extends (...args: any[]) => any
      ? jest.Mock<ReturnType<T[P]>, Parameters<T[P]>>
      : T[P];
  };
}

declare module '@suites/di.nestjs';
declare module '@testcontainers/postgresql';
declare module 'testcontainers'; 