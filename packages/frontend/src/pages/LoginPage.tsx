import { useState } from "react";
import InputField from "../components/InputField";
import NavLink from "../components/NavLink";
import InformationSection from "../components/InformationSection";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";
import { useNavigate } from "react-router-dom";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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

        navigate("/");
      }
    } catch (e) {
      console.error("An error occurred when logging in: ", e);
    }
  };

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        backgroundColor: "white"
      }}>
      {/*Login Box*/}
      <FormContainer
        header={"Welcome to Service Market Place"}
        textField={
          <>
            <InputField
              label="Email"
              type="email"
              placeHolder="johndoe@calpoly.edu"
              onChange={(e) => setEmail(e.target.value)}
            />{" "}
            <InputField
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
        link=<NavLink to= "/signup" label = "Create Account"/>
      />

      {/* Description Section*/}
      <InformationSection />
    </div>
  );
}
