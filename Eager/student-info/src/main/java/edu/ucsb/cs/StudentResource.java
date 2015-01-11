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
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/students")
public class StudentResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StudentListSummary getStudents(@DefaultValue("1") @QueryParam("chunkSize") int chunkSize) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Student");
        int count = 0;
        long apiStart = System.currentTimeMillis();
        List<Entity> results = datastore.prepare(query).asList(
                FetchOptions.Builder.withChunkSize(chunkSize));
        for (Entity r : results) {
            count++;
        }
        long apiEnd = System.currentTimeMillis();
        StudentListSummary summary = new StudentListSummary();
        summary.setCount(count);
        summary.setTime(apiEnd - apiStart);
        return summary;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addStudent(@FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {

        String studentId = UUID.randomUUID().toString();
        Entity student = new Entity("Student", studentId);
        student.setProperty("firstName", firstName);
        student.setProperty("lastName", lastName);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(student);

        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setStudentId(studentId);
        return Response.created(URI.create("/" + studentId)).entity(s).build();
    }

    @GET
    @Path("/{studentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudent(@PathParam("studentId") String studentId) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", studentId);
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setStudentId(studentId);
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            return Response.ok(s).build();
        } catch (EntityNotFoundException e) {
            return Response.status(404).build();
        }
    }
}
