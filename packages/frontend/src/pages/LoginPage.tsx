import { useState } from "react";
import InputField from "../components/InputField";
import NavLink from "../components/NavLink";
import InformationSection from "../components/InformationSection";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import "../Styles/LoginPage.css";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [error, setError] = useState("");
  const navigate = useNavigate();

  const loginUser = async () => {
    const user = {
      email: email,
      password: password
    };

    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/login",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(user)
        }
      );

      if (response.ok) {
        const data = await response.json();

        //Using local storage to store token for now, possibly use cookies in the future
        localStorage.setItem("jwt_token", data.token);
        navigate("/homepage");
      } else if (response.status === 401) {
        setError("Invalid Email or Password!");
      } else {
        setError("Something went wrong, please try again.");
        toast.warn(error);
      }

      if (error) {
        toast.error(error);
      }
    } catch {
      setError(
        "Unable to connect to the server, please try again."
      );
      toast.warn(error);
    }
  };

  return (
    <div className="login-container">
      {/*Login Box*/}
      <FormContainer
        header={"Welcome to Poly Services"}
        textField={
          <>
            <InputField
              value={email}
              label="Email"
              type="email"
              placeHolder="mustang@calpoly.edu"
              onChange={(e) => setEmail(e.target.value)}
            />{" "}
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
          label={"Login"}
          onClick={loginUser}
        />
        link=<NavLink to="/signup" label="Create Account" />
      />

      {/* Description Section*/}
      <InformationSection />
    </div>
  );
}
