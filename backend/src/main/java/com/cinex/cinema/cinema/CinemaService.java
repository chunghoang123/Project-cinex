package com.cinex.cinema.cinema;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CinemaService {
    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    public List<CinemaResponse> findAll(boolean includeInactive) {
        return cinemaRepository.findAll().stream()
            .filter(cinema -> includeInactive || cinema.isActive())
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public Cinema findById(Long id) {
        return cinemaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rạp không tồn tại"));
    }

    public CinemaResponse getResponse(Long id) {
        return toResponse(findById(id));
    }

    public CinemaResponse create(CinemaRequest request) {
        Cinema cinema = Cinema.builder()
            .name(request.getName())
            .address(request.getAddress())
            .phone(request.getPhone())
            .active(true)
            .build();
        return toResponse(cinemaRepository.save(cinema));
    }

    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema existing = findById(id);
        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setPhone(request.getPhone());
        return toResponse(cinemaRepository.save(existing));
    }

    public void delete(Long id) {
        Cinema cinema = findById(id);
        cinema.setActive(false);
        cinemaRepository.save(cinema);
    }

    public CinemaResponse toResponse(Cinema cinema) {
        return CinemaResponse.builder()
            .id(cinema.getId())
            .name(cinema.getName())
            .address(cinema.getAddress())
            .phone(cinema.getPhone())
            .active(cinema.isActive())
            .createdAt(cinema.getCreatedAt())
            .build();
    }
}
