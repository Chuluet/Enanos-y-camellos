import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { racesApi } from "../../api/races";
import type { RaceType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useAuth } from "react-oidc-context";
import dwarfCamelHero from "../../assets/japan.jpg";

type FormState = {
  name: string;
  description: string;
  scheduledDateTime: string;
  startLocation: string;
  finishLocation: string;
  distanceMeters: string;
  maxParticipants: string;
  raceType: RaceType;
  registrationDeadline: string;
};

const EMPTY_FORM: FormState = {
  name: "",
  description: "",
  scheduledDateTime: "",
  startLocation: "",
  finishLocation: "",
  distanceMeters: "",
  maxParticipants: "2",
  raceType: "MIXED",
  registrationDeadline: "",
};

function sanitizeNumeric(value: string): string {
  return value.replace(/[^0-9]/g, "");
}

export function RaceFormPage() {
  const navigate = useNavigate();
  const auth = useAuth();

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
    if (fieldErrors[key]) {
      setFieldErrors((errs) => {
        const next = { ...errs };
        delete next[key];
        return next;
      });
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    setFieldErrors({});

    try {
      const race = await racesApi.create({
        name: form.name,
        description: form.description || undefined,
        scheduledDateTime: form.scheduledDateTime,
        startLocation: form.startLocation,
        finishLocation: form.finishLocation,
        distanceMeters: Number(form.distanceMeters),
        maxParticipants: Number(form.maxParticipants),
        raceType: form.raceType,
        organizer: auth.user?.profile.preferred_username ?? "unknown",
        registrationDeadline: form.registrationDeadline,
      });
      navigate(`/races/${race.id}`, { state: { justCreated: true } });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.body?.validationErrors) {
          setFieldErrors(err.body.validationErrors);
        } else if (err.status === 409) {
          setFormError(err.body?.message ?? "This race conflicts with an existing one.");
        } else if (err.status === 400) {
          setFormError(err.body?.message ?? "The request couldn't be processed.");
        } else if (err.status === 403) {
          setFormError("You do not have permission to create a race.");
        } else {
          setFormError(err.body?.message ?? "Something went wrong creating this race.");
        }
      } else {
        setFormError("Something went wrong creating this race.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfCamelHero})` }} />
      <div>
        <Link to="/races" className="page-breadcrumb">
          ← Back to races
        </Link>

        <h1 className="page-title-banner page-title-banner--races">New race</h1>

        <div className="form-header">
          <p className="app-content__subtitle" style={{ marginBottom: 0 }}>
            Set up a new event for the league.
          </p>
        </div>

        {formError && (
          <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
            <p>{formError}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="detail-card form-card">
          <div className="form-section">
            <h3>Race details</h3>
            <div className="form-grid">
              <div className="form-field">
                <label>Name</label>
                <input value={form.name} onChange={(e) => updateField("name", e.target.value)} />
                {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
              </div>
              <div className="form-field">
                <label>Description</label>
                <input
                  value={form.description}
                  onChange={(e) => updateField("description", e.target.value)}
                />
                {fieldErrors.description && <span className="field-error">{fieldErrors.description}</span>}
              </div>
              <div className="form-field">
                <label>Race type</label>
                <select
                  value={form.raceType}
                  onChange={(e) => updateField("raceType", e.target.value as RaceType)}
                >
                  <option value="INDIVIDUAL">Individual</option>
                  <option value="TEAM">Team</option>
                  <option value="MIXED">Mixed</option>
                </select>
                {fieldErrors.raceType && <span className="field-error">{fieldErrors.raceType}</span>}
              </div>
            </div>
          </div>

          <div className="form-section">
            <h3>Route</h3>
            <div className="form-grid">
              <div className="form-field">
                <label>Start location</label>
                <input
                  value={form.startLocation}
                  onChange={(e) => updateField("startLocation", e.target.value)}
                />
                {fieldErrors.startLocation && <span className="field-error">{fieldErrors.startLocation}</span>}
              </div>
              <div className="form-field">
                <label>Finish location</label>
                <input
                  value={form.finishLocation}
                  onChange={(e) => updateField("finishLocation", e.target.value)}
                />
                {fieldErrors.finishLocation && <span className="field-error">{fieldErrors.finishLocation}</span>}
              </div>
              <div className="form-field">
                <label>Distance (meters)</label>
                <input
                  type="text"
                  inputMode="numeric"
                  value={form.distanceMeters}
                  onChange={(e) => updateField("distanceMeters", sanitizeNumeric(e.target.value))}
                />
                {fieldErrors.distanceMeters && <span className="field-error">{fieldErrors.distanceMeters}</span>}
              </div>
              <div className="form-field">
                <label>Max participants</label>
                <input
                  type="text"
                  inputMode="numeric"
                  value={form.maxParticipants}
                  onChange={(e) => updateField("maxParticipants", sanitizeNumeric(e.target.value))}
                />
                {fieldErrors.maxParticipants && <span className="field-error">{fieldErrors.maxParticipants}</span>}
              </div>
            </div>
          </div>

          <div className="form-section">
            <h3>Schedule</h3>
            <div className="form-grid">
              <div className="form-field">
                <label>Scheduled date/time</label>
                <input
                  type="datetime-local"
                  value={form.scheduledDateTime}
                  onChange={(e) => updateField("scheduledDateTime", e.target.value)}
                />
                {fieldErrors.scheduledDateTime && (
                  <span className="field-error">{fieldErrors.scheduledDateTime}</span>
                )}
              </div>
              <div className="form-field">
                <label>Registration deadline</label>
                <input
                  type="datetime-local"
                  value={form.registrationDeadline}
                  onChange={(e) => updateField("registrationDeadline", e.target.value)}
                />
                {fieldErrors.registrationDeadline && (
                  <span className="field-error">{fieldErrors.registrationDeadline}</span>
                )}
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Creating..." : "Create race"}
            </button>
            <Link to="/races" className="btn btn-secondary">
              Cancel
            </Link>
          </div>
        </form>
      </div>
    </>
  );
}