import { useCallback, useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import type { ApiConversation, ApiMessage } from "../utils/types";
import { useWebSocketContext } from "../context/WebSocketContext";
import "./styles/Inbox.css";

const TOKEN_KEY = "jwt_token";
const USER_ID_KEY = "user_id";

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`
  };
}

function formatTime(iso: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - d.getTime()) / 86400000);
  if (diffDays === 0) return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return d.toLocaleDateString([], { weekday: "short" });
  return d.toLocaleDateString([], { month: "short", day: "numeric" });
}

interface PriceOfferDialogProps {
  onSend: (price: number) => void;
  onCancel: () => void;
}

function PriceOfferDialog({ onSend, onCancel }: PriceOfferDialogProps) {
  const [price, setPrice] = useState("");
  const [error, setError] = useState("");

  function handleSend() {
    const num = Number(price);
    if (!Number.isFinite(num) || num <= 0) {
      setError("Please enter a valid price greater than 0.");
      return;
    }
    onSend(num);
  }

  return (
    <div className="offer-dialog-backdrop">
      <div className="offer-dialog">
        <h3>Make a Price Offer</h3>
        <p className="offer-dialog-hint">
          Enter the amount you would like to offer for this service.
        </p>
        <input
          type="number"
          min="0.01"
          step="0.01"
          placeholder="e.g. 50.00"
          value={price}
          onChange={(e) => { setPrice(e.target.value); setError(""); }}
          className="offer-dialog-input"
        />
        {error && <p className="offer-dialog-error">{error}</p>}
        <div className="offer-dialog-actions">
          <button className="offer-dialog-send" onClick={handleSend}>
            Send Offer
          </button>
          <button className="offer-dialog-cancel" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

interface MessageBubbleProps {
  message: ApiMessage;
  isMine: boolean;
  conversationId: string;
  isProvider: boolean;
  onOfferResponded: (updated: ApiMessage) => void;
}

function MessageBubble({ message, isMine, conversationId, isProvider, onOfferResponded }: MessageBubbleProps) {
  const [responding, setResponding] = useState(false);

  async function respond(accept: boolean) {
    setResponding(true);
    try {
      const endpoint = accept
        ? API_ENDPOINTS.conversations.acceptOffer(conversationId, message.id)
        : API_ENDPOINTS.conversations.rejectOffer(conversationId, message.id);

      const res = await fetch(endpoint, { method: "POST", headers: authHeaders() });
      if (res.ok) {
        const data = (await res.json()) as ApiMessage;
        onOfferResponded(data);
      }
    } finally {
      setResponding(false);
    }
  }

  if (message.type === "TEXT") {
    return (
      <div className={`message-bubble ${isMine ? "mine" : "theirs"}`}>
        <span className="message-sender">{isMine ? "You" : message.senderName}</span>
        <p className="message-content">{message.content}</p>
        <span className="message-time">{formatTime(message.createdAt)}</span>
      </div>
    );
  }

  if (message.type === "PRICE_OFFER") {
    return (
      <div className={`message-bubble offer-bubble ${isMine ? "mine" : "theirs"}`}>
        <span className="message-sender">{isMine ? "You" : message.senderName}</span>
        <div className="offer-amount">${Number(message.offeredPrice).toFixed(2)}</div>
        <p className="offer-label">Price Offer</p>
        {!message.offerResponded && !isMine && isProvider && (
          <div className="offer-actions">
            <button
              className="offer-accept"
              onClick={() => void respond(true)}
              disabled={responding}>
              Accept
            </button>
            <button
              className="offer-reject"
              onClick={() => void respond(false)}
              disabled={responding}>
              Decline
            </button>
          </div>
        )}
        {message.offerResponded && (
          <p className="offer-responded-note">Responded</p>
        )}
        <span className="message-time">{formatTime(message.createdAt)}</span>
      </div>
    );
  }

  if (message.type === "PRICE_ACCEPTED") {
    return (
      <div className={`message-bubble accepted-bubble ${isMine ? "mine" : "theirs"}`}>
        <span className="message-sender">{isMine ? "You" : message.senderName}</span>
        <p className="offer-response-text">{message.content}</p>
        <div className="offer-not-final-notice">
          This is not a formal agreement. To finalize, the customer must create a
          booking and the provider must confirm via email.
        </div>
        <span className="message-time">{formatTime(message.createdAt)}</span>
      </div>
    );
  }

  if (message.type === "PRICE_REJECTED") {
    return (
      <div className={`message-bubble rejected-bubble ${isMine ? "mine" : "theirs"}`}>
        <span className="message-sender">{isMine ? "You" : message.senderName}</span>
        <p className="offer-response-text">{message.content}</p>
        <span className="message-time">{formatTime(message.createdAt)}</span>
      </div>
    );
  }

  return null;
}

export default function Inbox() {
  const location = useLocation();
  const { latestMessage, refreshUnreadCounts } = useWebSocketContext();
  const currentUserId = localStorage.getItem(USER_ID_KEY) ?? "";

  const [conversations, setConversations] = useState<ApiConversation[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(
    (location.state as { conversationId?: string } | null)?.conversationId ?? null
  );
  const [messages, setMessages] = useState<ApiMessage[]>([]);
  const [text, setText] = useState("");
  const [showOfferDialog, setShowOfferDialog] = useState(false);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const selectedConversation = conversations.find((c) => c.id === selectedId) ?? null;
  const isProvider = selectedConversation?.providerId === currentUserId;
  const isCustomer = selectedConversation?.customerId === currentUserId;

  const loadConversations = useCallback(async () => {
    try {
      const res = await fetch(API_ENDPOINTS.conversations.all, { headers: authHeaders() });
      if (res.ok) setConversations((await res.json()) as ApiConversation[]);
    } catch {
      // silently ignore
    }
  }, []);

  const loadMessages = useCallback(async (conversationId: string) => {
    try {
      const res = await fetch(API_ENDPOINTS.conversations.messages(conversationId), {
        headers: authHeaders()
      });
      if (res.ok) {
        setMessages((await res.json()) as ApiMessage[]);
        refreshUnreadCounts();
        setConversations((prev) =>
          prev.map((c) => (c.id === conversationId ? { ...c, unreadCount: 0 } : c))
        );
      }
    } catch {
      // silently ignore
    }
  }, [refreshUnreadCounts]);

  useEffect(() => {
    void loadConversations();
  }, [loadConversations]);

  useEffect(() => {
    if (selectedId) void loadMessages(selectedId);
  }, [selectedId, loadMessages]);

  useEffect(() => {
    if (!latestMessage) return;
    if (latestMessage.conversationId === selectedId) {
      setMessages((prev) => [...prev, latestMessage]);
      refreshUnreadCounts();
    } else {
      setConversations((prev) =>
        prev.map((c) =>
          c.id === latestMessage.conversationId
            ? { ...c, unreadCount: c.unreadCount + 1, lastMessagePreview: latestMessage.content ?? "" }
            : c
        )
      );
    }
  }, [latestMessage, selectedId, refreshUnreadCounts]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function sendText() {
    if (!selectedId || !text.trim()) return;
    setSending(true);
    try {
      const res = await fetch(API_ENDPOINTS.conversations.sendMessage(selectedId), {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ type: "TEXT", content: text.trim() })
      });
      if (res.ok) {
        const msg = (await res.json()) as ApiMessage;
        setMessages((prev) => [...prev, msg]);
        setText("");
      }
    } finally {
      setSending(false);
    }
  }

  async function sendOffer(price: number) {
    if (!selectedId) return;
    setShowOfferDialog(false);
    setSending(true);
    try {
      const res = await fetch(API_ENDPOINTS.conversations.sendMessage(selectedId), {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ type: "PRICE_OFFER", offeredPrice: price })
      });
      if (res.ok) {
        const msg = (await res.json()) as ApiMessage;
        setMessages((prev) => [...prev, msg]);
      }
    } finally {
      setSending(false);
    }
  }

  function handleOfferResponded(responseMsg: ApiMessage) {
    setMessages((prev) =>
      prev
        .map((m) =>
          m.type === "PRICE_OFFER" && !m.offerResponded && m.conversationId === selectedId
            ? { ...m, offerResponded: true }
            : m
        )
        .concat(responseMsg)
    );
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void sendText();
    }
  }

  return (
    <div className="inbox-container">
      <aside className="inbox-sidebar">
        <h2 className="inbox-sidebar-title">Messages</h2>
        {conversations.length === 0 && (
          <p className="inbox-empty">No conversations yet.</p>
        )}
        {conversations.map((c) => {
          const otherName = c.customerId === currentUserId ? c.providerName : c.customerName;
          return (
            <button
              key={c.id}
              className={`inbox-conversation-item ${c.id === selectedId ? "selected" : ""}`}
              onClick={() => setSelectedId(c.id)}>
              <div className="inbox-conv-header">
                <span className="inbox-conv-name">{otherName}</span>
                <span className="inbox-conv-time">{formatTime(c.lastMessageAt)}</span>
              </div>
              <div className="inbox-conv-sub">
                <span className="inbox-conv-service">{c.serviceTitle}</span>
                {c.unreadCount > 0 && (
                  <span className="inbox-unread-badge">{c.unreadCount}</span>
                )}
              </div>
              {c.lastMessagePreview && (
                <p className="inbox-conv-preview">{c.lastMessagePreview}</p>
              )}
            </button>
          );
        })}
      </aside>

      <main className="inbox-chat">
        {!selectedConversation && (
          <div className="inbox-no-selection">
            <p>Select a conversation to start chatting.</p>
          </div>
        )}

        {selectedConversation && (
          <>
            <header className="inbox-chat-header">
              <div>
                <p className="inbox-chat-service">{selectedConversation.serviceTitle}</p>
                <p className="inbox-chat-other">
                  {selectedConversation.customerId === currentUserId
                    ? selectedConversation.providerName
                    : selectedConversation.customerName}
                </p>
              </div>
            </header>

            <div className="inbox-messages">
              {messages.map((m) => (
                <MessageBubble
                  key={m.id}
                  message={m}
                  isMine={m.senderId === currentUserId}
                  conversationId={selectedConversation.id}
                  isProvider={isProvider}
                  onOfferResponded={handleOfferResponded}
                />
              ))}
              <div ref={messagesEndRef} />
            </div>

            <footer className="inbox-input-area">
              <textarea
                className="inbox-textarea"
                placeholder="Type a message..."
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={handleKeyDown}
                rows={2}
              />
              <div className="inbox-input-actions">
                {isCustomer && (
                  <button
                    className="inbox-offer-btn"
                    onClick={() => setShowOfferDialog(true)}
                    disabled={sending}>
                    Make Offer
                  </button>
                )}
                <button
                  className="inbox-send-btn"
                  onClick={() => void sendText()}
                  disabled={sending || !text.trim()}>
                  Send
                </button>
              </div>
            </footer>
          </>
        )}
      </main>

      {showOfferDialog && (
        <PriceOfferDialog
          onSend={(p) => void sendOffer(p)}
          onCancel={() => setShowOfferDialog(false)}
        />
      )}
    </div>
  );
}
