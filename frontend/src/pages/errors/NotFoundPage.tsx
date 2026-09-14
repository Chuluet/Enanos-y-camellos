import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="state-box">
      <h1 style={{ fontSize: "3rem", marginBottom: 0 }}>404</h1>
      <p>This page doesn't exist — maybe the camel took a wrong turn.</p>
      <Link to="/" className="btn btn-primary">
        Back to home
      </Link>
    </div>
  );
}