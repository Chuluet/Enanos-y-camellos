import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useRoles } from "../auth/useRoles";

export function Layout() {
  const auth = useAuth();
  const { roles, isAdministrator } = useRoles();

  return (
    <div className="app-shell">
      <aside className="app-sidebar">
        <div className="app-brand">EIA<br />RACING</div>

        <nav className="app-nav">
          <NavLink to="/races" className={({ isActive }) => (isActive ? "active" : "")}>
            <i className="ti ti-flag" aria-hidden="true" />
            Races
          </NavLink>
          <NavLink to="/competitors" className={({ isActive }) => (isActive ? "active" : "")}>
            <i className="ti ti-users" aria-hidden="true" />
            Competitors
          </NavLink>
          <NavLink to="/teams" className={({ isActive }) => (isActive ? "active" : "")}>
            <i className="ti ti-shield" aria-hidden="true" />
            Teams
          </NavLink>
          <NavLink to="/standings" className={({ isActive }) => (isActive ? "active" : "")}>
            <i className="ti ti-chart-bar" aria-hidden="true" />
            Standings
          </NavLink>
          {isAdministrator && (
            <NavLink to="/audit-log" className={({ isActive }) => (isActive ? "active" : "")}>
              <i className="ti ti-history" aria-hidden="true" />
              Audit log
            </NavLink>
          )}
        </nav>

        {auth.isAuthenticated && (
          <div className="app-user">
            <span className="app-user__name">
              {auth.user?.profile.preferred_username}
            </span>
            <span className="app-user__role">{roles.join(", ")}</span>
            <button
              onClick={() => auth.removeUser().then(() => auth.signoutRedirect())}
            >
              Log out
            </button>
          </div>
        )}
      </aside>

      <main className="app-content">
        {!auth.isAuthenticated && (
          <button className="btn-primary" onClick={() => auth.signinRedirect()}>
            Log in
          </button>
        )}
        <Outlet />
      </main>
    </div>
  );
}
