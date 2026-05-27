import "./styles/NavBar.css";
import SearchBar from "./SearchBar";
import { useState } from "react";
import { FaBell, FaCalendarAlt } from "react-icons/fa";

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
    <nav className="navbar">
      <div className="navbar-search">
        <SearchBar
          value={query}
          placeholder="Search for Services"
          onChange={setQuery}
          onSearch={search}
        />
      </div>

      <div className="navbar-buttons">
        {/*TODO:  Modal Notification and calendar views*/}
        <button className="notification-icon">
          <FaBell size={25} />
        </button>

        <button className="calendar-icon">
          <FaCalendarAlt size={25} />
        </button>
      </div>
    </nav>
  );
}
