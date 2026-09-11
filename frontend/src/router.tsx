import { createBrowserRouter, Navigate } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { CallbackPage } from "./auth/CallbackPage";
import { RaceListPage } from "./pages/races/RaceListPage";

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
    ],
  },
]);