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

import edu.ucsb.cs.eager.sa.kitty.qbets.QBETSConfig;
import edu.ucsb.cs.eager.sa.kitty.simulation.SimulationConfig;
import junit.framework.TestCase;

import java.util.Collection;

public class KittyTest extends TestCase {

    public void testGetMethods() throws Exception {
        PredictionConfig config = new PredictionConfig();
        config.setClazz("net.eager.testing.TestClass3");
        config.setWholeProgramMode(true);
        Kitty kitty = new Kitty();
        Collection<MethodInfo> methods = kitty.getMethods(config);
        assertEquals(4, methods.size()); // <init>, main, doQuery, test.method1

        config.setMethods(new String[]{"doQuery"});
        methods = kitty.getMethods(config);
        assertEquals(1, methods.size());
    }

    public void testPredictionConfig1() throws Exception {
        PredictionConfig config = getConfig(new String[]{
                "-ccp", "/test/classpath",
                "-c", "edu.ucsb.cs.StudentResource",
                "-s", "http://localhost:8081",
                "-wp",
                "-q", "0.96",
                "-cn", "0.01",
                "-st", "100",
                "-en", "200",
                "-me", "1000",
                "-d1",
        });

        QBETSConfig qc = config.getQbetsConfig();
        assertNotNull(qc);
        assertEquals(100L, qc.getStart());
        assertEquals(200L, qc.getEnd());
        assertEquals(0.96, qc.getQuantile());
        assertEquals(0.01, qc.getConfidence());
        assertEquals("http://localhost:8081", qc.getBenchmarkDataSvc());
        assertTrue(qc.isDisableApproach1());

        assertNull(config.getSimulationConfig());
        assertFalse(config.isSimplePredictor());
        assertTrue(config.isWholeProgramMode());
        assertEquals("/test/classpath", config.getCerebroClasspath());
        assertEquals("edu.ucsb.cs.StudentResource", config.getClazz());
        assertEquals(1000, config.getMaxEntities());
    }

    public void testPredictionConfig2() throws Exception {
        PredictionConfig config = getConfig(new String[]{
                "-ccp", "/test/classpath",
                "-c", "edu.ucsb.cs.StudentResource",
                "-b", "test/path",
                "-wp",
                "-sn", "155",
        });

        SimulationConfig sc = config.getSimulationConfig();
        assertNotNull(sc);
        assertEquals("test/path", sc.getBenchmarkDataDir());
        assertEquals(155, sc.getSimulations());

        assertNull(config.getQbetsConfig());
    }

    public void testPredictionConfig3() throws Exception {
        try {
            getConfig(new String[]{
                    "-ccp", "/test/classpath",
                    "-c", "edu.ucsb.cs.StudentResource",
                    "-wp",
            });
            fail("no validation error thrown");
        } catch (PredictionConfigException ignore) {
        }
    }

    private PredictionConfig getConfig(String[] args) throws PredictionConfigException {
        PredictionConfigMaker configMaker = new PredictionConfigMaker();
        return configMaker.construct(args, "TestBinary");
    }

}
