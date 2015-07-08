package com.google.common.testing;

import static junit.framework.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collector;

/**
 * Tester for {@code Collector} implementations.
 *
 * <p>Example usage:
 * <pre>
 * CollectorTester.of(Collectors.summingInt(Integer::parseInt))
 *   .expectCollects(3, "1", "2")
 *   .expectCollects(10, "1", "4", "3", "2")
 *   .expectCollects(5, "-3", "0", "8");
 * </pre>
 *
 * @author Louis Wasserman
 */
public class CollectorTester<T, A, R> {
  /**
   * Creates a {@code CollectorTester} for the specified {@code Collector}, with the specified
   * equivalence relation.
   */
  public static <T, A, R> CollectorTester<T, A, R> of(Collector<T, A, R> collector) {
    return of(collector, Objects::equals);
  }
  /**
   * Creates a {@code CollectorTester} for the specified {@code Collector}, with the specified
   * equivalence relation.
   */
  public static <T, A, R> CollectorTester<T, A, R> of(Collector<T, A, R> collector,
      BiPredicate<? super R, ? super R> equivalence) {
    return new CollectorTester<T, A, R>(collector, equivalence);
  }
  
  private final Collector<T, A, R> collector;
  private final BiPredicate<? super R, ? super R> equivalence;

  private CollectorTester(
      Collector<T, A, R> collector, BiPredicate<? super R, ? super R> equivalence) {
    this.collector = collector;
    this.equivalence = equivalence;
  }

  /*
   * TODO(lowasser): allow different Equivalences to be used, so e.g. CONCURRENT
   * collectors can receive the elements in random order and still be tested
   * that the result is the same up to ordering differences.
   */
  
  /**
   * Different orderings for combining the elements of an input array, which must
   * all produce the same result.
   */
  enum CollectScheme {
    /**
     * Get one accumulator and accumulate the elements into it sequentially.
     */
    SEQUENTIAL {
      @SafeVarargs
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, T... inputs) {
        A accum = collector.supplier().get();
        for (T input : inputs) {
          collector.accumulator().accept(accum, input);
        }
        return accum;
      }
    },
    /**
     * Get one accumulator for each element and merge the accumulators
     * left-to-right.
     */
    MERGE_LEFT_ASSOCIATIVE {
      @SafeVarargs
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, T... inputs) {
        A accum = collector.supplier().get();
        for (T input : inputs) {
          A newAccum = collector.supplier().get();
          collector.accumulator().accept(newAccum, input);
          accum = collector.combiner().apply(accum, newAccum);
        }
        return accum;
      }
    },
    /**
     * Get one accumulator for each element and merge the accumulators
     * right-to-left.
     */
    MERGE_RIGHT_ASSOCIATIVE {
      @SafeVarargs
      @Override
      final <T, A, R> A result(Collector<T, A, R> collector, T... inputs) {
        Deque<A> stack = new ArrayDeque<>();
        for (T input : inputs) {
          A newAccum = collector.supplier().get();
          collector.accumulator().accept(newAccum, input);
          stack.push(newAccum);
        }
        stack.push(collector.supplier().get());
        while (stack.size() > 1) {
          A right = stack.pop();
          A left = stack.pop();
          stack.push(collector.combiner().apply(left, right));
        }
        return stack.pop();
      }
    };

    abstract <T, A, R> A result(Collector<T, A, R> collector, T... inputs);
  }

  /**
   * Verifies that the specified expected result is always produced by collecting the
   * specified inputs, regardless of how the elements are divided.
   */
  @SafeVarargs
  public final CollectorTester<T, A, R> expectCollects(R expectedResult, T... inputs) {
    for (CollectScheme scheme : EnumSet.allOf(CollectScheme.class)) {
      A finalAccum = scheme.result(collector, inputs);
      if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
        assertEquivalent(expectedResult, (R) finalAccum);
      }
      assertEquivalent(expectedResult, collector.finisher().apply(finalAccum));
    }
    return this;
  }

  private void assertEquivalent(R expected, R actual) {
    assertTrue(
        "Expected " + expected + " got " + actual + " modulo equivalence " + equivalence,
        equivalence.test(expected, actual));
  }
}

