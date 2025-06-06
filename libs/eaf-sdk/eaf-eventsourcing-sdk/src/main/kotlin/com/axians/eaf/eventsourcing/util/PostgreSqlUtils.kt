package com.axians.eaf.eventsourcing.util

import java.sql.PreparedStatement
import java.sql.Types

/**
 * Utility functions for PostgreSQL-specific operations.
 */
object PostgreSqlUtils {
    /**
     * Sets a JSONB parameter in a prepared statement.
     * This avoids direct dependency on PostgreSQL driver classes in the main code.
     */
    fun setJsonbParameter(
        stmt: PreparedStatement,
        parameterIndex: Int,
        json: String?,
    ) {
        if (json != null) {
            stmt.setObject(parameterIndex, json, Types.OTHER)
        } else {
            stmt.setNull(parameterIndex, Types.OTHER)
        }
    }

    /**
     * Creates a JSONB object for use with Spring's NamedParameterJdbcTemplate.
     * This method creates the appropriate object type for PostgreSQL JSONB.
     */
    fun createJsonbObject(json: String): Any {
        // Use a simple approach that works with Spring's parameter mapping
        return mapOf("type" to "jsonb", "value" to json)
    }
}
