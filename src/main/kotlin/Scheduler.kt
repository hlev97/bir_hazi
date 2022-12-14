val periodicTasks = listOf(
    PeriodicTask(name = "PT Scheduling", C = 1, T = 10, schedulable = true, scheduler = true),
    PeriodicTask(name = "PT 1", C = 100, T = 400),
    PeriodicTask(name = "PT 2", C = 150, T = 800),
    PeriodicTask(name = "PT 3", C = 200, T = 1600),
    PeriodicTask(name = "PT 4", C = 500, T = 3200),
)


sealed class Scheduler(
    val priorityOrder: MutableList<PeriodicTask> = mutableListOf(),
    private val tasks: List<PeriodicTask> = periodicTasks,
    val createPriorityOrder: (tasks: List<PeriodicTask>, tick: Int) -> Unit,
    val isPeriod: (tasks: List<PeriodicTask>, currentTask: PeriodicTask?, tick: Int) -> Boolean,
    val isThereTaskWithHigherPriority: (tasks: List<PeriodicTask>, currentTask: PeriodicTask, tick: Int) -> Boolean,
) {

    private var processorFreeTime = 0
    private var responseTimes: HashMap<String,Float> = hashMapOf()
    private var processorFreeTimePeriods: MutableSet<ProcessorFreeTimePeriod> = mutableSetOf()
    private var currentProcessorFreeTimePeriod: ProcessorFreeTimePeriod? = null

    fun schedule() {
        val tick = 0
        println("Create priority order")
        createPriorityOrder(tasks, tick)
        val index = tasks.indexOf(priorityOrder.first())
        val currentTask = tasks[index]
        scheduleTask(currentTask, tick)
        println()
        responseTimes.forEach { (task, responseTime) ->
            println("$task's reponse time: $responseTime ms")
        }
        println("processor free time: ${"%.1f".format(processorFreeTimePeriods.sumOf { it.duration.toDouble() })} ms")
        println("processor free time periods (${processorFreeTimePeriods.size} db): ")
        println("start:end:duration")
        processorFreeTimePeriods.forEach {
            println("${it.start}:${it.end}:${"%.1f".format(it.duration)}")
        }
    }

    private tailrec fun scheduleTask(_currentTask: PeriodicTask?, _tick: Int) {
        var tick = _tick
        var currentTask = _currentTask
        tasks.forEach { task ->
            task.checkIfIsPeriodicStart(tick)
        }

        if (currentTask != null) {

            if (tick > 3200) {
                return
            }

            if (isThereTaskWithHigherPriority(tasks, currentTask, tick) || isPeriod(tasks, currentTask, tick)) {
                createPriorityOrder(tasks, tick)
                val index = tasks.indexOf(priorityOrder.first())
                currentTask = tasks[index]
                scheduleTask(currentTask, tick)
            } else if (currentTask.timeLeft > 0) {

                println("t[${tick.toFloat() / 10}]: ${currentTask.name}")

                tick += currentTask.step(
                    tick,
                    tasks,
                    addToResponseTimes = { key, value ->
                        responseTimes[key] = value
                    }
                )
                tasks.filter { it != currentTask && !it.isDone && it.s < tick }
                    .forEach { it.responseTime++ }

                if (currentTask.timeLeft == 0) {
                    if (currentTask.scheduler) {
                        tasks.forEach {
                            if (it.s < tick && !it.isDone) {
                                it.schedulable = true
                            }
                        }
                    } else {
                        tasks.forEach {
                            if (!it.scheduler) {
                                it.schedulable = false
                            }
                        }
                    }
                    priorityOrder.remove(currentTask)
                    createPriorityOrder(tasks,tick)
                    if (priorityOrder.isNotEmpty()) {
                        currentTask = priorityOrder.first()
                        scheduleTask(currentTask, tick)
                    } else {
                        println("t[${tick.toFloat() / 10}]: IDLE")
                        currentProcessorFreeTimePeriod = ProcessorFreeTimePeriod(start = tick.toFloat()/10)
                        processorFreeTime++
                        scheduleTask(null, tick)
                    }
                } else {
                    scheduleTask(currentTask, tick)
                }
            }
        } else {
            if (isPeriod(tasks, currentTask, tick)) {
                with(currentProcessorFreeTimePeriod) {
                    this?.end = tick.toFloat() / 10
                    this?.countDuration()
                }
                processorFreeTimePeriods.add(currentProcessorFreeTimePeriod!!)
                currentProcessorFreeTimePeriod = null
                createPriorityOrder(tasks, tick)
                val index = tasks.indexOf(priorityOrder.first())
                currentTask = tasks[index]
                scheduleTask(currentTask, tick)
            } else {
                println("t[${tick.toFloat() / 10}]: IDLE")
                processorFreeTime++
                tick++
                tasks.filter { !it.isDone && it.s < tick }
                    .forEach { it.responseTime++ }
                scheduleTask(null, tick)
            }
        }
    }
}

object RM : Scheduler(
    createPriorityOrder = { tasks, tick ->
        RM.priorityOrder.clear()
        val notDoneTasks = tasks.filter { task ->
            (!task.isDone && task.s <= tick && task.schedulable) || (task.s <= tick && tick % task.T == 0 && task.schedulable)
        }.sortedBy { task ->
            task.T
        }
        RM.priorityOrder.addAll(notDoneTasks)
    },
    isPeriod = { tasks, currentTask, tick ->
        if (currentTask != null) {
            tasks.any { tick % it.T == 0 && currentTask.T > it.T && !it.isDone }
        } else {
            tasks.any { tick % it.T == 0 }
        }
    },
    isThereTaskWithHigherPriority = { tasks, currentTask, tick ->
        tasks.filter { task -> task != currentTask }.any { task ->
            task.T < currentTask.T && !task.isDone && task.s < tick
        }
    }
)

object EDF : Scheduler(
    createPriorityOrder = { tasks, tick ->
        EDF.priorityOrder.clear()
        val notDoneTasks = tasks.filter { task ->
            (!task.isDone && task.s <= tick && task.schedulable) || (task.s <= tick && tick % task.T == 0 && task.schedulable)
        }.sortedBy { task ->
            task.T - tick
        }
        EDF.priorityOrder.addAll(notDoneTasks)
    },
    isPeriod = { tasks, currentTask, tick ->
        if (currentTask != null) {
            tasks.any { tick % it.T == 0 && currentTask.T - tick > it.T - tick && !it.isDone }
        } else {
            tasks.any { tick % it.T == 0 }
        }
    },
    isThereTaskWithHigherPriority = { tasks, currentTask, tick ->
        tasks.filter { task -> task != currentTask }.any { task ->
            task.T - tick < currentTask.T - tick && !task.isDone && task.s < tick
        }
    }
)