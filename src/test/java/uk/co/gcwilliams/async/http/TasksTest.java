package uk.co.gcwilliams.async.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The tasks tests
 *
 * @author : Gareth Williams
 */
class TasksTest {

    @Test void traverse() throws Exception {

        // arrange
        Task<String> a = Task.of("A");
        Task<String> b = Task.of("B");

        // act
        List<String> traversed = Tasks.get(Tasks.traverse(List.of(a, b)), Duration.ZERO);

        // assert
        assertThat(traversed, containsInRelativeOrder("A", "B"));
    }

    @Test void traverseFailed() {

        // arrange
        Task<String> a = Task.of("A");
        Task<String> b = Task.of(new IllegalStateException("B"));

        // act
        assertThrows(IllegalStateException.class, () -> Tasks.get(Tasks.traverse(List.of(a, b)), Duration.ZERO));
    }

    @Test void traverseP() throws Exception {

        // arrange
        Task<String> a = Task.of("A");
        Task<String> b = Task.of("B");

        // act
        List<String> traversed = Tasks.get(Tasks.traverseP(List.of(a, b)), Duration.ZERO);

        // assert
        assertThat(traversed, containsInRelativeOrder("A", "B"));
    }

    @Test void traversePFailed() {

        // arrange
        Task<String> a = Task.of("A");
        Task<String> b = Task.of(new IllegalStateException("B"));

        // act
        assertThrows(IllegalStateException.class, () -> Tasks.get(Tasks.traverseP(List.of(a, b)), Duration.ZERO));
    }

    @Test void apply2() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            (a, b) -> Task.of(List.of(a, b))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B"));
    }

    @Test void apply3() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            (a, b, c) -> Task.of(List.of(a, b, c))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C"));
    }

    @Test void apply4() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            (a, b, c, d) -> Task.of(List.of(a, b, c, d))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D"));
    }

    @Test void apply5() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            (a, b, c, d, e) -> Task.of(List.of(a, b, c, d, e))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E"));
    }

    @Test void apply6() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            Task.of("F"),
            (a, b, c, d, e, f) -> Task.of(List.of(a, b, c, d, e, f))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E", "F"));
    }

    @Test void apply7() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            Task.of("F"),
            Task.of("G"),
            (a, b, c, d, e, f, g) -> Task.of(List.of(a, b, c, d, e, f, g))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E", "F", "G"));
    }

    @Test void apply8() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            Task.of("F"),
            Task.of("G"),
            Task.of("H"),
            (a, b, c, d, e, f, g, h) -> Task.of(List.of(a, b, c, d, e, f, g, h))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E", "F", "G", "H"));
    }

    @Test void apply9() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            Task.of("F"),
            Task.of("G"),
            Task.of("H"),
            Task.of("I"),
            (a, b, c, d, e, f, g, h, i) -> Task.of(List.of(a, b, c, d, e, f, g, h, i))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E", "F", "G", "H", "I"));
    }

    @Test void apply10() throws Exception {

        // act
        List<String> applied = Tasks.get(Tasks.apply(
            Task.of("A"),
            Task.of("B"),
            Task.of("C"),
            Task.of("D"),
            Task.of("E"),
            Task.of("F"),
            Task.of("G"),
            Task.of("H"),
            Task.of("I"),
            Task.of("J"),
            (a, b, c, d, e, f, g, h, i, j) -> Task.of(List.of(a, b, c, d, e, f, g, h, i, j))), Duration.ZERO);

        // assert
        assertThat(applied, containsInRelativeOrder("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"));
    }
}
