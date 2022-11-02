abstract class Task(
    val name: String,
    protected val C: Int,
    s0: Int,
    var schedulable: Boolean
) {
    var timeLeft: Int = C
        protected set
    var isDone: Boolean = timeLeft == 0
        protected set
    var responseTime: Int = 0
    var s: Int = s0
        protected set


    abstract fun step(tick: Int, tasks: List<Task>, addToResponseTimes: (String,Float) -> Unit): Int
}

class PeriodicTask(
    name: String,
    C: Int,
    s0: Int = 0,
    schedulable: Boolean = s0 == 0,
    val scheduler: Boolean = false,
    val T: Int,
): Task(name,C,s0,schedulable) {

    override fun step(tick: Int, tasks: List<Task>, addToResponseTimes: (String,Float) -> Unit): Int {
        if (tick % T == 0) {
            s = T
            timeLeft = C
            isDone = false
        }

        if (timeLeft > 0) {
            timeLeft -= 1
            isDone = timeLeft == 0
            responseTime += 1
            if (isDone) {
                addToResponseTimes(name,responseTime.toFloat()/10)
                responseTime = 0
            }
        }

        return 1
    }

    fun checkIfIsPeriodicStart(tick: Int) {
        if (tick % T == 0) {
            timeLeft = C
            isDone = false
        }
    }
}

class AperiodicTask(
    name: String,
    C: Int,
    s0: Int,
    schedulable: Boolean = false,
): Task(name,C,s0,schedulable) {

    override fun step(tick: Int, tasks: List<Task>, addToResponseTimes: (String,Float) -> Unit): Int {
        if (timeLeft > 0) {
            timeLeft -= 1
            isDone = timeLeft == 0
            responseTime += 1
            if (isDone) {
                addToResponseTimes(name,responseTime.toFloat()/10)
                responseTime = 0
            }
        }
        return 1
    }
}