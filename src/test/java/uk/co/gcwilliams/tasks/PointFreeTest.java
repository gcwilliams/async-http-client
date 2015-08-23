package uk.co.gcwilliams.tasks;

import org.junit.Assert;
import org.junit.Test;
import uk.co.gcwilliams.http.tasks.Task;
import uk.co.gcwilliams.http.tasks.Tasks;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.co.gcwilliams.http.tasks.PointFree.compose;
import static uk.co.gcwilliams.http.tasks.PointFree.fmap;

/**
 * The point free tests
 *
 * Created by GWilliams on 23/08/2015.
 */
public class PointFreeTest {

    @Test
    public void composeTwo() {

        // arrange
        Function<Integer, Integer> doubleIt = i -> i * 2;
        Function<Integer, Integer> fn = compose(doubleIt, doubleIt);

        // act
        int result = fn.apply(10);

        // assert
        assertThat(result, equalTo(40));
    }

    @Test
    public void composeThree() {

        // arrange
        Function<Integer, Integer> doubleIt = i -> i * 2;
        Function<Integer, Integer> fn = compose(doubleIt, doubleIt, doubleIt);

        // act
        int result = fn.apply(10);

        // assert
        assertThat(result, equalTo(80));
    }

    @Test
    public void composeFour() {

        // arrange
        Function<Integer, Integer> doubleIt = i -> i * 2;
        Function<Integer, Integer> fn = compose(doubleIt, doubleIt, doubleIt, doubleIt);

        // act
        int result = fn.apply(10);

        // assert
        assertThat(result, equalTo(160));
    }

    @Test
    public void functorMap() {

        // arrange
        Function<Integer, Integer> doubleIt = i -> i * 2;
        Function<Integer, Task<Integer, Void>> fn = compose(fmap(doubleIt), Tasks::<Integer, Void>of);

        // act
        Task<Integer, Void> task = fn.apply(10);

        // assert
        task.fork(i -> assertThat(i, equalTo(20)), e -> fail());
    }
}
