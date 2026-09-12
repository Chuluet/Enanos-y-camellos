import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { teamsApi } from "../../api/teams";
import { competitorsApi } from "../../api/competitors";
import type {CompetitorResponse, CompetitorStatus, TeamResponse, TeamStatus} from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import dwarfBg from "../../assets/dwarf.jpg";

const STATUS_TONE: Record<TeamStatus, string> = {
  ACTIVE: "oasis",
  SUSPENDED: "gold",
  INACTIVE: "stone",
};
const COMPETITOR_TONE: Record<CompetitorStatus, string> = {
  ACTIVE: "oasis",
  INJURED: "rust",
  SUSPENDED: "gold",
  RETIRED: "stone",
};

type FormState = {
  name: string;
  description: string;
  coach: string;
  maxMembers: string;
  status: TeamStatus;
};

function toFormState(t: TeamResponse): FormState {
  return {
    name: t.name,
    description: t.description ?? "",
    coach: t.coach,
    maxMembers: String(t.maxMembers),
    status: t.status,
  };
}

export function TeamDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isAdministrator } = useRoles();

  const [team, setTeam] = useState<TeamResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<FormState | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [statusDraft, setStatusDraft] = useState<TeamStatus | "">("");
  const [justApplied, setJustApplied] = useState(false);

  const [candidates, setCandidates] = useState<CompetitorResponse[] | null>(null);
  const [selectedCandidate, setSelectedCandidate] = useState("");

  function load() {
    if (!id) return;
    setTeam(null);
    setError(null);
    teamsApi
        .getById(id)
        .then((data) => {
          setTeam(data);
          setForm(toFormState(data));
          setStatusDraft(data.status);
        })
        .catch((err) => {
          setError(err instanceof ApiError ? err.message : "Something went wrong loading this team.");
        });
  }

  useEffect(load, [id]);

  useEffect(() => {
    if (!isAdministrator || !team) return;
    competitorsApi
        .getAllList()
        .then((all) =>
            // Only rule for joining a team is "not retired" — injured/suspended
            // competitors can still be assigned, they just can't race while
            // in that state.
            setCandidates(all.filter((c) => c.status !== "RETIRED" && !c.team))
        )
        .catch(() => setCandidates([]));
  }, [isAdministrator, team?.id]);

  const bg = <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />;

  if (error) {
    return (
        <>
          {bg}
          <div className="state-box state-box--error">
            <p>{error}</p>
            <Link to="/teams" className="btn btn-secondary">
              Back to teams
            </Link>
          </div>
        </>
    );
  }

  if (!team || !form) {
    return (
        <>
          {bg}
          <div className="state-box">
            <div className="spinner" />
            <p>Loading team...</p>
          </div>
        </>
    );
  }

  async function handleSave() {
    if (!id || !form) return;
    setSaving(true);
    setActionError(null);
    try {
      const updated = await teamsApi.update(id, {
        name: form.name,
        description: form.description || undefined,
        coach: form.coach,
        maxMembers: Number(form.maxMembers),
        status: form.status,
      });
      setTeam(updated);
      setForm(toFormState(updated));
      setEditing(false);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't save changes.");
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusChange() {
    if (!id || !statusDraft || !team || statusDraft === team.status) return;
    setSaving(true);
    setActionError(null);
    try {
      const updated = await teamsApi.update(id, {
        name: team.name,
        description: team.description || undefined,
        coach: team.coach,
        maxMembers: team.maxMembers,
        status: statusDraft,
      });
      setTeam(updated);
      setForm(toFormState(updated));
      setJustApplied(true);
      setTimeout(() => setJustApplied(false), 900);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't change status.");
    } finally {
      setSaving(false);
    }
  }

  async function handleAddMember() {
    if (!id || !selectedCandidate) return;
    setSaving(true);
    setActionError(null);
    try {
      const updated = await teamsApi.addMember(id, selectedCandidate);
      setTeam(updated);
      setSelectedCandidate("");
      setCandidates((prev) => (prev ? prev.filter((c) => c.id !== selectedCandidate) : prev));
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't add that competitor.");
    } finally {
      setSaving(false);
    }
  }

  async function handleRemoveMember(competitorId: string, name: string) {
    if (!id || !team) return;
    if (!window.confirm(`Remove ${name} from ${team.name}?`)) return;
    setSaving(true);
    setActionError(null);
    try {
      const updated = await teamsApi.removeMember(id, competitorId);
      setTeam(updated);
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't remove that member.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate() {
    if (!id || !team) return;
    if (
        !window.confirm(
            `Remove ${team.name}? If it has no official race history this deletes it permanently; otherwise it'll be deactivated and its history kept.`
        )
    ) {
      return;
    }
    setSaving(true);
    try {
      await teamsApi.deactivate(id);
      navigate("/teams");
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Couldn't deactivate this team.");
      setSaving(false);
    }
  }

  return (
      <>
        {bg}
        <div>
          <Link to="/teams" className="page-breadcrumb">
            ← Back to teams
          </Link>

          <div className="app-content__header">
            <h1>{team.name}</h1>
            {isAdministrator && !editing && (
                <div style={{ display: "flex", gap: 12 }}>
                  <button className="btn btn-secondary" onClick={() => setEditing(true)}>
                    Edit
                  </button>
                  <button className="btn btn-danger" onClick={handleDeactivate} disabled={saving}>
                    Deactivate
                  </button>
                </div>
            )}
          </div>
          <p className="app-content__subtitle">
            <span className={`sheet-row__status status-${STATUS_TONE[team.status]}`}>
              {team.status}
            </span>
            {" · "}Coach: {team.coach}
          </p>

          {actionError && (
              <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                <p>{actionError}</p>
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
                    <label>Coach</label>
                    <input value={form.coach} onChange={(e) => setForm({ ...form, coach: e.target.value })} />
                  </div>
                  <div className="form-field">
                    <label>Max members</label>
                    <input
                        type="text"
                        inputMode="numeric"
                        value={form.maxMembers}
                        onChange={(e) =>
                            setForm({ ...form, maxMembers: e.target.value.replace(/[^0-9]/g, "") })
                        }
                    />
                    {Number(form.maxMembers) < team.members.length && (
                        <span className="field-error">
                          Can't be below the current member count ({team.members.length}).
                        </span>
                    )}
                  </div>
                  <div className="form-field" style={{ gridColumn: "1 / -1" }}>
                    <label>Description</label>
                    <textarea
                        rows={2}
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
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
                        setForm(toFormState(team));
                        setActionError(null);
                        setEditing(false);
                      }}
                      disabled={saving}
                  >
                    Cancel
                  </button>
                </div>
              </div>
          ) : (
              <div className="detail-card">
                <dl className="detail-grid">
                  <div>
                    <dt>Description</dt>
                    <dd>{team.description || "—"}</dd>
                  </div>
                  <div>
                    <dt>Created</dt>
                    <dd>{new Date(team.creationDate).toLocaleDateString()}</dd>
                  </div>
                  <div>
                    <dt>Record</dt>
                    <dd>
                      {team.victories}W – {team.defeats}L
                    </dd>
                  </div>
                  <div>
                    <dt>Capacity</dt>
                    <dd>
                      {team.members.length} / {team.maxMembers} members
                    </dd>
                  </div>
                </dl>

                {isAdministrator && (
                    <div className="competitor-hero__status-panel">
                      <span className="competitor-hero__status-panel-label">Change status</span>
                      <select value={statusDraft} onChange={(e) => setStatusDraft(e.target.value as TeamStatus)}>
                        <option value="ACTIVE">Active</option>
                        <option value="SUSPENDED">Suspended</option>
                        <option value="INACTIVE">Inactive</option>
                      </select>
                      <button
                          className={`btn btn-secondary${justApplied ? " btn--confirmed" : ""}`}
                          onClick={handleStatusChange}
                          disabled={saving || statusDraft === team.status}
                      >
                        {justApplied ? "Applied ✓" : "Apply"}
                      </button>
                    </div>
                )}
              </div>
          )}

          <div className="detail-card" style={{ marginTop: 16 }}>
            <h3>Members</h3>
            {team.members.length === 0 ? (
                <p style={{ opacity: 0.6, fontSize: 13 }}>No members yet.</p>
            ) : (
                <div className="sheet">
                  {team.members.map((m, index) => (
                      <div
                          key={m.id}
                          className="sheet-row sheet-row--team cascade"
                          style={{ cursor: "default", animationDelay: `${index * 40}ms` }}
                      >
                        <span className="avatar-chip avatar-chip--lg" title={m.name}>
                          {m.nickname.slice(0, 2).toUpperCase()}
                        </span>
                        <div className="sheet-row__body">
                          <Link to={`/competitors/${m.id}`} className="sheet-row__title">
                            {m.name}{" "}
                            <span className="sheet-row__nickname">"{m.nickname}"</span>
                          </Link>
                          <div className="sheet-row__meta">
                            <span>{m.competitorType}</span>
                          </div>
                        </div>
                        <span className={`sheet-row__status status-${COMPETITOR_TONE[m.status] ?? "stone"}`}>
                          {m.status}
                        </span>
                        {isAdministrator && (
                            <button
                                className="btn btn-secondary"
                                onClick={() => handleRemoveMember(m.id, m.name)}
                                disabled={saving}
                            >
                              Remove
                            </button>
                        )}
                      </div>
                  ))}
                </div>
            )}

            {isAdministrator && (
                <div style={{ display: "flex", gap: 12, marginTop: 14, alignItems: "center" }}>
                  <select
                      value={selectedCandidate}
                      onChange={(e) => setSelectedCandidate(e.target.value)}
                      disabled={!candidates || candidates.length === 0}
                  >
                    <option value="">
                      {candidates === null
                          ? "Loading eligible competitors..."
                          : candidates.length === 0
                              ? "No eligible, team-less competitors available"
                              : "Add a competitor..."}
                    </option>
                    {candidates?.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name} ({c.nickname})
                        </option>
                    ))}
                  </select>
                  <button
                      className="btn btn-primary"
                      onClick={handleAddMember}
                      disabled={!selectedCandidate || saving || team.members.length >= team.maxMembers}
                  >
                    Add
                  </button>
                </div>
            )}
          </div>
        </div>
      </>
  );
}