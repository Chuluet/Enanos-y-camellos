import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";

/**
 * Keycloak redirects here after a successful login. react-oidc-context's
 * AuthProvider automatically processes the ?code=... in the URL and
 * exchanges it for a token — we just wait for that to finish, then send
 * the user back to the home page.
 */
export function CallbackPage() {
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth.isAuthenticated) {
      navigate("/", { replace: true });
    }
  }, [auth.isAuthenticated, navigate]);

  if (auth.error) {
    return <p>Login failed: {auth.error.message}</p>;
  }

  return <p>Signing you in...</p>;
}