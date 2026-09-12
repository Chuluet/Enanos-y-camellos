import { useEffect, useState } from "react";
import { resultsApi } from "../../api/results";
import { registrationsApi } from "../../api/registrations";
import type { RaceResultResponse, Registration, ResultStatus, Race } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useAuth } from "react-oidc-context";
import { useRoles } from "../../auth/useRoles";

const STATUS_TONE: Record<ResultStatus, string> = {
  FINISHED: "oasis",
  DISQUALIFIED: "rust",
  DID_NOT_FINISH: "stone",
  DID_NOT_START: "stone",
};

export function RaceResults({ race }: { race: Race }) {
  const auth = useAuth();
  const { canWrite } = useRoles();

const [results, setResults] = useState<RaceResultResponse[] | null>(null);
  const [approvedRegistrations, setApprovedRegistrations] = useState<Registration[]>([]);
  const [error, setError] = useState<string | null>(null);

  const [registrationId, setRegistrationId] = useState("");
  const [status, setStatus] = useState<ResultStatus>("FINISHED");
  const [finalPosition, setFinalPosition] = useState("");
  const [completionTime, setCompletionTime] = useState("");
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  function load() {
    setError(null);
    Promise.all([resultsApi.getByRace(race.id), registrationsApi.getByRace(race.id)])
      .then(([r, regs]) => {
        setResults(r);
        const recordedIds = new Set(r.map((x) => x.registrationId));
        setApprovedRegistrations(
          regs.filter((reg) => reg.status === "APPROVED" && !recordedIds.has(reg.id))
        );
      })
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "Couldn't load results.")
      );
  }

  useEffect(load, [race.id]);

  async function handleRecord() {
    if (!registrationId) return;
    setSaving(true);
    setFormError(null);
    try {
      await resultsApi.record(race.id, {
        registrationId,
        status,
        finalPosition: status === "FINISHED" && finalPosition ? Number(finalPosition) : undefined,
        completionTime: status === "FINISHED" && completionTime ? Number(completionTime) : undefined,
        recordedBy: auth.user?.profile.preferred_username ?? "unknown",
      });
      setRegistrationId("");
      setFinalPosition("");
      setCompletionTime("");
      setStatus("FINISHED");
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't record result.");
    } finally {
      setSaving(false);
    }
  }

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
      </div>
    );
  }

  if (results === null) {
    return (
      <div className="state-box">
        <div className="spinner" />
      </div>
    );
  }

  const canRecord = canWrite && race.status === "IN_PROGRESS" && approvedRegistrations.length > 0;

  return (
    <div>
      <h2>Results</h2>

      {formError && (
        <div className="state-box state-box--error" style={{ marginBottom: 12 }}>
          <p>{formError}</p>
        </div>
      )}

      {canRecord && (
        <div className="detail-card" style={{ marginBottom: 16 }}>
          <div className="detail-grid">
            <div className="form-field">
              <label>Participant</label>
              <select value={registrationId} onChange={(e) => setRegistrationId(e.target.value)}>
                <option value="">Select...</option>
                {approvedRegistrations.map((reg) => (
                  <option key={reg.id} value={reg.id}>
                    {reg.competitor ? reg.competitor.name : reg.team?.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label>Status</label>
              <select value={status} onChange={(e) => setStatus(e.target.value as ResultStatus)}>
                <option value="FINISHED">Finished</option>
                <option value="DISQUALIFIED">Disqualified</option>
                <option value="DID_NOT_FINISH">Did not finish</option>
                <option value="DID_NOT_START">Did not start</option>
              </select>
            </div>
            {status === "FINISHED" && (
              <>
                <div className="form-field">
                  <label>Final position</label>
                  <input
                    type="number"
                    value={finalPosition}
                    onChange={(e) => setFinalPosition(e.target.value)}
                  />
                </div>
                <div className="form-field">
                  <label>Completion time (seconds)</label>
                  <input
                    type="number"
                    value={completionTime}
                    onChange={(e) => setCompletionTime(e.target.value)}
                  />
                </div>
              </>
            )}
          </div>
          <button className="btn btn-primary" onClick={handleRecord} disabled={saving || !registrationId}>
            Record result
          </button>
        </div>
      )}

      {results.length === 0 ? (
        <div className="state-box">
          <p>No results recorded yet.</p>
        </div>
      ) : (
        <div className="sheet">
                    {results
            .slice()
            .sort((a, b) => (a.finalPosition ?? 999) - (b.finalPosition ?? 999))
            .map((result, index) => (
              <div
                key={result.id}
                className="sheet-row rise"
                style={{ animationDelay: `${index * 40}ms` }}
              >
                <span className="sheet-row__index">
                  {result.finalPosition ? String(result.finalPosition).padStart(2, "0") : "—"}
                </span>
                <span className={`sheet-row__stripe stripe-${STATUS_TONE[result.status]}`} />
                <div className="sheet-row__body">
                  <div className="sheet-row__title">
                    {result.competitor ? result.competitor.name : result.team?.name}
                  </div>
                  <div className="sheet-row__meta">
                    {result.completionTime && <span>{result.completionTime}s</span>}
                    <span>Recorded by {result.recordedBy}</span>
                  </div>
                </div>
                <span className={`sheet-row__status status-${STATUS_TONE[result.status]}`}>
                  {result.status}
                </span>
              </div>
            ))}
        </div>
      )}
    </div>
  );
}