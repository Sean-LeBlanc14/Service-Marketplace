import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import type {
  ApiNotification,
  NotificationType
} from "../utils/types";
import { useWebSocketContext } from "../context/WebSocketContext";
import "./styles/NotificationsPage.css";

const TOKEN_KEY = "jwt_token";

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`
  };
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  if (diffMins < 1) return "just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  return d.toLocaleDateString([], {
    month: "short",
    day: "numeric"
  });
}

function notifIcon(type: NotificationType): string {
  switch (type) {
    case "NEW_MESSAGE":
      return "💬";
    case "PRICE_OFFER_RECEIVED":
      return "💰";
    case "PRICE_OFFER_ACCEPTED":
      return "✅";
    case "PRICE_OFFER_REJECTED":
      return "❌";
    case "BOOKING_REQUESTED":
      return "📋";
    case "BOOKING_CONFIRMED":
      return "🎉";
    case "BOOKING_CANCELLED":
      return "🚫";
    case "REVIEW_RECEIVED":
      return "⭐";
    default:
      return "🔔";
  }
}

export default function NotificationsPage() {
  const navigate = useNavigate();
  const { refreshUnreadCounts, latestNotification } =
    useWebSocketContext();
  const [notifications, setNotifications] = useState<
    ApiNotification[]
  >([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      const res = await fetch(API_ENDPOINTS.notifications.all, {
        headers: authHeaders()
      });
      if (res.ok)
        setNotifications(
          (await res.json()) as ApiNotification[]
        );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (latestNotification) {
      setNotifications((prev) => [latestNotification, ...prev]);
    }
  }, [latestNotification]);

  async function markRead(id: string) {
    await fetch(API_ENDPOINTS.notifications.markRead(id), {
      method: "PATCH",
      headers: authHeaders()
    });
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    refreshUnreadCounts();
  }

  async function markAllRead() {
    await fetch(API_ENDPOINTS.notifications.markAllRead, {
      method: "PATCH",
      headers: authHeaders()
    });
    setNotifications((prev) =>
      prev.map((n) => ({ ...n, read: true }))
    );
    refreshUnreadCounts();
  }

  function handleClick(notif: ApiNotification) {
    if (!notif.read) void markRead(notif.id);

    if (
      notif.type === "NEW_MESSAGE" ||
      notif.type === "PRICE_OFFER_RECEIVED" ||
      notif.type === "PRICE_OFFER_ACCEPTED" ||
      notif.type === "PRICE_OFFER_REJECTED"
    ) {
      navigate("/inbox", {
        state: { conversationId: notif.referenceId }
      });
    }
  }

  const unreadCount = notifications.filter(
    (n) => !n.read
  ).length;

  return (
    <div className="notif-page">
      <div className="notif-header">
        <h1 className="notif-title">Notifications</h1>
        {unreadCount > 0 && (
          <button
            className="notif-mark-all"
            onClick={() => void markAllRead()}>
            Mark all as read
          </button>
        )}
      </div>

      {loading && <p className="notif-loading">Loading...</p>}

      {!loading && notifications.length === 0 && (
        <p className="notif-empty">No notifications yet.</p>
      )}

      <div className="notif-list">
        {notifications.map((n) => (
          <button
            key={n.id}
            className={`notif-item ${n.read ? "read" : "unread"}`}
            onClick={() => handleClick(n)}>
            <span className="notif-icon" aria-hidden="true">
              {notifIcon(n.type)}
            </span>
            <div className="notif-body">
              <p className="notif-item-title">{n.title}</p>
              <p className="notif-item-body">{n.body}</p>
              <span className="notif-time">
                {formatTime(n.createdAt)}
              </span>
            </div>
            {!n.read && (
              <span className="notif-dot" aria-label="unread" />
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
