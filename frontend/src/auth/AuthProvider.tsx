import type { ReactNode } from "react";
import { AuthProvider as OidcAuthProvider } from "react-oidc-context";
import { userManager } from "./oidcConfig";

export function AuthProvider({ children }: { children: ReactNode }) {
  return (
    <OidcAuthProvider userManager={userManager}>{children}</OidcAuthProvider>
  );
}