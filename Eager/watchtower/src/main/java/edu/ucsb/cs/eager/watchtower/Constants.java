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

package edu.ucsb.cs.eager.watchtower;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class Constants {

    public static final String BM_CONTEXT_KIND = "BM_CONTEXT";
    public static final String BM_CONTEXT_KEY = "BM_CONTEXT";
    public static final String BM_CONTEXT_FIRST_RECORD = "FirstRecord";
    public static final String BM_CONTEXT_STOP_COLLECTION = "StopCollection";
    public static final String BM_CONTEXT_INITIALIZED = "Initialized";

    public static final String DATA_POINT_KIND = "DATA_POINT";
    public static final String DATA_POINT_TIMESTAMP = "Timestamp";

    public static final Key BM_CONTEXT_PARENT = KeyFactory.createKey("BMContextParent", "Root");
    public static final Key DATA_POINT_PARENT = KeyFactory.createKey("DataPointParent", "Root");

    public class Datastore {
        public static final String PUT = "bm_datastore_put";
        public static final String GET = "bm_datastore_get";
        public static final String DELETE = "bm_datastore_delete";
        public static final String AS_LIST = "bm_datastore_asList";

        public static final String AS_LIST_LIM_1 = "bm_datastore_asList_lim1";
        public static final String AS_LIST_LIM_10 = "bm_datastore_asList_lim10";
        public static final String AS_LIST_LIM_100 = "bm_datastore_asList_lim100";
        public static final String AS_LIST_LIM_1000 = "bm_datastore_asList_lim1000";
        public static final String AS_LIST_CS_1 = "bm_datastore_asList_cs1";
        public static final String AS_LIST_CS_10 = "bm_datastore_asList_cs10";
        public static final String AS_LIST_CS_100 = "bm_datastore_asList_cs100";
        public static final String AS_LIST_CS_1000 = "bm_datastore_asList_cs1000";
    }

    public class DatastoreJDO {
        public static final String MAKE_PERSISTENT = "bm_datastore_jdo_makePersistent";
        public static final String EXECUTE = "bm_datastore_jdo_execute";
        public static final String DELETE = "bm_datastore_jdo_delete";
    }
}
