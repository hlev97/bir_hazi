val myTasks = listOf(
    PeriodicTask(name = "PT Scheduling", C = 1, T = 10, schedulable = true),
    PeriodicTask(name = "PT 1", C = 100, T = 400),
    PeriodicTask(name = "PT 2", C = 150, T = 800),
    PeriodicTask(name = "PT 3", C = 200, T = 1600),
    PeriodicTask(name = "PT 4", C = 500, T = 3200),
    AperiodicTask(name = "AT 1", C = 150, s0 = 250),
    AperiodicTask(name = "AT 2", C = 150, s0 = 550),
    AperiodicTask(name = "AT 3", C = 150, s0 = 1200)
)

sealed class Server(
    val priorityOrder: MutableList<Task> =  mutableListOf(),
    private val tasks: List<Task> = myTasks,
    val createPriorityOrder: (tasks: List<Task>, tick: Int) -> Unit,
    val isPeriod: (tasks: List<Task>, currentTask: Task?, tick: Int) -> Boolean,
    val isThereTaskWithHigherPriority: (tasks: List<Task>, currentTask: Task, tick: Int) -> Boolean,
    val T_S: Int,
    val C_S: Int,
) {

    fun schedule() {
        val tick = 0
        println("Create priority order")
        createPriorityOrder(tasks,tick)
        val index = tasks.indexOf(priorityOrder.first())
        val currentTask = tasks[index]
        println("current task is ${currentTask.name}")
        scheduleTask(currentTask,tick)
    }

    private tailrec fun scheduleTask(_currentTask: Task?, _tick: Int) {
        var tick = _tick
        var currentTask = _currentTask
        tasks.forEach { task ->
            if (task is PeriodicTask) {
                task.checkIfIsPeriodicStart(tick)
            }
        }

        if (currentTask != null) {

            if (tick > 3200) { return }

            if (isThereTaskWithHigherPriority(tasks,currentTask,tick) || isPeriod(tasks,currentTask,tick)) {
                createPriorityOrder(tasks,tick)
                val index = tasks.indexOf(priorityOrder.first())
                currentTask = tasks[index]
                scheduleTask(currentTask,tick)
            } else if (currentTask.timeLeft > 0) {

                println("t[${tick.toFloat()/10}]: ${currentTask.name}")

                tick += currentTask.step(tick,tasks)
                tasks.filter { it != currentTask && !it.isDone && it.s < tick }
                    .forEach { it.responseTime++ }

                if (currentTask.timeLeft == 0) {
                    priorityOrder.remove(currentTask)
                    if (priorityOrder.isNotEmpty()) {
                        currentTask = priorityOrder.first()
                        scheduleTask(currentTask,tick)
                    } else {
                        println("t[${tick.toFloat()/10}]: IDLE")
                        scheduleTask(null,tick)
                    }
                } else {
                    scheduleTask(currentTask,tick)
                }
            }
        } else {
            if (isPeriod(tasks,currentTask,tick)) {
                createPriorityOrder(tasks,tick)
                val index = tasks.indexOf(priorityOrder.first())
                currentTask = tasks[index]
                scheduleTask(currentTask,tick)
            } else {
                println("t[${tick.toFloat()/10}]: IDLE")
                tick++
                scheduleTask(null,tick)
            }

        }
    }
}

object DeferableServer : Server(
    createPriorityOrder = { tasks, tick ->
        DeferableServer.priorityOrder.clear()
        val notDoneTasks = tasks.filter { task ->
            if (task is PeriodicTask) {
                (!task.isDone && task.s <= tick) || (task.s <= tick && tick % task.T == 0)
            } else {
                (!task.isDone && task.s <= tick)
            }
        }.sortedBy { task ->
            if (task is PeriodicTask) {
                task.T
            } else {
                DeferableServer.T_S
            }
        }
        DeferableServer.priorityOrder.addAll(notDoneTasks)
    },
    isPeriod = { tasks, currentTask, tick ->
        if (currentTask != null) {
            if (currentTask is PeriodicTask) {
                tasks.filterIsInstance<PeriodicTask>()
                    .any { tick % it.T == 0 && currentTask.T > it.T && !it.isDone }
            } else {
                tasks.filterIsInstance<PeriodicTask>()
                    .any { tick % it.T == 0 && DeferableServer.T_S > it.T && !it.isDone }
            }

        } else {
            tasks.filterIsInstance<PeriodicTask>()
                .any { tick % it.T == 0 }
        }

    },
    isThereTaskWithHigherPriority = { tasks, currentTask, tick ->
        tasks.filter { task -> task != currentTask }.any { task ->
            if (currentTask is PeriodicTask) {
                if (task is PeriodicTask) {
                    task.T < currentTask.T && !task.isDone && task.s < tick
                } else {
                    DeferableServer.T_S < currentTask.T && !task.isDone && task.s < tick
                }
            } else {
                if (task is PeriodicTask) {
                    task.T < DeferableServer.T_S && !task.isDone && task.s < tick
                } else {
                    false
                }
            }
        }
    },
    T_S = 580,
    C_S = 58
)

