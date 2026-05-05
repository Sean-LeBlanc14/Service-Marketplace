import React from "react";
import { loginRequest } from "../authConfig";
import { useState, useEffect } from "react";

import { useMsal } from "@azure/msal-react";
import { InteractionStatus } from "@azure/msal-browser";
import {
  UnauthenticatedTemplate,
  AuthenticatedTemplate
} from "@azure/msal-react";
import {
  Container,
  Row,
  Col,
  Button,
  Card
} from "react-bootstrap";

function SignupPage() {
  const { instance, inProgress } = useMsal();

  const registerUser = async () => {
    const user = {
      email: userEmail,
      password: password
    };

    console.log(user);

    const response = await fetch(
      " http://localhost:8080/api/auth/register",
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(user)
      }
    );

    console.log(response.status);

    return response.ok;
  };

  /* 
  For testing cors
  useEffect(() => {
    const foo = async () => {
      const response = await fetch(
        "http://localhost:8080/test/Cors"
      );

      const data = await response.json();

      console.log(data);
    };
    foo();
  }, []);
  */
 
  const handleLoginRedirect = () => {
    // Only proceed if MSAL is idle (InteractionStatus.None)
    if (inProgress === "none") {
      instance
        .loginRedirect({
          scopes: ["User.Read"],
          loginHint: userEmail, // Passes your local state to Microsoft
          prompt: "create" // Forces the 'Create Account' UI
        })
        .catch((error) => {
          console.error(error);
        });
    }
  };

  const [userEmail, setUserEmail] = useState("");
  const [password, setPassword] = useState("");
  const [campus, setCampus] = useState("");

  return (
    <div className="landing-wrapper">
      {/*(<UnauthenticatedTemplate>*/}
      <header className="bg-light py-5 border-bottom">
        <Container className="px-5">
          <Row className="gx-5 justify-content-center">
            <Col lg={8} xl={7} xll={6}>
              <div className="my-5 text-center">
                <h1 className="display-3 fw-bolder text-dark mb-2">
                  Services Marketplace
                </h1>
                <p className="lead fw-normal text-muted mb-4">
                  Connect with verified Cal Poly students for
                  offered services
                </p>
                <div className="d-grid gap-3 d-sm-flex justify-content-sm-center">
                  {/*
                  <Button
                    className=""
                    variant="primary"
                    style={{
                      backgroundColor: "#154734",
                      color: "#FFFFFF"
                    }}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor =
                        "#0E3022")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor =
                        "#154734")
                    }
                    onClick={handleLoginRedirect}
                    disabled={
                      inProgress !== InteractionStatus.None
                    }>
                    {inProgress !== InteractionStatus.None
                      ? "Initializing..."
                      : "Cal Poly Portal Login"}
                  </Button>
                    */}
                  <input
                    type="email"
                    placeholder="Cal Poly Email"
                    onChange={(e) => {
                      setUserEmail(e.target.value);
                    }}
                  />

                  <input
                    type="password"
                    placeholder="Password"
                    onChange={(e) => {
                      setPassword(e.target.value);
                    }}
                  />

                  <Button
                    style={{
                      backgroundColor: "#154734",
                      color: "#FFFFFF"
                    }}
                    onClick={() => {
                      registerUser();
                    }}>
                    Sign Up for Service MarketPlace
                  </Button>

                  <Button
                    variant="outline-dark"
                    size="lg"
                    className="px-4">
                    Learn More
                  </Button>
                </div>
              </div>
            </Col>
          </Row>
        </Container>
      </header>

      <section className="py-5">
        <Container className="px-5 my-5">
          <Row className="gx-5">
            <Col lg={4} className="mb-5 mb-lg-0">
              <h2 className="h4 fw-bolder">Secure Login</h2>
              <p>
                We use Microsoft Entra ID to ensure your data
                stays within your organization's security
                boundary.
              </p>
            </Col>
            <Col lg={4} className="mb-5 mb-lg-0">
              <h2 className="h4 fw-bolder">
                Service Discovery
              </h2>
              <p>
                Find specialized help campus-wide using our
                Marketplace.
              </p>
            </Col>
            <Col lg={4}>
              <h2 className="h4 fw-bolder">
                Verified Profiles
              </h2>
              <p>
                Every user is authenticated against your tenant,
                ensuring zero-trust identity verification.
              </p>
            </Col>
          </Row>
        </Container>
      </section>
      {/*</UnauthenticatedTemplate>*/}
    </div>
  );
}

export default SignupPage;
