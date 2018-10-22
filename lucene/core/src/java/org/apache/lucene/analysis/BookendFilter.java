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
package org.apache.lucene.analysis;


import java.io.IOException;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * LUCENE-6624
 */
public class BookendFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private CharTermAttribute wrappedTermAtt;
  private boolean firstTokenProduced = false;
  private boolean lastTokenProduced = false;


  public static final String DEFAULT_START_TOKEN = "<start>";
  public static final String DEFAULT_END_TOKEN = "<end>";

  private final String startToken;
  private final String endToken;

  /**
   * Create a new BookendFilter, that add special markers at the beginning and at the end of the stream
   *
   * @param in TokenStream to filter
   */
  public BookendFilter(TokenStream in) {
    super(in);
    startToken = DEFAULT_START_TOKEN;
    endToken = DEFAULT_END_TOKEN;
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (!firstTokenProduced) {
      termAtt.setEmpty();
      termAtt.append(startToken);
      firstTokenProduced = true;
      return true;
    }
    if (input.incrementToken()) {
      // termAtt is good ?
      return true;
    } else if (!lastTokenProduced) {
      termAtt.setEmpty();
      termAtt.append(endToken);
      lastTokenProduced = true;
      return true;
    } else {
      return false;
    }
  }
}
