import { useState } from "react";
import { Col } from "react-bootstrap";
import { Link } from "react-router-dom";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const loginUser = async () => {
    //Need to Setup
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
            <input
              style={{
                borderRadius: "8px",
                border: "1px solid #ccc",
                padding: "12px",
                fontSize: "1rem"
              }}
              type="email"
              placeholder="johndoe@calpoly.edu"
              onChange={(e) => {
                setEmail(e.target.value);
              }}
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
            <input
              style={{
                borderRadius: "8px",
                border: "1px solid #ccc",
                padding: "12px",
                fontSize: "1rem"
              }}
              type="password"
              placeholder="Password"
              onChange={(e) => {
                setPassword(e.target.value);
              }}
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
      <div
        style={{
          flex: "1",
          backgroundColor: "#f4f4f4",
          padding: "60px"
        }}>
        <section
          style={{
            flex: "1",
            display: "flex",
            flexDirection: "row",
            justifyContent: "center",
            alignItems: "center",
            gap: "40px",
            maxWidth: "1000px",
            margin: "0 auto"
          }}>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> Verified Students </h2>
            <p>
              {" "}
              Exclusive to the Mustang community. Secure,
              student-only access.
            </p>
          </Col>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> On Campus </h2>
            <p>
              Your campus, your marketplace. Find specialized
              help and gear just a short walk from your dorm.
            </p>
          </Col>
          <Col
            style={{
              flex: 1,
              textAlign: "center",
              maxWidth: "300px"
            }}>
            <h2> n Categories </h2>
            <p>
              {" "}
              Built for student life. Specialized categories
              tailored to your major and dorm needs.
            </p>
          </Col>
        </section>
      </div>
    </div>
  );
}
