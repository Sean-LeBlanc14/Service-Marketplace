import "../Styles/FormContainer.css";

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
    <section className="form-section">
      <h1>{header}</h1>
      <div className="form-field">
        {textField}

        {submitButton}

        {link}
      </div>
    </section>
  );
}
