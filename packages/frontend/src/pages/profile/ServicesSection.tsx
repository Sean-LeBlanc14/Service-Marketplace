import type { FormEvent } from "react";
import {
  formatPriceUnit,
  PRICE_UNIT_OPTIONS
} from "../../utils/pricing";
import {
  NO_PRICE_UNIT_VALUE,
  SERVICE_CATEGORY_OPTIONS,
  SERVICE_DESCRIPTION_MAX_LENGTH,
  SERVICE_TITLE_MAX_LENGTH
} from "./constants";
import type {
  ServiceListing,
  ServicePricingType
} from "./types";
import {
  formatCategory,
  formatPrice,
  isPriceInputValue
} from "./utils";

interface ServicesSectionProps {
  deletingServiceId: string | null;
  hasCustomPriceUnit: boolean;
  isCreatingService: boolean;
  isEditingService: boolean;
  isServiceFormOpen: boolean;
  serviceCategory: string;
  serviceDescription: string;
  serviceLocation: string;
  serviceMaxPrice: string;
  serviceMessage: string;
  serviceMinPrice: string;
  servicePrice: string;
  servicePriceUnit: string;
  servicePricingType: ServicePricingType;
  services: ServiceListing[];
  serviceTags: string;
  serviceTitle: string;
  onCancelServiceForm: () => void;
  onEditService: (service: ServiceListing) => void;
  onOpenCreateService: () => void;
  onRequestDeleteService: (service: ServiceListing) => void;
  onServiceCategoryChange: (value: string) => void;
  onServiceDescriptionChange: (value: string) => void;
  onServiceLocationChange: (value: string) => void;
  onServiceMaxPriceChange: (value: string) => void;
  onServiceMinPriceChange: (value: string) => void;
  onServicePriceChange: (value: string) => void;
  onServicePriceUnitChange: (value: string) => void;
  onServicePricingTypeChange: (value: ServicePricingType) => void;
  onServiceTagsChange: (value: string) => void;
  onServiceTitleChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function ServicesSection({
  deletingServiceId,
  hasCustomPriceUnit,
  isCreatingService,
  isEditingService,
  isServiceFormOpen,
  serviceCategory,
  serviceDescription,
  serviceLocation,
  serviceMaxPrice,
  serviceMessage,
  serviceMinPrice,
  servicePrice,
  servicePriceUnit,
  servicePricingType,
  services,
  serviceTags,
  serviceTitle,
  onCancelServiceForm,
  onEditService,
  onOpenCreateService,
  onRequestDeleteService,
  onServiceCategoryChange,
  onServiceDescriptionChange,
  onServiceLocationChange,
  onServiceMaxPriceChange,
  onServiceMinPriceChange,
  onServicePriceChange,
  onServicePriceUnitChange,
  onServicePricingTypeChange,
  onServiceTagsChange,
  onServiceTitleChange,
  onSubmit
}: ServicesSectionProps) {
  return (
    <section
      className="profile-section services-section"
      aria-label="Services">
      <div className="section-heading">
        <div>
          <h2>Services</h2>
          <p>Services shown on this profile.</p>
        </div>
        {!isServiceFormOpen && (
          <button
            type="button"
            className="section-action-button"
            onClick={onOpenCreateService}>
            Create Service
          </button>
        )}
      </div>

      {!isServiceFormOpen && serviceMessage && (
        <p role="status" className="form-success">
          {serviceMessage}
        </p>
      )}

      {isServiceFormOpen && (
        <form
          className="service-form"
          aria-label={
            isEditingService
              ? "Edit service listing"
              : "Create service listing"
          }
          onSubmit={onSubmit}>
          <div className="service-form-header">
            <h3>
              {isEditingService ? "Edit Service" : "Create Service"}
            </h3>
          </div>
          <div className="service-form-grid">
            <label>
              <span>Title</span>
              <input
                value={serviceTitle}
                onChange={(event) =>
                  onServiceTitleChange(event.target.value)
                }
                maxLength={SERVICE_TITLE_MAX_LENGTH}
                placeholder="Calculus tutoring"
                required
              />
            </label>

            <label>
              <span>Category</span>
              <select
                value={serviceCategory}
                onChange={(event) =>
                  onServiceCategoryChange(event.target.value)
                }
                required>
                <option value="" disabled hidden>
                  Select a category
                </option>
                {SERVICE_CATEGORY_OPTIONS.map((category) => (
                  <option
                    key={category.value}
                    value={category.value}>
                    {category.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="service-pricing">
            <div className="service-pricing-heading">
              <span>Pricing</span>
              <div
                className="pricing-toggle"
                aria-label="Choose pricing type">
                <button
                  type="button"
                  className={
                    servicePricingType === "flat" ? "active" : ""
                  }
                  aria-pressed={servicePricingType === "flat"}
                  onClick={() =>
                    onServicePricingTypeChange("flat")
                  }>
                  Flat Price
                </button>
                <button
                  type="button"
                  className={
                    servicePricingType === "range"
                      ? "active"
                      : ""
                  }
                  aria-pressed={servicePricingType === "range"}
                  onClick={() =>
                    onServicePricingTypeChange("range")
                  }>
                  Range
                </button>
              </div>
            </div>

            <div
              className={`service-form-grid service-price-grid ${
                servicePricingType === "flat"
                  ? "service-price-grid-flat"
                  : ""
              }`}>
              {servicePricingType === "flat" ? (
                <label>
                  <span>Price</span>
                  <input
                    value={servicePrice}
                    onChange={(event) => {
                      const nextPrice = event.target.value;

                      if (!isPriceInputValue(nextPrice)) {
                        return;
                      }

                      onServicePriceChange(nextPrice);
                    }}
                    inputMode="decimal"
                    pattern="[0-9]*[.]?[0-9]{0,2}"
                    type="text"
                    placeholder="$"
                    required
                  />
                </label>
              ) : (
                <>
                  <label>
                    <span>Price Min</span>
                    <input
                      value={serviceMinPrice}
                      onChange={(event) => {
                        const nextPrice = event.target.value;

                        if (!isPriceInputValue(nextPrice)) {
                          return;
                        }

                        onServiceMinPriceChange(nextPrice);
                      }}
                      inputMode="decimal"
                      pattern="[0-9]*[.]?[0-9]{0,2}"
                      type="text"
                      placeholder="$"
                      required
                    />
                  </label>

                  <label>
                    <span>Price Max</span>
                    <input
                      value={serviceMaxPrice}
                      onChange={(event) => {
                        const nextPrice = event.target.value;

                        if (!isPriceInputValue(nextPrice)) {
                          return;
                        }

                        onServiceMaxPriceChange(nextPrice);
                      }}
                      inputMode="decimal"
                      pattern="[0-9]*[.]?[0-9]{0,2}"
                      type="text"
                      placeholder="$$$"
                      required
                    />
                  </label>
                </>
              )}

              <label>
                <span>Price Unit (optional)</span>
                <select
                  value={servicePriceUnit}
                  onChange={(event) =>
                    onServicePriceUnitChange(event.target.value)
                  }>
                  <option value="" disabled hidden>
                    Select a price unit
                  </option>
                  <option value={NO_PRICE_UNIT_VALUE}>N/A</option>
                  {hasCustomPriceUnit && (
                    <option value={servicePriceUnit}>
                      {formatPriceUnit(servicePriceUnit)}
                    </option>
                  )}
                  {PRICE_UNIT_OPTIONS.map((unit) => (
                    <option key={unit.value} value={unit.value}>
                      {unit.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          <label>
            <span>Description</span>
            <textarea
              value={serviceDescription}
              onChange={(event) =>
                onServiceDescriptionChange(event.target.value)
              }
              maxLength={SERVICE_DESCRIPTION_MAX_LENGTH}
              placeholder="Describe what you are offering"
              rows={4}
              required
            />
          </label>

          <label>
            <span>Location</span>
            <input
              value={serviceLocation}
              onChange={(event) =>
                onServiceLocationChange(event.target.value)
              }
              placeholder="Campus or address"
              required
            />
          </label>

          <label>
            <span>Tags (optional)</span>
            <textarea
              value={serviceTags}
              onChange={(event) =>
                onServiceTagsChange(event.target.value)
              }
              placeholder="e.g. Python, Data Science, Algorithms"
              rows={2}
            />
          </label>

          <div className="service-form-actions">
            <button type="submit" disabled={isCreatingService}>
              {isCreatingService
                ? isEditingService
                  ? "Saving..."
                  : "Creating..."
                : isEditingService
                  ? "Save Changes"
                  : "Create Service"}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={onCancelServiceForm}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {services.length === 0 ? (
        <p className="empty-state">
          No services are listed on this profile yet.
        </p>
      ) : (
        <div className="listing-grid">
          {services.map((service) => (
            <article className="listing-card" key={service.id}>
              <div>
                <div className="listing-card-heading">
                  <h3>{service.title}</h3>
                  <div
                    className="listing-card-actions"
                    role="group"
                    aria-label={`${service.title} actions`}>
                    <button
                      type="button"
                      onClick={() => onEditService(service)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="danger-button"
                      disabled={deletingServiceId === service.id}
                      onClick={() =>
                        onRequestDeleteService(service)
                      }>
                      {deletingServiceId === service.id
                        ? "Taking down..."
                        : "Take Down"}
                    </button>
                  </div>
                </div>
                {service.category && (
                  <p className="listing-category">
                    {formatCategory(service.category)}
                  </p>
                )}
                <p className="listing-description">
                  {service.description}
                </p>
                <p className="listing-location">
                  <span
                    aria-hidden="true"
                    className="listing-location-pin">
                    <svg viewBox="0 0 24 24" focusable="false">
                      <path d="M12 21s7-6.1 7-12A7 7 0 0 0 5 9c0 5.9 7 12 7 12Z" />
                      <circle cx="12" cy="9" r="2.4" />
                    </svg>
                  </span>
                  {service.location}
                </p>
                {service.tags.length > 0 && (
                  <div
                    className="listing-tags"
                    aria-label="Service tags">
                    {service.tags.map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                )}
              </div>
              <div className="listing-card-footer">
                <strong>{formatPrice(service)}</strong>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
