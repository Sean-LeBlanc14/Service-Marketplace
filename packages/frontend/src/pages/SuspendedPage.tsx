import { useNavigate } from "react-router-dom";

function SuspendedPage() {
  const navigate = useNavigate();

  function handleDifferentAccountLogin() {
    localStorage.clear();
    navigate("/login");
  }

  return (
    <main
      style={{
        minHeight: "100vh",
        display: "grid",
        placeItems: "center",
        padding: "24px",
        background: "#f7f9fb",
        color: "#071b2d",
        textAlign: "center"
      }}>
      <div
        style={{
          maxWidth: "620px",
          display: "grid",
          gap: "18px"
        }}>
        <p
          style={{
            margin: 0,
            fontSize: "22px",
            fontWeight: 800,
            lineHeight: 1.45
          }}>
          Your account has been suspended for violating our guidelines. Contact support if you believe this is a mistake.
        </p>
        <button
          type="button"
          onClick={handleDifferentAccountLogin}
          style={{
            border: "none",
            padding: 0,
            background: "transparent",
            color: "#1f4a3f",
            font: "inherit",
            fontSize: "16px",
            fontWeight: 800,
            textDecoration: "underline",
            cursor: "pointer"
          }}>
          Log in with a different account
        </button>
      </div>
    </main>
  );
}

export default SuspendedPage;
