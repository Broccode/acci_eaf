import { Injectable } from '@nestjs/common';
import { EntityManager } from '@mikro-orm/postgresql';
import { AuditLog } from 'infrastructure';

@Injectable()
export class AuditLogService {
  constructor(private readonly em: EntityManager) {}

  async write(entry: Omit<AuditLog, 'id' | 'timestamp'>) {
    const log = this.em.create(AuditLog, { ...entry, timestamp: new Date() });
    await this.em.persistAndFlush(log);
  }
}
