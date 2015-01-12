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

package edu.ucsb.cs;

import com.google.appengine.api.datastore.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/timing")
public class TimingResource {

    private static final Key TIMING_VALUE_KEY = KeyFactory.createKey("TimingValue", "TimingValue");
    private static final String TIME = "Time";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public long getTime() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        try {
            Entity entity = datastore.get(TIMING_VALUE_KEY);
            return (Long) entity.getProperty(TIME);
        } catch (EntityNotFoundException e) {
            return -1;
        }
    }

    public static void saveTime(long time) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity entity = new Entity(TIMING_VALUE_KEY);
        entity.setProperty(TIME, time);
        datastore.put(entity);
    }
}
