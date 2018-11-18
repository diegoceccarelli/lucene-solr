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
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.English;

public class TestBookendFilter extends BaseTokenStreamTestCase {

  private void assertNextTokenEquals(TokenStream stream, CharTermAttribute termAtt, String expectedToken) throws IOException {
    stream.incrementToken();
    assertEquals(expectedToken, termAtt.toString());
  }

  public void testExactMatch() throws IOException {
    StringReader reader = new StringReader("Now is The Time");
    final MockTokenizer in = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    in.setReader(reader);
    TokenStream stream = new BookendFilter(in, "<start>", "<end>");
    CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
    stream.reset();
    assertNextTokenEquals(stream, termAtt, "<start>");
    assertNextTokenEquals(stream, termAtt, "Now");
    assertNextTokenEquals(stream, termAtt, "is");
    assertNextTokenEquals(stream, termAtt, "The");
    assertNextTokenEquals(stream, termAtt, "Time");
    assertNextTokenEquals(stream, termAtt, "<end>");
  }

  public void testExactMatchWithDefaultMarkerTokens() throws IOException {
    StringReader reader = new StringReader("Now is The Time");
    final MockTokenizer in = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    in.setReader(reader);
    TokenStream stream = new BookendFilter(in);
    CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
    stream.reset();
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_START_TOKEN);
    assertNextTokenEquals(stream, termAtt, "Now");
    assertNextTokenEquals(stream, termAtt, "is");
    assertNextTokenEquals(stream, termAtt, "The");
    assertNextTokenEquals(stream, termAtt, "Time");
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_END_TOKEN);
  }

  public void testExactMatchWithStopWordsAndLowercase() throws IOException {
    StringReader reader = new StringReader("Now is the Time");
    final MockTokenizer in = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    in.setReader(reader);
    LowerCaseFilter lowercaseFilter = new LowerCaseFilter(in);
    // add a stopword filter that will take away the stop words is and the
    StopFilter stopFilter = new StopFilter(lowercaseFilter, StopFilter.makeStopSet(new String[] {"is", "the"}));
    TokenStream stream = new BookendFilter(stopFilter); // chain with the stopFilter
    CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
    stream.reset();
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_START_TOKEN);
    assertNextTokenEquals(stream, termAtt, "now");
    assertNextTokenEquals(stream, termAtt, "time");
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_END_TOKEN);
  }

  public void testExactMatchWithEmptyString() throws IOException {
    StringReader reader = new StringReader("");
    final MockTokenizer in = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    in.setReader(reader);
    TokenStream stream = new BookendFilter(in); // chain with the stopFilter
    CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
    stream.reset();
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_START_TOKEN);
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_END_TOKEN);
  }

  public void testExactMatchWithEmptyStringAfterFilters() throws IOException {
    StringReader reader = new StringReader("This is going to be EMPTY");
    final MockTokenizer in = new MockTokenizer(MockTokenizer.WHITESPACE, false);
    in.setReader(reader);
    LowerCaseFilter lowercaseFilter = new LowerCaseFilter(in);
    // add all the words in reader as stop words
    StopFilter stopFilter = new StopFilter(lowercaseFilter, StopFilter.makeStopSet(new String[] {"this", "is", "going", "to", "be", "empty"}));
    TokenStream stream = new BookendFilter(stopFilter); // chain with the stopFilter
    CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
    stream.reset();
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_START_TOKEN);
    assertNextTokenEquals(stream, termAtt, BookendFilter.DEFAULT_END_TOKEN);
  }


}