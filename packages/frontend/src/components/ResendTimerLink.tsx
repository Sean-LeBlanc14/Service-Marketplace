import React, { useState, useEffect, useRef } from "react";

interface ResendTimerLinkProps {
  onResend: () => Promise<void> | void;
}

const COOLDOWN_MS = 10 * 60 * 1000; // 10 minutes
const LOCAL_STORAGE_KEY = "polyServices_auth_cooldown_end";

export const ResendTimerLink: React.FC<
  ResendTimerLinkProps
> = ({ onResend }) => {
  const [timeLeft, setTimeLeft] = useState<number>(0);
  const [isSending, setIsSending] = useState<boolean>(false);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    const savedEndTime = localStorage.getItem(
      LOCAL_STORAGE_KEY
    );
    if (savedEndTime) {
      const remaining = parseInt(savedEndTime, 10) - Date.now();
      if (remaining > 0) {
        setTimeLeft(remaining);
        startCountdown(parseInt(savedEndTime, 10));
      } else {
        localStorage.removeItem(LOCAL_STORAGE_KEY);
      }
    }
    return () => {
      if (intervalRef.current)
        clearInterval(intervalRef.current);
    };
  }, []);

  const startCountdown = (endTime: number) => {
    if (intervalRef.current) clearInterval(intervalRef.current);
    intervalRef.current = setInterval(() => {
      const remaining = endTime - Date.now();
      if (remaining <= 0) {
        if (intervalRef.current)
          clearInterval(intervalRef.current);
        setTimeLeft(0);
        localStorage.removeItem(LOCAL_STORAGE_KEY);
      } else {
        setTimeLeft(remaining);
      }
    }, 1000);
  };

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
      <span className="resend-button loading">Sending...</span>
    );
  }

  if (timeLeft > 0) {
    return (
      <span className="resend-button disabled-timer">
        Resend code in {formatTime(timeLeft)}
      </span>
    );
  }

  return (
    <span
      onClick={handleTriggerClick}
      className="resend-button">
      Resend code
    </span>
  );
};
