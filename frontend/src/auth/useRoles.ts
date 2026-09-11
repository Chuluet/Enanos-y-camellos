import { jwtDecode } from "jwt-decode";
import { useAuth } from "react-oidc-context";

export type Role = "ADMINISTRATOR" | "RACE_ORGANIZER" | "VIEWER";

interface RealmAccessClaims {
  realm_access?: {
    roles?: string[];
  };
}

/**
 * Decodes the access token's realm_access.roles claim — the SAME claim
 * RaceResultServiceTest's counterpart, SecurityConfig.rolesDelRealm(jwt),
 * reads on the backend. Keeping this claim identical on both sides means
 * "what the UI shows" and "what the API actually allows" never drift apart.
 */
export function useRoles() {
  const auth = useAuth();

  const roles: Role[] = (() => {
    const token = auth.user?.access_token;
    if (!token) return [];

    try {
      const claims = jwtDecode<RealmAccessClaims>(token);
      return (claims.realm_access?.roles ?? []).map((r) =>
        r.toUpperCase()
      ) as Role[];
    } catch {
      return [];
    }
  })();

  const hasRole = (role: Role) => roles.includes(role);
  const hasAnyRole = (...required: Role[]) =>
    required.some((r) => roles.includes(r));

  return {
    roles,
    hasRole,
    hasAnyRole,
    isAdministrator: hasRole("ADMINISTRATOR"),
    isRaceOrganizer: hasRole("RACE_ORGANIZER"),
    isViewer: hasRole("VIEWER"),
    /** True for ADMINISTRATOR or RACE_ORGANIZER — the two write-capable roles. */
    canWrite: hasAnyRole("ADMINISTRATOR", "RACE_ORGANIZER"),
  };
}