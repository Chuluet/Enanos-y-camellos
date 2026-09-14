import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import dwarfBg from "../../assets/dwarf.jpg";

/**
 * Doesn't render a username/password form: the actual credentials screen
 * lives in Keycloak (hosted login page), same as CallbackPage already
 * assumes. This page is just the branded "front door" — a single button
 * that kicks off the standard OIDC redirect + PKCE flow already wired up
 * in AuthProvider/oidcConfig.ts.
 */
export function LoginPage() {
    const auth = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (auth.isAuthenticated) {
            navigate("/", { replace: true });
        }
    }, [auth.isAuthenticated, navigate]);

    return (
        <>
            <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />
            <div>
                <Link to="/" className="page-breadcrumb">
                    ← Main menu
                </Link>

                <h1 className="page-title-banner page-title-banner--teams">Log in</h1>
                <p className="app-content__subtitle" style={{ textAlign: "center" }}>
                    Sign in to manage teams, competitors and races.
                </p>

                {auth.error && (
                    <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                        <p>{auth.error.message}</p>
                    </div>
                )}

                <div className="detail-card form-card" style={{ textAlign: "center" }}>
                    <p style={{ marginBottom: 20, opacity: 0.8 }}>
                        You'll be taken to our identity provider to sign in securely.
                    </p>

                    <div className="form-actions" style={{ justifyContent: "center" }}>
                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={() => auth.signinRedirect()}
                            disabled={auth.isLoading}
                        >
                            {auth.isLoading ? "Redirecting..." : "Continue to log in"}
                        </button>
                    </div>

                    <p style={{ marginTop: 20, fontSize: 13, opacity: 0.7 }}>
                        Don't have an account? <Link to="/register">Create one</Link>.
                    </p>
                </div>
            </div>
        </>
    );
}