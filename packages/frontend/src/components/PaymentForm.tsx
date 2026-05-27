import { useState } from "react";
import { loadStripe } from "@stripe/stripe-js";
import {
    Elements,
    PaymentElement,
    useStripe,
    useElements
} from "@stripe/react-stripe-js";
import SubmitButton from "./SubmitButton";

const STRIPE_PUBLISHABLE_KEY = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string;

const stripePromise = loadStripe(STRIPE_PUBLISHABLE_KEY);

interface PaymentFormInnerProps {
    onSuccess: () => void;
    onError: (message: string) => void;
}

function PaymentFormInner({ onSuccess, onError }: PaymentFormInnerProps) {
    const stripe = useStripe();
    const elements = useElements();
    const [isLoading, setIsLoading] = useState(false);

    async function handleSubmit() {
        if (!stripe || !elements) return;

        setIsLoading(true);

        const { error } = await stripe.confirmPayment({
        elements,
        confirmParams: {
            return_url: window.location.href
        },
        redirect: "if_required"
        });

        if (error) {
        onError(error.message ?? "Payment failed.");
        } else {
        onSuccess();
        }

        setIsLoading(false);
    }

    return (
        <div className="payment-form">
        <PaymentElement />
        <div style={{ marginTop: "1rem" }}>
            <SubmitButton
            label={isLoading ? "Processing..." : "Pay Now"}
            onClick={handleSubmit}
            />
        </div>
        </div>
    );
    }

    interface PaymentFormProps {
    clientSecret: string;
    onSuccess: () => void;
    onError: (message: string) => void;
    }

    function PaymentForm({ clientSecret, onSuccess, onError }: PaymentFormProps) {
    return (
        <Elements stripe={stripePromise} options={{ clientSecret }}>
        <PaymentFormInner onSuccess={onSuccess} onError={onError} />
        </Elements>
    );
}

export default PaymentForm;