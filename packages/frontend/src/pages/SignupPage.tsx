import { useState } from "react";
import { useNavigate } from "react-router-dom";
import InputField from "../components/InputField";
import NavLink from "../components/NavLink";
import InformationSection from "../components/InformationSection";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";
import DropDown from "../components/DropDown";
import { toast } from "react-toastify";
import "../Styles/SignupPage.css";

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
    if (!email.endsWith("@calpoly.edu")) {
      toast.error("Please use a valid Cal Poly email");
      return;
    }
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
        const data = await response.json();
        localStorage.setItem("jwt_token", data.token);
        navigate("/verify", {
          state: { email: data.email, token: data.token }
        });
      } else if (response.status === 409) {
        setError("An account with this email already exists.");
      } else if (response.status === 400) {
        setError(
          "Please enter a valid email and a password of at least 8 characters."
        );
      } else {
        setError("Something went wrong. Please try again.");
      }

      if (error) {
        toast.error(error);
      }
    } catch {
      setError(
        "Unable to connect to the server. Please try again."
      );
      toast.warn(error);
    }
  };

  return (
    <div className="signup-container">
      <FormContainer
        header={"Welcome to Poly Services"}
        textField={
          <>
            <div className="field-container">
              <div className="user-field">
                <InputField
                  value={firstName}
                  label="First Name"
                  placeHolder="Your Name"
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>

              <div className="user-field">
                <InputField
                  value={lastName}
                  label="Last Name"
                  placeHolder="Your Last Name"
                  onChange={(e) => setLastName(e.target.value)}
                />
              </div>
            </div>

            <div className="field-container">
              <div className="user-field">
                <InputField
                  value={major}
                  label="Major"
                  placeHolder="Your Major"
                  onChange={(e) => setMajor(e.target.value)}
                />
              </div>

              <div className="user-field">
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
              value={email}
              label="Email"
              type="email"
              placeHolder="johndoe@calpoly.edu"
              onChange={(e) => setEmail(e.target.value)}
            />
            <InputField
              value={password}
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
