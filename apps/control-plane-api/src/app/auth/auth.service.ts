import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as bcrypt from 'bcrypt';

export interface AdminUser {
  id: string;
  username: string;
  password: string;
}

export interface JwtPayload {
  sub: string;
  username: string;
  isAdmin: boolean;
}

// In a real implementation, this would be stored in a database
// For simplicity in this prototype, we're using an in-memory store
@Injectable()
export class AuthService {
  private readonly adminUsers: Map<string, AdminUser> = new Map();

  constructor(
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {
    // Initialize with a default admin user if specified in environment
    const defaultAdminUsername = this.configService.get<string>('ADMIN_USERNAME');
    const defaultAdminPassword = this.configService.get<string>('ADMIN_PASSWORD');
    
    if (defaultAdminUsername && defaultAdminPassword) {
      this.createAdmin(defaultAdminUsername, defaultAdminPassword);
    }
  }

  async validateAdmin(username: string, password: string): Promise<AdminUser | null> {
    const adminUsers = Array.from(this.adminUsers.values());
    const user = adminUsers.find(u => u.username === username);
    
    if (!user) {
      return null;
    }
    
    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return null;
    }
    
    return user;
  }

  async login(user: AdminUser): Promise<{ access_token: string }> {
    const payload: JwtPayload = {
      sub: user.id,
      username: user.username,
      isAdmin: true,
    };
    
    return {
      access_token: this.jwtService.sign(payload),
    };
  }

  async createAdmin(username: string, password: string): Promise<AdminUser> {
    // Check if admin already exists
    const existingAdmin = Array.from(this.adminUsers.values())
      .find(u => u.username === username);
    
    if (existingAdmin) {
      throw new UnauthorizedException('Admin user already exists');
    }
    
    // Hash the password
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // Create new admin user
    const id = `admin-${Date.now()}`;
    const newAdmin: AdminUser = {
      id,
      username,
      password: hashedPassword,
    };
    
    this.adminUsers.set(id, newAdmin);
    
    return newAdmin;
  }

  async findAdminById(id: string): Promise<AdminUser | undefined> {
    return this.adminUsers.get(id);
  }

  async findAdminByUsername(username: string): Promise<AdminUser | undefined> {
    return Array.from(this.adminUsers.values())
      .find(u => u.username === username);
  }
} 