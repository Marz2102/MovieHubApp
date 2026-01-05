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
                writeResponse(exchange, "Такого метода не существует", 405);
        }
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String moviesJson = gson.toJson(moviesStore.getAllMovies());

        writeResponse(exchange, moviesJson, 200);
    }

    private void handlePostMovies(HttpExchange exchange) throws IOException {
        JsonElement jsonElement = JsonParser.parseString(readRequestAsString(exchange));
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        if (!jsonElement.isJsonObject() || contentType == null || !contentType.contains("application/json")) {
            writeResponse(exchange, "Передайте тело запроса в формате Json", 415);
            return;
        }

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has("title") || !jsonObject.has("year")) {
            writeResponse(exchange, "Поля Json некорректны", 422);
        }

        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();

        List<String> details = new ArrayList<>();

        if (year < 1888 || year > LocalDateTime.now().getYear() + 1) {
            details.add("Год должен быть между 1888 и " + LocalDateTime.now().plusYears(1).getYear());
        }

        if (title.isEmpty()) {
            details.add("Название не должно быть пустым");
        } else if (title.length() > 100) {
            details.add("Название слишком длинное");
        }

        if (!details.isEmpty()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String detailsJson = gson.toJson(details);
            writeResponse(exchange, detailsJson, 422);
            return;
        }

        Movie movie = new Movie(title, year);
        int id = moviesStore.addMovie(movie);

        jsonObject.addProperty("id", id);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String movieJson = gson.toJson(jsonObject);

        writeResponse(exchange, movieJson, 201);
    }

    private void handleGetMovieById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = getMovieId(exchange);
        if (idOpt.isEmpty()) {
            writeResponse(exchange, "Некорректный ID", 400);
            return;
        }

        int id = idOpt.get();
        Movie movie = moviesStore.getMovieById(id);
        if (movie == null) {
            writeResponse(exchange, "Фильм не найден", 404);
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String movieJson = gson.toJson(movie);

        writeResponse(exchange, movieJson, 200);

    }

    private void handleDeleteMovieById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = getMovieId(exchange);
        if (idOpt.isEmpty()) {
            writeResponse(exchange, "Некорректный ID", 400);
            return;
        }

        int id = idOpt.get();
        Movie movie = moviesStore.getMovieById(id);
        if (movie == null) {
            writeResponse(exchange, "Фильм не найден", 404);
            return;
        }

        moviesStore.deleteMovieById(id);

        writeResponse(exchange, "Фильм успешно удалён", 204);
    }

    private void handleGetMovieByYear(HttpExchange exchange) throws IOException {
        Optional<Integer> yearOpt = getMovieYear(exchange);
        if (yearOpt.isEmpty()) {
            writeResponse(exchange, "Некорректный параметр запроса — 'year'", 400);
            return;
        }

        int year = yearOpt.get();
        List<Movie> movieList = moviesStore.getMoviesByYear(year);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String movieJson = gson.toJson(movieList);

        writeResponse(exchange, movieJson, 200);
    }

    private void writeResponse(HttpExchange exchange, String response, int responseCode) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(responseCode, response.getBytes().length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response.getBytes(DEFAULT_CHARSET));
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
}
