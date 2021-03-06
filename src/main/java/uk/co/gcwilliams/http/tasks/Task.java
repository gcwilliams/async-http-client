package uk.co.gcwilliams.http.tasks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The task class
 *
 * Created by GWilliams on 15/08/2015.
 */
public class Task<V, E> implements Functor<V> {

    private final BiConsumer<Consumer<V>, Consumer<E>> computation;

    /**
     * Default constructor
     *
     * @param computation the computation that will be performed asynchronous
     */
    public Task(BiConsumer<Consumer<V>, Consumer<E>> computation) {
        this.computation = computation;
    }

    /**
     * Maps the function over the result of the task
     *
     * @param computation the computation
     * @return the task
     */
    @Override
    @SuppressWarnings("unchecked")
    public <NV, F extends Functor<NV>> F map(Function<V, NV> computation) {
        BiConsumer<Consumer<V>, Consumer<E>> previous = this.computation;
        return (F) new Task<NV, E>((resolve, reject) ->
            previous.accept(v -> resolve.accept(computation.apply(v)), reject::accept)
        );
    }

    /**
     * Creates a new task, which is the result of chaining this and another
     * task together
     *
     * @param fn the function to chain
     * @return the task
     */
    public <NV> Task<NV, E> chain(Function<V, Task<NV, E>> fn) {
        BiConsumer<Consumer<V>, Consumer<E>> previous = this.computation;
        return new Task<>((resolve, reject) ->
            previous.accept(v -> fn.apply(v).fork(resolve, reject), reject::accept)
        );
    }

    /**
     * Attempts to the resolve the underlying value of the task
     *
     * @param resolve the resolve function
     * @param reject the reject function
     */
    public void fork(Consumer<V> resolve, Consumer<E> reject) {
        computation.accept(resolve, reject);
    }
}
