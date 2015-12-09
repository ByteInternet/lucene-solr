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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.util.IOUtils;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;
import org.apache.solr.util.plugin.ResourceLoaderAware;

/**
 * Factory for {@link org.apache.lucene.analysis.ja.JapaneseTokenizer}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_ja" class="solr.TextField"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.JapaneseTokenizerFactory"
 *       mode=NORMAL
 *       userDictionary=user.txt
 *       userDictionaryEncoding=UTF-8
 *     /&gt;
 *     &lt;filter class="solr.JapaneseBaseFormFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;
 * </pre>
 */
public class JapaneseTokenizerFactory extends BaseTokenizerFactory implements ResourceLoaderAware {
  private static final String MODE = "mode";
  
  private static final String USER_DICT_PATH = "userDictionary";
  
  private static final String USER_DICT_ENCODING = "userDictionaryEncoding";

  private UserDictionary userDictionary;
  private Mode mode;
  
  //@Override
  public void inform(ResourceLoader loader) {
    mode = getMode(args);
    String userDictionaryPath = args.get(USER_DICT_PATH);
    try {
      if (userDictionaryPath != null) {
        InputStream stream = loader.openResource(userDictionaryPath);
        String encoding = args.get(USER_DICT_ENCODING);
        if (encoding == null) {
          encoding = IOUtils.UTF_8;
        }
        CharsetDecoder decoder = Charset.forName(encoding).newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        Reader reader = new InputStreamReader(stream, decoder);
        userDictionary = new UserDictionary(reader);
      } else {
        userDictionary = null;
      }
    } catch (Exception e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
    }
  }
  
  //@Override
  public Tokenizer create(Reader input) {
    return new JapaneseTokenizer(input, userDictionary, true, mode);
  }
  
  private Mode getMode(Map<String, String> args) {
    String mode = args.get(MODE);
    if (mode != null) {
      return Mode.valueOf(mode.toUpperCase(Locale.ENGLISH));
    } else {
      return JapaneseTokenizer.DEFAULT_MODE;
    }
  }
}
