/**
 * Copyright 2013 Cloudera Inc.
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
package org.kitesdk.data.hbase;

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetDescriptor;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.Key;
import org.kitesdk.data.spi.Marker;
import org.kitesdk.data.PartitionKey;
import org.kitesdk.data.PartitionStrategy;
import org.kitesdk.data.RandomAccessDataset;
import org.kitesdk.data.View;
import org.kitesdk.data.hbase.impl.Dao;
import org.kitesdk.data.spi.AbstractDataset;
import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DaoDataset<E> extends AbstractDataset<E> implements RandomAccessDataset<E> {

  private static final Logger logger = LoggerFactory
      .getLogger(DaoDataset.class);

  private String name;
  private Dao<E> dao;
  private DatasetDescriptor descriptor;
  private final DaoView<E> unbounded;

  public DaoDataset(String name, Dao<E> dao, DatasetDescriptor descriptor) {
    this.name = name;
    this.dao = dao;
    this.descriptor = descriptor;
    this.unbounded = new DaoView<E>(this);
  }

  Dao<E> getDao() {
    return dao;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DatasetDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public Dataset<E> getPartition(PartitionKey key, boolean autoCreate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dropPartition(PartitionKey key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterable<Dataset<E>> getPartitions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DatasetWriter<E> newWriter() {
    logger.debug("Getting writer to dataset:{}", this);

    return unbounded.newWriter();
  }

  @Override
  public DatasetReader<E> newReader() {
    logger.debug("Getting reader for dataset:{}", this);

    return unbounded.newReader();
  }

  @Override
  public Iterable<View<E>> getCoveringPartitions() {
    Preconditions.checkState(descriptor.isPartitioned(),
        "Attempt to get partitions on a non-partitioned dataset (name:%s)",
        name);

    return unbounded.getCoveringPartitions();
  }

  @Override
  public DaoView<E> from(Marker start) {
    return (DaoView<E>) unbounded.from(start);
  }

  @Override
  public DaoView<E> fromAfter(Marker start) {
    return (DaoView<E>) unbounded.fromAfter(start);
  }

  @Override
  public DaoView<E> to(Marker end) {
    return (DaoView<E>) unbounded.to(end);
  }

  @Override
  public DaoView<E> toBefore(Marker end) {
    return (DaoView<E>) unbounded.toBefore(end);
  }

  @Override
  public DaoView<E> of(Marker partial) {
    return (DaoView<E>) unbounded.of(partial);
  }

  @Override
  public View<E> from(String name, Object value) {
    return unbounded.from(name, value);
  }

  @Override
  public View<E> with(String name) {
    return unbounded.with(name);
  }

  @Override
  public View<E> with(String name, Object value) {
    return unbounded.with(name, value);
  }

  @Override
  public View<E> fromAfter(String name, Object value) {
    return unbounded.fromAfter(name, value);
  }

  @Override
  public View<E> to(String name, Object value) {
    return unbounded.to(name, value);
  }

  @Override
  public View<E> toBefore(String name, Object value) {
    return unbounded.toBefore(name, value);
  }

  @Override
  public View<E> union(View<E> other) {
    return unbounded.union(other);
  }

  @Override
  public View<E> complement() {
    return unbounded.complement();
  }

  @Override
  @SuppressWarnings("deprecation")
  public E get(Key key) {
    return dao.get(keyFor(getDescriptor().getPartitionStrategy(), key));
  }

  @Override
  public boolean put(E entity) {
    return dao.put(entity);
  }

  @Override
  @SuppressWarnings("deprecation")
  public long increment(Key key, String fieldName, long amount) {
    return dao.increment(keyFor(getDescriptor().getPartitionStrategy(), key), fieldName, amount);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void delete(Key key) {
    dao.delete(keyFor(getDescriptor().getPartitionStrategy(), key));
  }

  @Override
  public boolean delete(E entity) {
    return dao.delete(entity);
  }

  @Deprecated
  static PartitionKey keyFor(PartitionStrategy strategy, Key key) {
    final int size = strategy.getFieldPartitioners().size();
    final Object[] values = new Object[size];

    for (int i = 0; i < size; i += 1) {
      values[i] = key.get(i);
    }

    return strategy.partitionKey(values);
  }
}
