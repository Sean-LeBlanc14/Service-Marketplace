import "./styles/InputField.css";

interface InputFieldProps {
  value: string;
  label: string;
  type: string;
  placeHolder: string;
  onChange: React.ChangeEventHandler<HTMLInputElement>;
}

export default function InputField({
  value,
  label,
  type,
  placeHolder,
  onChange
}: InputFieldProps) {
  return (
    <div className="input-container">
      <label className="input-label">{label}</label>

      <input
        className="input-input"
        value={value}
        type={type}
        placeholder={placeHolder}
        onChange={onChange}
      />
    </div>
  );
}
