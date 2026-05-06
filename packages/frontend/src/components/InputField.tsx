interface InputFieldProps {
  label: string;
  type: string;
  placeHolder: string;
  onChange: React.ChangeEventHandler<HTMLInputElement>;
}

export default function InputField({
  label,
  type,
  placeHolder,
  onChange
}: InputFieldProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: "4px",
        width: "100%",
        textAlign: "left"
      }}>
      <label
        style={{
          fontSize: "0.9rem",
          fontWeight: "600",
          color: "#333"
        }}>
        {label}
      </label>

      <input
        style={{
          borderRadius: "8px",
          border: "1px solid #ccc",
          padding: "12px",
          fontSize: "1rem"
        }}
        type={type}
        placeholder={placeHolder}
        onChange={onChange}
      />
    </div>
  );
}
