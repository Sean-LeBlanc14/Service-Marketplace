import "../Styles/NavBar.css";
import SearchBar from "./SearchBar";
import { useState } from "react";
import { FaBell } from "react-icons/fa";
import "../assets/logo.png"

export default function NavBar() {

  const imagePath = "../assets/logo.png";
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
      <img src={imagePath}/>

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
