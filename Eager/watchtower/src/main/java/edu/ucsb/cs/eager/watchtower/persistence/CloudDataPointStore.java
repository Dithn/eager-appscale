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

package edu.ucsb.cs.eager.watchtower.persistence;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Transaction;
import edu.ucsb.cs.eager.watchtower.DataPoint;

import javax.servlet.ServletContext;

public class CloudDataPointStore extends DataPointStore {

    public CloudDataPointStore(ServletContext context) {
        super(context);
    }

    @Override
    public boolean save(DataPoint p) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();
        boolean result = true;
        try {
            datastore.put(txn, CloudDataPointStoreUtils.toEntity(p));
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                result = false;
            }
        }
        return result;
    }
}
