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

import java.util.Collection;

public class KittyTest extends TestCase {

    public void testGetMethods() throws Exception {
        PredictionConfig config = new PredictionConfig();
        config.setClazz("net.eager.testing.TestClass3");
        config.setWholeProgramMode(true);
        Kitty kitty = new Kitty();
        Collection<MethodInfo> methods = kitty.getMethods(config);
        assertEquals(4, methods.size());
    }

}
