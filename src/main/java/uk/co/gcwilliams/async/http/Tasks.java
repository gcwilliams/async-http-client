package uk.co.gcwilliams.async.http;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.concat;

/**
 * The tasks utility
 *
 * @author : Gareth Williams
 */
public class Tasks {

    private Tasks() { // static
    }

    /**
     * Gets the value from the task
     *
     * @param task the task
     * @return the value
     */
    public static <T> T get(Task<T> task) throws Exception {

        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable complete = () -> {
            synchronized (completed) {
                completed.set(true);
                completed.notify();
            }
        };

        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();

        task.fork(
            v -> { value.set(v); complete.run(); },
            ex -> { exception.set(ex); complete.run(); });

        synchronized (completed) {
            while (!completed.get()) {
                try {
                    completed.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        if (exception.get() != null) {
            throw exception.get();
        }

        return value.get();
    }

    /**
     * Traverses the tasks
     *
     * @param tasks the tasks
     * @return the task
     */
    public static <T> Task<List<T>> traverse(List<Task<T>> tasks) {
        return tasks.stream().reduce(
            Task.of(List.of()),
            (ta, tb) -> ta.flatMap(a -> tb.map(b -> concat(a.stream(), Stream.of(b)).collect(toList()))),
            (ta, tb) -> ta.flatMap(a -> tb.map(b -> concat(a.stream(), b.stream()).collect(toList()))));
    }

    /**
     * Traverses the tasks in parallel. The task will reject with the first exception that's generated. The result
     * from any successful task will be discarded.
     *
     * @param tasks the tasks
     * @return the task
     */
    public static <T> Task<List<T>> traverseP(List<Task<T>> tasks) {
        if (tasks.isEmpty()) {
            return Task.of(List.of());
        }
        return Task.of((resolve, reject) -> {
            AtomicInteger counter = new AtomicInteger(tasks.size());
            AtomicReferenceArray<T> collector = new AtomicReferenceArray<>(tasks.size());
            BiConsumer<Integer, T> resolveHandler = (idx, value) -> {
                collector.set(idx, value);
                if (counter.decrementAndGet() == 0) {
                    resolve.accept(range(0, tasks.size()).mapToObj(collector::get).collect(toList()));
                }
            };
            AtomicBoolean failed = new AtomicBoolean(false);
            Consumer<Exception> rejectHandler = exception -> {
                if (!failed.getAndSet(true)) {
                    reject.accept(exception);
                }
            };
            range(0, tasks.size()).forEach(idx -> tasks.get(idx).fork(value ->
                resolveHandler.accept(idx, value), rejectHandler));
        });
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param fn the function
     * @return the task of C
     */
    public static <A, B, C> Task<C> apply(Task<A> ta, Task<B> tb, Fn2<A, B, C> fn) {
        return ta.flatMap(a -> tb.flatMap(b -> fn.apply(a, b)));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param fn the function
     * @return the task of D
     */
    public static <A, B, C, D> Task<D> apply(Task<A> ta, Task<B> tb, Task<C> tc, Fn3<A, B, C, D> fn) {
        return ta.flatMap(a -> tb.flatMap(b -> tc.flatMap(c -> fn.apply(a, b, c))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param fn the function
     * @return the task of E
     */
    public static <A, B, C, D, E> Task<E> apply(Task<A> ta, Task<B> tb, Task<C> tc, Task<D> td, Fn4<A, B, C, D, E> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d -> fn.apply(a, b, c, d)))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param fn the function
     * @return the task of F
     */
    public static <A, B, C, D, E, F> Task<F> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Fn5<A, B, C, D, E, F> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e -> fn.apply(a, b, c, d, e))))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param tf the task of F
     * @param fn the function
     * @return the task of G
     */
    public static <A, B, C, D, E, F, G> Task<G> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Task<F> tf,
                Fn6<A, B, C, D, E, F, G> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e ->
            tf.flatMap(f -> fn.apply(a, b, c, d, e, f)))))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param tf the task of F
     * @param tg  the task of G
     * @param fn the function
     * @return the task of H
     */
    public static <A, B, C, D, E, F, G, H> Task<H> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Task<F> tf,
                Task<G> tg,
                Fn7<A, B, C, D, E, F, G, H> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e ->
            tf.flatMap(f ->
            tg.flatMap(g -> fn.apply(a, b, c, d, e, f, g))))))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param tf the task of F
     * @param tg the task of G
     * @param th the task of H
     * @param fn the function
     * @return the task of I
     */
    public static <A, B, C, D, E, F, G, H, I> Task<I> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Task<F> tf,
                Task<G> tg,
                Task<H> th,
                Fn8<A, B, C, D, E, F, G, H, I> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e ->
            tf.flatMap(f ->
            tg.flatMap(g ->
            th.flatMap(h -> fn.apply(a, b, c, d, e, f, g, h)))))))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param tf the task of F
     * @param tg the task of G
     * @param th the task of H
     * @param ti the task of I
     * @param fn the function
     * @return the task of J
     */
    public static <A, B, C, D, E, F, G, H, I, J> Task<J> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Task<F> tf,
                Task<G> tg,
                Task<H> th,
                Task<I> ti,
                Fn9<A, B, C, D, E, F, G, H, I, J> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e ->
            tf.flatMap(f ->
            tg.flatMap(g ->
            th.flatMap(h ->
            ti.flatMap(i -> fn.apply(a, b, c, d, e, f, g, h, i))))))))));
    }

    /**
     * Applies the task
     *
     * @param ta the task of A
     * @param tb the task of B
     * @param tc the task of C
     * @param td the task of D
     * @param te the task of E
     * @param tf the task of F
     * @param tg the task of G
     * @param th the task of H
     * @param ti the task of I
     * @param tj the task of J
     * @param fn the function
     * @return the task of K
     */
    public static <A, B, C, D, E, F, G, H, I, J, K> Task<K> apply(
                Task<A> ta,
                Task<B> tb,
                Task<C> tc,
                Task<D> td,
                Task<E> te,
                Task<F> tf,
                Task<G> tg,
                Task<H> th,
                Task<I> ti,
                Task<J> tj,
                Fn10<A, B, C, D, E, F, G, H, I, J, K> fn) {
        return ta.flatMap(a ->
            tb.flatMap(b ->
            tc.flatMap(c ->
            td.flatMap(d ->
            te.flatMap(e ->
            tf.flatMap(f ->
            tg.flatMap(g ->
            th.flatMap(h ->
            ti.flatMap(i ->
            tj.flatMap(j -> fn.apply(a, b, c, d, e, f, g, h, i, j)))))))))));
    }

    @FunctionalInterface
    public interface Fn2<A, B, C> { Task<C> apply(A a, B b); }

    @FunctionalInterface
    public interface Fn3<A, B, C, D> { Task<D> apply(A a, B b, C c); }

    @FunctionalInterface
    public interface Fn4<A, B, C, D, E> { Task<E> apply(A a, B b, C c, D d); }

    @FunctionalInterface
    public interface Fn5<A, B, C, D, E, F> { Task<F> apply(A a, B b, C c, D d, E e); }

    @FunctionalInterface
    public interface Fn6<A, B, C, D, E, F, G> { Task<G> apply(A a, B b, C c, D d, E e, F f); }

    @FunctionalInterface
    public interface Fn7<A, B, C, D, E, F, G, H> { Task<H> apply(A a, B b, C c, D d, E e, F f, G g); }

    @FunctionalInterface
    public interface Fn8<A, B, C, D, E, F, G, H, I> { Task<I> apply(A a, B b, C c, D d, E e, F f, G g, H h); }

    @FunctionalInterface
    public interface Fn9<A, B, C, D, E, F, G, H, I, J> { Task<J> apply(A a, B b, C c, D d, E e, F f, G g, H h, I i); }

    @FunctionalInterface
    public interface Fn10<A, B, C, D, E, F, G, H, I, J, K> { Task<K> apply(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j); }
}
