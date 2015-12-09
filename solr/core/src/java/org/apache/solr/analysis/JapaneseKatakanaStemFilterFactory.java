package org.apache.solr.analysis;

/**
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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.solr.common.SolrException;

import java.util.Map;

/**
 * Factory for {@link JapaneseKatakanaStemFilterFactory}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ja" class="solr.TextField"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.JapaneseTokenizerFactory"/&gt;
 *     &lt;filter class="solr.JapaneseKatakanaStemFilterFactory"
 *             minimumLength="4"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class JapaneseKatakanaStemFilterFactory extends BaseTokenFilterFactory {
  private static final String MINIMUM_LENGTH_PARAM = "minimumLength";
  private int minimumLength;
  
  //@Override
  public void init(Map<String, String> args) {
    super.init(args);
    minimumLength = getInt(MINIMUM_LENGTH_PARAM, JapaneseKatakanaStemFilter.DEFAULT_MINIMUM_LENGTH);
    if (minimumLength < 2) {
      throw new SolrException(SolrException.ErrorCode.UNKNOWN,
                              "Illegal " + MINIMUM_LENGTH_PARAM + " " + minimumLength + " (must be 2 or greater)");
    }
  }

  public TokenStream create(TokenStream input) {
    return new JapaneseKatakanaStemFilter(input, minimumLength);
  }
}
