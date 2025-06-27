package com.axians.eaf.iam.infrastructure.adapter.outbound.persistence.entity

import com.axians.eaf.iam.domain.model.User
import com.axians.eaf.iam.domain.model.UserRole
import com.axians.eaf.iam.domain.model.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
data class UserEntity(
    @Id @Column(name = "user_id", nullable = false, unique = true) val userId: String,
    @Column(name = "tenant_id", nullable = false) val tenantId: String,
    @Column(name = "email", nullable = false, length = 320, unique = true) val email: String,
    @Column(name = "username", nullable = true, length = 255) val username: String? = null,
    @Column(name = "password_hash", nullable = true, length = 255) val passwordHash: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    val role: UserRole,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    val status: UserStatus,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant,
    @Column(name = "last_login", nullable = true) val lastLogin: Instant? = null,
)

fun UserEntity.toDomain(): User =
    User(
        userId = this.userId,
        tenantId = this.tenantId,
        email = this.email,
        username = this.username,
        passwordHash = this.passwordHash,
        role = this.role,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastLogin = this.lastLogin,
    )

fun User.toEntity(): UserEntity =
    UserEntity(
        userId = this.userId,
        tenantId = this.tenantId,
        email = this.email,
        username = this.username,
        passwordHash = this.passwordHash,
        role = this.role,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastLogin = this.lastLogin,
    )
