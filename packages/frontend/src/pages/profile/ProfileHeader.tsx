import type { UserProfile } from "./types";

interface ProfileHeaderProps {
  displayName: string;
  profile: UserProfile;
  ratingText: string;
  onReviewsClick: () => void;
}

export function ProfileHeader({
  displayName,
  profile,
  ratingText,
  onReviewsClick
}: ProfileHeaderProps) {
  return (
    <header className="profile-header">
      <div>
        <h1>{displayName}</h1>
        <p>{profile.email}</p>
        {(profile.major || profile.campus) && (
          <p>
            {[profile.major, profile.campus]
              .filter(Boolean)
              .join(" - ")}
          </p>
        )}
      </div>
      <div className="profile-header-stats">
        <p>{profile.services.length} services</p>
        <button
          type="button"
          className="profile-rating-button"
          onClick={onReviewsClick}
          aria-label="View your reviews">
          <p className="profile-rating-summary">
            <span aria-hidden="true">{"\u2605"}</span>
            {ratingText}
          </p>
        </button>
      </div>
    </header>
  );
}
