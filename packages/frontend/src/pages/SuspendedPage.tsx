function SuspendedPage() {
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
      <p
        style={{
          maxWidth: "620px",
          margin: 0,
          fontSize: "22px",
          fontWeight: 800,
          lineHeight: 1.45
        }}>
        Your account has been suspended for violating our guidelines. Contact support if you believe this is a mistake.
      </p>
    </main>
  );
}

export default SuspendedPage;
