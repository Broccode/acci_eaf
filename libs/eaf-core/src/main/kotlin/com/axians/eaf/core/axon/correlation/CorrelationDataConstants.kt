package com.axians.eaf.core.axon.correlation

/**
 * Constants for correlation data keys and limits used throughout the correlation data providers.
 */
object CorrelationDataConstants {
    // Security Context Keys
    const val TENANT_ID = "tenant_id"
    const val USER_ID = "user_id"
    const val USER_EMAIL = "user_email"
    const val USER_ROLES = "user_roles"
    const val AUTH_TIME = "authentication_time"
    const val EXTRACTION_TIMESTAMP = "extraction_timestamp"

    // Request Context Keys
    const val CLIENT_IP = "client_ip"
    const val USER_AGENT = "user_agent"
    const val ACCEPT_LANGUAGE = "accept_language"
    const val REFERER = "referer"
    const val CONTENT_TYPE = "content_type"
    const val CORRELATION_ID = "correlation_id"
    const val REQUEST_CONTEXT_TYPE = "request_context_type"

    // Request Metadata Keys
    const val HTTP_METHOD = "http_method"
    const val REQUEST_URI = "request_uri"
    const val QUERY_PARAMS = "query_params"
    const val CONTENT_LENGTH = "content_length"
    const val CONTENT_ENCODING = "content_encoding"
    const val SESSION_ID = "session_id"
    const val REQUEST_TIMESTAMP = "request_timestamp"

    // Non-Web Context Keys
    const val PROCESS_TYPE = "process_type"
    const val PROCESS_ID = "process_id"
    const val THREAD_NAME = "thread_name"

    // Security Keys
    const val DATA_SANITIZED = "data_sanitized"
    const val COLLECTION_ENABLED = "collection_enabled"
    const val EXTRACTION_ERROR = "extraction_error"

    // Size Limits
    const val MAX_CORRELATION_ID_LENGTH = 128
    const val MAX_FIELD_SIZE_BYTES = 1024
    const val MAX_FIELD_VALUE_LENGTH = 512
    const val MAX_TOTAL_SIZE_BYTES = 10240
    const val MAX_QUERY_PARAMS_LENGTH = 1024

    // Context Types
    const val CONTEXT_TYPE_WEB = "web"
    const val CONTEXT_TYPE_NON_SERVLET = "non-servlet"
    const val CONTEXT_TYPE_SECURITY_ERROR = "security-error"
    const val CONTEXT_TYPE_INVALID_REQUEST = "invalid-request"

    // Process Types
    const val PROCESS_TYPE_SCHEDULED = "scheduled"
    const val PROCESS_TYPE_MESSAGE_DRIVEN = "message-driven"
    const val PROCESS_TYPE_BATCH = "batch"
    const val PROCESS_TYPE_ASYNC = "async"
    const val PROCESS_TYPE_TEST = "test"
    const val PROCESS_TYPE_SYSTEM = "system"

    // IP Address Constants
    const val UNKNOWN_IP = "unknown"
    const val LOCALHOST_IP = "0.0.0.0"

    // Header Names
    const val FORWARDED_FOR_HEADER = "X-Forwarded-For"
    const val REAL_IP_HEADER = "X-Real-IP"
    const val CONTENT_ENCODING_HEADER = "Content-Encoding"

    // Collections and patterns must remain as val since they're not primitive types
    val CORRELATION_HEADERS = listOf("X-Correlation-ID", "X-Request-ID", "X-Trace-ID")

    // Validation Patterns
    val CORRELATION_ID_PATTERN = Regex("^[a-zA-Z0-9\\-_.]+$")
    val EMAIL_PATTERN = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val PHONE_PATTERN = Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b")
    val CREDIT_CARD_PATTERN = Regex("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b")
    val SSN_PATTERN = Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b")

    // Sensitive Parameter Names
    val SENSITIVE_PARAMETERS =
        setOf(
            "password",
            "passwd",
            "pwd",
            "secret",
            "token",
            "key",
            "api_key",
            "apikey",
            "access_token",
            "refresh_token",
            "auth",
            "authorization",
            "session",
            "cookie",
            "ssn",
            "social_security",
            "credit_card",
            "creditcard",
            "cvv",
            "pin",
        )

    // Sensitive Field Patterns for filtering
    val SENSITIVE_FIELD_PATTERNS =
        setOf(
            "password",
            "passwd",
            "secret",
            "token",
            "key",
            "auth",
            "session",
            "cookie",
            "ssn",
            "credit",
            "cvv",
            "pin",
        )

    // Core Security Fields
    val CORE_SECURITY_FIELDS =
        setOf(
            TENANT_ID,
            USER_ID,
            USER_EMAIL,
            USER_ROLES,
            AUTH_TIME,
            EXTRACTION_TIMESTAMP,
            REQUEST_CONTEXT_TYPE,
            CORRELATION_ID,
            PROCESS_TYPE,
            THREAD_NAME,
            PROCESS_ID,
        )

    // Safe Request Fields
    val SAFE_REQUEST_FIELDS =
        setOf(
            HTTP_METHOD,
            REQUEST_URI,
            CONTENT_LENGTH,
            CONTENT_ENCODING,
            CLIENT_IP,
            REQUEST_TIMESTAMP,
        )

    // Sanitized Data Fields
    val SANITIZED_DATA_FIELDS =
        setOf(QUERY_PARAMS, USER_AGENT, ACCEPT_LANGUAGE, REFERER, CONTENT_TYPE)
}
