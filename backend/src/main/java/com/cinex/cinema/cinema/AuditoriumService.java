package com.cinex.cinema.cinema;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;

    public AuditoriumService(AuditoriumRepository auditoriumRepository,
                             CinemaRepository cinemaRepository,
                             SeatRepository seatRepository) {
        this.auditoriumRepository = auditoriumRepository;
        this.cinemaRepository = cinemaRepository;
        this.seatRepository = seatRepository;
    }

    public List<AuditoriumResponse> findByCinema(Long cinemaId, boolean includeInactive) {
        List<Auditorium> auditoriums = includeInactive
            ? auditoriumRepository.findByCinemaId(cinemaId)
            : auditoriumRepository.findByCinemaIdAndActiveTrue(cinemaId);
        return auditoriums.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AuditoriumResponse create(Long cinemaId, AuditoriumRequest request) {
        Cinema cinema = cinemaRepository.findById(cinemaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rạp không tồn tại"));

        Auditorium auditorium = Auditorium.builder()
            .name(request.getName())
            .totalRows(request.getTotalRows())
            .columnsPerRow(request.getColumnsPerRow())
            .cinema(cinema)
            .active(true)
            .build();
        auditorium = auditoriumRepository.save(auditorium);
        generateSeats(auditorium);
        return toResponse(auditorium);
    }

    public AuditoriumResponse update(Long id, AuditoriumRequest request) {
        Auditorium auditorium = findById(id);
        auditorium.setName(request.getName());
        auditorium.setTotalRows(request.getTotalRows());
        auditorium.setColumnsPerRow(request.getColumnsPerRow());
        auditorium = auditoriumRepository.save(auditorium);
        generateSeats(auditorium);
        return toResponse(auditorium);
    }

    public void delete(Long id) {
        Auditorium auditorium = findById(id);
        auditorium.setActive(false);
        auditoriumRepository.save(auditorium);
    }

    public List<SeatResponse> getSeats(Long auditoriumId) {
        findById(auditoriumId);
        return seatRepository.findByAuditoriumId(auditoriumId).stream()
            .sorted(Comparator.comparing(Seat::getRowNumber).thenComparing(Seat::getColumnNumber))
            .map(this::toSeatResponse)
            .collect(Collectors.toList());
    }

    public List<SeatResponse> generateSeats(Long auditoriumId) {
        Auditorium auditorium = findById(auditoriumId);
        generateSeats(auditorium);
        return getSeats(auditoriumId);
    }

    /**
     * Sinh ghế còn thiếu và cập nhật loại ghế theo kích thước phòng.
     * Ghế nằm ngoài kích thước mới sẽ bị vô hiệu hóa thay vì xóa để giữ lịch sử vé.
     */
    public List<Seat> generateSeats(Auditorium auditorium) {
        List<Seat> existing = seatRepository.findByAuditoriumId(auditorium.getId());
        List<Seat> result = new ArrayList<>();

        for (int row = 1; row <= auditorium.getTotalRows(); row++) {
            for (int column = 1; column <= auditorium.getColumnsPerRow(); column++) {
                String code = rowLabel(row) + column;
                Seat seat = existing.stream()
                    .filter(item -> code.equals(item.getSeatCode()))
                    .findFirst()
                    .orElseGet(() -> Seat.builder()
                        .seatCode(code)
                        .auditorium(auditorium)
                        .build());

                seat.setRowNumber(row);
                seat.setColumnNumber(column);
                seat.setSeatType(seatType(row, auditorium.getTotalRows()));
                seat.setActive(true);
                result.add(seatRepository.save(seat));
            }
        }

        existing.stream()
            .filter(seat -> seat.getRowNumber() > auditorium.getTotalRows()
                || seat.getColumnNumber() > auditorium.getColumnsPerRow())
            .forEach(seat -> {
                seat.setActive(false);
                seatRepository.save(seat);
            });

        return result;
    }

    public Auditorium findById(Long id) {
        return auditoriumRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phòng chiếu không tồn tại"));
    }

    public AuditoriumResponse toResponse(Auditorium auditorium) {
        long seatCount = seatRepository.findByAuditoriumIdAndActiveTrue(auditorium.getId()).size();
        return AuditoriumResponse.builder()
            .id(auditorium.getId())
            .name(auditorium.getName())
            .totalRows(auditorium.getTotalRows())
            .columnsPerRow(auditorium.getColumnsPerRow())
            .cinemaId(auditorium.getCinema().getId())
            .cinemaName(auditorium.getCinema().getName())
            .active(auditorium.isActive())
            .seatCount(seatCount)
            .build();
    }

    public SeatResponse toSeatResponse(Seat seat) {
        return SeatResponse.builder()
            .id(seat.getId())
            .seatCode(seat.getSeatCode())
            .seatType(seat.getSeatType())
            .rowNumber(seat.getRowNumber())
            .columnNumber(seat.getColumnNumber())
            .active(seat.isActive())
            .build();
    }

    private String rowLabel(int row) {
        if (row <= 26) {
            return String.valueOf((char) ('A' + row - 1));
        }
        return "R" + row;
    }

    private String seatType(int row, int totalRows) {
        if (row == totalRows) {
            return "COUPLE";
        }
        if (row > Math.max(1, Math.round(totalRows * 0.6f))) {
            return "VIP";
        }
        return "STANDARD";
    }
}
