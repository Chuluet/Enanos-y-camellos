import { createBrowserRouter, Navigate } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { CallbackPage } from "./auth/CallbackPage";
import { RaceListPage } from "./pages/races/RaceListPage";
import { CompetitorListPage } from "./pages/competitors/CompetitorListPage";
import { CompetitorCreatePage } from "./pages/competitors/CompetitorCreatePage";
import { CompetitorDetailPage } from "./pages/competitors/CompetitorDetailPage";
import { TeamListPage } from "./pages/teams/TeamListPage";
import { TeamCreatePage } from "./pages/teams/TeamCreatePage";
import { TeamDetailPage } from "./pages/teams/TeamDetailPage";

export const router = createBrowserRouter([
    {
        path: "/callback",
        element: <CallbackPage />,
    },
    {
        path: "/",
        element: <Layout />,
        children: [
            {
                index: true,
                element: <Navigate to="/races" replace />,
            },
            {
                path: "races",
                element: (
                    <ProtectedRoute>
                        <RaceListPage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "competitors",
                element: (
                    <ProtectedRoute>
                        <CompetitorListPage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "competitors/new",
                element: (
                    <ProtectedRoute>
                        <CompetitorCreatePage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "competitors/:id",
                element: (
                    <ProtectedRoute>
                        <CompetitorDetailPage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "teams",
                element: (
                    <ProtectedRoute>
                        <TeamListPage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "teams/new",
                element: (
                    <ProtectedRoute>
                        <TeamCreatePage />
                    </ProtectedRoute>
                ),
            },
            {
                path: "teams/:id",
                element: (
                    <ProtectedRoute>
                        <TeamDetailPage />
                    </ProtectedRoute>
                ),
            },
        ],
    },
]);