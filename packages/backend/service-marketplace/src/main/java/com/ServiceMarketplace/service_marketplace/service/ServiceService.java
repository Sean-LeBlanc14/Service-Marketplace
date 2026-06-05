package com.ServiceMarketplace.service_marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ServiceMarketplace.service_marketplace.dto.CreateServiceRequest;
import com.ServiceMarketplace.service_marketplace.dto.ServiceDto;
import com.ServiceMarketplace.service_marketplace.dto.UpdateServiceRequest;
import com.ServiceMarketplace.service_marketplace.exception.ResourceNotFoundException;
import com.ServiceMarketplace.service_marketplace.model.Booking;
import com.ServiceMarketplace.service_marketplace.model.Service;
import com.ServiceMarketplace.service_marketplace.model.User;
import com.ServiceMarketplace.service_marketplace.repository.BookingRepository;
import com.ServiceMarketplace.service_marketplace.repository.ServiceRepository;
import com.ServiceMarketplace.service_marketplace.repository.UserRepository;

@org.springframework.stereotype.Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public ServiceService(ServiceRepository serviceRepository, UserRepository userRepository,
            BookingRepository bookingRepository) {
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    public record ProviderRatingSummary(Double averageRating, int reviewCount) {}

    private ServiceDto toDto(Service service) {
        return toDto(service, getProviderName(service.getUserId()), getProviderRatingSummary(service.getUserId()));
    }

    private ServiceDto toDto(Service service, User provider) {
        return toDto(service, getDisplayName(provider), getProviderRatingSummary(provider.getId()));
    }

    private ServiceDto toDto(Service service, String providerName, ProviderRatingSummary providerRating) {
        return new ServiceDto(
            service.getId(),
            service.getTitle(),
            service.getCategory(),
            service.getUserId(),
            providerName,
            providerRating.averageRating(),
            providerRating.reviewCount(),
            service.getPriceMin(),
            service.getPriceMax(),
            service.getPriceUnit(),
            service.getDescription(),
            service.getLocation(),
            service.getTags()
        );
    }

    public List<ServiceDto> getAllServices() {
        List<Service> services = serviceRepository.findAll()
            .stream()
            .filter(service -> service.getIsAvailable() == null || service.getIsAvailable())
            .collect(Collectors.toList());

        return toDtos(services);
    }

    public List<ServiceDto> getServicesByCategory(String category) {
        List<Service> services = serviceRepository.findByCategory(category)
            .stream()
            .filter(service -> service.getIsAvailable() == null || service.getIsAvailable())
            .collect(Collectors.toList());

        return toDtos(services);
    }

    public List<ServiceDto> getServicesByUserId(String userId) {
        return toDtos(serviceRepository.findByUserId(userId));
    }

    public ProviderRatingSummary getProviderRatingSummary(String userId) {
        String cleanUserId = clean(userId);

        if (cleanUserId.isBlank()) {
            return new ProviderRatingSummary(null, 0);
        }

        List<Booking> reviewedBookings = bookingRepository.findReviewedBookingsByProviderId(cleanUserId);

        if (reviewedBookings == null || reviewedBookings.isEmpty()) {
            return new ProviderRatingSummary(null, 0);
        }

        List<Integer> ratings = reviewedBookings.stream()
            .map(Booking::getRating)
            .filter(rating -> rating != null)
            .toList();

        if (ratings.isEmpty()) {
            return new ProviderRatingSummary(null, 0);
        }

        double average = ratings.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);

        return new ProviderRatingSummary(Math.round(average * 10.0) / 10.0, ratings.size());
    }

    private List<ServiceDto> toDtos(List<Service> services) {
        Set<String> providerIds = services.stream()
            .map(service -> clean(service.getUserId()))
            .filter(providerId -> !providerId.isBlank())
            .collect(Collectors.toCollection(HashSet::new));

        Map<String, User> providersById = providerIds.isEmpty()
            ? Map.of()
            : userRepository.findAllById(providerIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, ProviderRatingSummary> ratingsByProviderId = getProviderRatingSummaries(providerIds);

        return services.stream()
            .map(service -> {
                String providerId = clean(service.getUserId());
                User provider = providersById.get(providerId);
                String providerName = provider == null ? "Service creator" : getDisplayName(provider);
                ProviderRatingSummary ratingSummary = ratingsByProviderId.getOrDefault(
                    providerId,
                    new ProviderRatingSummary(null, 0)
                );

                return toDto(service, providerName, ratingSummary);
            })
            .collect(Collectors.toList());
    }

    private Map<String, ProviderRatingSummary> getProviderRatingSummaries(Collection<String> providerIds) {
        List<String> cleanProviderIds = providerIds.stream()
            .map(this::clean)
            .filter(providerId -> !providerId.isBlank())
            .distinct()
            .toList();

        if (cleanProviderIds.isEmpty()) {
            return Map.of();
        }

        return bookingRepository.findReviewedBookingsByProviderIdIn(cleanProviderIds)
            .stream()
            .filter(booking -> booking.getRating() != null)
            .collect(Collectors.groupingBy(
                Booking::getProviderId,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    this::summarizeReviewedBookings
                )
            ));
    }

    private ProviderRatingSummary summarizeReviewedBookings(List<Booking> reviewedBookings) {
        List<Integer> ratings = reviewedBookings.stream()
            .map(Booking::getRating)
            .filter(rating -> rating != null)
            .toList();

        if (ratings.isEmpty()) {
            return new ProviderRatingSummary(null, 0);
        }

        double average = ratings.stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0);

        return new ProviderRatingSummary(Math.round(average * 10.0) / 10.0, ratings.size());
    }

    public ServiceDto createService(CreateServiceRequest request, UserDetails userDetails) {
        var user = getCurrentUser(userDetails);

        Service service = new Service();
        applyServiceFields(
            service,
            request.getTitle(),
            request.getDescription(),
            request.getPriceMin(),
            request.getPriceMax(),
            request.getPriceUnit(),
            request.getCategory(),
            request.getLocation(),
            request.getTags(),
            request.getPostingType()
        );
        service.setUserId(user.getId());
        service.setIsAvailable(true);
        service.setCreatedAt(Instant.now());

        return toDto(serviceRepository.save(service), user);
    }

    public ServiceDto updateService(String serviceId, UpdateServiceRequest request, UserDetails userDetails) {
        var user = getCurrentUser(userDetails);
        var service = getOwnedService(serviceId, user);

        applyServiceFields(
            service,
            request.getTitle(),
            request.getDescription(),
            request.getPriceMin(),
            request.getPriceMax(),
            request.getPriceUnit(),
            request.getCategory(),
            request.getLocation(),
            request.getTags(),
            request.getPostingType()
        );

        return toDto(serviceRepository.save(service), user);
    }

    public void deleteService(String serviceId, UserDetails userDetails) {
        var user = getCurrentUser(userDetails);
        var service = getOwnedService(serviceId, user);

        serviceRepository.delete(service);
    }

    private void applyServiceFields(
        Service service,
        String title,
        String description,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String priceUnit,
        String category,
        String location,
        List<String> tags,
        String postingType) {
        service.setTitle(clean(title));
        service.setDescription(clean(description));
        service.setPriceMin(priceMin);
        service.setPriceMax(priceMax);
        service.setPriceUnit(clean(priceUnit));
        service.setCategory(clean(category));
        service.setLocation(clean(location));
        service.setTags(normalizeTags(tags));
        service.setPostingType(clean(postingType));
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private Service getOwnedService(String serviceId, User user) {
        var service = serviceRepository.findById(clean(serviceId))
            .orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));

        if (!clean(service.getUserId()).equals(user.getId())) {
            throw new AccessDeniedException("You can only modify your own services.");
        }

        return service;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
            .map(this::normalizeTag)
            .filter(tag -> !tag.isBlank())
            .collect(Collectors.toList());
    }

    private String normalizeTag(String tag) {
        String normalizedTag = clean(tag).replaceAll("\\s+", " ");

        if (normalizedTag.isBlank()) {
            return "";
        }

        String lowerCaseTag = normalizedTag.toLowerCase(Locale.ROOT);
        StringBuilder titleCaseTag = new StringBuilder(lowerCaseTag.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < lowerCaseTag.length(); i++) {
            char current = lowerCaseTag.charAt(i);

            if (Character.isWhitespace(current)) {
                titleCaseTag.append(current);
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext && Character.isLetter(current)) {
                titleCaseTag.append(Character.toTitleCase(current));
            } else {
                titleCaseTag.append(current);
            }

            capitalizeNext = false;
        }

        return titleCaseTag.toString();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String getProviderName(String userId) {
        String cleanUserId = clean(userId);

        if (cleanUserId.isBlank()) {
            return "Service creator";
        }

        return userRepository.findById(cleanUserId)
            .map(this::getDisplayName)
            .orElse("Service creator");
    }

    private String getDisplayName(User user) {
        String fullName = (clean(user.getFirstName()) + " " + clean(user.getLastName())).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        String email = clean(user.getEmail());
        return email.isBlank() ? "Service creator" : email;
    }
}
