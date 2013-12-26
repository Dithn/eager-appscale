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

package edu.ucsb.cs.eager.service;

import edu.ucsb.cs.eager.internal.EagerAPIManagementComponent;
import edu.ucsb.cs.eager.models.APIInfo;
import edu.ucsb.cs.eager.models.DependencyInfo;
import edu.ucsb.cs.eager.models.ValidationInfo;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.APIStatus;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;

import java.util.*;

public class EagerAdmin {

    public boolean isAPIAvailable(APIInfo api) throws APIManagementException {
        String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        APIProvider provider = getAPIProvider(eagerAdmin);
        APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
        return provider.isAPIAvailable(apiId);
    }

    public APIInfo[] getAPIsWithContext(String context) throws APIManagementException {
        String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        APIProvider provider = getAPIProvider(eagerAdmin);
        List<API> apiList = provider.getAllAPIs();
        List<APIInfo> results = new ArrayList<APIInfo>();
        for (API api : apiList) {
            if (api.getContext().equals(context)) {
                results.add(new APIInfo(api.getId()));
            }
        }
        return results.toArray(new APIInfo[results.size()]);
    }

    /**
     * Record the dependencies of an API
     *
     * @param api Dependent API
     * @param dependencies An array of DependencyInfo objects, one per dependency
     * @return a boolean value indicating success or failure
     */
    public boolean recordDependencies(APIInfo api, DependencyInfo[] dependencies) {
        return true;
    }

    /**
     * Get the information required to perform dependency checking.
     *
     * @param api A potential dependency API
     * @return A ValidationInfo object carrying the specification of the API and its dependents
     */
    public ValidationInfo getValidationInfo(APIInfo api) {
        return null;
    }

    public boolean createAPI(APIInfo api, String specification) throws APIManagementException {
        if (isAPIAvailable(api)) {
            return false;
        }

        String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        APIProvider provider = getAPIProvider(eagerAdmin);
        APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
        API newAPI = new API(apiId);
        newAPI.setContext("/" + api.getName().toLowerCase());
        newAPI.setUrl("http://eager4appscale.com");
        newAPI.setStatus(APIStatus.CREATED);

        String[] methods = new String[] {
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        };
        Set<URITemplate> templates = new HashSet<URITemplate>();
        for (String method : methods) {
            URITemplate template = new URITemplate();
            template.setHTTPVerb(method);
            template.setUriTemplate("/*");
            template.setAuthType(APIConstants.AUTH_APPLICATION_OR_USER_LEVEL_TOKEN);
            template.setResourceURI("http://eager4appscale.com");
            templates.add(template);
        }
        newAPI.setUriTemplates(templates);
        newAPI.setLastUpdated(new Date());
        provider.addAPI(newAPI);
        return true;
    }

    public boolean publishAPI(APIInfo api, String url) throws APIManagementException {
        if (!isAPIAvailable(api)) {
            return false;
        }

        String eagerAdmin = EagerAPIManagementComponent.getEagerAdmin();
        APIProvider provider = getAPIProvider(eagerAdmin);
        APIIdentifier apiId = new APIIdentifier(eagerAdmin, api.getName(), api.getVersion());
        API existingAPI = provider.getAPI(apiId);
        if (existingAPI.getStatus() != APIStatus.PUBLISHED) {
            existingAPI.setUrl(url);
            for (URITemplate template : existingAPI.getUriTemplates()) {
                template.setResourceURI(url);
            }
            existingAPI.setLastUpdated(new Date());
            provider.updateAPI(existingAPI);
            provider.changeAPIStatus(existingAPI, APIStatus.PUBLISHED, eagerAdmin, true);
            return true;
        }
        return false;
    }

    private APIProvider getAPIProvider(String providerName) throws APIManagementException {
        return APIManagerFactory.getInstance().getAPIProvider(providerName);
    }

}