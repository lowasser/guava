/*
 * Copyright (C) 2015 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Implementation of a {@link Spliterator} indexed by an arbitrary function.
 * 
 * @author Louis Wasserman
 */
final class IndexedSpliterator<E> implements Spliterator<E> {
  private int offset;
  private final int length;
  private final int characteristics;
  private final IntFunction<E> indexFunction;
  private final Comparator<? super E> comparator;

  private IndexedSpliterator(int offset, int length, IntFunction<E> indexFunction,
      Comparator<? super E> comparator, int characteristics) {
    this.offset = offset;
    this.length = length;
    this.indexFunction = indexFunction;
    this.comparator = comparator;
    this.characteristics = characteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
  }

  IndexedSpliterator(int length, IntFunction<E> indexFunction, Comparator<? super E> comparator,
      int characteristics) {
    this(0, length, indexFunction, comparator, characteristics);
  }

  IndexedSpliterator(int length, IntFunction<E> indexFunction, int characteristics) {
    this(length, indexFunction, null, characteristics);
  }

  @Override
  public boolean tryAdvance(Consumer<? super E> action) {
    checkNotNull(action);
    if (offset < length) {
      try {
        action.accept(indexFunction.apply(offset));
      } finally {
        offset++;
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void forEachRemaining(Consumer<? super E> action) {
    checkNotNull(action);
    while (offset < length) {
      try {
        action.accept(indexFunction.apply(offset));
      } finally {
        offset++;
      }
    }
  }

  @Override
  public Spliterator<E> trySplit() {
    int mid = (offset + length) >>> 1;
    if (offset >= mid) {
      return null;
    }
    Spliterator<E> result =
        new IndexedSpliterator<E>(offset, mid, indexFunction, comparator, characteristics);
    this.offset = mid;
    return result;
  }

  @Override
  public long estimateSize() {
    return length - offset;
  }

  @Override
  public int characteristics() {
    return characteristics;
  }

  @Override
  public Comparator<? super E> getComparator() {
    if (hasCharacteristics(Spliterator.SORTED)) {
      return comparator;
    } else {
      throw new IllegalStateException();
    }
  }
}
