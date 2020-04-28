package stroom.task.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.task.shared.TaskId;

public class TaskLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskLog.class);

    public static void log(String section, TaskId taskId, String taskName) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n==================== \n\t");
        sb.append(section);
        sb.append("\n\t");
        sb.append(taskId.path());
        if (taskName != null) {
            sb.append(" (");
            sb.append(taskName);
            sb.append(")");
        }

//        LOGGER.info(ConsoleColour.yellow(sb.toString()) + ConsoleColour.blue(stack()) + ConsoleColour.yellow("\n===================="));
    }

    public static String stack() {
        return stack(Thread.currentThread().getStackTrace());
    }

    public static String stack(final StackTraceElement[] stackTraceElements) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 3; i < stackTraceElements.length; i++) {
            final StackTraceElement stackTraceElement = stackTraceElements[i];
            if (stackTraceElement.getClassName().contains("stroom")) {
                sb.append(stackTraceElement.getClassName());
                sb.append(".");
                sb.append(stackTraceElement.getMethodName());
                sb.append(":");
                sb.append(stackTraceElement.getLineNumber());
                sb.append(" < ");
            }
        }
        if (sb.length() > 3) {
            sb.setLength(sb.length() - 3);
        }

        sb.insert(0, "\n\t");
        return sb.toString();
    }
}
