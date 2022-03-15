package uk.co.gcwilliams.async.http;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The task
 *
 * @author : Gareth Williams
 */
public class Task<T> {

    private final BiConsumer<Consumer<T>, Consumer<Exception>> computation;

    /**
     * Constructor
     *
     * @param computation the computation
     */
    private Task(BiConsumer<Consumer<T>, Consumer<Exception>> computation) {
        this.computation = computation;
    }

    /**
     * Maps over the task
     *
     * @param mapper the mapping function
     * @return the mapped task
     */
    public <R> Task<R> map(Function<T, R> mapper) {
        BiConsumer<Consumer<T>, Consumer<Exception>> previous = this.computation;
        return new Task<>((resolve, reject) -> previous.accept(value -> resolve.accept(mapper.apply(value)), reject));
    }

    /**
     * Flat maps over the task
     *
     * @param mapper the mapping function
     * @return the mapped task
     */
    public <R> Task<R> flatMap(Function<T, Task<R>> mapper) {
        BiConsumer<Consumer<T>, Consumer<Exception>> previous = this.computation;
        return new Task<>((resolve, reject) -> previous.accept(value -> mapper.apply(value).fork(resolve, reject), reject));
    }

    /**
     * Forks the task
     *
     * @param resolve the resolve function
     * @param reject the reject function
     */
    public void fork(Consumer<T> resolve, Consumer<Exception> reject) {
        computation.accept(resolve, reject);
    }

    /**
     * Lifts the value into a task
     *
     * @param value the value
     * @return the task
     */
    public static <T> Task<T> of(T value) {
        return new Task<>((resolve, reject) -> resolve.accept(value));
    }

    /**
     * Lifts the exception into a task
     *
     * @param exception the exception
     * @return the task
     */
    public static <T> Task<T> of(Exception exception) {
        return new Task<>((resolve, reject) -> reject.accept(exception));
    }

    /**
     * Lifts the computation into a task
     *
     * @param computation the computation
     * @return the task
     */
    public static <T> Task<T> of(BiConsumer<Consumer<T>, Consumer<Exception>> computation) {
        return new Task<>(computation);
    }
}
