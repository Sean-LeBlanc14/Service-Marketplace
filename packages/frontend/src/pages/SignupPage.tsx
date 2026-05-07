import { useState } from "react";
import { useNavigate } from "react-router-dom";
import InputField from "../components/InputField";
import NavLink from "../components/NavLink";
import InformationSection from "../components/InformationSection";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";
import DropDown from "../components/DropDown";

function SignupPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [major, setMajor] = useState("");
  type CampusType = "San Luis Obispo" | "Maritime Academy" | "";
  const campusOptions = ["San Luis Obispo", "Maritime Academy"];
  const [campus, setCampus] = useState<CampusType>("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    const user = {
      email: email,
      password: password,
      firstName: firstName,
      lastName: lastName,
      campus: campus,
      major: major
    };
    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/register",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(user)
        }
      );

      if (response.ok) {
        navigate("/");
      } else if (response.status === 409) {
        setError("An account with this email already exists.");
      } else if (response.status === 400) {
        setError(
          "Please enter a valid email and a password of at least 8 characters."
        );
      } else {
        setError("Something went wrong. Please try again.");
      }
    } catch {
      setError(
        "Unable to connect to the server. Please try again."
      );
    }
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        backgroundColor: "white"
      }}>
      <FormContainer
        header={"Welcome to Service Market Place"}
        textField={
          <>
            <div
              style={{
                display: "flex",
                flexDirection: "row",
                gap: "10px"
              }}>
              <div style={{ flex: 1 }}>
                <InputField
                  label="First Name"
                  placeHolder="Your Name"
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>

              <div style={{ flex: 1 }}>
                <InputField
                  label="Last Name"
                  placeHolder="Your Last Name"
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
            </div>

            <div
              style={{
                display: "flex",
                flexDirection: "row",
                gap: "10px"
              }}>
              <div style={{ flex: 1 }}>
                <InputField
                  label="Major"
                  placeHolder="Your Major"
                  onChange={(e) => setMajor(e.target.value)}
                />
              </div>

              <div style={{ flex: 1 }}>
                <DropDown
                  label="Campus"
                  value={campus}
                  placeHolder={"Select a Campus"}
                  options={
                    <>
                      <option value="San Luis Obispo">
                        San Luis Obispo
                      </option>

                      <option value="Maritime Academy">
                        Maritime Academy
                      </option>
                    </>
                  }
                  onChange={(e) => setCampus(e.target.value)}
                />
              </div>
            </div>

            <InputField
              label="Email"
              type="email"
              placeHolder="johndoe@calpoly.edu"
              onChange={(e) => setEmail(e.target.value)}
            />
            <InputField
              label="Password"
              type="password"
              placeHolder="Password"
              onChange={(e) => setPassword(e.target.value)}
            />
          </>
        }
        submitButton=<SubmitButton
          label={"Sign Up"}
          onClick={handleSubmit}
        />
        link=<NavLink
          to="/login"
          label="Already have an account?"
        />
      />

      <InformationSection />
    </div>
  );
}

export default SignupPage;
