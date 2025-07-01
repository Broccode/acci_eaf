package com.axians.eaf.core.axon.correlation

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Handles extraction of non-web context information for correlation data. Focused on system
 * processes, scheduled tasks, batch jobs, etc.
 */
@Component
class NonWebContextExtractor {
    companion object {
        private val logger = LoggerFactory.getLogger(NonWebContextExtractor::class.java)
    }

    /**
     * Extracts non-web context information and adds it to correlation data. Determines process type
     * based on thread name and execution context.
     */
    fun extractNonWebContext(correlationData: MutableMap<String, Any>) {
        val processType = determineProcessType()
        correlationData[CorrelationDataConstants.PROCESS_TYPE] = processType
        correlationData[CorrelationDataConstants.REQUEST_CONTEXT_TYPE] = processType

        addThreadInformation(correlationData)
        addProcessSpecificContext(processType, correlationData)
        addCorrelationId(correlationData)

        logger.debug("Extracted non-web context for process type: {}", processType)
    }

    private fun determineProcessType(): String {
        val threadName = Thread.currentThread().name.lowercase()

        return when {
            isScheduledProcess(threadName) -> CorrelationDataConstants.PROCESS_TYPE_SCHEDULED
            isMessageDrivenProcess(threadName) ->
                CorrelationDataConstants.PROCESS_TYPE_MESSAGE_DRIVEN
            isBatchProcess(threadName) -> CorrelationDataConstants.PROCESS_TYPE_BATCH
            isAsyncProcess(threadName) -> CorrelationDataConstants.PROCESS_TYPE_ASYNC
            isTestProcess(threadName) -> CorrelationDataConstants.PROCESS_TYPE_TEST
            else -> CorrelationDataConstants.PROCESS_TYPE_SYSTEM
        }
    }

    private fun isScheduledProcess(threadName: String): Boolean =
        threadName.contains("scheduler") ||
            threadName.contains("quartz") ||
            threadName.contains("cron") ||
            threadName.contains("timer")

    private fun isMessageDrivenProcess(threadName: String): Boolean =
        threadName.contains("jms") ||
            threadName.contains("amqp") ||
            threadName.contains("message") ||
            threadName.contains("nats")

    private fun isBatchProcess(threadName: String): Boolean =
        threadName.contains("batch") ||
            threadName.contains("job") ||
            threadName.contains("worker")

    private fun isAsyncProcess(threadName: String): Boolean =
        threadName.contains("async") || threadName.contains("executor")

    private fun isTestProcess(threadName: String): Boolean = threadName.contains("test")

    private fun addThreadInformation(correlationData: MutableMap<String, Any>) {
        val currentThread = Thread.currentThread()
        correlationData[CorrelationDataConstants.THREAD_NAME] = currentThread.name
        correlationData[CorrelationDataConstants.PROCESS_ID] = currentThread.threadId().toString()
    }

    private fun addProcessSpecificContext(
        processType: String,
        correlationData: MutableMap<String, Any>,
    ) {
        val executionType =
            when (processType) {
                CorrelationDataConstants.PROCESS_TYPE_SCHEDULED -> "scheduled_task"
                CorrelationDataConstants.PROCESS_TYPE_MESSAGE_DRIVEN -> "message_consumer"
                CorrelationDataConstants.PROCESS_TYPE_BATCH -> "batch_process"
                CorrelationDataConstants.PROCESS_TYPE_ASYNC -> "async_operation"
                CorrelationDataConstants.PROCESS_TYPE_TEST -> "test_execution"
                else -> "system_operation"
            }
        correlationData["execution_type"] = executionType
    }

    private fun addCorrelationId(correlationData: MutableMap<String, Any>) {
        correlationData[CorrelationDataConstants.CORRELATION_ID] = UUID.randomUUID().toString()
    }
}
