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
package org.apache.solr.handler;

import org.apache.commons.lang.ObjectUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.BufferingRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.util.AbstractSolrTestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.ContentStreamBase;
import org.junit.Test;

public class XmlUpdateRequestHandlerTest extends AbstractSolrTestCase 
{
  private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
  protected XmlUpdateRequestHandler handler;

@Override public String getSchemaFile() { return "schema.xml"; }
@Override public String getSolrConfigFile() { return "solrconfig.xml"; }

  @Override 
  public void setUp() throws Exception {
    super.setUp();
    handler = new XmlUpdateRequestHandler();
  }
  
  @Override 
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testReadDoc() throws Exception
  {
    String xml = 
      "<doc boost=\"5.5\">" +
      "  <field name=\"id\" boost=\"2.2\">12345</field>" +
      "  <field name=\"name\">kitten</field>" +
      "  <field name=\"cat\" boost=\"3\">aaa</field>" +
      "  <field name=\"cat\" boost=\"4\">bbb</field>" +
      "  <field name=\"cat\" boost=\"5\">bbb</field>" +
      "  <field name=\"ab\">a&amp;b</field>" +
      "</doc>";

    XMLStreamReader parser = 
      inputFactory.createXMLStreamReader( new StringReader( xml ) );
    parser.next(); // read the START document...
    //null for the processor is all right here
    XMLLoader loader = new XMLLoader(null, inputFactory);
    SolrInputDocument doc = loader.readDoc( parser );
    
    // Read boosts
    assertEquals( 5.5f, doc.getDocumentBoost(), 0.1);
    assertEquals( 1.0f, doc.getField( "name" ).getBoost(), 0.1);
    assertEquals( 2.2f, doc.getField( "id" ).getBoost(), 0.1);
    // Boost is the product of each value
    assertEquals( (3*4*5.0f), doc.getField( "cat" ).getBoost(), 0.1);
    
    // Read values
    assertEquals( "12345", doc.getField( "id" ).getValue() );
    assertEquals( "kitten", doc.getField( "name").getValue() );
    assertEquals( "a&b", doc.getField( "ab").getValue() ); // read something with escaped characters
    
    Collection<Object> out = doc.getField( "cat" ).getValues();
    assertEquals( 3, out.size() );
    assertEquals( "[aaa, bbb, bbb]", out.toString() );
  }

  @Test
  public void testRequestParams() throws Exception
  {
    String xml = 
      "<add>" +
      "  <doc>" +
      "    <field name=\"id\">12345</field>" +
      "    <field name=\"name\">kitten</field>" +
      "  </doc>" +
      "</add>";

    SolrQueryRequest req = req("commitWithin","100","overwrite","false");
    SolrQueryResponse rsp = new SolrQueryResponse();
    BufferingRequestProcessor p = new BufferingRequestProcessor(null);

    XMLLoader loader = new XMLLoader(p, inputFactory);
    loader.load(req, rsp, new ContentStreamBase.StringStream(xml));

    AddUpdateCommand add = p.addCommands.get(0);
    assertEquals(100, add.commitWithin);
    assertEquals(true, add.allowDups);
    req.close();
  }
  
  @Test
  public void testReadDelete() throws Exception {
	    String xml =
	      "<update>" +
	      " <delete>" +
	      "   <query>id:150</query>" +
	      "   <id>150</id>" +
	      "   <id>200</id>" +
	      "   <query>id:200</query>" +
	      " </delete>" +
	      " <delete commitWithin=\"500\">" +
	      "   <query>id:150</query>" +
	      " </delete>" +
	      " <delete fromPending=\"false\">" +
	      "   <id>150</id>" +
	      " </delete>" +
	      " <delete fromCommitted=\"false\">" +
	      "   <id>150</id>" +
	      " </delete>" +
	      "</update>";
	    
	    MockUpdateRequestProcessor p = new MockUpdateRequestProcessor(null);
	    p.expectDelete(null, "id:150", true, true, -1);
	    p.expectDelete("150", null, true, true, -1);
	    p.expectDelete("200", null, true, true, -1);
	    p.expectDelete(null, "id:200", true, true, -1);
	    p.expectDelete(null, "id:150", true, true, 500);
	    p.expectDelete("150", null, false, true, -1);
	    p.expectDelete("150", null, true, false, -1);

	    XMLLoader loader = new XMLLoader(p, inputFactory);
	    loader.load(req(), new SolrQueryResponse(), new ContentStreamBase.StringStream(xml));
	    
	    p.assertNoCommandsPending();
	  }
	  
	  private class MockUpdateRequestProcessor extends UpdateRequestProcessor {
	    
	    private Queue<DeleteUpdateCommand> deleteCommands = new LinkedList<DeleteUpdateCommand>();
	    
	    public MockUpdateRequestProcessor(UpdateRequestProcessor next) {
	      super(next);
	    }
	    
	    public void expectDelete(String id, String query, boolean fromPending, boolean fromCommitted, int commitWithin) {
	      DeleteUpdateCommand cmd = new DeleteUpdateCommand();
	      cmd.id = id;
	      cmd.query = query;
	      cmd.fromCommitted = fromCommitted;
	      cmd.fromPending = fromPending;
	      cmd.commitWithin = commitWithin;
	      deleteCommands.add(cmd);
	    }
	    
	    public void assertNoCommandsPending() {
	      assertTrue(deleteCommands.isEmpty());
	    }
	    
	    @Override
	    public void processDelete(DeleteUpdateCommand cmd) throws IOException {
	      DeleteUpdateCommand expected = deleteCommands.poll();
	      assertNotNull("Unexpected delete command: [" + cmd + "]", expected);
	      assertTrue("Expected [" + expected + "] but found [" + cmd + "]",
	          ObjectUtils.equals(expected.id, cmd.id) &&
	          ObjectUtils.equals(expected.query, cmd.query) &&
	          expected.fromPending==cmd.fromPending &&
	          expected.fromCommitted==cmd.fromCommitted &&
	          expected.commitWithin==cmd.commitWithin);
	    }
	  }

}
