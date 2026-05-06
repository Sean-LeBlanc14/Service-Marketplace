interface SubmitButtonProps {
  label: string;
  onClick: React.MouseEventHandler<HTMLButtonElement>;
}

export default function SubmitButton({
  label,
  onClick
}: SubmitButtonProps) {
  return (
    <button
      style={{
        width: "100%",
        padding: "12px",
        backgroundColor: "#003831",
        color: "white",
        border: "none",
        borderRadius: "8px",
        cursor: "pointer",
        fontWeight: "bold"
      }}
      onClick={onClick}>
      {label}
    </button>
  );
}
