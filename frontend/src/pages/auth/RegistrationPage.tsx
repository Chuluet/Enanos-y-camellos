import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import dwarfBg from "../../assets/dwarf.jpg";

/**
 * Same idea as LoginPage — no form here, no call to a backend endpoint.
 * `kc_action: "register"` is a Keycloak-specific extra query param on the
 * standard OIDC authorization request: it tells Keycloak's hosted UI to
 * show the registration form instead of the login form, but it's still the
 * exact same authorization-code + PKCE flow, so CallbackPage handles the
 * return trip without any changes.
 *
 * Requires "registrationAllowed": true in the realm — see
 * realm-enanosycamellos.json.
 */
export function RegisterPage() {
    const auth = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (auth.isAuthenticated) {
            navigate("/", { replace: true });
        }
    }, [auth.isAuthenticated, navigate]);

    function handleRegister() {
        auth.signinRedirect({
            extraQueryParams: { kc_action: "register" },
        });
    }

    return (
        <>
            <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />
            <div>
                <Link to="/" className="page-breadcrumb">
                    ← Main menu
                </Link>

                <h1 className="page-title-banner page-title-banner--teams">Create an account</h1>
                <p className="app-content__subtitle" style={{ textAlign: "center" }}>
                    New accounts start with read-only (viewer) access.
                </p>

                {auth.error && (
                    <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                        <p>{auth.error.message}</p>
                    </div>
                )}

                <div className="detail-card form-card" style={{ textAlign: "center" }}>
                    <p style={{ marginBottom: 20, opacity: 0.8 }}>
                        You'll be taken to our identity provider to create your account securely.
                    </p>

                    <div className="form-actions" style={{ justifyContent: "center" }}>
                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={handleRegister}
                            disabled={auth.isLoading}
                        >
                            {auth.isLoading ? "Redirecting..." : "Continue to sign up"}
                        </button>
                    </div>

                    <p style={{ marginTop: 20, fontSize: 13, opacity: 0.7 }}>
                        Already have an account? <Link to="/login">Log in</Link>.
                    </p>
                </div>
            </div>
        </>
    );
}