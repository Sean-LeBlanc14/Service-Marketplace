import { useState, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import FormContainer from "../components/FormContainer";
import SubmitButton from "../components/SubmitButton";

export default function VerifyAccount() {
    const [code, setCode] = useState<string[]>(["", "", "", "", "", ""]);
    const [error, setError] = useState("");
    const inputs = useRef<(HTMLInputElement | null)[]>([]);
    const navigate = useNavigate();
    const location = useLocation();
    const { email, token } = location.state || {};

    const handleChange = (index: number, value: string) => {
        if (!/^\d*$/.test(value)) return;
        const newCode = [...code];
        newCode[index] = value.slice(-1);
        setCode(newCode);
        if (value && index < 5) {
        inputs.current[index + 1]?.focus();
        }
    };

    const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Backspace" && !code[index] && index > 0) {
        inputs.current[index - 1]?.focus();
        }
    };

    const handleSubmit = async (e: React.MouseEvent) => {
        e.preventDefault();
        setError("");
        const fullCode = code.join("");
        if (fullCode.length < 6) {
        setError("Please enter the full 6-digit code.");
        return;
        }

        try {
        const response = await fetch("http://localhost:8080/api/verification/code", {
            method: "POST",
            headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ code: fullCode })
        });

        if (response.ok) {
            navigate("/");
        } else if (response.status === 401) {
            setError("Session expired. Please sign up again.");
        } else if (response.status === 400) {
            setError("Invalid verification code.");
        } else {
            setError("Something went wrong. Please try again.");
        }
        } catch {
        setError("Unable to connect to the server. Please try again.");
        }
    };

    const handleResend = async () => {
        try {
        await fetch("http://localhost:8080/api/verification/resend", {
            method: "PUT",
            headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({})
        });
        } catch {
        setError("Unable to resend code. Please try again.");
        }
    };

    return (
        <FormContainer
        header="Verify Your Email"
        textField={
            <>
            <p style={{ textAlign: "center", color: "#555", margin: 0 }}>
                We sent a 6-digit code to <strong>{email}</strong>
            </p>
            <div style={{ display: "flex", gap: "10px", justifyContent: "center" }}>
                {code.map((digit, i) => (
                <input
                    key={i}
                    ref={(el) => (inputs.current[i] = el)}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleChange(i, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(i, e)}
                    style={{
                    width: "45px",
                    height: "55px",
                    textAlign: "center",
                    fontSize: "1.5rem",
                    borderRadius: "8px",
                    border: "1px solid #e0e0e0",
                    boxShadow: "0 2px 6px rgba(0,0,0,0.06)",
                    outline: "none"
                    }}
                />
                ))}
            </div>
            {error && <p style={{ color: "red", textAlign: "center", margin: 0 }}>{error}</p>}
            </>
        }
        submitButton={<SubmitButton label="Verify" onClick={handleSubmit} />}
        link={
            <span
            onClick={handleResend}
            style={{ fontSize: "0.9rem", color: "#003831", cursor: "pointer" }}>
            Resend code
            </span>
        }
        />
    );
}