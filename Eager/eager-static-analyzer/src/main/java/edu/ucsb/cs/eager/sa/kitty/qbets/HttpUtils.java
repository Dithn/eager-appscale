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

package edu.ucsb.cs.eager.sa.kitty.qbets;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class HttpUtils {

    public static JSONObject doPost(String url, JSONObject msg) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost request = new HttpPost(url);
            StringEntity params = new StringEntity(msg.toString());
            request.addHeader("content-type", "application/json");
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            InputStream in = response.getEntity().getContent();
            StringBuilder sb = new StringBuilder();
            byte[] data = new byte[1024];
            int len;
            while ((len = in.read(data)) != -1) {
                sb.append(new String(data, 0, len));
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException(sb.toString());
            }
            return new JSONObject(sb.toString());
        } finally {
            httpClient.close();
        }
    }
}
