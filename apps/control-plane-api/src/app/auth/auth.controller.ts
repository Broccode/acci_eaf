import { Body, Controller, Post, UseGuards, Request, UnauthorizedException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { Public } from './decorators/is-public.decorator';
import { IsString } from 'class-validator';
import { Request as ExpressRequest } from 'express';
import { AdminUser } from './auth.service';

// Erweitere den Express.Request-Typ für den Auth-Kontext
interface RequestWithUser extends ExpressRequest {
  user: AdminUser;
}

export class LoginDto {
  username!: string;
  password!: string;
}

export class CreateAdminDto {
  @IsString()
  username!: string;
  @IsString()
  password!: string;
  @IsString()
  adminToken!: string; // Used for bootstrapping only
}

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @UseGuards(AuthGuard('local'))
  @Post('login')
  @Public()
  async login(@Request() req: RequestWithUser) {
    return this.authService.login(req.user);
  }

  @Post('create-admin')
  @Public()
  async createAdmin(@Body() createAdminDto: CreateAdminDto) {
    // For bootstrapping only - this would be secured differently in production
    // e.g., with a special bootstrap token that is only valid once
    const bootstrapToken = process.env['BOOTSTRAP_ADMIN_TOKEN'];
    
    // Debug output
    console.log('Creating admin, received token:', createAdminDto.adminToken);
    console.log('Bootstrap token from env:', bootstrapToken);
    
    if (!bootstrapToken || createAdminDto.adminToken !== bootstrapToken) {
      console.log('Token validation failed');
      throw new UnauthorizedException('Invalid admin creation token');
    }
    
    const { username, password } = createAdminDto;
    const admin = await this.authService.createAdmin(username, password);
    
    return { 
      message: 'Admin created successfully',
      username: admin.username,
      id: admin.id
    };
  }
} 