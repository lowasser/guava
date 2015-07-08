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

import java.util.Spliterator;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * Implementation of a {@link Spliterator} for a single element.
 * 
 * @author Louis Wasserman
 */
final class SingletonSpliterator<E> implements Spliterator<E> {
  private E element;
  private boolean consumed;
  
  SingletonSpliterator(@Nullable E element) {
    this.element = element;
    this.consumed = false;
  }

  @Override
  public boolean tryAdvance(Consumer<? super E> action) {
    checkNotNull(action);
    if (!consumed) {
      try {
        action.accept(element);
      } finally {
        element = null; // eliminate the reference for GC; we'll never need it again
        consumed = true;
      }
      return true;
    }
    return false;
  }

  @Override
  public void forEachRemaining(Consumer<? super E> action) {
    tryAdvance(action);
  }

  @Override
  public Spliterator<E> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return consumed ? 0 : 1;
  }

  @Override
  public int characteristics() {
    // TODO(lowasser): we could do NONNULL, but keeping it consistent across invocations as per
    // spec might be tricky
    return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.SIZED
        | Spliterator.SUBSIZED;
  }
}
