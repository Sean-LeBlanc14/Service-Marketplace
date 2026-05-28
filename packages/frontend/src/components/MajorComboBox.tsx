import { useEffect, useMemo, useRef, useState } from "react";
import "./styles/InputField.css";
import "./styles/MajorComboBox.css";

interface MajorComboBoxProps {
  value: string;
  onChange: (value: string) => void;
}

const MAJORS = [
  "Aerospace Engineering",
  "Agricultural Business",
  "Agricultural Communication",
  "Agricultural Science",
  "Agricultural Systems Management",
  "Animal Science",
  "Anthropology and Geography",
  "Architectural Engineering",
  "Architecture",
  "Art and Design",
  "Biochemistry",
  "Biological Sciences",
  "Biomedical Engineering",
  "BioResource and Agricultural Engineering",
  "Business Administration",
  "Chemistry",
  "Child Development",
  "City and Regional Planning",
  "Civil Engineering",
  "Communication Studies",
  "Comparative Ethnic Studies",
  "Computer Engineering",
  "Computer Science",
  "Construction Management",
  "Dairy Science",
  "Economics",
  "Electrical Engineering",
  "English",
  "Environmental Earth and Soil Sciences",
  "Environmental Engineering",
  "Environmental Management and Protection",
  "Experience and Event Management",
  "Facilities Engineering Technology",
  "Food Science",
  "Forest and Fire Sciences",
  "General Engineering",
  "Graphic Communication",
  "History",
  "Industrial Engineering",
  "Industrial Technology and Packaging",
  "Interdisciplinary Studies",
  "International Strategy and Security",
  "Journalism",
  "Kinesiology",
  "Landscape Architecture",
  "Liberal Arts and Engineering Studies",
  "Liberal Studies",
  "Manufacturing Engineering",
  "Marine Engineering Technology",
  "Marine Sciences",
  "Marine Transportation",
  "Materials Engineering",
  "Mathematics",
  "Mechanical Engineering",
  "Microbiology",
  "Music",
  "Nutrition",
  "Oceanography",
  "Philosophy",
  "Physics",
  "Plant Sciences",
  "Political Science",
  "Psychology",
  "Public Health",
  "Sociology",
  "Software Engineering",
  "Spanish",
  "Statistics",
  "Theatre Arts",
  "Wine and Viticulture"
];

export default function MajorComboBox({ value, onChange }: MajorComboBoxProps) {
  const [isFocused, setIsFocused] = useState(false);
  const [inputText, setInputText] = useState(value);
  const [selectedMajor, setSelectedMajor] = useState(value);
  const isClearingSelectionFromInput = useRef(false);

  useEffect(() => {
    if (isClearingSelectionFromInput.current && value === "") {
      isClearingSelectionFromInput.current = false;
      setSelectedMajor("");
      return;
    }

    isClearingSelectionFromInput.current = false;
    setInputText(value);
    setSelectedMajor(value);
  }, [value]);

  const filteredMajors = useMemo(() => {
    const query = inputText.trim().toLowerCase();

    if (!query) {
      return MAJORS;
    }

    return MAJORS.filter((major) => major.toLowerCase().includes(query));
  }, [inputText]);

  const showDropdown = isFocused && filteredMajors.length > 0;

  return (
    <div className="input-container major-combobox">
      <label className="input-label">Major</label>

      <input
        className="input-input"
        value={inputText}
        type="text"
        onKeyDown={(event) => {
          if (event.key !== "Tab" || !showDropdown) {
            return;
          }

          const firstMatch = filteredMajors[0];

          event.preventDefault();
          setInputText(firstMatch);
          setSelectedMajor(firstMatch);
          onChange(firstMatch);
          setIsFocused(false);
        }}
        onChange={(event) => {
          setInputText(event.target.value);
          if (selectedMajor) {
            setSelectedMajor("");
          }
          isClearingSelectionFromInput.current = true;
          onChange("");
          setIsFocused(true);
        }}
        onFocus={() => setIsFocused(true)}
        onBlur={() => setIsFocused(false)}
      />

      {showDropdown && (
        <div className="major-combobox-dropdown">
          {filteredMajors.map((major) => (
            <div
              className="major-combobox-option"
              key={major}
              onMouseDown={(event) => {
                event.preventDefault();
                setInputText(major);
                setSelectedMajor(major);
                onChange(major);
                setIsFocused(false);
              }}>
              {major}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
