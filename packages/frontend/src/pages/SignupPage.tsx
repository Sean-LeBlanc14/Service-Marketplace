import { useState } from "react";

function SignupPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError("");

        try {
            const response = await fetch("http://localhost:8080/api/auth/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                setSuccess(true);
            } else if (response.status === 409) {
                setError("An account with this email already exists.");
            } else if (response.status === 400) {
                setError("Please enter a valid email and a password of at least 8 characters.");
            } else {
                setError("Something went wrong. Please try again.");
            }
        } catch {
            setError("Unable to connect to the server. Please try again.");
        }
    };

    if (success) {
        return (
            <div>
                <h2>You're registered!</h2>
                <p>Your account has been created successfully.</p>
            </div>
        );
    }

    return (
        <div>
            <h2>Sign Up</h2>
            <form onSubmit={handleSubmit}>
                <div>
                    <label htmlFor="email">University Email</label>
                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>
                {error && <p style={{ color: "red" }}>{error}</p>}
                <button type="submit">Sign Up</button>
            </form>
        </div>
    );
}

export default SignupPage;