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

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;

@Path("/test_jaxrs")
public class InMemoryStudentResource {

    /**
     * @responseMessage 201 Student resource created
     * @responseMessage 404 {edu.ucsb.cs.Error} Student not found
     * @output edu.ucsb.cs.Student
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response addStudent(@FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {
        String studentId = UUID.randomUUID().toString();
        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setStudentId(studentId);
        StudentDB.getInstance().setStudent(studentId, s);
        return Response.created(URI.create("/" + studentId)).entity(s).build();
    }

    /**
     * @responseMessage 200 Student resource found
     * @responseMessage 404 {edu.ucsb.cs.Error} Student not found
     * @output edu.ucsb.cs.Student
     */
    @GET
    @Path("/{studentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStudent(@PathParam("studentId") String studentId) {
        long start = System.currentTimeMillis();
        Response resp;
        try {
            Student s = StudentDB.getInstance().getStudent(studentId);
            if (s != null) {
                resp = Response.ok(s).build();
            } else {
                resp = Response.status(404).build();
            }
        } finally {
            long end = System.currentTimeMillis();
            long total = end - start;
            TimeValues.getInstance().setTimes(0, total);
        }
        return resp;
    }

}
