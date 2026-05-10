import {
  useEffect,
  useMemo,
  useState,
  type FormEvent
} from "react";
import "./ProfilePage.css";

const LISTINGS_STORAGE_KEY =
  "service-marketplace-profile-listings";

type ListingStatus = "active" | "taken-down";

interface ServiceListing {
  id: string;
  title: string;
  description: string;
  price: string;
  isHourly: boolean;
  location: string;
  tags: string[];
  status: ListingStatus;
  createdAt: string;
  updatedAt: string;
}

interface ListingForm {
  title: string;
  description: string;
  price: string;
  isHourly: boolean;
  location: string;
  tags: string;
}

const emptyForm: ListingForm = {
  title: "",
  description: "",
  price: "",
  isHourly: false,
  location: "",
  tags: ""
};

function formatPrice(price: string, isHourly: boolean) {
  const cleanPrice = price
    .replace(/^\$/, "")
    .replace(/\/hr$/, "")
    .trim();
  const displayPrice = cleanPrice ? `$${cleanPrice}` : "$0";

  return isHourly ? `${displayPrice}/hr` : displayPrice;
}

function loadListings(): ServiceListing[] {
  const storedListings = window.localStorage.getItem(
    LISTINGS_STORAGE_KEY
  );

  if (!storedListings) {
    return [];
  }

  try {
    const parsedListings = JSON.parse(
      storedListings
    ) as Partial<ServiceListing>[];

    if (!Array.isArray(parsedListings)) {
      return [];
    }

    return parsedListings.map((listing) => {
      const savedPrice = listing.price ?? "";
      const cleanPrice = savedPrice
        .replace(/^\$/, "")
        .replace(/\/hr$/, "")
        .trim();

      return {
        id: listing.id ?? crypto.randomUUID(),
        title: listing.title ?? "",
        description: listing.description ?? "",
        price: cleanPrice,
        isHourly:
          listing.isHourly ?? savedPrice.endsWith("/hr"),
        location: listing.location ?? "",
        tags: Array.isArray(listing.tags) ? listing.tags : [],
        status:
          listing.status === "taken-down"
            ? "taken-down"
            : "active",
        createdAt:
          listing.createdAt ?? new Date().toISOString(),
        updatedAt: listing.updatedAt ?? new Date().toISOString()
      };
    });
  } catch {
    return [];
  }
}

