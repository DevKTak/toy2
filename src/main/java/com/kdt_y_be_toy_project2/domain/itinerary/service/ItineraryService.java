package com.kdt_y_be_toy_project2.domain.itinerary.service;

import com.kdt_y_be_toy_project2.domain.itinerary.api.RoadAddressInfoAPI;
import com.kdt_y_be_toy_project2.domain.itinerary.domain.*;
import com.kdt_y_be_toy_project2.domain.itinerary.dto.ItineraryRequest;
import com.kdt_y_be_toy_project2.domain.itinerary.dto.ItineraryResponse;
import com.kdt_y_be_toy_project2.domain.itinerary.dto.request.AccommodationInfoRequest;
import com.kdt_y_be_toy_project2.domain.itinerary.dto.request.MoveInfoRequest;
import com.kdt_y_be_toy_project2.domain.itinerary.dto.request.StayInfoRequest;
import com.kdt_y_be_toy_project2.domain.itinerary.exception.InvalidDateException;
import com.kdt_y_be_toy_project2.domain.itinerary.exception.InvalidItineraryDurationException;
import com.kdt_y_be_toy_project2.domain.itinerary.exception.ItineraryNotFoundException;
import com.kdt_y_be_toy_project2.domain.itinerary.exception.TripNotFoundException;
import com.kdt_y_be_toy_project2.domain.itinerary.repository.ItineraryRepository;
import com.kdt_y_be_toy_project2.domain.model.DateTimeScheduleInfo;
import com.kdt_y_be_toy_project2.domain.trip.domain.Trip;
import com.kdt_y_be_toy_project2.domain.trip.repository.TripRepository;
import com.kdt_y_be_toy_project2.global.util.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final TripRepository tripRepository;
    private final RoadAddressInfoAPI roadAddressInfoAPI;

    @Transactional(readOnly = true)
    public List<ItineraryResponse> getAllItineraries(final Long tripId) {

        List<ItineraryResponse> itineraryResponses = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new)
                .getItineraries().stream().map(itinerary -> ItineraryResponse.from(itinerary)).toList();

        if (itineraryResponses.size() == 0) {
            throw new ItineraryNotFoundException();
        }
        return itineraryResponses;
    }

    @Transactional(readOnly = true)
    public ItineraryResponse getItineraryById(final Long tripId, final Long itineraryId) {

        Trip retrivedTrip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        for (Itinerary itinerary : retrivedTrip.getItineraries()) {
            if (itinerary.getId().equals(itineraryId)) {
                return Optional.of(itinerary)
                        .map(ItineraryResponse::from)
                        .orElseThrow();
            }
        }

        throw new ItineraryNotFoundException();
    }

    public ItineraryResponse createItinerary(final Long tripId, final ItineraryRequest request) {

        Trip retrivedTrip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);
        checkItineraryDuration(retrivedTrip, request);
        checkInvalidDate(request);

        Itinerary itinerary = updateItineraryWithRoadAddresses(request, retrivedTrip);

        Itinerary savedItinerary = Optional.of(itineraryRepository.save(itinerary)).orElseThrow();
        retrivedTrip.getItineraries().add(savedItinerary);

        return Optional.of(ItineraryResponse.from(savedItinerary)).orElseThrow();
    }

    public ItineraryResponse editItinerary(final Long tripId, final Long itineraryId, final ItineraryRequest request) {

        Trip retrivedTrip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        Itinerary updateItinerary = updateItineraryWithRoadAddresses(request, retrivedTrip);

        for (Itinerary itinerary : retrivedTrip.getItineraries()) {
            if (itinerary.getId().equals(itineraryId)) {
                itinerary.update(updateItinerary);
                checkItineraryDuration(itinerary.getTrip(), request);
                checkInvalidDate(request);

                return Optional.of(itineraryRepository.save(itinerary))
                        .map(ItineraryResponse::from)
                        .orElseThrow();
            }
        }

        throw new ItineraryNotFoundException();
    }

    void checkItineraryDuration(Trip trip, ItineraryRequest itinerary) {
        LocalDateTime tripStartTime = trip.getTripSchedule().getStartDate().atStartOfDay();
        LocalDateTime tripEndTime = trip.getTripSchedule().getEndDate().atStartOfDay().plusDays(1);

        if (LocalDateTimeUtil.toLocalDateTime(itinerary.stayInfoRequest().startDateTime()).isBefore(tripStartTime) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.accommodationInfoRequest().startDateTime()).isBefore(tripStartTime) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.moveInfoRequest().startDateTime()).isBefore(tripStartTime) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.stayInfoRequest().endDateTime()).isAfter(tripEndTime) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.accommodationInfoRequest().endDateTime()).isAfter(tripEndTime) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.moveInfoRequest().endDateTime()).isAfter(tripEndTime)
        ) throw new InvalidItineraryDurationException();
    }

    void checkInvalidDate(ItineraryRequest itinerary) {

        if (LocalDateTimeUtil.toLocalDateTime(itinerary.moveInfoRequest().startDateTime()).isAfter(LocalDateTimeUtil.toLocalDateTime(itinerary.moveInfoRequest().endDateTime())) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.accommodationInfoRequest().startDateTime()).isAfter(LocalDateTimeUtil.toLocalDateTime(itinerary.accommodationInfoRequest().endDateTime())) ||
                LocalDateTimeUtil.toLocalDateTime(itinerary.stayInfoRequest().startDateTime()).isAfter(LocalDateTimeUtil.toLocalDateTime(itinerary.stayInfoRequest().endDateTime()))
        ) throw new InvalidDateException();
    }

    Itinerary updateItineraryWithRoadAddresses(ItineraryRequest request, Trip trip) {

        MoveInfoRequest moveInfoRequest = request.moveInfoRequest();
        AccommodationInfoRequest accommodationInfoRequest = request.accommodationInfoRequest();
        StayInfoRequest stayInfoRequest = request.stayInfoRequest();

        return Itinerary.builder()
                .stayInfo(buildStayInfo(stayInfoRequest, roadAddressInfoAPI))
                .moveInfo(buildMoveInfo(moveInfoRequest, roadAddressInfoAPI))
                .accommodationInfo(buildAccommodationInfo(accommodationInfoRequest, roadAddressInfoAPI))
                .trip(trip)
                .build();
    }

    MoveInfo buildMoveInfo(MoveInfoRequest moveInfoRequest, RoadAddressInfoAPI roadAddressInfoAPI) {
        return MoveInfo.builder()
                .moveSchedule(DateTimeScheduleInfo.builder()
                        .startDateTime(LocalDateTimeUtil.toLocalDateTime(moveInfoRequest.startDateTime()))
                        .endDateTime(LocalDateTimeUtil.toLocalDateTime(moveInfoRequest.endDateTime()))
                        .build())
                .sourcePlaceInfo(roadAddressInfoAPI.getPlaceInfoByKeyword(moveInfoRequest.sourcePlaceName()))
                .destPlaceInfo(roadAddressInfoAPI.getPlaceInfoByKeyword(moveInfoRequest.destPlaceName()))
                .transportationType(TransportationType.getByValue(moveInfoRequest.transportationType()))
                .build();
    }

    StayInfo buildStayInfo(StayInfoRequest stayInfoRequest, RoadAddressInfoAPI roadAddressInfoAPI) {
        return StayInfo.builder()
                .staySchedule(DateTimeScheduleInfo.builder().startDateTime(LocalDateTimeUtil.toLocalDateTime(stayInfoRequest.startDateTime()))
                        .endDateTime(LocalDateTimeUtil.toLocalDateTime(stayInfoRequest.endDateTime())).build())
                .stayPlaceInfo(roadAddressInfoAPI.getPlaceInfoByKeyword(stayInfoRequest.stayPlaceName()))
                .build();
    }

    AccommodationInfo buildAccommodationInfo(AccommodationInfoRequest accommodationInfoRequest, RoadAddressInfoAPI roadAddressInfoAPI) {
        return AccommodationInfo.builder()
                .accommodationSchedule(DateTimeScheduleInfo.builder()
                        .startDateTime(LocalDateTimeUtil.toLocalDateTime(accommodationInfoRequest.startDateTime()))
                        .endDateTime(LocalDateTimeUtil.toLocalDateTime(accommodationInfoRequest.endDateTime()))
                        .build())
                .accommodationPlaceInfo(roadAddressInfoAPI.getPlaceInfoByKeyword(accommodationInfoRequest.accommodationPlaceName()))
                .build();
    }

}
