import { useState, useRef, useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";
import { toast } from "react-toastify";
import "../Styles/VerifyAccount.css";
import { ResendTimerLink } from "../components/ResendTimerLink";

export default function VerifyAccount() {
  const [code, setCode] = useState<string[]>([
    "",
    "",
    "",
    "",
    "",
    ""
  ]);
  const [error, setError] = useState("");
  const inputs = useRef<(HTMLInputElement | null)[]>([]);
  const navigate = useNavigate();
  const location = useLocation();
  const { email, token } = location.state || {};

  useEffect(() => {
    if (!token) {
      navigate("/signup");
    }
  }, [token, navigate]);

  if (!token) {
    return null;
  }

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;
    const newCode = [...code];
    newCode[index] = value.slice(-1);
    setCode(newCode);
    if (value && index < 5) {
      inputs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (
    index: number,
    e: React.KeyboardEvent<HTMLInputElement>
  ) => {
    if (e.key === "Backspace" && !code[index] && index > 0) {
      inputs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (
    e: React.ClipboardEvent<HTMLInputElement>
  ) => {
    e.preventDefault();
    const pasted = e.clipboardData
      .getData("text")
      .replace(/\D/g, "")
      .slice(0, 6);
    const newCode = [...code];
    pasted.split("").forEach((char, i) => {
      newCode[i] = char;
    });
    setCode(newCode);
    inputs.current[Math.min(pasted.length, 5)]?.focus();
  };

  const handleSubmit = async (e: React.MouseEvent) => {
    e.preventDefault();
    setError("");
    const fullCode = code.join("");
    if (fullCode.length < 6) {
      setError("Please enter the full 6-digit code.");
      toast.error(error);
      return;
    }

    try {
      const response = await fetch(
        "http://localhost:8080/api/verification/code",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ code: fullCode })
        }
      );

      if (response.ok) {
        navigate("/homepage");
      } else if (response.status === 401) {
        setError("Session expired. Please sign up again.");
      } else if (response.status === 400) {
        setError("Invalid verification code.");
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

  const handleResend = async () => {
    try {
      await fetch(
        "http://localhost:8080/api/verification/resend",
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({})
        }
      );
    } catch {
      setError("Unable to resend code. Please try again.");
      toast.warn(error);
    }
  };

  return (
    <FormContainer
      header="Verify Your Email"
      textField={
        <>
          <p className="message-text">
            We sent a 6-digit code to <strong>{email}</strong>{" "}
            If you don't see it, check your junk or spam folder.
          </p>
          <div className="code-container">
            {code.map((digit, i) => (
              <input
                key={i}
                ref={(el) => (inputs.current[i] = el)}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) =>
                  handleChange(i, e.target.value)
                }
                onKeyDown={(e) => handleKeyDown(i, e)}
                onPaste={i === 0 ? handlePaste : undefined}
                className="code-input"
              />
            ))}
          </div>
        </>
      }
      submitButton={
        <SubmitButton label="Verify" onClick={handleSubmit} />
      }
      link={<ResendTimerLink onResend={handleResend} />}
    />
  );
}
