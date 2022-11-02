data class ProcessorFreeTimePeriod(
    var start: Float = 0f,
    var end: Float = 0f,
    var duration: Float = 0f
) {
    fun countDuration() {
        duration = end.minus(start)
    }
}