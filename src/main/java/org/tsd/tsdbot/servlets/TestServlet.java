package org.tsd.tsdbot.servlets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.tsd.tsdbot.TSDBot;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Joe on 1/10/2015.
 */
@SuppressWarnings("serial")
@Singleton
public class TestServlet extends HttpServlet {

    @Inject
    private TSDBot bot;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/test.jsp").forward(req,resp);
    }
}
