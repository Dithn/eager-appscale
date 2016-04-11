package edu.ucsb.cs;

import com.google.appengine.api.datastore.*;
import com.google.appengine.repackaged.com.google.common.base.Strings;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

public class MultiPathStudentServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {

        String operation = req.getParameter("op");
        if ("create".equals(operation)) {
            addStudent(req, resp);
        } else if ("get".equals(operation)) {
            getStudent(req, resp);
        } else {
            reportError(400, "Operation is required", resp);
        }
    }

    private void getStudent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentId = req.getParameter("studentId");
        if (Strings.isNullOrEmpty(studentId)) {
            reportError(400, "Student ID is required", resp);
            return;
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", studentId);
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setStudentId(studentId);
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));

            resp.setStatus(200);
            resp.setContentType("application/json");
            resp.getOutputStream().print(gson.toJson(s));
        } catch (EntityNotFoundException e) {
            reportError(404, "No such student", resp);
        }
    }

    private void addStudent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String studentId = UUID.randomUUID().toString();
        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        if (Strings.isNullOrEmpty(firstName) || Strings.isNullOrEmpty(lastName)) {
            reportError(400, "First and last names are required", resp);
            return;
        }

        Entity student = new Entity("Student", studentId);
        student.setProperty("firstName", firstName);
        student.setProperty("lastName", lastName);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(student);

        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setStudentId(studentId);

        resp.setStatus(201);
        resp.addHeader("Location", URI.create("/" + studentId).toString());
        resp.setContentType("application/json");
        resp.getOutputStream().print(gson.toJson(s));
    }

    private void reportError(int status, String message, HttpServletResponse resp) throws IOException {
        resp.setStatus(status);
        String body = gson.toJson(ImmutableMap.of("error", message));
        resp.setContentType("application/json");
        resp.getOutputStream().print(body);
    }

}
