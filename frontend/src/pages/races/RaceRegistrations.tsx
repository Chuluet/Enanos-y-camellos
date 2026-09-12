import { useEffect, useState } from "react";
import { registrationsApi } from "../../api/registrations";
import { competitorsApi } from "../../api/competitors";
import { teamsApi } from "../../api/teams";
import type { Registration, Race, CompetitorResponse, TeamResponse } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useAuth } from "react-oidc-context";
import { useRoles } from "../../auth/useRoles";

const STATUS_TONE: Record<Registration["status"], string> = {
  PENDING: "amber",
  APPROVED: "oasis",
  REJECTED: "rust",
  CANCELLED: "stone",
};

export function RaceRegistrations({ race }: { race: Race }) {
  const auth = useAuth();
  const { canWrite } = useRoles();

  const [registrations, setRegistrations] = useState<Registration[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [competitors, setCompetitors] = useState<CompetitorResponse[]>([]);
  const [teams, setTeams] = useState<TeamResponse[]>([]);
  const [participantId, setParticipantId] = useState("");
  const [registering, setRegistering] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  function load() {
    setError(null);
    registrationsApi
      .getByRace(race.id)
      .then(setRegistrations)
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "Couldn't load registrations.")
      );
  }

  useEffect(load, [race.id]);

  useEffect(() => {
    if (race.raceType !== "TEAM") {
      competitorsApi.getAll({ status: "ACTIVE", size: 100 }).then((r) => setCompetitors(r.content));
    }
    if (race.raceType !== "INDIVIDUAL") {
      teamsApi.getAll("ACTIVE").then(setTeams);
    }
  }, [race.raceType]);

  async function handleRegister() {
    if (!participantId) return;
    setRegistering(true);
    setFormError(null);
    try {
      const isTeam = teams.some((t) => t.id === participantId);
      await registrationsApi.register(race.id, {
        competitorId: isTeam ? undefined : participantId,
        teamId: isTeam ? participantId : undefined,
        registeredBy: auth.user?.profile.preferred_username ?? "unknown",
      });
      setParticipantId("");
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't register.");
    } finally {
      setRegistering(false);
    }
  }

  async function handleApprove(id: string) {
    try {
      await registrationsApi.approve(id);
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't approve.");
    }
  }

  async function handleReject(id: string) {
    const reason = window.prompt("Reason for rejection:");
    if (!reason) return;
    try {
      await registrationsApi.reject(id, reason);
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't reject.");
    }
  }

  async function handleCancel(id: string) {
    if (!window.confirm("Cancel this registration?")) return;
    try {
      await registrationsApi.cancel(id);
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't cancel.");
    }
  }

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
      </div>
    );
  }

  if (registrations === null) {
    return (
      <div className="state-box">
        <div className="spinner" />
      </div>
    );
  }

  const canRegisterMore = canWrite && race.status === "OPEN_FOR_REGISTRATION";

  return (
    <div>
      <h2>Registrations</h2>

      {formError && (
        <div className="state-box state-box--error" style={{ marginBottom: 12 }}>
          <p>{formError}</p>
        </div>
      )}

      {canRegisterMore && (
        <div className="detail-card form-card" style={{ marginBottom: 16 }}>
          <div className="form-section">
            <h3>Register a participant</h3>
            <div className="form-grid">
              <div className="form-field">
                <label>Competitor or team</label>
                <select value={participantId} onChange={(e) => setParticipantId(e.target.value)}>
                  <option value="">Select...</option>
                  {competitors.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} ({c.nickname})
                    </option>
                  ))}
                  {teams.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} (team)
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleRegister} disabled={registering || !participantId}>
              {registering ? "Registering..." : "Register"}
            </button>
          </div>
        </div>
      )}

      {registrations.length === 0 ? (
        <div className="state-box">
          <p>No registrations yet.</p>
        </div>
      ) : (
        <div className="sheet">
          {registrations.map((reg, index) => (
            <div
              key={reg.id}
              className="sheet-row rise"
              style={{ animationDelay: `${index * 40}ms` }}
            >
              <span className={`sheet-row__stripe stripe-${STATUS_TONE[reg.status]}`} />
              <div className="sheet-row__body">
                <div className="sheet-row__title">
                  {reg.competitor ? reg.competitor.name : reg.team?.name}
                </div>
                <div className="sheet-row__meta">
                  <span>{reg.competitor ? "Individual" : "Team"}</span>
                  <span>Registered by {reg.registeredBy}</span>
                  {reg.validationNotes && <span>Note: {reg.validationNotes}</span>}
                </div>
              </div>
              <span className={`sheet-row__status status-${STATUS_TONE[reg.status]}`}>
                {reg.status}
              </span>
              {canWrite && (
                <div style={{ display: "flex", gap: 6, marginLeft: 12 }}>
                  {reg.status === "PENDING" && (
                    <>
                      <button className="btn btn-secondary" onClick={() => handleApprove(reg.id)}>
                        Approve
                      </button>
                      <button className="btn btn-danger" onClick={() => handleReject(reg.id)}>
                        Reject
                      </button>
                    </>
                  )}
                  {(reg.status === "PENDING" || reg.status === "APPROVED") && (
                    <button className="btn btn-secondary" onClick={() => handleCancel(reg.id)}>
                      Cancel
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}