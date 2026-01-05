package com.MovieHub;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler implements HttpHandler {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final MoviesStore moviesStore;
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Endpoint endpoint = getEndpoint(exchange.getRequestURI(), exchange.getRequestMethod());

        switch (endpoint) {
            case GET_MOVIES: {
                handleGetMovies(exchange);
                break;
            }
            case POST_MOVIES: {
                handlePostMovies(exchange);
                break;
            }
            case GET_MOVIE_BY_ID: {
                handleGetMovieById(exchange);
                break;
            }
            case DELETE_MOVIE_BY_ID: {
                handleDeleteMovieById(exchange);
                break;
            }
            case GET_MOVIE_BY_YEAR: {
                handleGetMovieByYear(exchange);
                break;
            }
            default:
                String errorJson = getErrorJson("Такого метода не существует", List.of());
                writeResponse(exchange, errorJson, 405);
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        writeResponse(exchange, GSON.toJson(moviesStore.getAllMovies()), 200);
    }

    private void handlePostMovies(HttpExchange exchange) throws IOException {
        try {
            JsonElement jsonElement = JsonParser.parseString(readRequestAsString(exchange));
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

            if (!jsonElement.isJsonObject() || contentType == null || !contentType.contains("application/json")) {
                String errorJson = getErrorJson("Передайте тело запроса в формате Json", List.of());
                writeResponse(exchange, errorJson, 415);
                return;
            }

            List<String> details = new ArrayList<>();
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (!jsonObject.has("title")) {
                details.add("Отсутствует поле 'title'");
            }
            if (!jsonObject.has("year")) {
                details.add("Отсутствует поле 'year'");
            }

            if (!details.isEmpty()) {
                String errorJson = getErrorJson("Поля Json некорректны", details);
                writeResponse(exchange, errorJson, 422);
                return;
            }

            String title = jsonObject.get("title").getAsString();
            int year = jsonObject.get("year").getAsInt();

            if (year < 1888 || year > LocalDateTime.now().getYear() + 1) {
                details.add("Год должен быть между 1888 и " + LocalDateTime.now().plusYears(1).getYear());
            }

            if (title.isEmpty()) {
                details.add("Название не должно быть пустым");
            } else if (title.length() > 100) {
                details.add("Название слишком длинное");
            }

            if (!details.isEmpty()) {
                String errorJson = getErrorJson("Ошибка с названием или годом выпуска", details);
                writeResponse(exchange, errorJson, 422);
                return;
            }

            Movie movie = new Movie(title, year);
            int id = moviesStore.addMovie(movie);

            jsonObject.addProperty("id", id);
            writeResponse(exchange, GSON.toJson(jsonObject), 201);
        } catch (JsonSyntaxException e) {
            String errorJson = getErrorJson("Ошибка при парсинге Json", List.of());
            writeResponse(exchange, errorJson, 415);
        }
    }

    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = getMovieId(exchange);
        if (idOpt.isEmpty()) {
            String errorJson = getErrorJson("Некорректный ID", List.of());
            writeResponse(exchange, errorJson, 400);
            return;
        }

        int id = idOpt.get();
        Movie movie = moviesStore.getMovieById(id);
        if (movie == null) {
            String errorJson = getErrorJson("Фильм не найден", List.of());
            writeResponse(exchange, errorJson, 404);
            return;
        }

        writeResponse(exchange, GSON.toJson(movie), 200);
    }

    private void handleDeleteMovieById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = getMovieId(exchange);
        if (idOpt.isEmpty()) {
            String errorJson = getErrorJson("Некорректный ID", List.of());
            writeResponse(exchange, errorJson, 400);
            return;
        }

        int id = idOpt.get();
        Movie movie = moviesStore.getMovieById(id);
        if (movie == null) {
            String errorJson = getErrorJson("Фильм не найден", List.of());
            writeResponse(exchange, errorJson, 404);
            return;
        }

        moviesStore.deleteMovieById(id);
        writeResponse(exchange, "Фильм успешно удалён", 204);
    }

    private void handleGetMovieByYear(HttpExchange exchange) throws IOException {
        Optional<Integer> yearOpt = getMovieYear(exchange);
        if (yearOpt.isEmpty()) {
            String errorJson = getErrorJson("Некорректный параметр запроса — 'year'", List.of());
            writeResponse(exchange, errorJson, 400);
            return;
        }

        int year = yearOpt.get();
        List<Movie> movieList = moviesStore.getMoviesByYear(year);

        writeResponse(exchange, GSON.toJson(movieList), 200);
    }

    private void writeResponse(HttpExchange exchange, String response, int responseCode) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = response.getBytes(DEFAULT_CHARSET);
        exchange.sendResponseHeaders(responseCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String readRequestAsString(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, DEFAULT_CHARSET))) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

    private Optional<Integer> getMovieId(HttpExchange exchange) {
        String[] paths = exchange.getRequestURI().getPath().split("/");
        try {
            return Optional.of(Integer.parseInt(paths[2]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Integer> getMovieYear(HttpExchange exchange) {
        String[] paths = exchange.getRequestURI().getQuery().split("=");
        try {
            return Optional.of(Integer.parseInt(paths[1]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Endpoint getEndpoint(URI uri, String method) {
        String[] paths = uri.getPath().split("/");
        String query = uri.getQuery();

        if (paths.length == 2 && paths[1].equals("movies") && query == null && method.equals("GET")) {
            return Endpoint.GET_MOVIES;
        }

        if (paths.length == 2 && method.equals("POST")) {
            return Endpoint.POST_MOVIES;
        }

        if (paths.length == 3 && method.equals("GET")) {
            return Endpoint.GET_MOVIE_BY_ID;
        }

        if (paths.length == 3 && method.equals("DELETE")) {
            return Endpoint.DELETE_MOVIE_BY_ID;
        }

        if (paths.length == 2 && query != null && method.equals("GET")) {
            return Endpoint.GET_MOVIE_BY_YEAR;
        }

        return Endpoint.UNKNOWN;
    }

    private String getErrorJson(String error, List<String> details) {
        ErrorResponse errorResponse = new ErrorResponse(error, details);
        return GSON.toJson(errorResponse);
    }
}
