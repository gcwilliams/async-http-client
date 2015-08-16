package uk.co.gcwilliams.http.tasks;

/**
 * A task exception
 *
 * Created by GWilliams on 16/08/2015.
 */
public class TaskException extends RuntimeException {

    /**
     * Default constructor
     *
     */
    public TaskException() {
    }

    /**
     * Constructor taking a message
     *
     * @param message the message
     */
    public TaskException(String message) {
        super(message);
    }

    /**
     * Constructor taking a message and a throwable
     *
     * @param message the message
     * @param cause the throwable
     */
    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }
}
