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
    public StudentListSummary getStudents() {
        long start = System.currentTimeMillis();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Student");
        int count = 0;
        List<Entity> results = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for (Entity r : results) {
            count++;
        }
        StudentListSummary summary = new StudentListSummary();
        summary.setCount(count);
        summary.setTime(0);
        long end = System.currentTimeMillis();
        TimingResource.saveTime(end - start);
        return summary;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addStudent(@FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {

        long start = System.currentTimeMillis();
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
        Response response = Response.created(URI.create("/" + studentId)).entity(s).build();
        long end = System.currentTimeMillis();
        TimingResource.saveTime(end - start);
        return response;
    }

    @GET
    @Path("/{studentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudent(@PathParam("studentId") String studentId) {
        long start = System.currentTimeMillis();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", studentId);
        Response response;
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setStudentId(studentId);
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            response = Response.ok(s).build();
        } catch (EntityNotFoundException e) {
            response = Response.status(404).build();
        } finally {
            long end = System.currentTimeMillis();
            TimingResource.saveTime(end - start);
        }
        return response;
    }

    @DELETE
    @Path("/{studentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteStudent(@PathParam("studentId") String studentId) {
        long start = System.currentTimeMillis();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", studentId);
        Response response;
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setStudentId(studentId);
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            datastore.delete(key);
            response = Response.ok(s).build();
        } catch (EntityNotFoundException e) {
            response = Response.status(404).build();
        } finally {
            long end = System.currentTimeMillis();
            TimingResource.saveTime(end - start);
        }
        return response;
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public StudentListSummary deleteAll() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Student");
        int count = 0;
        List<Entity> results = datastore.prepare(query).asList(
                FetchOptions.Builder.withChunkSize(100));
        List<Key> keys = new ArrayList<Key>();
        for (Entity r : results) {
            keys.add(r.getKey());
            count++;
        }

        long apiStart = System.currentTimeMillis();
        datastore.delete(keys);
        long apiEnd = System.currentTimeMillis();

        StudentListSummary summary = new StudentListSummary();
        summary.setCount(count);
        summary.setTime(apiEnd - apiStart);
        return summary;
    }

}
