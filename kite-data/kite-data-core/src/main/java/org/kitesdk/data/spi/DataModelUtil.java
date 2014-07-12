/*
 * Copyright 2014 Cloudera, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecord;
import org.kitesdk.data.IncompatibleSchemaException;
import org.kitesdk.data.PartitionStrategy;

/**
 * Utilities for determining the appropriate data model at runtime.
 *
 * @since 0.15.0
 */
public class DataModelUtil {

  /**
   * Get the data model for the given type.
   *
   * @param <E> The entity type
   * @param type The Java class of the entity type
   * @return The appropriate data model for the given type
   */
  public static <E> GenericData getDataModelForType(Class<E> type) {
    // Need to check if SpecificRecord first because specific records also
    // implement GenericRecord
    if (SpecificRecord.class.isAssignableFrom(type)) {
      return new SpecificData(type.getClassLoader());
    } else if (GenericRecord.class.isAssignableFrom(type)) {
      return GenericData.get();
    } else {
      return ReflectData.get();
    }
  }

  /**
   * Get the DatumReader for the given type.
   *
   * @param <E> The entity type
   * @param type The Java class of the entity type
   * @param writerSchema The {@link Schema} for entities
   * @return The DatumReader for the given type
   */
  @SuppressWarnings("unchecked")
  public static <E> DatumReader<E> getDatumReaderForType(Class<E> type, Schema writerSchema) {
    Schema readerSchema = getReaderSchema(type, writerSchema);
    return getDataModelForType(type).createDatumReader(writerSchema, readerSchema);
  }

  /**
   * Resolves the type based on the given schema. In most cases, the type should
   * stay as is. However, if the type is Object, then that means that the old
   * default behavior of determining the class from ReflectData#getClass(Schema)
   * should be used. If a class can't be found, it will default to
   * GenericData.Record.
   *
   * @param <E> The entity type
   * @param type The Java class of the entity type
   * @param schema The {@link Schema} for the entity
   * @return The resolved Java class object
   * @throws IncompatibleSchemaException The schema for the resolved type is not
   * compatible with the schema that was given.
   */
  @SuppressWarnings("unchecked")
  public static <E> Class<E> resolveType(Class<E> type, Schema schema) {
    if (type == Object.class) {
      type = ReflectData.get().getClass(schema);
    }

    if (type == null) {
      type = (Class<E>) GenericData.Record.class;
    }

    Schema readerSchema = getReaderSchema(type, schema);
    if (false == SchemaValidationUtil.canRead(schema, readerSchema)) {
      throw new IncompatibleSchemaException(
          String.format("The reader schema derived from %s is not compatible "
          + "with the dataset's given writer schema.", type.toString()));
    }

    return type;
  }

  /**
   * Get the reader schema based on the given type and writer schema.
   *
   * @param <E> The entity type
   * @param type The Java class of the entity type
   * @param writerSchema The writer {@link Schema} for the entity
   * @return The reader schema based on the given type and writer schema
   */
  public static <E> Schema getReaderSchema(Class<E> type, Schema writerSchema) {
    Schema readerSchema = writerSchema;
    GenericData dataModel = getDataModelForType(type);

    if (dataModel instanceof SpecificData) {
      readerSchema = ((SpecificData)dataModel).getSchema(type);
    }

    return readerSchema;
  }
}
