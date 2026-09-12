import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { competitorsApi } from "../../api/competitors";
import type { CompetitorResponse, CompetitorStatus, CompetitorType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import { COMPETITOR_AVATAR } from "../../assets/competitorAvatars.ts";
import camelBg from "../../assets/camel-bg.jpg";

const STATUS_TONE: Record<CompetitorStatus, string> = {
  ACTIVE: "oasis",
  INJURED: "rust",
  SUSPENDED: "gold",
  RETIRED: "stone",
};

type FormState = {
  name: string;
  nickname: string;
  competitorType: CompetitorType;
  dateOfBirth: string;
  approximateAge: string;
  height: string;
  weight: string;
  origin: string;
};

function toFormState(c: CompetitorResponse): FormState {
  return {
    name: c.name,
    nickname: c.nickname,
    competitorType: c.competitorType,
    dateOfBirth: c.dateOfBirth ?? "",
    approximateAge: c.approximateAge?.toString() ?? "",
    height: String(c.height),
    weight: String(c.weight),
    origin: c.origin,
  };
}

export function CompetitorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAdministrator } = useRoles();

  const [competitor, setCompetitor] = useState<CompetitorResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<FormState | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [statusDraft, setStatusDraft] = useState<CompetitorStatus | "">("");
  const [justApplied, setJustApplied] = useState(false);

  function load() {
    if (!id) return;
    setCompetitor(null);
    setError(null);
    competitorsApi
        .getById(id)
        .then((data) => {
          setCompetitor(data);
          setForm(toFormState(data));
          setStatusDraft(data.status);
        })
        .catch((err) => {
          setError(err instanceof ApiError ? err.message : "Something went wrong loading this competitor.");
        });
  }

  useEffect(load, [id]);

  const bg = <div className="page-bg-fixed" style={{ backgroundImage: `url(${camelBg})` }} />;

  if (error) {
    return (
        <>
          {bg}
          <div className="state-box state-box--error">
            <p>{error}</p>
            <Link to="/competitors" className="btn btn-secondary">
              Back to competitors
            </Link>
          </div>
        </>
    );
  }

  if (!competitor || !form) {
    return (
        <>
          {bg}
          <div className="state-box">
            <div className="spinner" />
            <p>Loading competitor...</p>
          </div>
        </>
    );
  }

  async function handleSave() {
    if (!id || !form) return;
    setSaving(true);
    setFormError(null);
    try {
      const height = Number(form.height);
      const weight = Number(form.weight);
      if (!form.dateOfBirth && !form.approximateAge) {
        throw new Error("Provide a date of birth or an approximate age.");
      }
      const updated = await competitorsApi.update(id, {
        name: form.name,
        nickname: form.nickname,
        competitorType: form.competitorType,
        dateOfBirth: form.dateOfBirth || undefined,
        approximateAge: form.approximateAge ? Number(form.approximateAge) : undefined,
        height,
        weight,
        origin: form.origin,
      });
      setCompetitor(updated);
      setForm(toFormState(updated));
      setEditing(false);
    } catch (err) {
      setFormError(
          err instanceof ApiError ? err.message : err instanceof Error ? err.message : "Couldn't save changes."
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusChange() {
    if (!id || !statusDraft || !competitor || statusDraft === competitor.status) return;
    setSaving(true);
    setFormError(null);
    try {
      const updated = await competitorsApi.changeStatus(id, statusDraft);
      setCompetitor(updated);
      setJustApplied(true);
      setTimeout(() => setJustApplied(false), 900);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't change status.");
    } finally {
      setSaving(false);
    }
  }

  async function handleRetire() {
    if (!id || !competitor) return;
    if (
        !window.confirm(
            `Remove ${competitor.name}? If they have no official race results this deletes them permanently; otherwise they'll be retired and their race history kept.`
        )
    ) {
      return;
    }
    setSaving(true);
    try {
      await competitorsApi.retire(id);
      navigate("/competitors");
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't retire this competitor.");
      setSaving(false);
    }
  }

  const avatarSrc = COMPETITOR_AVATAR[competitor.competitorType];

  return (
      <>
        {bg}
        <div>
          <Link to="/competitors" className="page-breadcrumb">
            ← Back to competitors
          </Link>

          <div className="app-content__header">
            <h1>
              {competitor.name}{" "}
              <span className="sheet-row__nickname" style={{ fontSize: "0.7em" }}>
              "{competitor.nickname}"
            </span>
            </h1>
            {isAdministrator && !editing && (
                <div style={{ display: "flex", gap: 12 }}>
                  <button className="btn btn-secondary" onClick={() => setEditing(true)}>
                    Edit
                  </button>
                  {competitor.status !== "RETIRED" && (
                      <button className="btn btn-danger" onClick={handleRetire} disabled={saving}>
                        Retire
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
              <div className="detail-card form-card">
                <div className="detail-grid">
                  <div className="form-field">
                    <label>Name</label>
                    <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                  </div>
                  <div className="form-field">
                    <label>Nickname</label>
                    <input value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} />
                  </div>
                  <div className="form-field">
                    <label>Type</label>
                    <select
                        value={form.competitorType}
                        onChange={(e) => setForm({ ...form, competitorType: e.target.value as CompetitorType })}
                    >
                      <option value="CAMEL">Camel</option>
                      <option value="DWARF">Dwarf</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  <div className="form-field">
                    <label>Origin</label>
                    <input value={form.origin} onChange={(e) => setForm({ ...form, origin: e.target.value })} />
                  </div>
                  <div className="form-field">
                    <label>Date of birth</label>
                    <input
                        type="date"
                        value={form.dateOfBirth}
                        onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                    />
                  </div>
                  <div className="form-field">
                    <label>Approximate age (if no date of birth)</label>
                    <input
                        type="number"
                        value={form.approximateAge}
                        onChange={(e) => setForm({ ...form, approximateAge: e.target.value })}
                    />
                  </div>
                  <div className="form-field">
                    <label>Height (cm)</label>
                    <input
                        type="number"
                        step="0.1"
                        value={form.height}
                        onChange={(e) => setForm({ ...form, height: e.target.value })}
                    />
                  </div>
                  <div className="form-field">
                    <label>Weight (kg)</label>
                    <input
                        type="number"
                        step="0.1"
                        value={form.weight}
                        onChange={(e) => setForm({ ...form, weight: e.target.value })}
                    />
                  </div>
                </div>
                <div className="form-actions">
                  <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                    Save
                  </button>
                  <button
                      className="btn btn-secondary"
                      onClick={() => {
                        setForm(toFormState(competitor));
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
                <img src={avatarSrc} alt="" className="competitor-hero__avatar" />

                <div className="competitor-hero__body">
                  <p className="competitor-hero__status-line">
                <span className={`sheet-row__status status-${STATUS_TONE[competitor.status]}`}>
                  {competitor.status}
                </span>
                    {" · "}
                    {competitor.team ? (
                        <Link to={`/teams/${competitor.team.id}`}>{competitor.team.name}</Link>
                    ) : (
                        "No team"
                    )}
                  </p>

                  <dl className="detail-grid">
                    <div>
                      <dt>Origin</dt>
                      <dd>{competitor.origin}</dd>
                    </div>
                    <div>
                      <dt>Age</dt>
                      <dd>
                        {competitor.dateOfBirth
                            ? new Date(competitor.dateOfBirth).toLocaleDateString()
                            : `~${competitor.approximateAge} years`}
                      </dd>
                    </div>
                    <div>
                      <dt>Height / weight</dt>
                      <dd>
                        {competitor.height} cm · {competitor.weight} kg
                      </dd>
                    </div>
                    <div>
                      <dt>Registered</dt>
                      <dd>{new Date(competitor.registrationDate).toLocaleDateString()}</dd>
                    </div>
                    <div>
                      <dt>Record</dt>
                      <dd>
                        {competitor.victories}W – {competitor.defeats}L · {competitor.completedRaces} races
                      </dd>
                    </div>
                  </dl>
                </div>

                {isAdministrator && (
                    competitor.status === "RETIRED" ? (
                        <p className="competitor-hero__status-panel-label" style={{ opacity: 0.7 }}>
                          This competitor has retired and can't be reactivated.
                        </p>
                    ) : (
                        <div className="competitor-hero__status-panel">
                          <span className="competitor-hero__status-panel-label">Change status</span>
                          <select value={statusDraft} onChange={(e) => setStatusDraft(e.target.value as CompetitorStatus)}>
                            <option value="ACTIVE">Active</option>
                            <option value="INJURED">Injured</option>
                            <option value="SUSPENDED">Suspended</option>
                            <option value="RETIRED">Retired</option>
                          </select>
                          <button
                              className={`btn btn-secondary${justApplied ? " btn--confirmed" : ""}`}
                              onClick={handleStatusChange}
                              disabled={saving || statusDraft === competitor.status}
                          >
                            {justApplied ? "Applied ✓" : "Apply"}
                          </button>
                        </div>
                    )
                )}
              </div>
          )}
        </div>
      </>
  );
}