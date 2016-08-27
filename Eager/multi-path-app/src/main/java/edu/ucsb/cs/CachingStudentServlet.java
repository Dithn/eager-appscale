package edu.ucsb.cs;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Strings;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class CachingStudentServlet extends HttpServlet {

    private static final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        checkArgument(!Strings.isNullOrEmpty(id));

        MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
        Student student = (Student) cache.get(id);
        if (student == null) {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Key key = KeyFactory.createKey("Student", id);
            try {
                Entity entity = datastore.get(key);
                student = new Student();
                student.setFirstName((String) entity.getProperty("firstName"));
                student.setLastName((String) entity.getProperty("lastName"));
                student.setStudentId(id);
                cache.put(id, student, Expiration.byDeltaSeconds(30));
            } catch (EntityNotFoundException e) {
                resp.sendError(404);
            }
        }

        resp.setContentType("application/json");
        ServletOutputStream outputStream = resp.getOutputStream();
        outputStream.print(gson.toJson(student));
        outputStream.flush();
        outputStream.close();
    }
}
