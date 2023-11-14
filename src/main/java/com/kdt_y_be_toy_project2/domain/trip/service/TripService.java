package com.kdt_y_be_toy_project2.domain.trip.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kdt_y_be_toy_project2.domain.model.exception.InvalidDateRangeException;
import com.kdt_y_be_toy_project2.domain.trip.domain.Trip;
import com.kdt_y_be_toy_project2.domain.trip.dto.TripRequest;
import com.kdt_y_be_toy_project2.domain.trip.dto.TripResponse;
import com.kdt_y_be_toy_project2.domain.trip.dto.TripSearchRequest;
import com.kdt_y_be_toy_project2.domain.trip.exception.TripNotFoundException;
import com.kdt_y_be_toy_project2.domain.trip.exception.TripSearchIllegalArgumentException;
import com.kdt_y_be_toy_project2.domain.trip.repository.TripRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TripService {

	private final TripRepository tripRepository;

	@Transactional(readOnly = true)
	public List<TripResponse> getAllTrips() {
		List<Trip> trips = tripRepository.findAll();

		if (trips.isEmpty()) {
			throw new TripNotFoundException();
		}
		return trips
			.stream().map(TripResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public TripResponse getTripById(final Long tripId) {
		return tripRepository.findById(tripId)
			.map(TripResponse::from)
			.orElseThrow(TripNotFoundException::new);
	}

	public TripResponse createTrip(final TripRequest request) {
		return Optional.of(tripRepository.save(TripRequest.toEntity(request)))
			.map(TripResponse::from)
			.orElseThrow();
	}

	public TripResponse editTrip(final Long tripId, final TripRequest request) {
		Trip updatedTrip = tripRepository.findById(tripId)
			.map(trip -> trip.update(TripRequest.toEntity(request)))
			.orElseThrow(TripNotFoundException::new);

		return TripResponse.from(updatedTrip);
	}

	public List<TripResponse> searchTrips(TripSearchRequest request) {
		validateSearchTripRequest(request);
		return tripRepository.searchTrips(
				request.name(),
				request.getTripType(),
				request.getStartDate(),
				request.getEndDate()
			)
			.stream().map(TripResponse::from)
			.toList();
	}

	private void validateSearchTripRequest(TripSearchRequest request) {
		if (request.isAllNull()) {
			throw new TripSearchIllegalArgumentException();
		}

		if (isAllValidDate(request.getStartDate(), request.getEndDate())) {
			validateDateRange(
				Objects.requireNonNull(request.getStartDate()),
				Objects.requireNonNull(request.getEndDate())
			);
		}
	}

	private boolean isAllValidDate(LocalDate start, LocalDate end) {
		return start != null && end != null;
	}

	private void validateDateRange(LocalDate start, LocalDate end) {
		if (start.isAfter(end) && isDayTrip(start, end)) {
			throw new InvalidDateRangeException("startDate 는 항상 endDate 보다 과거여야 합니다. "
				+ "{\"startDate = \" %s / endDate = %s}".formatted(start, end));
		}
	}

	private boolean isDayTrip(LocalDate start, LocalDate end) {
		return !start.equals(end);
	}
}
