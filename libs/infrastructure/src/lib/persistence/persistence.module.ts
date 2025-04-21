import { Module } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
// Adjust the path to the root config file
import mikroOrmConfig from '../../../../../mikro-orm.config';

@Module({
  imports: [
    MikroOrmModule.forRoot({
      ...mikroOrmConfig,
      // Explicitly set registerRequestContext here as it's crucial for tenancy
      registerRequestContext: true,
      // autoLoadEntities: true, // Convenient, but less explicit. Consider importing entities via MikroOrmModule.forFeature in specific feature modules.
      // Alternatively, ensure entities are correctly listed in mikro-orm.config.ts and rely on that.
      // Let's rely on the config paths for now.
    }),
  ],
  exports: [
    MikroOrmModule, // Export the module to allow injecting EntityManager, Repositories etc.
  ],
})
export class PersistenceModule {} 