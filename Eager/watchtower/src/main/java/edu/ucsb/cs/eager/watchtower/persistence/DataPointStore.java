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

import edu.ucsb.cs.eager.watchtower.DataPoint;

import javax.servlet.ServletContext;
import java.util.List;

public abstract class DataPointStore {

    public abstract boolean save(DataPoint p);

    public abstract List<DataPoint> getAll();

    public abstract List<DataPoint> getRange(long start, int limit);

    public abstract boolean restore(List<DataPoint> dataPoints);

    public abstract void close();

    public static DataPointStore init(ServletContext context) {
        String persistence = context.getInitParameter("dataPointStore");
        if ("elk".equals(persistence)) {
            return new ElasticSearchDataPointStore(context);
        }
        return new CloudDataPointStore();
    }

}
