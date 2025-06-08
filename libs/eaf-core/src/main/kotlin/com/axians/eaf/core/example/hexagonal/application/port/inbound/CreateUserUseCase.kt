package com.axians.eaf.core.example.hexagonal.application.port.inbound

import com.axians.eaf.core.hexagonal.port.InboundPort

/**
 * Example inbound port for creating users.
 *
 * This demonstrates how to define use cases as inbound ports in the EAF.
 * The application core defines this interface, and the infrastructure layer
 * (e.g., REST controllers, message listeners) will call this port.
 */
interface CreateUserUseCase : InboundPort<CreateUserCommand, CreateUserResult>
