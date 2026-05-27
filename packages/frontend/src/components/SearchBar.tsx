import "./styles/SearchBar.css";
import { FaMagnifyingGlass } from "react-icons/fa6";

interface SearchBarProps {
  value: string;
  placeholder: string;
  onChange: (val: string) => void;
  onSearch: () => void;
}

export default function SearchBar({
  value,
  placeholder,
  onChange,
  onSearch
}: SearchBarProps) {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch();
  };

  return (
    <form onSubmit={handleSubmit} className="search-container">
      <FaMagnifyingGlass size={20} />
      <input
        type="search"
        className="searchBar"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
      />
    </form>
  );
}
