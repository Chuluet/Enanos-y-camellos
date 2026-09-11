import type { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { useRoles, type Role } from "./useRoles";

interface ProtectedRouteProps {
  children: ReactNode;
  /** If given, the user also needs at least one of these roles, not just to be logged in. */
  requiredRoles?: Role[];
}

export function ProtectedRoute({ children, requiredRoles }: ProtectedRouteProps) {
  const auth = useAuth();
  const { hasAnyRole } = useRoles();

  if (auth.isLoading) {
    return <p>Loading...</p>;
  }

  if (!auth.isAuthenticated) {
    return <p>You must log in to view this page.</p>;
  }

  if (requiredRoles && !hasAnyRole(...requiredRoles)) {
    return <p>You don't have permission to view this page.</p>;
  }

  return <>{children}</>;
}