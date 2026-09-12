import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { racesApi } from "../../api/races";
import type { Race, RaceStatus, RaceType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import dwarfCamelHero from "../../assets/dwarf-camel-hero.png";

const STATUS_STYLE: Record<RaceStatus, { label: string; tone: string }> = {
  DRAFT: { label: "Draft", tone: "stone" },
  OPEN_FOR_REGISTRATION: { label: "Open", tone: "oasis" },
  CLOSED_FOR_REGISTRATION: { label: "Closed", tone: "amber" },
  IN_PROGRESS: { label: "In progress", tone: "gold" },
  COMPLETED: { label: "Completed", tone: "violet" },
  CANCELLED: { label: "Cancelled", tone: "rust" },
};

export function RaceListPage() {
  const [status, setStatus] = useState<RaceStatus | "">("");
  const [type, setType] = useState<RaceType | "">("");
  const [races, setRaces] = useState<Race[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { canWrite, isAdministrator } = useRoles();

  useEffect(() => {
    let cancelled = false;
    setRaces(null);
    setError(null);

    racesApi
      .getAll({ status: status || undefined, type: type || undefined })
      .then((data) => {
        if (!cancelled) setRaces(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(
          err instanceof ApiError
            ? err.message
            : "Something went wrong loading races."
        );
      });

    return () => {
      cancelled = true;
    };
  }, [status, type]);

  return (
    <>
      <div
        className="page-bg-fixed"
        style={{ backgroundImage: `url(${dwarfCamelHero})` }}
      />
      <div>
        <Link to="/" className="page-breadcrumb">
          ← Main menu
        </Link>
        <h1 className="page-title-banner">Races</h1>

        <div className="page-session-bar">
          <span>Role: {isAdministrator ? "Admin" : "Viewer"}</span>
          {canWrite && (
            <Link to="/races/new" className="btn btn-primary">
              + New race
            </Link>
          )}
        </div>

        {error ? (
          <div className="state-box state-box--error">
            <p>{error}</p>
          </div>
        ) : races === null ? (
          <div className="state-box">
            <div className="spinner" />
            <p>Loading races...</p>
          </div>
        ) : (
          <>
            <div className="filter-bar">
              <select
                value={type}
                onChange={(e) => setType(e.target.value as RaceType | "")}
              >
                <option value="">All types</option>
                <option value="INDIVIDUAL">Individual</option>
                <option value="TEAM">Team</option>
                <option value="MIXED">Mixed</option>
              </select>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as RaceStatus | "")}
              >
                <option value="">All statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="OPEN_FOR_REGISTRATION">Open for registration</option>
                <option value="CLOSED_FOR_REGISTRATION">Closed for registration</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            {races.length === 0 ? (
              <div className="state-box">
                <p>No races match these filters.</p>
              </div>
            ) : (
              <div className="sheet">
                {races.map((race, index) => {
                  const style = STATUS_STYLE[race.status];
                  return (
                    <Link
                      key={race.id}
                      to={`/races/${race.id}`}
                      className="sheet-row cascade"
                      style={{ color: "inherit", animationDelay: `${index * 40}ms` }}
                    >
                      <span className="sheet-row__index">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      <span className={`sheet-row__stripe stripe-${style.tone}`} />
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
                      <span className={`sheet-row__status status-${style.tone}`}>
                        {style.label}
                      </span>
                    </Link>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
}