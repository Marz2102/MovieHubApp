package com.MovieHub;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MovieHubAppTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore = new MoviesStore();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(moviesStore);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clearMovies();
    }

    @Test
    void getMoviesWhenEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMoviesWhenListNotEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(response.body());
        assertTrue(jsonElement.isJsonArray(), "Ожидается Json массив");

        JsonObject jsonObject = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();

        assertAll(
                () -> assertEquals("Аватар", title),
                () -> assertEquals(2009, year)
        );
    }

    @Test
    void postMoviesWhenJsonCorrect() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("title", "Аватар");
        jsonObjectMovie.addProperty("year", 2009);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(), "POST /movies с корректным JSON должен вернуть 201");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(response.body());
        assertTrue(jsonElement.isJsonObject(), "Ожидается Json объект");

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();
        int id = jsonObject.get("id").getAsInt();

        assertAll(
                () -> assertEquals("Аватар", title),
                () -> assertEquals(2009, year),
                () -> assertEquals(1, id)
        );
    }

    @Test
    void postMoviesWhenTitleTooLong() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("title", "fnsdjnfsdnfksmdkfmsdklfmdsklmfkjsfdnjfnndskdnksfndfkskfndkfsdklfndklnfkdlsfedmmslkfdmskmfkldsmflksdmflkdsnf");
        jsonObjectMovie.addProperty("year", 2009);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "POST /movies с длинным названием должен вернуть 422");
    }

    @Test
    void postMoviesWhenTitleEmpty() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("title", "");
        jsonObjectMovie.addProperty("year", 2009);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "POST /movies с пустым названием должен вернуть 422");
    }

    @Test
    void postMoviesWhenYearNotCorrect() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("title", "Аватар");
        jsonObjectMovie.addProperty("year", 2099);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "POST /movies с некорректным годом должен вернуть 422");
    }

    @Test
    void postMoviesWhenHeaderNotCorrect() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("title", "Аватар");
        jsonObjectMovie.addProperty("year", 2009);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode(), "POST /movies без хедера об обработке Json должен вернуть 415");
    }

    @Test
    void postMoviesWhenJsonNotCorrect() throws Exception {
        JsonObject jsonObjectMovie = new JsonObject();
        jsonObjectMovie.addProperty("tittle", "Аватар");
        jsonObjectMovie.addProperty("year", 2009);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObjectMovie.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(), "POST /movies с некорректным Json должен вернуть 422");
    }

    @Test
    void getMoviesByCorrectId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies/1 должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(response.body());
        assertTrue(jsonElement.isJsonObject(), "Ожидается Json объект");

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();

        assertAll(
                () -> assertEquals("Аватар", title),
                () -> assertEquals(2009, year)
        );
    }

    @Test
    void getMoviesByIncorrectId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode(), "GET /movies/2 должен вернуть 404");
    }

    @Test
    void getMoviesByIdWhenIdNotANumber() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/avatar"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "GET /movies/avatar должен вернуть 400");
    }

    @Test
    void deleteMoviesByCorrectId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, response.statusCode(), "DELETE /movies/1 должен вернуть 204");
    }

    @Test
    void deleteMoviesByIncorrectId() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode(), "GET /movies/2 должен вернуть 404");
    }

    @Test
    void deleteMoviesByIdWhenIdNotANumber() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/avatar"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "GET /movies/avatar должен вернуть 400");
    }

    @Test
    void getMoviesByYearWhenListNotEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2009"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));
        moviesStore.addMovie(new Movie("Хранители", 2009));
        moviesStore.addMovie(new Movie("Джентльмены", 2019));
        moviesStore.addMovie(new Movie("Гладиатор", 2000));
        moviesStore.addMovie(new Movie("2012", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies?year=2009 должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(response.body());
        assertTrue(jsonElement.isJsonArray(), "Ожидается Json массив");

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        assertEquals(3, jsonArray.size());
    }

    @Test
    void getMoviesByYearWhenListEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2008"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));
        moviesStore.addMovie(new Movie("Хранители", 2009));
        moviesStore.addMovie(new Movie("Джентльмены", 2019));
        moviesStore.addMovie(new Movie("Гладиатор", 2000));
        moviesStore.addMovie(new Movie("2012", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "GET /movies?year=2008 должен вернуть 200");

        String contentTypeHeaderValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=utf-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        JsonElement jsonElement = JsonParser.parseString(response.body());
        assertTrue(jsonElement.isJsonArray(), "Ожидается Json массив");

        JsonArray jsonArray = jsonElement.getAsJsonArray();
        assertEquals(0, jsonArray.size());
    }

    @Test
    void getMoviesByYearWhenYearIsNotANumber() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        moviesStore.addMovie(new Movie("Аватар", 2009));
        moviesStore.addMovie(new Movie("Хранители", 2009));
        moviesStore.addMovie(new Movie("Джентльмены", 2019));
        moviesStore.addMovie(new Movie("Гладиатор", 2000));
        moviesStore.addMovie(new Movie("2012", 2009));

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(), "GET /movies?year=abc должен вернуть 400");
    }

    @Test
    void testNotAllowedMethod() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, response.statusCode(), "PUT /movies должен вернуть 405");
    }
}