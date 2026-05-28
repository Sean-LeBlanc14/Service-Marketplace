import React, { useState, useEffect, useRef } from "react";
import "./styles/ResendTimer.css";

interface ResendTimerLinkProps {
  onResend: () => Promise<void> | void;
}

const COOLDOWN_MS = 10 * 60 * 1000; // 10 minutes
const LOCAL_STORAGE_KEY = "polyServices_auth_cooldown_end";

export const ResendTimerLink: React.FC<
  ResendTimerLinkProps
> = ({ onResend }) => {
  const [timeLeft, setTimeLeft] = useState<number>(() => {
    const savedEndTime = localStorage.getItem(
      LOCAL_STORAGE_KEY
    );
    if (!savedEndTime) return 0;

    const endTime = parseInt(savedEndTime, 10);
    const remaining = endTime - Date.now();

    if (remaining > 0) {
      return remaining;
    }

    localStorage.removeItem(LOCAL_STORAGE_KEY);
    return 0;
  });
  const [isSending, setIsSending] = useState<boolean>(false);
  const intervalRef = useRef<number | null>(null);

  const startCountdown = (endTime: number) => {
    if (intervalRef.current !== null)
      window.clearInterval(intervalRef.current);
    intervalRef.current = window.setInterval(() => {
      const remaining = endTime - Date.now();
      if (remaining <= 0) {
        if (intervalRef.current !== null)
          window.clearInterval(intervalRef.current);
        setTimeLeft(0);
        localStorage.removeItem(LOCAL_STORAGE_KEY);
      } else {
        setTimeLeft(remaining);
      }
    }, 1000);
  };

  useEffect(() => {
    const savedEndTime = localStorage.getItem(
      LOCAL_STORAGE_KEY
    );
    if (savedEndTime) {
      const endTime = parseInt(savedEndTime, 10);
      const remaining = endTime - Date.now();
      if (remaining > 0) {
        startCountdown(endTime);
      } else {
        localStorage.removeItem(LOCAL_STORAGE_KEY);
      }
    }

    return () => {
      if (intervalRef.current !== null)
        window.clearInterval(intervalRef.current);
    };
  }, []);

  const handleTriggerClick = async () => {
    if (timeLeft > 0 || isSending) return;

    try {
      setIsSending(true);
      await onResend();

      const newEndTime = Date.now() + COOLDOWN_MS;
      localStorage.setItem(
        LOCAL_STORAGE_KEY,
        newEndTime.toString()
      );
      setTimeLeft(COOLDOWN_MS);
      startCountdown(newEndTime);
    } catch (error) {
      console.error("Could not trigger code resend:", error);
    } finally {
      setIsSending(false);
    }
  };

  const formatTime = (ms: number): string => {
    const totalSeconds = Math.ceil(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
  };

  if (isSending) {
    return (
      <button
        type="button"
        className="resend-button loading"
        disabled>
        Sending...
      </button>
    );
  }

  if (timeLeft > 0) {
    return (
      <button
        type="button"
        className="resend-button disabled-timer"
        disabled>
        Resend code in {formatTime(timeLeft)}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleTriggerClick}
      className="resend-button">
      Resend code
    </button>
  );
};
