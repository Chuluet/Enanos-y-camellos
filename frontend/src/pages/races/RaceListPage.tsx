import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { racesApi } from "../../api/races";
import type { Race } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";

const STATUS_STYLE: Record<Race["status"], { label: string; tone: string }> = {
  DRAFT: { label: "Draft", tone: "stone" },
  OPEN_FOR_REGISTRATION: { label: "Open", tone: "oasis" },
  CLOSED_FOR_REGISTRATION: { label: "Closed", tone: "gold" },
  IN_PROGRESS: { label: "In progress", tone: "gold" },
  COMPLETED: { label: "Completed", tone: "stone" },
  CANCELLED: { label: "Cancelled", tone: "rust" },
};

export function RaceListPage() {
  const [races, setRaces] = useState<Race[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { canWrite } = useRoles();

  useEffect(() => {
    let cancelled = false;

    racesApi
      .getAll()
      .then((data) => {
        if (!cancelled) setRaces(data);
      })
      .catch((err) => {
        if (cancelled) return;
        const message =
          err instanceof ApiError
            ? err.message
            : "Something went wrong loading races.";
        setError(message);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (error) {
    return (
      <div className="state-box state-box--error">
        <p>{error}</p>
      </div>
    );
  }

  if (races === null) {
    return (
      <div className="state-box">
        <div className="spinner" />
        <p>Loading races...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="app-content__header">
        <h1>Races</h1>
        {canWrite && (
          <Link to="/races/new" className="btn btn-primary">
            + New race
          </Link>
        )}
      </div>
      <p className="app-content__subtitle">
        {races.length} {races.length === 1 ? "race" : "races"} on record
      </p>

      {races.length === 0 ? (
        <div className="state-box">
          <p>No races have been created yet.</p>
        </div>
      ) : (
        <div className="sheet">
          {races.map((race, index) => {
            const status = STATUS_STYLE[race.status];
            return (
              <Link
                key={race.id}
                to={`/races/${race.id}`}
                className="sheet-row"
                style={{ color: "inherit" }}
              >
                <span className="sheet-row__index">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className={`sheet-row__stripe stripe-${status.tone}`} />
                <div className="sheet-row__body">
                  <div className="sheet-row__title">{race.name}</div>
                  <div className="sheet-row__meta">
                    <span>{race.raceType}</span>
                    <span>{race.distanceMeters}m</span>
                    <span>
                      {new Date(race.scheduledDateTime).toLocaleDateString()}
                    </span>
                    <span>{race.organizer}</span>
                  </div>
                </div>
                <span className={`sheet-row__status status-${status.tone}`}>
                  {status.label}
                </span>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}