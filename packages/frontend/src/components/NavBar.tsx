import "../Styles/NavBar.css";
import SearchBar from "./SearchBar";
import { useState } from "react";
import { FaBell } from "react-icons/fa";

export default function NavBar() {
  const [query, setQuery] = useState("");

  const search = async () => {
    try {
      //Query logic
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <nav className="navbar-container">
      {/*Place holder for logo*/}
      <p> Logo</p>

      <SearchBar
        value={query}
        placeholder="Search for Services"
        onChange={setQuery}
        onSearch={search}
      />

      <button className="notification-icon">
        <FaBell size={20} />
      </button>
    </nav>
  );
}
