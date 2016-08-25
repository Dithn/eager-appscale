package edu.ucsb.cs;

import com.google.appengine.api.datastore.*;
import com.google.appengine.repackaged.com.google.api.client.util.Strings;
import com.google.appengine.repackaged.com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static com.google.appengine.repackaged.com.google.api.client.util.Preconditions.checkArgument;

public class StudentServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp) throws ServletException, IOException {
        String operation = req.getParameter("operation");
        if ("create".equals(operation)) {
            createStudent(req, resp);
        } else if ("read".equals(operation)) {
            readStudent(req, resp);
        } else if ("update".equals(operation)) {
            updateStudent(req, resp);
        } else if ("delete".equals(operation)) {
            deleteStudent(req, resp);
        } else {
            resp.sendError(400);
        }
    }

    private void createStudent(HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        String studentId = UUID.randomUUID().toString();
        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        checkArgument(!Strings.isNullOrEmpty(firstName));
        checkArgument(!Strings.isNullOrEmpty(lastName));

        Entity student = new Entity("Student", studentId);
        student.setProperty("firstName", firstName);
        student.setProperty("lastName", lastName);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(student);

        Student s = new Student();
        s.setFirstName(firstName);
        s.setLastName(lastName);
        s.setStudentId(studentId);
        writeOutput(resp, s);
    }

    private void readStudent(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        checkArgument(!Strings.isNullOrEmpty(id));
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", id);
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            s.setStudentId(id);
            writeOutput(resp, s);
        } catch (EntityNotFoundException e) {
            resp.sendError(404);
        }
    }

    private void updateStudent(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        checkArgument(!Strings.isNullOrEmpty(id));

        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", id);
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            s.setStudentId(id);

            if (!Strings.isNullOrEmpty(firstName)) {
                entity.setProperty("firstName", firstName);
                s.setFirstName(firstName);
            }
            if (!Strings.isNullOrEmpty(lastName)) {
                entity.setProperty("lastName", lastName);
                s.setLastName(lastName);
            }
            datastore.put(entity);

            writeOutput(resp, s);
        } catch (EntityNotFoundException e) {
            resp.sendError(404);
        }
    }

    private void deleteStudent(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        String id = req.getParameter("id");
        checkArgument(!Strings.isNullOrEmpty(id));
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Student", id);
        try {
            Entity entity = datastore.get(key);
            Student s = new Student();
            s.setFirstName((String) entity.getProperty("firstName"));
            s.setLastName((String) entity.getProperty("lastName"));
            s.setStudentId(id);
            datastore.delete(key);
            writeOutput(resp, s);
        } catch (EntityNotFoundException e) {
            resp.sendError(404);
        }
    }

    private void writeOutput(HttpServletResponse resp, Student s) throws IOException {
        resp.setContentType("application/json");
        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.print(gson.toJson(s));
        outputStream.flush();
        outputStream.close();
    }

}
