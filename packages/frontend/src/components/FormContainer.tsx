interface FormContainerProps {
  header: string;
  textField: React.ReactNode;
  submitButton: React.ReactNode;
  link: React.ReactNode;
}

export default function FormContainer({
  header,
  textField,
  submitButton,
  link
}: FormContainerProps) {
  return (
    <section
      style={{
        display: "grid",
        placeItems: "center",
        gap: "1rem",
        paddingTop: "55px",
        paddingBottom: "30px"
      }}>
      <h1>{header}</h1>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "1rem",
          padding: "2rem",
          borderRadius: "12px",
          border: "1px solid #e0e0e0",
          boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
          backgroundColor: "white",
          width: "100%",
          maxWidth: "350px",
          margin: "20px auto"
        }}>
        {textField}

        {submitButton}

        {link}
      </div>
    </section>
  );
}
