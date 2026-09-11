import { UserManager, type UserManagerSettings } from "oidc-client-ts";

const settings: UserManagerSettings = {
  authority: import.meta.env.VITE_KEYCLOAK_AUTHORITY,
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: window.location.origin,
  response_type: "code",
  scope: "openid profile",
  automaticSilentRenew: true,
};

/**
 * A single shared UserManager instance, used both by AuthProvider (for
 * React components via the useAuth() hook) and by apiClient.ts (which is
 * NOT a component, so it can't use hooks — it reads the cached session
 * directly from this same instance instead).
 */
export const userManager = new UserManager(settings);

export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL;