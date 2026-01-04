import { Router, useLocation } from "@solidjs/router";
import { FileRoutes } from "@solidjs/start/router";
import { Suspense } from "solid-js";
import "./app.css";

export default function App() {
  return (
    <Router
      root={props => {
        const location = useLocation();
        const isLandingPage = () => location.pathname === '/';

        return (
          <>
            <Suspense>{props.children}</Suspense>
          </>
        );
      }}
    >
      <FileRoutes />
    </Router>
  );
}
