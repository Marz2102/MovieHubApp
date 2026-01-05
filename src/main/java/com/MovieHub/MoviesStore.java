package com.MovieHub;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();

    public int addMovie(Movie movie) {
        movies.put(movies.size() + 1, movie);
        return movies.size();
    }

    public void deleteMovieById(int id) {
        Movie movie = movies.get(id);
        if (movie != null) {
            movies.remove(id);
        }
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return movies.values()
                .stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public List<Movie> getAllMovies() {
        return Collections.unmodifiableList(new ArrayList<>(movies.values()));
    }

    public void clearMovies() {
        movies.clear();
    }
}
