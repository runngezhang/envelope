/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.labs.envelope.partition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.spark.HashPartitioner;
import org.apache.spark.Partitioner;
import org.apache.spark.RangePartitioner;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.junit.Test;

import com.cloudera.labs.envelope.spark.Contexts;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TestPartitionerFactory {

  @Test
  public void testHash() throws Exception {
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put("type", "hash");
    
    JavaPairRDD<Row, Row> base = getDummyRDD(10);
    Config config = ConfigFactory.parseMap(configMap);
    Partitioner p = PartitionerFactory.create(config, base);
    
    assertTrue(p instanceof HashPartitioner);
    assertEquals(p.numPartitions(), 10);
  }
  
  @Test
  public void testRange() throws Exception {
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put("type", "range");
    
    JavaPairRDD<Row, Row> base = getDummyRDD(10);
    Config config = ConfigFactory.parseMap(configMap);
    Partitioner p = PartitionerFactory.create(config, base);
    
    assertTrue(p instanceof RangePartitioner);
    assertEquals(p.numPartitions(), 10);
  }
  
  @Test
  public void testCustom() throws Exception {
    Map<String, Object> configMap = Maps.newHashMap();
    configMap.put("type", "com.cloudera.labs.envelope.partition.DummyPartitioner");
    
    JavaPairRDD<Row, Row> base = getDummyRDD(10);
    Config config = ConfigFactory.parseMap(configMap);
    DummyPartitioner p = (DummyPartitioner)PartitionerFactory.create(config, base);
    
    assertTrue(p instanceof DummyPartitioner);
    assertTrue(p.hasBeenConfigured());
  }
  
  private JavaPairRDD<Row, Row> getDummyRDD(int numPartitions) {
    return Contexts.getSparkSession().range(numPartitions).javaRDD()
        .map(new LongToRowFunction()).keyBy(new ItselfFunction<Row>()).repartition(numPartitions);
  }
  
  @SuppressWarnings("serial")
  private static class LongToRowFunction implements Function<Long, Row> {
    @Override
    public Row call(Long longValue) throws Exception {
      return RowFactory.create(longValue);
    }
  }
  
  @SuppressWarnings("serial")
  private static class ItselfFunction<T> implements Function<T, T> {
    @Override
    public T call(T v1) throws Exception {
      return v1;
    }
  }
}
