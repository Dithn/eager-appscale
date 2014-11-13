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

public class BenchmarkContext {

    private static final BenchmarkContext instance = new BenchmarkContext();

    private boolean firstRecord = true;
    private boolean collectionStopped = false;

    private BenchmarkContext(){
    }

    public static BenchmarkContext getInstance() {
        return instance;
    }

    public boolean isFirstRecord() {
        return firstRecord;
    }

    public void setFirstRecord(boolean firstRecord) {
        this.firstRecord = firstRecord;
    }

    public boolean isCollectionStopped() {
        return collectionStopped;
    }

    public void setCollectionStopped(boolean collectionStopped) {
        this.collectionStopped = collectionStopped;
    }
}
