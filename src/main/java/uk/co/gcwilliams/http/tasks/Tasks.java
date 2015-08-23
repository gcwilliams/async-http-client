package uk.co.gcwilliams.http.tasks;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * The tasks utility class
 *
 * Created by GWilliams on 15/08/2015.
 */
public class Tasks {

    /**
     * Creates a task of the specified value
     *
     * @param value the value
     * @return the new task
     */
    public static <V, E> Task<V, E> of(V value) {
        return new Task<>((resolve, reject) -> resolve.accept(value));
    }

    /**
     * Creates a rejected task
     *
     * @param error the error
     * @return the new task
     */
    public static <V, E> Task<V, E> rejected(E error) {
        return new Task<>((resolve, reject) -> reject.accept(error));
    }

    /**
     * Creates a task that resolves when all the specified tasks
     * resolve. If any task fails, the reject function will be called
     *
     * @param tasks the task the tasks to execute concurrently
     * @return the new task
     */
    @SafeVarargs
    public static <V, E> Task<List<V>, List<E>> all(Task<V, E>... tasks) {
        return all(asList(tasks));
    }

    /**
     * Creates a task that resolves when all the specified tasks
     * resolve. If any task fails, the reject function will be called
     *
     * @param tasks the task the tasks to execute concurrently
     * @return the new task
     */
    public static <V, E> Task<List<V>, List<E>> all(List<Task<V, E>> tasks) {
        return new Task<>((resolve, reject) -> {
            AtomicInteger count = new AtomicInteger(tasks.size());
            BiConsumer<Queue<V>, Queue<E>> resolveAndReject = (successful, failures) -> {
                if (count.decrementAndGet() == 0) {
                    reject.accept(failures.stream().collect(toList()));
                    resolve.accept(successful.stream().collect(toList()));
                }
            };
            Queue<V> results = new ConcurrentLinkedQueue<>();
            Queue<E> errors = new ConcurrentLinkedQueue<>();
            Consumer<Task<V, E>> onComplete = t -> t.fork(
            v -> {
                results.add(v);
                resolveAndReject.accept(results, errors);
            },
            e -> {
                errors.add(e);
                resolveAndReject.accept(results, errors);
            });
            tasks.stream().forEach(onComplete);
        });
    }

    /**
     * Creates a task that resolves when the first specified task
     * resolves. If any task fails, the reject function will be called
     *
     * @param tasks the task the tasks to execute concurrently
     * @return the new task
     */
    @SafeVarargs
    public static <V, E> Task<V, E> any(Task<V, E>... tasks) {
        return any(asList(tasks));
    }

    /**
     * Creates a task that resolves when the first specified task
     * resolves. If any task fails, the reject function will be called
     *
     * @param tasks the task the tasks to execute concurrently
     * @return the new task
     */
    public static <V, E> Task<V, E> any(List<Task<V, E>> tasks) {
        return new Task<>((resolve, reject) -> {
            AtomicBoolean resolved = new AtomicBoolean();
            AtomicInteger count = new AtomicInteger(tasks.size());
            Consumer<Task<V, E>> onComplete = t -> t.fork(
            v -> {
                if (!resolved.getAndSet(true)) {
                    resolve.accept(v);
                }
            },
            e -> {
                if (count.decrementAndGet() == 0 && !resolved.get()) {
                    reject.accept(e);
                }
            });
            tasks.stream().forEach(onComplete);
        });
    }

    /**
     * Gets the value from a task, waiting the specified amount of time. If the
     * task is rejected, an error is throw. If the task times out, an error is
     * thrown.
     *
     * @param task the task to wait on
     * @param timeout the timeout
     * @return the underlying value of the task
     */
    public static <V> V get(Task<V, Exception> task, long timeout) throws Exception {

        final Mutable<Boolean> pending = new Mutable<>(true);
        final Mutable<V> result = new Mutable<>();
        final Mutable<Exception> error = new Mutable<>();

        Consumer<V> resolve = v -> {
            if (pending.value().get()) {
                synchronized (pending) {
                    pending.value(false);
                    result.value(v);
                    pending.notify();
                }
            }
        };

        Consumer<Exception> reject = e -> {
            if (pending.value().get()) {
                synchronized (pending) {
                    pending.value(false);
                    error.value(e);
                    pending.notify();
                }
            }
        };

        synchronized (pending) {
            task.fork(resolve, reject);
            try {
                pending.wait(timeout);
                if (pending.value().get()) {
                    throw new TaskException("The task timed out");
                }
                if (error.value().isPresent()) {
                    throw error.value().get();
                }
                return result.value().get();
            } catch (InterruptedException e) {
                throw new TaskException("An error occurred when getting the value of a task");
            }
        }
    }

    /**
     * A simple mutable class
     *
     */
    private static final class Mutable<V> {

        private volatile Optional<V> value;

        /**
         * Default constructor
         *
         */
        private Mutable() {
            this(null);
        }

        /**
         * Constructor taking default value
         *
         * @param value the default value
         */
        private Mutable(V value) {
            this.value = ofNullable(value);
        }

        /**
         * Gets the value
         *
         * @return the value
         */
        public Optional<V> value() {
            return value;
        }

        /**
         * Sets the value
         *
         * @param value the value
         */
        public void value(V value) {
            this.value = ofNullable(value);
        }
    }
}
