import type { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { useRoles, type Role } from "./useRoles";
import { AccessDeniedPage } from "../pages/errors/AccessDeniedPage";

interface ProtectedRouteProps {
  children: ReactNode;
  /** If given, the user also needs at least one of these roles, not just to be logged in. */
  requiredRoles?: Role[];
}

export function ProtectedRoute({ children, requiredRoles }: ProtectedRouteProps) {
  const auth = useAuth();
  const { hasAnyRole } = useRoles();

  if (auth.isLoading) {
    return (
      <div className="state-box">
        <div className="spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return <AccessDeniedPage reason="unauthenticated" />;
  }

  if (requiredRoles && !hasAnyRole(...requiredRoles)) {
    return <AccessDeniedPage reason="insufficient-role" />;
  }

  return <>{children}</>;
}