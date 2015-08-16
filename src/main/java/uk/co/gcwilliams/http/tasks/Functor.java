package uk.co.gcwilliams.http.tasks;

import java.util.function.Function;

/**
 * The functor interface
 *
 * Created by GWilliams on 16/08/2015.
 */
public interface Functor<V> {

    /**
     * The functor map
     *
     * @param fn the function to call
     * @return the new value
     */
    <NV, F extends Functor<NV>> F map(Function<V, NV> fn);
}
