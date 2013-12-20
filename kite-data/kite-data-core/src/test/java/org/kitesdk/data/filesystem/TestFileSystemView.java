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

package org.kitesdk.data.filesystem;

import org.kitesdk.data.DatasetRepository;
import org.kitesdk.data.DatasetWriter;
import org.kitesdk.data.spi.TestRangeViews;
import org.kitesdk.data.View;
import org.kitesdk.data.event.StandardEvent;
import org.kitesdk.data.spi.StorageKey;
import com.google.common.collect.Sets;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

public class TestFileSystemView extends TestRangeViews {

  public TestFileSystemView(boolean distributed) {
    super(distributed);
  }

  @Override
  public DatasetRepository newRepo() {
    return new FileSystemDatasetRepository.Builder()
        .configuration(conf)
        .rootDirectory(URI.create("target/data"))
        .build();
  }

  @After
  public void removeDataPath() throws IOException {
    fs.delete(new Path("target/data"), true);
  }

  @Test
  @Override
  @SuppressWarnings("unchecked")
  public void testCoveringPartitions() {
    final FileSystemDataset dataset = (FileSystemDataset<StandardEvent>)
        testDataset;
    // NOTE: this is an un-restricted write so all should succeed
    final DatasetWriter<StandardEvent> writer = testDataset.newWriter();
    try {
      writer.open();
      writer.write(sepEvent);
      writer.write(octEvent);
      writer.write(novEvent);
    } finally {
      writer.close();
    }

    final Set<View<StandardEvent>> expected = (Set) Sets.newHashSet(
        dataset.of(new StorageKey.Builder(strategy).buildFrom(sepEvent)),
        dataset.of(new StorageKey.Builder(strategy).buildFrom(octEvent)),
        dataset.of(new StorageKey.Builder(strategy).buildFrom(novEvent)));

    Assert.assertEquals("Covering partitions should match",
        expected, Sets.newHashSet(dataset.getCoveringPartitions()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testDelete() throws Exception {
    final FileSystemView<StandardEvent> view =
        new FileSystemView((FileSystemDataset) testDataset);
    // NOTE: this is an un-restricted write so all should succeed
    final DatasetWriter<StandardEvent> writer = testDataset.newWriter();
    try {
      writer.open();
      writer.write(sepEvent);
      writer.write(octEvent);
      writer.write(novEvent);
    } finally {
      writer.close();
    }

    final Path root = new Path("target/data/test");
    final Path y2013 = new Path("target/data/test/year=2013");
    final Path sep = new Path("target/data/test/year=2013/month=09");
    final Path sep12 = new Path("target/data/test/year=2013/month=09/day=12");
    final Path oct = new Path("target/data/test/year=2013/month=10");
    final Path oct12 = new Path("target/data/test/year=2013/month=10/day=12");
    final Path nov = new Path("target/data/test/year=2013/month=11");
    final Path nov11 = new Path("target/data/test/year=2013/month=11/day=11");
    assertDirectoriesExist(fs, root, y2013, sep, sep12, oct, oct12, nov, nov11);

    Assert.assertFalse("Delete should return false to indicate no changes",
        view.from(newMarker(2013, 6)).toBefore(newMarker(2013, 9))
            .deleteAll());
    Assert.assertFalse("Delete should return false to indicate no changes",
        view.from(newMarker(2013, 11, 12)).deleteAll());

    // delete everything up to September
    Assert.assertTrue("Delete should return true to indicate FS changed",
        view.to(newMarker(2013, 9)).deleteAll());
    assertDirectoriesDoNotExist(fs, sep12, sep);
    assertDirectoriesExist(fs, root, y2013, oct, oct12, nov, nov11);
    Assert.assertFalse("Delete should return false to indicate no changes",
        view.to(newMarker(2013, 9)).deleteAll());

    // delete November 11 and later
    Assert.assertTrue("Delete should return true to indicate FS changed",
        view.from(newMarker(2013, 11, 11)).to(newMarker(2013, 11, 12))
            .deleteAll());
    assertDirectoriesDoNotExist(fs, sep12, sep, nov11, nov);
    assertDirectoriesExist(fs, root, y2013, oct, oct12);
    Assert.assertFalse("Delete should return false to indicate no changes",
        view.from(newMarker(2013, 11, 11)).to(newMarker(2013, 11, 12))
            .deleteAll());

    // delete October and the 2013 directory
    Assert.assertTrue("Delete should return true to indicate FS changed",
        view.of(newMarker(2013, 10, 12)).deleteAll());
    assertDirectoriesDoNotExist(fs, y2013, sep12, sep, oct12, oct, nov11, nov);
    assertDirectoriesExist(fs, root);
    Assert.assertFalse("Delete should return false to indicate no changes",
        view.of(newMarker(2013, 10, 12)).deleteAll());

    Assert.assertFalse("Delete should return false to indicate no changes",
        view.deleteAll());
  }

  @SuppressWarnings("deprecation")
  public static void assertDirectoriesExist(FileSystem fs, Path... dirs)
      throws IOException {
    for (Path path : dirs) {
      Assert.assertTrue("Directory should exist: " + path,
          fs.exists(path) && fs.isDirectory(path));
    }
  }

  public static void assertDirectoriesDoNotExist(FileSystem fs, Path... dirs)
      throws IOException {
    for (Path path : dirs) {
      Assert.assertTrue("Directory should not exist: " + path,
          !fs.exists(path));
    }
  }
}
