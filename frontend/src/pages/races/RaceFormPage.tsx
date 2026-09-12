import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { racesApi } from "../../api/races";
import type { RaceType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useAuth } from "react-oidc-context";

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

export function RaceFormPage() {
  const navigate = useNavigate();
  const auth = useAuth();
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit() {
    setSaving(true);
    setError(null);
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
      navigate(`/races/${race.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't create the race.");
      setSaving(false);
    }
  }

  return (
    <div>
      <Link to="/races" className="page-breadcrumb">
        ← Back to races
      </Link>
      <h1>New race</h1>

      {error && (
        <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
          <p>{error}</p>
        </div>
      )}

      <div className="detail-card">
        <div className="detail-grid">
          <div className="form-field">
            <label>Name</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          </div>
          <div className="form-field">
            <label>Description</label>
            <input
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Race type</label>
            <select
              value={form.raceType}
              onChange={(e) => setForm({ ...form, raceType: e.target.value as RaceType })}
            >
              <option value="INDIVIDUAL">Individual</option>
              <option value="TEAM">Team</option>
              <option value="MIXED">Mixed</option>
            </select>
          </div>
          <div className="form-field">
            <label>Scheduled date/time</label>
            <input
              type="datetime-local"
              value={form.scheduledDateTime}
              onChange={(e) => setForm({ ...form, scheduledDateTime: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Registration deadline</label>
            <input
              type="datetime-local"
              value={form.registrationDeadline}
              onChange={(e) => setForm({ ...form, registrationDeadline: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Start location</label>
            <input
              value={form.startLocation}
              onChange={(e) => setForm({ ...form, startLocation: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Finish location</label>
            <input
              value={form.finishLocation}
              onChange={(e) => setForm({ ...form, finishLocation: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Distance (meters)</label>
            <input
              type="number"
              value={form.distanceMeters}
              onChange={(e) => setForm({ ...form, distanceMeters: e.target.value })}
            />
          </div>
          <div className="form-field">
            <label>Max participants</label>
            <input
              type="number"
              value={form.maxParticipants}
              onChange={(e) => setForm({ ...form, maxParticipants: e.target.value })}
            />
          </div>
        </div>
        <button className="btn btn-primary" onClick={handleSubmit} disabled={saving}>
          {saving ? "Creating..." : "Create race"}
        </button>
      </div>
    </div>
  );
}