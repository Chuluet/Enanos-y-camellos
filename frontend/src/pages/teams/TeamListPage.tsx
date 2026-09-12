import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { teamsApi } from "../../api/teams";
import type { TeamResponse, TeamStatus } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { useRoles } from "../../auth/useRoles";
import dwarfBg from "../../assets/dwarf.jpg";

const STATUS_TONE: Record<TeamStatus, string> = {
  ACTIVE: "oasis",
  SUSPENDED: "gold",
  INACTIVE: "stone",
};

export function TeamListPage() {
  const [status, setStatus] = useState<TeamStatus | "">("");
  const [teams, setTeams] = useState<TeamResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { isAdministrator } = useRoles();

  useEffect(() => {
    let cancelled = false;
    setTeams(null);
    setError(null);

    teamsApi
        .getAll(status || undefined)
        .then((data) => {
          if (!cancelled) setTeams(data);
        })
        .catch((err) => {
          if (cancelled) return;
          setError(err instanceof ApiError ? err.message : "Something went wrong loading teams.");
        });

    return () => {
      cancelled = true;
    };
  }, [status]);

  return (
      <>
        <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />
        <div>
          <Link to="/" className="page-breadcrumb">
            ← Main menu
          </Link>
          <h1 className="page-title-banner page-title-banner--teams">Teams</h1>

          <div className="page-session-bar">
            <span>{teams ? `${teams.length} registered` : "Loading..."}</span>
            {isAdministrator && (
                <Link to="/teams/new" className="btn btn-primary">
                  + New team
                </Link>
            )}
          </div>

          {error ? (
              <div className="state-box state-box--error">
                <p>{error}</p>
              </div>
          ) : teams === null ? (
              <div className="state-box">
                <div className="spinner" />
                <p>Loading teams...</p>
              </div>
          ) : (
              <>
                <div className="filter-bar">
                  <select value={status} onChange={(e) => setStatus(e.target.value as TeamStatus | "")}>
                    <option value="">All statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="SUSPENDED">Suspended</option>
                    <option value="INACTIVE">Inactive</option>
                  </select>
                </div>

                {teams.length === 0 ? (
                    <div className="state-box">
                      <p>No teams match this filter.</p>
                    </div>
                ) : (
                    <div className="team-grid">
                      {teams.map((team, index) => (
                          <Link
                              key={team.id}
                              to={`/teams/${team.id}`}
                              className="team-card cascade"
                              style={{ animationDelay: `${index * 40}ms` }}
                          >
                            <div className="team-card__header">
                              <div>
                                <div className="team-card__name">{team.name}</div>
                                <div className="team-card__coach">Coach: {team.coach}</div>
                              </div>
                              <span className={`sheet-row__status status-${STATUS_TONE[team.status]}`}>
                        {team.status}
                      </span>
                            </div>
                            <div className="team-card__members">
                              {team.members.slice(0, 4).map((m) => (
                                  <span key={m.id} className="avatar-chip" title={m.name}>
                          {m.nickname.slice(0, 2).toUpperCase()}
                        </span>
                              ))}
                              {team.members.length > 4 && (
                                  <span className="avatar-chip avatar-chip--overflow">
                          +{team.members.length - 4}
                        </span>
                              )}
                              <span className="team-card__stats">
                        {team.members.length}/{team.maxMembers} members · {team.victories}W-{team.defeats}L
                      </span>
                            </div>
                          </Link>
                      ))}
                    </div>
                )}
              </>
          )}
        </div>
      </>
  );
}