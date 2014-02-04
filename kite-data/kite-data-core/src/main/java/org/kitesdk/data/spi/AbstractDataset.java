/*
 * Copyright 2013 Cloudera.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kitesdk.data.spi;

import org.kitesdk.data.Dataset;
import org.kitesdk.data.DatasetReader;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.RefineableView;
import org.kitesdk.data.View;
import javax.annotation.concurrent.Immutable;

/**
 * A common Dataset base class to simplify implementations.
 *
 * @param <E> The type of entities stored in this {@code Dataset}.
 * @since 0.9.0
 */
@Immutable
public abstract class AbstractDataset<E> implements Dataset<E>, RefineableView<E> {

  @Override
  public Dataset<E> getDataset() {
    return this;
  }

  @Override
  @Deprecated
  public DatasetReader<E> getReader() {
    return newReader();
  }

  @Override
  @Deprecated
  public DatasetWriter<E> getWriter() {
    return newWriter();
  }

  @Override
  public boolean contains(E entity) {
    return true;
  }

}
