import { CallHandler, ExecutionContext, Injectable, NestInterceptor } from '@nestjs/common';
import { Observable, tap } from 'rxjs';
import { AuditLogService } from './audit-log.service';

@Injectable()
export class AuditLogInterceptor implements NestInterceptor {
  constructor(private readonly audit: AuditLogService) {}

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const httpCtx = context.switchToHttp();
    const request = httpCtx.getRequest();
    const { method, originalUrl, user, body, params } = request;

    return next.handle().pipe(
      tap(async (response) => {
        // Map HTTP method to action keyword
        const actionMap: Record<string, string> = {
          POST: 'create',
          PATCH: 'update',
          DELETE: 'delete',
        };

        const action = actionMap[method] ?? 'read';

        await this.audit.write({
          actorId: user?.id ?? 'anonymous',
          action: `${originalUrl}:${action}`,
          resource: 'Tenant',
          resourceId: params?.id ?? response?.id ?? 'n/a',
          payload: { body, response },
        });
      }),
    );
  }
} 