package net.gotev.uploadservice.data

data class RetryPolicyConfig(
    /**
     * Sets the time to wait in seconds before the next attempt when an upload fails
     * for the first time. From the second time onwards, this value will be multiplied by
     * [multiplier] to get the time to wait before the next attempt.
     */
    val initialWaitTimeSeconds: Int,

    /**
     * Sets the maximum time to wait in seconds between two upload attempts.
     * This is useful because every time an upload fails, the wait time gets multiplied by
     * [multiplier] and it's not convenient that the value grows
     * indefinitely.
     */
    val maxWaitTimeSeconds: Int,

    /**
     * Sets the backoff timer multiplier. For example, if is set to 2, every time that an upload
     * fails, the time to wait between retries will be multiplied by 2.
     * The first time the wait time is [initialWaitTimeSeconds],
     * the second time it will be [initialWaitTimeSeconds] * [multiplier] and so on.
     */
    val multiplier: Int,

    /**
     * Sets the default number of retries for each request.
     */
    val defaultMaxRetries: Int
) {
    override fun toString(): String {
        return """{"initialWaitTimeSeconds": $initialWaitTimeSeconds, "maxWaitTimeSeconds": $maxWaitTimeSeconds, "multiplier": $multiplier, "defaultMaxRetries": $defaultMaxRetries}"""
    }
}
