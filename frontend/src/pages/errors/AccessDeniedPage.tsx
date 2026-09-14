import { Link } from "react-router-dom";
import { useAuth } from "react-oidc-context";

interface AccessDeniedPageProps {
  /** Distinguishes "not logged in at all" from "logged in but role isn't enough". */
  reason: "unauthenticated" | "insufficient-role";
}

export function AccessDeniedPage({ reason }: AccessDeniedPageProps) {
  const auth = useAuth();

  return (
    <div className="state-box state-box--error">
      <h1 style={{ fontSize: "2rem" }}>Access denied</h1>
      {reason === "unauthenticated" ? (
        <>
          <p>You must log in to view this page.</p>
          <button className="btn btn-primary" onClick={() => auth.signinRedirect()}>
            Log in
          </button>
        </>
      ) : (
        <>
          <p>Your current role doesn't have permission to view this page.</p>
          <Link to="/" className="btn btn-secondary">
            Back to home
          </Link>
        </>
      )}
    </div>
  );
}