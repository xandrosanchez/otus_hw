package ru.otus.servlet;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import ru.otus.crm.model.Client;
import ru.otus.crm.service.DBServiceClient;

@SuppressWarnings({"java:S1989", "java:S1192", "java:S1948"})
public class ClientsApiServlet extends HttpServlet {
    private final DBServiceClient dbServiceClient;
    private final Gson gson;

    public ClientsApiServlet(DBServiceClient dbServiceClient, Gson gson) {
        this.dbServiceClient = dbServiceClient;
        this.gson = gson;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // Возвращаем всех клиентов
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().print(gson.toJson(dbServiceClient.findAll()));
        } else {
            // Возвращаем клиента по ID
            try {
                long id = Long.parseLong(pathInfo.substring(1));
                dbServiceClient
                        .getClient(id)
                        .ifPresentOrElse(
                                client -> {
                                    try {
                                        resp.setContentType("application/json;charset=UTF-8");
                                        resp.getWriter().print(gson.toJson(client));
                                    } catch (IOException e) {
                                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    }
                                },
                                () -> resp.setStatus(HttpServletResponse.SC_NOT_FOUND));
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Client newClient = gson.fromJson(req.getReader(), Client.class);
        Client savedClient = dbServiceClient.saveClient(newClient);

        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().print(gson.toJson(savedClient));
    }
}
