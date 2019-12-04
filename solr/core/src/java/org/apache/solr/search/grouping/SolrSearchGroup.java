/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.grouping.SearchGroup;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.grouping.distributed.shardresultserializer.ShardResultTransformerUtils;

public class SolrSearchGroup<T> extends SearchGroup<T> {


  public SolrSearchGroup(SearchGroup<T> searchGroup){
    this.groupValue = searchGroup.groupValue;
    this.sortValues = searchGroup.sortValues;
  }

  public Object serialize(SortField[] groupSortField, IndexSchema schema){
    Object[] convertedSortValues = new Object[sortValues.length];
    for (int i = 0; i < sortValues.length; i++) {
      Object sortValue = sortValues[i];
      SchemaField field = groupSortField[i].getField() != null ?
          schema.getFieldOrNull(groupSortField[i].getField()) : null;
      convertedSortValues[i] = ShardResultTransformerUtils.marshalSortValue(sortValue, field);
    }
    return convertedSortValues;
  }


  public String getGroupValue(SchemaField field){
    if ( null == groupValue) return null;
    if ( ! (groupValue instanceof BytesRef)) {
      return null;
    }
    return field.getType().indexedToReadable((BytesRef)groupValue, new CharsRefBuilder()).toString();
  }
}
