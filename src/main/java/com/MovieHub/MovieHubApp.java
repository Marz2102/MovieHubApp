package com.MovieHub;

public class MovieHubApp {
    public static void main(String[] args) {
        MoviesServer server = new MoviesServer(new MoviesStore());
        server.start();
    }
}
