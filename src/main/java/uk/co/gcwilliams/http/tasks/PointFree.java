package uk.co.gcwilliams.http.tasks;

import java.util.function.Function;

/**
 * The point free helper methods
 *
 * Created by GWilliams on 16/08/2015.
 */
public class PointFree {

    private PointFree() { // static
    }

    /**
     * Composes two functions together, right to left
     *
     * @param one the first function
     * @param two the second function
     * @return the composed function
     */
    public static <A, B, C> Function<A, C> compose(Function<B, C> two, Function<A, B> one) {
        return a -> two.apply(one.apply(a));
    }

    /**
     * Composes three functions together, right to left
     *
     * @param one the first function
     * @param two the second function
     * @param three the third function
     * @return the composed function
     */
    public static <A, B, C, D> Function<A, D> compose(Function<C, D> three, Function<B, C> two, Function<A, B> one) {
        return a -> three.apply(two.apply(one.apply(a)));
    }

    /**
     * Composes three functions together, right to left
     *
     * @param one the first function
     * @param two the second function
     * @param three the third function
     * @param four the fourth function
     * @return the composed function
     */
    public static <A, B, C, D, E> Function<A, E> compose(Function<D, E> four, Function<C, D> three, Function<B, C> two, Function<A, B> one) {
        return a -> four.apply(three.apply(two.apply(one.apply(a))));
    }

    /**
     * Creates a function fmap over
     *
     * @param f the function to fmap over
     * @return the function
     */
    public static <V, NV, FIn extends Functor<V>, FOut extends Functor<NV>> Function<FIn, FOut> fmap(Function<V, NV> f) {
        return functor -> functor.map(f);
    }
}
