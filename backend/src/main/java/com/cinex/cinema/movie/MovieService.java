package com.cinex.cinema.movie;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieResponse> findAll(MovieStatus status) {
        List<Movie> movies = (status != null)
            ? movieRepository.findByStatusAndActiveTrue(status)
            : movieRepository.findAll().stream().filter(Movie::isActive).collect(Collectors.toList());
        return movies.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MovieResponse> findAllAdmin() {
        return movieRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public MovieResponse findById(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        return toResponse(movie);
    }

    public MovieResponse create(MovieRequest request) {
        Movie movie = Movie.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .posterUrl(request.getPosterUrl())
            .trailerUrl(request.getTrailerUrl())
            .durationMinutes(request.getDurationMinutes())
            .ageRating(request.getAgeRating())
            .genre(request.getGenre())
            .releaseDate(request.getReleaseDate())
            .status(request.getStatus())
            .active(true)
            .build();
        return toResponse(movieRepository.save(movie));
    }

    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setPosterUrl(request.getPosterUrl());
        movie.setTrailerUrl(request.getTrailerUrl());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setGenre(request.getGenre());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setStatus(request.getStatus());
        return toResponse(movieRepository.save(movie));
    }

    public void delete(Long id) {
        Movie movie = movieRepository.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Phim không tồn tại"));
        movie.setActive(false);
        movieRepository.save(movie);
    }

    private MovieResponse toResponse(Movie movie) {
        return MovieResponse.builder()
            .id(movie.getId())
            .tmdbId(movie.getTmdbId())
            .title(movie.getTitle())
            .description(movie.getDescription())
            .posterUrl(movie.getPosterUrl())
            .trailerUrl(movie.getTrailerUrl())
            .durationMinutes(movie.getDurationMinutes())
            .ageRating(movie.getAgeRating())
            .genre(movie.getGenre())
            .status(movie.getStatus())
            .releaseDate(movie.getReleaseDate())
            .active(movie.isActive())
            .createdAt(movie.getCreatedAt())
            .updatedAt(movie.getUpdatedAt())
            .build();
    }
}