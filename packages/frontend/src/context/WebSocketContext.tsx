import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import type {
  ApiMessage,
  ApiNotification,
  ApiUnreadCounts
} from "../utils/types";
import { API_ENDPOINTS } from "../utils/api";

function buildWsUrl(): string {
  const apiBase = import.meta.env.VITE_API_BASE_ROUTE as
    | string
    | undefined;
  const httpBase =
    apiBase?.replace("/api", "") ?? "http://localhost:8080";
  return httpBase.replace(/^http/, "ws") + "/ws";
}

interface WebSocketContextValue {
  unreadCounts: ApiUnreadCounts;
  latestMessage: ApiMessage | null;
  latestNotification: ApiNotification | null;
  refreshUnreadCounts: () => void;
}

const WebSocketContext = createContext<WebSocketContextValue>({
  unreadCounts: { messages: 0, notifications: 0 },
  latestMessage: null,
  latestNotification: null,
  refreshUnreadCounts: () => {}
});

// eslint-disable-next-line react-refresh/only-export-components
export function useWebSocketContext() {
  return useContext(WebSocketContext);
}

export function WebSocketProvider({
  children
}: {
  children: React.ReactNode;
}) {
  const [unreadCounts, setUnreadCounts] =
    useState<ApiUnreadCounts>({
      messages: 0,
      notifications: 0
    });
  const [latestMessage, setLatestMessage] =
    useState<ApiMessage | null>(null);
  const [latestNotification, setLatestNotification] =
    useState<ApiNotification | null>(null);
  const clientRef = useRef<Client | null>(null);

  const fetchUnreadCounts = useCallback(async () => {
    const token = localStorage.getItem("jwt_token");
    if (!token) return;
    try {
      const res = await fetch(API_ENDPOINTS.unreadCounts, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (res.ok) {
        const data = (await res.json()) as ApiUnreadCounts;
        setUnreadCounts(data);
      }
    } catch {
      // silently ignore network errors
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("jwt_token");
    if (!token) return;

    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchUnreadCounts();

    const client = new Client({
      brokerURL: buildWsUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(
          "/user/queue/messages",
          (frame: IMessage) => {
            const msg = JSON.parse(frame.body) as ApiMessage;
            setLatestMessage(msg);
            setUnreadCounts((prev) => ({
              ...prev,
              messages: prev.messages + 1
            }));
          }
        );

        client.subscribe(
          "/user/queue/notifications",
          (frame: IMessage) => {
            const notif = JSON.parse(
              frame.body
            ) as ApiNotification;
            setLatestNotification(notif);
            setUnreadCounts((prev) => ({
              ...prev,
              notifications: prev.notifications + 1
            }));
          }
        );
      }
    });

    client.activate();
    clientRef.current = client;

    return () => {
      void client.deactivate();
    };
  }, [fetchUnreadCounts]);

  return (
    <WebSocketContext.Provider
      value={{
        unreadCounts,
        latestMessage,
        latestNotification,
        refreshUnreadCounts: fetchUnreadCounts
      }}>
      {children}
    </WebSocketContext.Provider>
  );
}
