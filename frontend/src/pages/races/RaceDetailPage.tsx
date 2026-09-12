import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { racesApi } from "../../api/races";
import type { Race, RaceStatus } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import { RaceRegistrations } from "./RaceRegistrations";
import { RaceResults } from "./RaceResults";

const STATUS_TONE: Record<RaceStatus, string> = {
  DRAFT: "stone",
  OPEN_FOR_REGISTRATION: "oasis",
  CLOSED_FOR_REGISTRATION: "amber",
  IN_PROGRESS: "gold",
  COMPLETED: "violet",
  CANCELLED: "rust",
};

/** Mirrors RaceService.ALLOWED_TRANSITIONS on the backend, so the dropdown
 * never offers a transition the API would reject with a 409. */
const ALLOWED_TRANSITIONS: Record<RaceStatus, RaceStatus[]> = {
  DRAFT: ["OPEN_FOR_REGISTRATION", "CANCELLED"],
  OPEN_FOR_REGISTRATION: ["CLOSED_FOR_REGISTRATION", "CANCELLED"],
  CLOSED_FOR_REGISTRATION: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
};

type FormState = {
  name: string;
  description: string;
  scheduledDateTime: string;
  startLocation: string;
  finishLocation: string;
  distanceMeters: string;
  maxParticipants: string;
  registrationDeadline: string;
};

function toFormState(r: Race): FormState {
  return {
    name: r.name,
    description: r.description ?? "",
    scheduledDateTime: r.scheduledDateTime.slice(0, 16),
    startLocation: r.startLocation,
    finishLocation: r.finishLocation,
    distanceMeters: String(r.distanceMeters),
    maxParticipants: String(r.maxParticipants),
    registrationDeadline: r.registrationDeadline.slice(0, 16),
  };
}

export function RaceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { canWrite } = useRoles();

  const [race, setRace] = useState<Race | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<FormState | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [statusDraft, setStatusDraft] = useState<RaceStatus | "">("");
  const [justApplied, setJustApplied] = useState(false);

  function load() {
    if (!id) return;
    setRace(null);
    setError(null);
    racesApi
      .getById(id)
      .then((data) => {
        setRace(data);
        setForm(toFormState(data));
        setStatusDraft("");
      })
      .catch((err) => {
        setError(
          err instanceof ApiError ? err.message : "Something went wrong loading this race."
        );
      });
  }

  useEffect(load, [id]);

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
        <Link to="/races" className="btn btn-secondary">
          Back to races
        </Link>
      </div>
    );
  }

  if (!race || !form) {
    return (
      <div className="state-box">
        <div className="spinner" />
        <p>Loading race...</p>
      </div>
    );
  }

  const nextStatuses = ALLOWED_TRANSITIONS[race.status];

  async function handleSave() {
    if (!id || !form) return;
    setSaving(true);
    setFormError(null);
    try {
      const updated = await racesApi.update(id, {
        name: form.name,
        description: form.description || undefined,
        scheduledDateTime: form.scheduledDateTime,
        startLocation: form.startLocation,
        finishLocation: form.finishLocation,
        distanceMeters: Number(form.distanceMeters),
        maxParticipants: Number(form.maxParticipants),
        registrationDeadline: form.registrationDeadline,
      });
      setRace(updated);
      setForm(toFormState(updated));
      setEditing(false);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't save changes.");
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusChange() {
    if (!id || !statusDraft) return;
    setSaving(true);
    setFormError(null);
    try {
      const updated = await racesApi.changeStatus(id, statusDraft);
      setRace(updated);
      setStatusDraft("");
      setJustApplied(true);
      setTimeout(() => setJustApplied(false), 900);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't change status.");
    } finally {
      setSaving(false);
    }
  }

  async function handleCancel() {
    if (!id || !race) return;
    if (!window.confirm(`Cancel "${race.name}"? This cannot be undone.`)) return;
    setSaving(true);
    try {
      await racesApi.cancel(id);
      navigate("/races");
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't cancel this race.");
      setSaving(false);
    }
  }

  const canCancel = race.status !== "COMPLETED" && race.status !== "CANCELLED";

  return (
    <div>
      <Link to="/races" className="page-breadcrumb">
        ← Back to races
      </Link>

      <div className="app-content__header">
        <h1>{race.name}</h1>
        {canWrite && !editing && race.status !== "COMPLETED" && (
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => setEditing(true)}>
              Edit
            </button>
            {canCancel && (
              <button className="btn btn-danger" onClick={handleCancel} disabled={saving}>
                Cancel race
              </button>
            )}
          </div>
        )}
      </div>

      {formError && (
        <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
          <p>{formError}</p>
        </div>
      )}

      {editing ? (
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
          <div style={{ display: "flex", gap: 8 }}>
            <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
              Save
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setForm(toFormState(race));
                setFormError(null);
                setEditing(false);
              }}
              disabled={saving}
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div className="detail-card competitor-hero">
          <div className="competitor-hero__body">
            <p className="competitor-hero__status-line">
              <span className={`status-${STATUS_TONE[race.status]}`} style={{ fontWeight: 700 }}>
                {race.status}
              </span>
              {" · "}
              {race.raceType}
            </p>

            <dl className="detail-grid">
              <div>
                <dt>Scheduled</dt>
                <dd>{new Date(race.scheduledDateTime).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Registration deadline</dt>
                <dd>{new Date(race.registrationDeadline).toLocaleString()}</dd>
              </div>
              <div>
                <dt>Route</dt>
                <dd>
                  {race.startLocation} → {race.finishLocation}
                </dd>
              </div>
              <div>
                <dt>Distance</dt>
                <dd>{race.distanceMeters}m</dd>
              </div>
              <div>
                <dt>Max participants</dt>
                <dd>{race.maxParticipants}</dd>
              </div>
              <div>
                <dt>Organizer</dt>
                <dd>{race.organizer}</dd>
              </div>
            </dl>
          </div>

          {canWrite && nextStatuses.length > 0 && (
            <div className="competitor-hero__status-panel">
              <span className="competitor-hero__status-panel-label">Change status</span>
              <select
                value={statusDraft}
                onChange={(e) => setStatusDraft(e.target.value as RaceStatus)}
              >
                <option value="">Select...</option>
                {nextStatuses.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
              <button
                className={`btn btn-secondary${justApplied ? " btn--confirmed" : ""}`}
                onClick={handleStatusChange}
                disabled={saving || !statusDraft}
              >
                {justApplied ? "Applied ✓" : "Apply"}
              </button>
            </div>
          )}
        </div>
      )}
            {!editing && (
        <div style={{ marginTop: 32 }}>
          <RaceRegistrations race={race} />
          <div style={{ marginTop: 32 }}>
            <RaceResults race={race} />
          </div>
        </div>
      )}
    </div>
  );
}