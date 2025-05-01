import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { ConfigService } from '@nestjs/config';
import { AuthService, JwtPayload } from '../auth.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    private readonly authService: AuthService,
    private readonly configService: ConfigService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.get<string>('JWT_SECRET', 'devSecretDoNotUseInProduction'),
    });
  }

  async validate(payload: JwtPayload): Promise<any> {
    const user = await this.authService.findAdminById(payload.sub);
    
    if (!user || !payload.isAdmin) {
      throw new UnauthorizedException('Invalid token or insufficient permissions');
    }
    
    return { 
      id: payload.sub, 
      username: payload.username,
      isAdmin: payload.isAdmin,
    };
  }
} 