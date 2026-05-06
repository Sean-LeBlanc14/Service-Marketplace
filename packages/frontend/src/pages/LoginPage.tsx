import { useState } from "react";

import { Link } from "react-router-dom";
import TextField from "../components/TextField";
import InformationSection from "../components/InformationSection";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const loginUser = async () => {

    const user = {
      email: email,
      password: password
    }

    try{
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user)
      });

      if (response.ok){
        const data = await response.json();

        console.log(data);
      }
    }catch(e){
      console.error('An error occurred when logging in: ', e);
    }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        backgroundColor: "white",
        margin: "0px"
      }}>
      {/*Login Box*/}
      <section
        style={{
          display: "grid",
          placeItems: "center",
          gap: "1rem",
          paddingTop: "55px",
          paddingBottom: "30px"
        }}>
        <h1>Welcome to Service Market Place</h1>
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "1rem",
            padding: "2rem",
            borderRadius: "12px",
            border: "1px solid #e0e0e0",
            boxShadow: "0 4px 12px rgba(0,0,0,0.08)",
            backgroundColor: "white",
            width: "100%",
            maxWidth: "350px",
            margin: "20px auto"
          }}>
          {/*Email*/}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "4px",
              width: "100%",
              textAlign: "left"
            }}>
            <label
              style={{
                fontSize: "0.9rem",
                fontWeight: "600",
                color: "#333"
              }}>
              Email
            </label>
            
            <TextField
              type="email"
              placeHolder="johndoe@calpoly.edu"
              onChange={(e)=> {setEmail(e.target.value)}}
            />
          </div>
          {/*Password*/}
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "4px",
              width: "100%",
              textAlign: "left"
            }}>
            <label
              style={{
                fontSize: "0.9rem",
                fontWeight: "600",
                color: "#333"
              }}>
              Password
            </label>
            <TextField
              type="password"
              placeHolder="Password"
              onChange={(e)=>{setPassword(e.target.value)}}
            />
          </div>
          <button
            style={{
              width: "100%",
              padding: "12px",
              backgroundColor: "#003831",
              color: "white",
              border: "none",
              borderRadius: "8px",
              cursor: "pointer",
              fontWeight: "bold"
            }}
            onClick={loginUser}>
            Login
          </button>

          <Link
            to="/signup"
            style={{ fontSize: "0.9rem", color: "#003831" }}>
            Create an Account
          </Link>
        </div>
      </section>
      
      {/* Description Section*/}
      <InformationSection/>
    </div>
  );
}
