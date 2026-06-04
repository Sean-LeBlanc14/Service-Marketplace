import type { FormEvent } from "react";
import type { UserProfile } from "./types";

interface BioSectionProps {
  profile: UserProfile;
  bioDraft: string;
  bioMessage: string;
  error: string;
  isEditingBio: boolean;
  isSaving: boolean;
  onBioDraftChange: (value: string) => void;
  onCancel: () => void;
  onEdit: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function BioSection({
  profile,
  bioDraft,
  bioMessage,
  error,
  isEditingBio,
  isSaving,
  onBioDraftChange,
  onCancel,
  onEdit,
  onSubmit
}: BioSectionProps) {
  return (
    <section
      className="profile-section profile-bio"
      aria-label="Bio">
      <div className="section-heading">
        <div>
          <h2>Bio</h2>
          <p>Profile bio shown with your services.</p>
        </div>
        {!isEditingBio && (
          <button
            type="button"
            className="section-action-button"
            onClick={onEdit}>
            Edit Bio
          </button>
        )}
      </div>
      {profile.bio ? (
        <p>{profile.bio}</p>
      ) : (
        <p>Write a short bio for your profile.</p>
      )}
      {(bioMessage || error) && (
        <p
          role="status"
          className={`status-message ${error ? "form-error" : "form-success"}`}>
          {error || bioMessage}
        </p>
      )}
      {isEditingBio ? (
        <form
          aria-label="Edit profile bio"
          onSubmit={onSubmit}
          className="bio-form">
          <textarea
            aria-label="Profile bio"
            value={bioDraft}
            onChange={(event) =>
              onBioDraftChange(event.target.value)
            }
            placeholder="Bio"
            rows={4}
            className="bio-textarea"
          />
          <div className="bio-form-actions">
            <button
              type="submit"
              disabled={isSaving}
              className="primary-button">
              {isSaving ? "Saving..." : "Save Bio"}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={onCancel}>
              Cancel
            </button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
