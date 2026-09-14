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

type ResultFormState = {
  status: ResultStatus;
  finalPosition: string;
  completionTime: string;
  penaltyTime: string;
  notes: string;
};

const EMPTY_FORM: ResultFormState = {
  status: "FINISHED",
  finalPosition: "",
  completionTime: "",
  penaltyTime: "",
  notes: "",
};

function toFormState(r: RaceResultResponse): ResultFormState {
  return {
    status: r.status,
    finalPosition: r.finalPosition != null ? String(r.finalPosition) : "",
    completionTime: r.completionTime != null ? String(r.completionTime) : "",
    penaltyTime: r.penaltyTime != null ? String(r.penaltyTime) : "",
    notes: r.notes ?? "",
  };
}

export function RaceResults({ race }: { race: Race }) {
  const auth = useAuth();
  const { canWrite } = useRoles();

  const [results, setResults] = useState<RaceResultResponse[] | null>(null);
  const [approvedRegistrations, setApprovedRegistrations] = useState<Registration[]>([]);
  const [error, setError] = useState<string | null>(null);

  // -------- new result form --------
  const [registrationId, setRegistrationId] = useState("");
  const [newForm, setNewForm] = useState<ResultFormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // -------- edit existing result --------
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<ResultFormState>(EMPTY_FORM);
  const [editSaving, setEditSaving] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

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
        status: newForm.status,
        finalPosition:
          newForm.status === "FINISHED" && newForm.finalPosition ? Number(newForm.finalPosition) : undefined,
        completionTime:
          newForm.status === "FINISHED" && newForm.completionTime ? Number(newForm.completionTime) : undefined,
        recordedBy: auth.user?.profile.preferred_username ?? "unknown",
      });
      setRegistrationId("");
      setNewForm(EMPTY_FORM);
      load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Couldn't record result.");
    } finally {
      setSaving(false);
    }
  }

  function startEditing(result: RaceResultResponse) {
    setEditingId(result.id);
    setEditForm(toFormState(result));
    setEditError(null);
  }

  async function handleUpdate(id: string) {
    setEditSaving(true);
    setEditError(null);
    try {
      await resultsApi.update(id, {
        status: editForm.status,
        finalPosition: editForm.status === "FINISHED" && editForm.finalPosition ? Number(editForm.finalPosition) : undefined,
        completionTime: editForm.status === "FINISHED" && editForm.completionTime ? Number(editForm.completionTime) : undefined,
        penaltyTime: editForm.penaltyTime ? Number(editForm.penaltyTime) : undefined,
        notes: editForm.notes || undefined,
        recordedBy: auth.user?.profile.preferred_username ?? "unknown",
      });
      setEditingId(null);
      load();
    } catch (err) {
      if (err instanceof ApiError) {
        setEditError(err.body?.message ?? "Couldn't save changes.");
      } else {
        setEditError("Couldn't save changes.");
      }
    } finally {
      setEditSaving(false);
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
  const canEdit = canWrite && race.status === "IN_PROGRESS";

  return (
    <div>
      <h2>Results</h2>

      {formError && (
        <div className="state-box state-box--error" style={{ marginBottom: 12 }}>
          <p>{formError}</p>
        </div>
      )}

      {canRecord && (
        <div className="detail-card form-card" style={{ marginBottom: 16 }}>
          <div className="form-section">
            <h3>Record a result</h3>
            <div className="form-grid">
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
                <select
                  value={newForm.status}
                  onChange={(e) => setNewForm({ ...newForm, status: e.target.value as ResultStatus })}
                >
                  <option value="FINISHED">Finished</option>
                  <option value="DISQUALIFIED">Disqualified</option>
                  <option value="DID_NOT_FINISH">Did not finish</option>
                  <option value="DID_NOT_START">Did not start</option>
                </select>
              </div>
              {newForm.status === "FINISHED" && (
                <>
                  <div className="form-field">
                    <label>Final position</label>
                    <input
                      type="number"
                      value={newForm.finalPosition}
                      onChange={(e) => setNewForm({ ...newForm, finalPosition: e.target.value })}
                    />
                  </div>
                  <div className="form-field">
                    <label>Completion time (seconds)</label>
                    <input
                      type="number"
                      value={newForm.completionTime}
                      onChange={(e) => setNewForm({ ...newForm, completionTime: e.target.value })}
                    />
                  </div>
                </>
              )}
            </div>
          </div>
          <div className="form-actions">
            <button className="btn btn-primary" onClick={handleRecord} disabled={saving || !registrationId}>
              {saving ? "Recording..." : "Record result"}
            </button>
          </div>
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
            .map((result, index) =>
              editingId === result.id ? (
                <div key={result.id} className="detail-card form-card" style={{ marginBottom: 12 }}>
                  {editError && (
                    <div className="state-box state-box--error" style={{ marginBottom: 12 }}>
                      <p>{editError}</p>
                    </div>
                  )}
                  <div className="form-section">
                    <h3>
                      Editing result — {result.competitor ? result.competitor.name : result.team?.name}
                    </h3>
                    <div className="form-grid">
                      <div className="form-field">
                        <label>Status</label>
                        <select
                          value={editForm.status}
                          onChange={(e) => setEditForm({ ...editForm, status: e.target.value as ResultStatus })}
                        >
                          <option value="FINISHED">Finished</option>
                          <option value="DISQUALIFIED">Disqualified</option>
                          <option value="DID_NOT_FINISH">Did not finish</option>
                          <option value="DID_NOT_START">Did not start</option>
                        </select>
                      </div>
                      {editForm.status === "FINISHED" && (
                        <>
                          <div className="form-field">
                            <label>Final position</label>
                            <input
                              type="number"
                              value={editForm.finalPosition}
                              onChange={(e) => setEditForm({ ...editForm, finalPosition: e.target.value })}
                            />
                          </div>
                          <div className="form-field">
                            <label>Completion time (seconds)</label>
                            <input
                              type="number"
                              value={editForm.completionTime}
                              onChange={(e) => setEditForm({ ...editForm, completionTime: e.target.value })}
                            />
                          </div>
                          <div className="form-field">
                            <label>Penalty time (seconds)</label>
                            <input
                              type="number"
                              value={editForm.penaltyTime}
                              onChange={(e) => setEditForm({ ...editForm, penaltyTime: e.target.value })}
                            />
                          </div>
                        </>
                      )}
                      <div className="form-field">
                        <label>Notes</label>
                        <input
                          value={editForm.notes}
                          onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>
                  <div className="form-actions">
                    <button
                      className="btn btn-primary"
                      onClick={() => handleUpdate(result.id)}
                      disabled={editSaving}
                    >
                      {editSaving ? "Saving..." : "Save"}
                    </button>
                    <button
                      className="btn btn-secondary"
                      onClick={() => setEditingId(null)}
                      disabled={editSaving}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
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
                      {result.notes && <span>{result.notes}</span>}
                    </div>
                  </div>
                  <span className={`sheet-row__status status-${STATUS_TONE[result.status]}`}>
                    {result.status}
                  </span>
                  {canEdit && (
                    <button
                      className="btn btn-secondary"
                      style={{ marginLeft: 12 }}
                      onClick={() => startEditing(result)}
                    >
                      Edit
                    </button>
                  )}
                </div>
              )
            )}
        </div>
      )}
    </div>
  );
}