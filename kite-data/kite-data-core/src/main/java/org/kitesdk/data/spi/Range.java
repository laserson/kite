/*
 * Copyright 2013 Cloudera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.data.spi;


import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.BoundType;
import com.google.common.collect.DiscreteDomain;
import java.util.Set;
import javax.annotation.Nullable;

public class Range<T> implements Predicate<T> {

  public static <T> Range<T> open(T lower, T upper) {
    return new Range<T>(bound(lower, false), bound(upper, false));
  }

  public static <T> Range<T> closed(T lower, T upper) {
    return new Range<T>(bound(lower, true), bound(upper, true));
  }

  public static <T> Range<T> closedOpen(T lower, T upper) {
    return new Range<T>(bound(lower, true), bound(upper, false));
  }

  public static <T> Range<T> openClosed(T lower, T upper) {
    return new Range<T>(bound(lower, false), bound(upper, true));
  }

  public static <T> Range<T> greaterThan(T endpoint) {
    return new Range<T>(bound(endpoint, false), null);
  }

  public static <T> Range<T> atLeast(T endpoint) {
    return new Range<T>(bound(endpoint, true), null);
  }

  public static <T> Range<T> lessThan(T endpoint) {
    return new Range<T>(null, bound(endpoint, false));
  }

  public static <T> Range<T> atMost(T endpoint) {
    return new Range<T>(null, bound(endpoint, true));
  }

  public static <T> Range<T> singleton(T endpoint) {
    return new Range<T>(bound(endpoint, true), bound(endpoint, true));
  }

  public static <C extends Comparable<C>> Set<C> asSet(
      Range<C> range, DiscreteDomain<C> domain) {
    // cheat and pass this to guava
    com.google.common.collect.Range<C> guavaRange;
    if (range.hasLowerBound()) {
      if (range.hasUpperBound()) {
        guavaRange = com.google.common.collect.Ranges.range(
            range.lower.endpoint(),
            range.isLowerBoundOpen() ? BoundType.OPEN : BoundType.CLOSED,
            range.upper.endpoint(),
            range.isUpperBoundOpen() ? BoundType.OPEN : BoundType.CLOSED);
      } else {
        guavaRange = com.google.common.collect.Ranges.downTo(
            range.lower.endpoint(),
            range.isLowerBoundOpen() ? BoundType.OPEN : BoundType.CLOSED);
      }
    } else if (range.hasUpperBound()) {
      guavaRange = com.google.common.collect.Ranges.upTo(
          range.upper.endpoint(),
          range.isUpperBoundOpen() ? BoundType.OPEN : BoundType.CLOSED);
    } else {
      guavaRange = com.google.common.collect.Ranges.all();
    }
    return guavaRange.asSet(domain);
  }

  @SuppressWarnings("unchecked")
  private static <T> Bound<T> bound(T endpoint, boolean inclusive) {
    if (endpoint instanceof CharSequence) {
      return (Bound<T>) new CharSequenceBound((CharSequence) endpoint, inclusive);
    } else if (endpoint instanceof Comparable) {
      return (Bound<T>) new ComparableBound((Comparable) endpoint, inclusive);
    } else {
      throw new RuntimeException();
    }
  }

  private final Bound<T> lower;
  private final Bound<T> upper;

  @SuppressWarnings("unchecked")
  private Range(Bound<T> lower, Bound<T> upper) {
    this.lower = (lower == null ? ((Bound<T>) INF) : lower);
    this.upper = (upper == null ? ((Bound<T>) INF) : upper);
  }

  public boolean isLowerBoundOpen() {
    return !lower.inclusive();
  }

  public boolean isUpperBoundOpen() {
    return !upper.inclusive();
  }

  public boolean isLowerBoundClosed() {
    return lower.inclusive();
  }

  public boolean isUpperBoundClosed() {
    return upper.inclusive();
  }

  public boolean hasLowerBound() {
    return lower != INF;
  }

  public boolean hasUpperBound() {
    return upper != INF;
  }

  public T lowerEndpoint() {
    return lower.endpoint();
  }

  public T upperEndpoint() {
    return upper.endpoint();
  }

  public Range<T> intersection(Range<T> other) {
    Bound<T> newLower;
    if (other.lower == INF) {
      newLower = lower;
    } else {
      newLower = lower.isLessThan(other.lower.endpoint()) ? other.lower : lower;
    }
    Bound<T> newUpper;
    if (other.upper == INF) {
      newUpper = upper;
    } else {
      newUpper = upper.isGreaterThan(other.upper.endpoint()) ? other.upper : upper;
    }
    return new Range<T>(newLower, newUpper);
  }

  @Override
  public boolean apply(@Nullable T input) {
    return contains(input);
  }

  public boolean contains(T input) {
    return lower.isLessThan(input) && upper.isGreaterThan(input);
  }

  @Override
  public String toString() {
    return (isLowerBoundClosed() ? "[" : "(") +
        lower.toString() + ", " + upper.toString() +
        (isUpperBoundClosed() ? "]" : ")");
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || object.getClass() != getClass()) {
      return false;
    }
    Range other = (Range) object;
    return Objects.equal(lower, other.lower) &&
        Objects.equal(upper, other.upper);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(lower, upper);
  }

  private static interface Bound<T> {
    public boolean inclusive();
    public T endpoint();
    public boolean isLessThan(T other);
    public boolean isGreaterThan(T other);
  }

  private static Bound<Object> INF = new Bound<Object>() {
    @Override
    public boolean inclusive() {
      return false;
    }

    @Override
    public Object endpoint() {
      return null;
    }

    @Override
    public boolean isLessThan(Object other) {
      return true;
    }

    @Override
    public boolean isGreaterThan(Object other) {
      return true;
    }

    @Override
    public String toString() {
      return "inf";
    }
  };

  private abstract static class AbstractBound<T> implements Bound<T> {
    private final T endpoint;
    private final boolean inclusive;

    private AbstractBound(T endpoint, boolean inclusive) {
      this.endpoint = endpoint;
      this.inclusive = inclusive;
    }

    protected abstract int compare(T left, T right);

    public boolean inclusive() {
      return inclusive;
    }

    public T endpoint() {
      return endpoint;
    }

    public boolean isLessThan(T other) {
      int cmp = compare(endpoint, other);
      if (cmp < 0) {
        return true;
      } else if (cmp == 0) {
        return inclusive;
      }
      return false;
    }

    public boolean isGreaterThan(T other) {
      int cmp = compare(other, endpoint);
      if (cmp < 0) {
        return true;
      } else if (cmp == 0) {
        return inclusive;
      }
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || obj.getClass() != getClass()) {
        return false;
      }
      AbstractBound<T> other = (AbstractBound<T>) obj;
      return (inclusive == other.inclusive) &&
          (compare(endpoint, other.endpoint) == 0);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(inclusive, endpoint);
    }

    @Override
    public String toString() {
      return endpoint.toString();
    }
  }

  private static class CharSequenceBound extends AbstractBound<CharSequence> {
    private CharSequenceBound(CharSequence endpoint, boolean inclusive) {
      super(endpoint, inclusive);
    }

    @Override
    protected int compare(CharSequence left, CharSequence right) {
      return CharSequences.compare(left, right);
    }

    @Override
    public boolean equals(Object obj) {
      // without this, findbugs complains. correct because super calls compare.
      return super.equals(obj);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(inclusive(), CharSequences.hashCode(endpoint()));
    }
  }

  private static class ComparableBound<C extends Comparable<C>> extends AbstractBound<C> {
    private ComparableBound(C endpoint, boolean inclusive) {
      super(endpoint, inclusive);
    }

    @Override
    protected int compare(C left, C right) {
      return left.compareTo(right);
    }
  }
}
