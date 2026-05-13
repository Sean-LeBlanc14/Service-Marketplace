import "../Styles/DropDown.css";

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
      <label className="drop-down-label">
        {label}
      </label>
      <select
        value={value}
        onChange={onChange}
        className={`drop-down-select ${!value ? "is-placeholder" : ""}`}>
        <option value="" disabled>
          {placeHolder}
        </option>
        {options}
      </select>
    </div>
  );
}
