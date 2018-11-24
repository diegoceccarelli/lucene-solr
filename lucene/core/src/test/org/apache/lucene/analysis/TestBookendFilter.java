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

import com.sun.tools.internal.xjc.generator.util.WhitespaceNormalizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TestIndexWriterExceptions;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

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

  public void testQueryWithBookFilter() throws IOException {
    Directory dir = newDirectory();

    Analyzer analyzer = new Analyzer(Analyzer.GLOBAL_REUSE_STRATEGY) {
      @Override
      public TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream stream = tokenizer;
        stream = new BookendFilter(stream);
        return new TokenStreamComponents(tokenizer, stream);
      }
    };
    IndexWriterConfig conf = newIndexWriterConfig(analyzer).setMergePolicy(NoMergePolicy.INSTANCE);
    RandomIndexWriter w = new RandomIndexWriter(random(), dir, conf);

    // segment that contains the term
    Document doc = new Document();
    doc.add(new TextField("title", "jonathan livingston seagull", Field.Store.NO));
    w.addDocument(doc);
    // segment that does not contain the term
    doc = new Document();
    doc.add(new TextField("title", "jonathan seagull", Field.Store.NO));
    w.addDocument(doc);
    // segment that does not contain the field
    w.addDocument(new Document());
    w.commit();

    DirectoryReader reader = w.getReader();
    IndexSearcher searcher = newSearcher(reader);

    BooleanQuery.Builder query = new BooleanQuery.Builder();
    query.add(new BooleanClause(new TermQuery(new Term("title", "jonathan")), BooleanClause.Occur.MUST));
    query.add(new BooleanClause(new TermQuery(new Term("title", "seagull")), BooleanClause.Occur.MUST));

    TopFieldDocs topDocs = searcher.search(query.build(), 3, Sort.RELEVANCE);
    System.out.println(topDocs.totalHits);
    reader.close();
    w.close();
    dir.close();
  }

}