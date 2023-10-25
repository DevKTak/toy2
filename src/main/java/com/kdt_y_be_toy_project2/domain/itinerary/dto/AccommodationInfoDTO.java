package com.kdt_y_be_toy_project2.domain.itinerary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kdt_y_be_toy_project2.domain.itinerary.domain.AccommodationInfo;
import com.kdt_y_be_toy_project2.domain.model.PlaceInfo;
import com.kdt_y_be_toy_project2.domain.model.TimeScheduleInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


@Builder
public record AccommodationInfoDTO(
        @NotNull(message = "숙박 시작 시간을 입력해야 합니다. (예: 2023-10-25 12:00)")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime startDateTime,

        @NotNull(message = "숙박 종료 시간을 입력해야 합니다. (예: 2023-10-25 12:00)")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime endDateTime,

        PlaceInfo accommodationPlaceInfo
) {
    public static AccommodationInfoDTO from(final AccommodationInfo accommodationInfo) {
        return AccommodationInfoDTO.builder()
                .startDateTime(accommodationInfo.getAccommodationSchedule().getStartDateTime())
                .endDateTime(accommodationInfo.getAccommodationSchedule().getEndDateTime())
                .accommodationPlaceInfo(accommodationInfo.getAccommodationPlaceInfo())
                .build();
    }
}
