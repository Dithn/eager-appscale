/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package edu.ucsb.cs.eager.sa.kitty;

import junit.framework.TestCase;

public class APICallTest extends TestCase {

    public void testShortName1() {
        String name = "com.google.appengine.api.datastore.DataStoreService#get()";
        APICall call = new APICall(name);
        assertEquals(name, call.getName());
        assertEquals("bm_datastore_get", call.getId());
    }

    public void testShortName2() {
        String name = "foo.bar.SomeWeirdRandomService#op()";
        APICall call = new APICall(name);
        assertEquals(name, call.getName());
        try {
            assertEquals("bm_datastore_get", call.getId());
            fail("no exception thrown on unsupported API call name");
        } catch (Exception ignored) {
        }
    }
}
