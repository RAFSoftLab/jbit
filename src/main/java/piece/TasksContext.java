package piece;

import tasks.Task;
import tasks.TaskType;

import java.util.Queue;

public class TasksContext {

    private final Queue<Task> readTasks;
    private final Queue<Task> writeTasks;

    public TasksContext(Queue<Task> readTasks, Queue<Task> writeTasks) {
        this.readTasks = readTasks;
        this.writeTasks = writeTasks;
    }

    public synchronized void addTask(Task task) {
        TaskType taskType = task.getTaskType();
        if (taskType == TaskType.WRITE) {
            writeTasks.add(task);
        } else {
            readTasks.add(task);
        }
    }

    public Task getTask(TaskType taskType) {
        if (taskType == TaskType.WRITE) {
            return writeTasks.poll();
        } else {
            return readTasks.poll();
        }
    }

    public int sizeOfWriteTasks(){
        return writeTasks.size();
    }

}
