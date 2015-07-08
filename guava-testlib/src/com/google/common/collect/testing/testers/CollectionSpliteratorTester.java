/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect.testing.testers;

import static com.google.common.collect.testing.features.CollectionFeature.ALLOWS_NULL_VALUES;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_ADD;
import static com.google.common.collect.testing.features.CollectionFeature.SUPPORTS_REMOVE;
import static com.google.common.collect.testing.features.CollectionSize.ZERO;

import com.google.common.collect.Ordering;
import com.google.common.collect.testing.AbstractCollectionTester;
import com.google.common.collect.testing.Helpers;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A generic JUnit test which tests {@code spliterator} operations on a collection.
 * Can't be invoked directly; please see
 * {@link com.google.common.collect.testing.CollectionTestSuiteBuilder}.
 *
 * @author Louis Wasserman
 */
public class CollectionSpliteratorTester<E> extends AbstractCollectionTester<E> {
  /**
   * Different ways of decomposing a Spliterator, all of which must produce the same
   * elements (up to ordering, if Spliterator.ORDERED is not present).
   */
  enum SpliteratorDecompositionStrategy {
    NO_SPLIT_FOR_EACH_REMAINING {
      @Override <E> void forEach(Spliterator<E> spliterator, Consumer<? super E> consumer) {
        spliterator.forEachRemaining(consumer);
      }
    },
    NO_SPLIT_TRY_ADVANCE {
      @Override <E> void forEach(Spliterator<E> spliterator, Consumer<? super E> consumer) {
        while (spliterator.tryAdvance(consumer)) {
          // do nothing
        }
      }
    },
    MAXIMUM_SPLIT {
      @Override <E> void forEach(Spliterator<E> spliterator, Consumer<? super E> consumer) {
        for (Spliterator<E> prefix = spliterator.trySplit(); prefix != null;
            prefix = spliterator.trySplit()) {
          forEach(prefix, consumer);
        }
        spliterator.forEachRemaining(consumer);
      }
    };
    abstract <E> void forEach(Spliterator<E> spliterator, Consumer<? super E> consumer);
  }

  public void testSpliterator() {
    for (SpliteratorDecompositionStrategy strategy :
        EnumSet.allOf(SpliteratorDecompositionStrategy.class)) {
      List<E> elements = new ArrayList<E>();
      strategy.forEach(collection.spliterator(), elements::add);
      Helpers.assertEqualIgnoringOrder(getSampleElements(), elements);
    }
  }

  public void testSpliteratorKnownOrder() {
    if (collection.spliterator().hasCharacteristics(Spliterator.ORDERED)) {
      for (SpliteratorDecompositionStrategy strategy :
          EnumSet.allOf(SpliteratorDecompositionStrategy.class)) {
        List<E> elements = new ArrayList<E>();
        strategy.forEach(collection.spliterator(), elements::add);
        List<E> expected = Helpers.copyToList(getOrderedElements());
        assertEquals(expected, elements);
      }
    }
  }

  public void testSpliteratorGetComparator() {
    if (collection.spliterator().hasCharacteristics(Spliterator.SORTED)) {
      for (SpliteratorDecompositionStrategy strategy :
          EnumSet.allOf(SpliteratorDecompositionStrategy.class)) {
        Spliterator<E> spliterator = collection.spliterator();
        Comparator<? super E> comparator = spliterator.getComparator();
        if (comparator == null) {
          comparator = (Comparator) Ordering.natural();
        }
        List<E> elements = new ArrayList<E>();
        strategy.forEach(spliterator, elements::add);
        assertTrue(Ordering.from(comparator).isOrdered(elements));
      }
    } else {
      try {
        collection.spliterator().getComparator();
        fail("Expected IllegalStateException");
      } catch (IllegalStateException expected) {
        // success
      }
    }
  }

  public void testEstimateSize() {
    Spliterator<E> spliterator = collection.spliterator();
    if (spliterator.hasCharacteristics(Spliterator.SIZED)) {
      assertEquals(getNumElements(), spliterator.estimateSize());
      assertEquals(getNumElements(), spliterator.getExactSizeIfKnown());
      if (spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
        Spliterator<E> part = spliterator.trySplit();
        assertEquals(getNumElements(),
            ((part == null) ? 0 : part.estimateSize()) + spliterator.estimateSize());
      }
    }
  }

  @CollectionFeature.Require(ALLOWS_NULL_VALUES)
  @CollectionSize.Require(absent = ZERO)
  public void testSpliteratorNullable() {
    // If null values are allowed, verify that NONNULL is not reported.
    assertFalse(collection.spliterator().hasCharacteristics(Spliterator.NONNULL));
  }

  @CollectionFeature.Require(SUPPORTS_ADD)
  public void testSpliteratorNotImmutable_CollectionAllowsAdd() {
    // If add is supported, verify that IMMUTABLE is not reported.
    assertFalse(collection.spliterator().hasCharacteristics(Spliterator.IMMUTABLE));
  }

  @CollectionFeature.Require(SUPPORTS_REMOVE)
  public void testSpliteratorNotImmutable_CollectionAllowsRemove() {
    // If remove is supported, verify that IMMUTABLE is not reported.
    assertFalse(collection.spliterator().hasCharacteristics(Spliterator.IMMUTABLE));
  }
}