function ProfilePage() {
  const [listings, setListings] =
    useState<ServiceListing[]>(loadListings);
  const [form, setForm] = useState<ListingForm>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(
    null
  );
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [visibleListingStatus, setVisibleListingStatus] =
    useState<ListingStatus | null>(null);
  const [error, setError] = useState("");

  const activeListings = useMemo(
    () =>
      listings.filter((listing) => listing.status === "active"),
    [listings]
  );

  const takenDownListings = useMemo(
    () =>
      listings.filter(
        (listing) => listing.status === "taken-down"
      ),
    [listings]
  );

  useEffect(() => {
    window.localStorage.setItem(
      LISTINGS_STORAGE_KEY,
      JSON.stringify(listings)
    );
  }, [listings]);

  const updateForm = (
    field: keyof ListingForm,
    value: string
  ) => {
    setForm((currentForm) => ({
      ...currentForm,
      [field]: value
    }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setIsFormOpen(false);
    setError("");
  };

  const openCreateForm = () => {
    setForm(emptyForm);
    setEditingId(null);
    setIsFormOpen(true);
    setError("");
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const title = form.title.trim();
    const description = form.description.trim();
    const price = form.price.replace(/^\$/, "").trim();
    const location = form.location.trim();
    const tags = form.tags
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean);

    if (!title || !description || !price || !location) {
      setError(
        "Please fill in the title, description, price, and location."
      );
      return;
    }

    const now = new Date().toISOString();

    if (editingId) {
      setListings((currentListings) =>
        currentListings.map((listing) =>
          listing.id === editingId
            ? {
                ...listing,
                title,
                description,
                price,
                isHourly: form.isHourly,
                location,
                tags,
                updatedAt: now
              }
            : listing
        )
      );
    } else {
      setListings((currentListings) => [
        {
          id: crypto.randomUUID(),
          title,
          description,
          price,
          isHourly: form.isHourly,
          location,
          tags,
          status: "active",
          createdAt: now,
          updatedAt: now
        },
        ...currentListings
      ]);
    }

    resetForm();
  };

  const editListing = (listing: ServiceListing) => {
    setForm({
      title: listing.title,
      description: listing.description,
      price: listing.price,
      isHourly: listing.isHourly,
      location: listing.location,
      tags: listing.tags.join(", ")
    });
    setEditingId(listing.id);
    setIsFormOpen(true);
    setError("");
  };

  const updateListingStatus = (
    listingId: string,
    status: ListingStatus
  ) => {
    setListings((currentListings) =>
      currentListings.map((listing) =>
        listing.id === listingId
          ? {
              ...listing,
              status,
              updatedAt: new Date().toISOString()
            }
          : listing
      )
    );

    if (editingId === listingId) {
      resetForm();
    }
  };

  return (
    <main className="profile-screen">
      <header className="profile-header">
        <h1>Profile</h1>
        <p>{activeListings.length} active services</p>
      </header>

      <section
        className="profile-section profile-bio"
        aria-label="Bio">
        <h2>Bio</h2>
        <p>
          Your personal information can live here once signup
          and login are ready.
        </p>
      </section>

      <section
        className="profile-section services-section"
        aria-label="Services">
        <div className="section-heading">
          <div>
            <h2>Services</h2>
            <p>
              Create and manage the services shown on your
              profile.
            </p>
            {!isFormOpen && (
              <button
                className="primary-button"
                type="button"
                onClick={openCreateForm}>
                Add service
              </button>
            )}
          </div>
        </div>

        {isFormOpen && (
          <form
            className="listing-form"
            onSubmit={handleSubmit}>
            <div className="form-row form-row-full">
              <label htmlFor="listing-title">Title</label>
              <input
                id="listing-title"
                type="text"
                value={form.title}
                onChange={(event) =>
                  updateForm("title", event.target.value)
                }
                placeholder="Calculus tutoring"
              />
            </div>

            <div className="service-details-row">
              <div className="form-row">
                <label htmlFor="listing-location">
                  Location
                </label>
                <input
                  id="listing-location"
                  type="text"
                  value={form.location}
                  onChange={(event) =>
                    updateForm("location", event.target.value)
                  }
                  placeholder="Main Library or Online"
                />
              </div>

              <div className="pricing-details">
                <div className="form-row">
                  <label htmlFor="listing-price">Price</label>
                  <div className="price-input">
                    <span>$</span>
                    <input
                      id="listing-price"
                      type="number"
                      min="0"
                      step="0.01"
                      value={form.price}
                      onChange={(event) =>
                        updateForm("price", event.target.value)
                      }
                      placeholder="25"
                    />
                  </div>
                </div>

                <label
                  className="checkbox-row"
                  htmlFor="listing-hourly">
                  <input
                    id="listing-hourly"
                    type="checkbox"
                    checked={form.isHourly}
                    onChange={(event) =>
                      setForm((currentForm) => ({
                        ...currentForm,
                        isHourly: event.target.checked
                      }))
                    }
                  />
                  Hourly
                </label>
              </div>
            </div>

            <div className="form-row form-row-full">
              <label htmlFor="listing-tags">Tags</label>
              <input
                id="listing-tags"
                type="text"
                value={form.tags}
                onChange={(event) =>
                  updateForm("tags", event.target.value)
                }
                placeholder="Mathematics, One-on-one, Group sessions"
              />
            </div>

            <div className="form-row form-row-full">
              <label htmlFor="listing-description">
                Description
              </label>
              <textarea
                id="listing-description"
                value={form.description}
                onChange={(event) =>
                  updateForm("description", event.target.value)
                }
                placeholder="Describe what you offer, availability, and any details students should know."
                rows={4}
              />
            </div>

            {error && <p className="form-error">{error}</p>}

            <div className="form-actions">
              <button
                className="secondary-button"
                type="button"
                onClick={resetForm}>
                Cancel
              </button>
              <button className="primary-button" type="submit">
                {editingId ? "Save changes" : "Create listing"}
              </button>
            </div>
          </form>
        )}

        <div className="listing-groups">
          <div
            className="listing-view-actions"
            aria-label="Service list views">
            <button
              className={
                visibleListingStatus === "active"
                  ? "active-view"
                  : ""
              }
              aria-pressed={visibleListingStatus === "active"}
              type="button"
              onClick={() =>
                setVisibleListingStatus((currentStatus) =>
                  currentStatus === "active" ? null : "active"
                )
              }>
              Active ({activeListings.length})
            </button>
            <button
              className={
                visibleListingStatus === "taken-down"
                  ? "active-view"
                  : ""
              }
              aria-pressed={
                visibleListingStatus === "taken-down"
              }
              type="button"
              onClick={() =>
                setVisibleListingStatus((currentStatus) =>
                  currentStatus === "taken-down"
                    ? null
                    : "taken-down"
                )
              }>
              Taken down ({takenDownListings.length})
            </button>
          </div>

          {visibleListingStatus === "active" && (
            <div className="listing-group">
              <h3>Active</h3>
              {activeListings.length === 0 ? (
                <p className="empty-state">
                  No active services yet.
                </p>
              ) : (
                <div className="listing-grid">
                  {activeListings.map((listing) => (
                    <article
                      className="listing-card"
                      key={listing.id}>
                      <div>
                        <div className="listing-card-heading">
                          <h4>{listing.title}</h4>
                        </div>
                        <p className="listing-description">
                          {listing.description}
                        </p>
                        <p className="listing-location">
                          {listing.location}
                        </p>
                        {listing.tags.length > 0 && (
                          <div
                            className="listing-tags"
                            aria-label="Service tags">
                            {listing.tags.map((tag) => (
                              <span key={tag}>{tag}</span>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="listing-card-footer">
                        <strong>
                          {formatPrice(
                            listing.price,
                            listing.isHourly
                          )}
                        </strong>
                        <div className="listing-actions">
                          <button
                            type="button"
                            onClick={() =>
                              editListing(listing)
                            }>
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              updateListingStatus(
                                listing.id,
                                "taken-down"
                              )
                            }>
                            Take down
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          )}

          {visibleListingStatus === "taken-down" && (
            <div className="listing-group">
              <h3>Taken down</h3>
              {takenDownListings.length === 0 ? (
                <p className="empty-state">
                  No taken-down services yet.
                </p>
              ) : (
                <div className="listing-grid">
                  {takenDownListings.map((listing) => (
                    <article
                      className="listing-card listing-card-muted"
                      key={listing.id}>
                      <div>
                        <div className="listing-card-heading">
                          <h4>{listing.title}</h4>
                        </div>
                        <p className="listing-description">
                          {listing.description}
                        </p>
                        <p className="listing-location">
                          {listing.location}
                        </p>
                        {listing.tags.length > 0 && (
                          <div
                            className="listing-tags"
                            aria-label="Service tags">
                            {listing.tags.map((tag) => (
                              <span key={tag}>{tag}</span>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="listing-card-footer">
                        <strong>
                          {formatPrice(
                            listing.price,
                            listing.isHourly
                          )}
                        </strong>
                        <div className="listing-actions">
                          <button
                            type="button"
                            onClick={() =>
                              editListing(listing)
                            }>
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              updateListingStatus(
                                listing.id,
                                "active"
                              )
                            }>
                            Relist
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </main>
  );
}

export default ProfilePage;
