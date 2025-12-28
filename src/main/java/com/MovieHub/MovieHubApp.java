package com.MovieHub;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class MovieHubApp {
    private static final int PORT = 8080;
    private static final Map<Integer, Movie> movies = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        fillMoviesMap();
        server.createContext("/movies", new MoviesHandler(movies));
        server.start();
    }

    private static void fillMoviesMap() {
        movies.put(1, new Movie("Аватар", 2009));
        movies.put(2, new Movie("Хранители", 2009));
        movies.put(3, new Movie("Джентльмены", 2019));
        movies.put(4, new Movie("Гладиатор", 2000));
        movies.put(5, new Movie("2012", 2009));
    }
}
