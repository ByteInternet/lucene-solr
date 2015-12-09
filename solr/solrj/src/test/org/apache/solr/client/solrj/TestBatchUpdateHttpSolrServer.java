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
package org.apache.solr.client.solrj;

import org.apache.solr.SolrJettyTestBase;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.RequestWriter;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.util.ExternalPaths;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

/**
 * Test for SOLR-1038
 */
public class TestBatchUpdateHttpSolrServer extends SolrJettyTestBase {

  @BeforeClass
  public static void beforeTest() throws Exception {
    createJetty(ExternalPaths.EXAMPLE_HOME, null, null);
  }

  static final int numdocs = 1000;  

  @Override
  public SolrServer createNewSolrServer()
  {
    try {
      // setup the server...
      String url = "http://localhost:"+port+context;
      HttpSolrServer s = new HttpSolrServer( url );
      s.setConnectionTimeout(100); // 1/10th sec
      s.setDefaultMaxConnectionsPerHost(100);
      s.setMaxTotalConnections(100);
      return s;
    }
    catch( Exception ex ) {
      throw new RuntimeException( ex );
    }
  }


  @Test
  public void testWithXml() throws Exception {
    HttpSolrServer solrServer = (HttpSolrServer) getSolrServer();
    solrServer.setRequestWriter(new RequestWriter());
    solrServer.deleteByQuery( "*:*" ); // delete everything!    
    doIt(solrServer);
  }

  @Test
  public void testWithBinary()throws Exception{
    HttpSolrServer solrServer = (HttpSolrServer) getSolrServer();
    solrServer.setRequestWriter(new BinaryRequestWriter());
    solrServer.deleteByQuery( "*:*" ); // delete everything!
    doIt(solrServer);
  }

  @Test
  public void testWithBinaryBean()throws Exception{
    HttpSolrServer solrServer = (HttpSolrServer) getSolrServer();
    solrServer.setRequestWriter(new BinaryRequestWriter());
    solrServer.deleteByQuery( "*:*" ); // delete everything!
    final int[] counter = new int[1];
    counter[0] = 0;
    solrServer.addBeans(new Iterator<Bean>() {

      public boolean hasNext() {
        return counter[0] < numdocs;
      }

      public Bean next() {
        Bean bean = new Bean();
        bean.id = "" + (++counter[0]);
        bean.cat = "foocat";
        return bean;
      }

      public void remove() {
        //do nothing
      }
    });
    solrServer.commit();
    SolrQuery query = new SolrQuery("*:*");
    QueryResponse response = solrServer.query(query);
    assertEquals(0, response.getStatus());
    assertEquals(numdocs, response.getResults().getNumFound());
  }

  public static class Bean{
    @Field
    String id;
    @Field
    String cat;
  }
       
  private void doIt(HttpSolrServer solrServer) throws SolrServerException, IOException {
    final int[] counter = new int[1];
    counter[0] = 0;
    solrServer.add(new Iterator<SolrInputDocument>() {

      public boolean hasNext() {
        return counter[0] < numdocs;
      }

      public SolrInputDocument next() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "" + (++counter[0]));
        doc.addField("cat", "foocat");
        return doc;
      }

      public void remove() {
        //do nothing

      }
    });
    solrServer.commit();
    SolrQuery query = new SolrQuery("*:*");
    QueryResponse response = solrServer.query(query);
    assertEquals(0, response.getStatus());
    assertEquals(numdocs, response.getResults().getNumFound());
  }
}
