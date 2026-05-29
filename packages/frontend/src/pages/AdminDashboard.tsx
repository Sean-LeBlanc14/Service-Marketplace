import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { toast } from "react-toastify";
import { API_ENDPOINTS } from "../utils/api";
import "../components/styles/AdminDashboard.css";

const TOKEN_STORAGE_KEY = "jwt_token";

interface Report {
  id: string;
  reporterId: string;
  listingId: string;
  providerId: string;
  reason: string;
  status: string;
  createdAt?: string;
}

interface AdminUser {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: string;
}

type ReportAction = "remove" | "suspend";

function formatDate(value?: string) {
  if (!value) {
    return "Unknown";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

function getUserName(user: AdminUser) {
  const name = `${user.firstName ?? ""} ${user.lastName ?? ""}`.trim();
  return name || "Unknown";
}

function getAuthHeaders() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  return {
    Authorization: `Bearer ${token}`
  };
}

function AdminDashboard() {
  const authToken = localStorage.getItem(TOKEN_STORAGE_KEY);
  const [reports, setReports] = useState<Report[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [reportUsers, setReportUsers] = useState<Record<string, AdminUser>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [accessDenied, setAccessDenied] = useState(false);
  const [showResolved, setShowResolved] = useState(false);
  const [activeReportAction, setActiveReportAction] = useState<{
    reportId: string;
    action: ReportAction;
  } | null>(null);
  const [activeUserAction, setActiveUserAction] = useState<string | null>(null);
  const [reloadVersion, setReloadVersion] = useState(0);
  const currentUserId = localStorage.getItem("user_id");
  const suspendedUsers = users.filter((user) => user.role === "suspended");
  const visibleReports = showResolved
    ? reports
    : reports.filter((report) => report.status !== "resolved");

  useEffect(() => {
    if (authToken === null) {
      return;
    }

    let cancelled = false;

    async function load() {
      setIsLoading(true);

      try {
        const headers = getAuthHeaders();
        const reportsResponse = await fetch(API_ENDPOINTS.reports.all, {
          headers
        });

        if (reportsResponse.status === 403) {
          if (!cancelled) {
            setAccessDenied(true);
            setReports([]);
            setUsers([]);
            setReportUsers({});
          }
          return;
        }

        if (!reportsResponse.ok) {
          throw new Error("Failed to load reports");
        }

        const reportsData = (await reportsResponse.json()) as Report[];
        const nextReports = Array.isArray(reportsData) ? reportsData : [];
        const reportUserIds = Array.from(
          new Set(
            nextReports
              .flatMap((report) => [report.reporterId, report.providerId])
              .filter(Boolean)
          )
        );
        const reportUserEntries = await Promise.all(
          reportUserIds.map(async (userId) => {
            const userResponse = await fetch(API_ENDPOINTS.users.getById(userId), {
              headers
            });

            if (!userResponse.ok) {
              return null;
            }

            const user = (await userResponse.json()) as AdminUser;
            return [userId, user] as const;
          })
        );
        const nextReportUsers: Record<string, AdminUser> = {};
        reportUserEntries.forEach((entry) => {
          if (entry) {
            const [userId, user] = entry;
            nextReportUsers[userId] = user;
          }
        });

        const usersResponse = await fetch(API_ENDPOINTS.users.all, {
          headers
        });

        if (usersResponse.status === 403) {
          if (!cancelled) {
            setAccessDenied(true);
            setReports([]);
            setUsers([]);
            setReportUsers({});
          }
          return;
        }

        if (!usersResponse.ok) {
          throw new Error("Failed to load users");
        }

        const usersData = (await usersResponse.json()) as AdminUser[];

        if (!cancelled) {
          setReports(nextReports);
          setUsers(Array.isArray(usersData) ? usersData : []);
          setReportUsers(nextReportUsers);
          setAccessDenied(false);
        }
      } catch {
        if (!cancelled) {
          toast.error("Failed to load admin dashboard");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [authToken, reloadVersion]);

  async function resolveReport(reportId: string) {
    const response = await fetch(API_ENDPOINTS.reports.resolve(reportId), {
      method: "PUT",
      headers: getAuthHeaders()
    });

    if (!response.ok) {
      throw new Error("Failed to resolve report");
    }
  }

  async function removeListing(report: Report) {
    setActiveReportAction({ reportId: report.id, action: "remove" });

    try {
      const response = await fetch(API_ENDPOINTS.services.delete(report.listingId), {
        method: "DELETE",
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error("Failed to remove listing");
      }

      await resolveReport(report.id);
      toast.success("Listing removed");
      setReloadVersion((version) => version + 1);
    } catch {
      toast.error("Failed to remove listing");
    } finally {
      setActiveReportAction(null);
    }
  }

  async function suspendUser(report: Report) {
    setActiveReportAction({ reportId: report.id, action: "suspend" });

    try {
      const response = await fetch(API_ENDPOINTS.users.suspend(report.providerId), {
        method: "PUT",
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error("Failed to suspend user");
      }

      await resolveReport(report.id);
      toast.success("User suspended");
      setReloadVersion((version) => version + 1);
    } catch {
      toast.error("Failed to suspend user");
    } finally {
      setActiveReportAction(null);
    }
  }

  async function unsuspendUser(user: AdminUser) {
    setActiveUserAction(user.id);

    try {
      const response = await fetch(API_ENDPOINTS.users.unsuspend(user.id), {
        method: "PUT",
        headers: getAuthHeaders()
      });

      if (!response.ok) {
        throw new Error("Failed to unsuspend user");
      }

      toast.success("User unsuspended");
      setReloadVersion((version) => version + 1);
    } catch {
      toast.error("Failed to unsuspend user");
    } finally {
      setActiveUserAction(null);
    }
  }

  function getReportUserName(userId: string) {
    const user = reportUsers[userId];

    if (!user) {
      return userId;
    }

    return getUserName(user);
  }

  if (authToken === null) {
    return <Navigate to="/login" replace />;
  }

  if (accessDenied) {
    return (
      <main className="admin-dashboard">
        <p className="admin-dashboard-message">Access denied &mdash; admins only</p>
      </main>
    );
  }

  return (
    <main className="admin-dashboard">
      <div className="admin-dashboard-header">
        <h1>Admin Dashboard</h1>
        <div className="admin-dashboard-header-actions">
          <p>
            {visibleReports.length} {showResolved ? "reports" : "open reports"}
          </p>
          <button
            type="button"
            className="admin-dashboard-toggle"
            onClick={() => setShowResolved((value) => !value)}>
            {showResolved ? "Hide Resolved" : "Show Resolved"}
          </button>
        </div>
      </div>

      {isLoading ? (
        <p className="admin-dashboard-message">Loading reports...</p>
      ) : (
        <>
          <div className="admin-dashboard-table-wrap">
            <table className="admin-dashboard-table">
              <thead>
                <tr>
                  <th>Reported By</th>
                  <th>Listing ID</th>
                  <th>Provider ID</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleReports.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="admin-dashboard-empty">
                      {showResolved ? "No reports found." : "No open reports found."}
                    </td>
                  </tr>
                ) : (
                  visibleReports.map((report) => {
                    const isSelfSuspension = report.providerId === currentUserId;

                    return (
                      <tr key={report.id}>
                        <td>{getReportUserName(report.reporterId)}</td>
                        <td>{report.listingId}</td>
                        <td>{getReportUserName(report.providerId)}</td>
                        <td>{report.reason}</td>
                        <td>{report.status}</td>
                        <td>{formatDate(report.createdAt)}</td>
                        <td>
                          <div className="admin-dashboard-actions">
                            <button
                              type="button"
                              className="admin-dashboard-action admin-dashboard-action-danger"
                              disabled={
                                activeReportAction?.reportId === report.id &&
                                activeReportAction.action === "remove"
                              }
                              onClick={() => void removeListing(report)}>
                              Remove Listing
                            </button>
                            <button
                              type="button"
                              className="admin-dashboard-action admin-dashboard-action-warning"
                              disabled={isSelfSuspension}
                              onClick={() => void suspendUser(report)}>
                              Suspend User
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          <section className="admin-dashboard-section">
            <div className="admin-dashboard-section-header">
              <h2>Suspended Users</h2>
              <p>{suspendedUsers.length} users</p>
            </div>

            <div className="admin-dashboard-table-wrap">
              <table className="admin-dashboard-table admin-dashboard-users-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {suspendedUsers.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="admin-dashboard-empty">
                        No suspended users.
                      </td>
                    </tr>
                  ) : (
                    suspendedUsers.map((user) => (
                      <tr key={user.id}>
                        <td>{getUserName(user)}</td>
                        <td>{user.email}</td>
                        <td>
                          <button
                            type="button"
                            className="admin-dashboard-action admin-dashboard-action-success"
                            disabled={activeUserAction === user.id}
                            onClick={() => void unsuspendUser(user)}>
                            Unsuspend
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </main>
  );
}

export default AdminDashboard;
