package com.axians.eaf.eventsourcing.annotation

/**
 * Marker annotation for aggregate identifier fields.
 *
 * This annotation is used to mark the field or property that serves as the unique identifier
 * for an aggregate instance. This helps with automatic discovery and validation of aggregate
 * identity within the EAF framework.
 *
 * The annotated field/property should:
 * - Be immutable once set
 * - Have a reliable equals() and hashCode() implementation
 * - Be serializable (for snapshots and event metadata)
 * - Be unique within the aggregate type and tenant scope
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AggregateIdentifier
