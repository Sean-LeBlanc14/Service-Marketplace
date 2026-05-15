interface DropDownProps {
  label: string;
  options: React.ReactNode;
  value: string;
  placeHolder: string;
  onChange: React.ChangeEventHandler<HTMLSelectElement>;
}
export default function DropDown({
  label,
  options,
  value,
  placeHolder,
  onChange
}: DropDownProps) {
  return (
    <div>
      <label
        style={{
          fontSize: "0.9rem",
          fontWeight: "600",
          color: "#333"
        }}>
        {label}
      </label>
      <select
        value={value}
        onChange={onChange}
        style={{
          borderRadius: "8px",
          border: "1px solid #ccc",
          padding: "12px",
          fontSize: "1rem",
          width: "100%",
          backgroundColor: "white",
          color: value === "" ? "#A9A9A9" : "black"
        }}>
        <option value="" disabled>
          {placeHolder}
        </option>
        {options}
      </select>
    </div>
  );
}
