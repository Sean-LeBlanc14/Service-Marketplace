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
import { API_ENDPOINTS } from "../utils/api";

function SignupPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [major, setMajor] = useState("");
  type CampusType = "San Luis Obispo" | "Maritime Academy" | "";
  const [campus, setCampus] = useState<CampusType>("");
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

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
        API_ENDPOINTS.auth.signup,
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
        toast.error("An account with this email already exists.");
      } else if (response.status === 400) {
        toast.error(
          "Please enter a valid email and a password of at least 8 characters."
        );
      } else {
        toast.error("Something went wrong. Please try again.");
      }

    } catch {
      toast.warning(
        "Unable to connect to the server. Please try again."
      );
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
                  type="text"
                  placeHolder="Your Name"
                  onChange={(e) => setFirstName(e.target.value)}
                />
              </div>

              <div className="user-field">
                <InputField
                  value={lastName}
                  label="Last Name"
                  type="text"
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
                  type="text"
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
                  onChange={(e) => setCampus(e.target.value as CampusType)}
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
