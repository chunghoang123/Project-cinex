package com.cinex.cinema.movie;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MovieRequest {
    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 200, message = "Tên phim tối đa 200 ký tự")
    private String title;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @Size(max = 500)
    private String posterUrl;

    @Size(max = 500)
    private String trailerUrl;

    @NotNull(message = "Thời lượng không được để trống")
    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private Integer durationMinutes;

    private Integer ageRating;

    @Size(max = 100)
    private String genre;

    private LocalDate releaseDate;

    @NotNull(message = "Trạng thái không được để trống")
    private MovieStatus status;
}