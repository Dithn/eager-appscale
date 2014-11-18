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

public class PathTest extends TestCase {

    public void testPathEquivalence1() {
        Path p1 = new Path();
        p1.add(new APICall("c1"));
        p1.add(new APICall("c2"));
        p1.add(new APICall("c3"));

        Path p2 = new Path();
        p2.add(new APICall("c1"));
        p2.add(new APICall("c2"));
        p2.add(new APICall("c3"));

        Path p3 = new Path();
        p3.add(new APICall("c1"));
        p3.add(new APICall("c2"));

        Path p4 = new Path();
        p4.add(new APICall("c1"));
        p4.add(new APICall("c2"));
        p4.add(new APICall("c3"));
        p4.add(new APICall("c4"));

        Path p5 = new Path();
        p5.add(new APICall("c3"));
        p5.add(new APICall("c1"));
        p5.add(new APICall("c2"));

        // self equivalence
        assertTrue(p1.equivalent(p1));
        assertTrue(p2.equivalent(p2));
        assertTrue(p3.equivalent(p3));
        assertTrue(p4.equivalent(p4));
        assertTrue(p5.equivalent(p5));

        assertTrue(p1.equivalent(p2));
        assertTrue(p2.equivalent(p1));

        assertFalse(p1.equivalent(p3));
        assertFalse(p3.equivalent(p1));

        assertFalse(p1.equivalent(p4));
        assertFalse(p4.equivalent(p1));

        assertTrue(p1.equivalent(p5));
        assertTrue(p5.equivalent(p1));
    }

    public void testPathEquivalence2() {
        Path p1 = new Path();
        p1.add(new APICall("c1"));
        p1.add(new APICall("c1"));
        p1.add(new APICall("c2"));

        Path p2 = new Path();
        p2.add(new APICall("c1"));
        p2.add(new APICall("c2"));
        p2.add(new APICall("c1"));

        assertTrue(p1.equivalent(p2));
        assertTrue(p2.equivalent(p1));
    }

    public void testPathId() {
        Path p1 = new Path();
        p1.add(new APICall("c1"));
        p1.add(new APICall("c2"));
        p1.add(new APICall("c3"));
        p1.add(new APICall("c2"));
        assertEquals("c1:c2:c2:c3", p1.getId());
    }
}
