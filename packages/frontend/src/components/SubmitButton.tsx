import "../Styles/SubmitButton.css";

interface SubmitButtonProps {
  label: string;
  onClick: React.MouseEventHandler<HTMLButtonElement>;
}

export default function SubmitButton({
  label,
  onClick
}: SubmitButtonProps) {
  return (
    <button className="button"
    onClick={onClick}>
      {label}
    </button>
  );
}
