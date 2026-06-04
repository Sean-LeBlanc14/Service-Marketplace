import { toast } from "react-toastify";
import "../styles/Settings.css";
import { getToken } from "../utils/helper";
import { useNavigate } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import { useState } from "react";
import SubmitButton from "../components/SubmitButton";
import InputField from "../components/InputField";
import {
  FaBug,
  FaSignOutAlt,
  FaTrash,
  FaLock,
  FaSchool,
  FaBook
} from "react-icons/fa";
import { BsBoxArrowUpRight } from "react-icons/bs";
import Modal from "../components/Modal";
import Footer from "../components/Footer";
import MajorComboBox from "../components/MajorComboBox";
import DropDown from "../components/DropDown";
import { USER_ID_KEY } from "./profile/constants";

export default function Settings() {
  const navigate = useNavigate();

  // Variables to handle password changes
  const [isChangingPassword, setIsChangingPassword] =
    useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] =
    useState("");

  //Variables to handle account deletion + reUse currentPassword for confirmation
  const [isDeletingAccount, setIsDeletingAccount] =
    useState(false);
  const [userEmail, setUserEmail] = useState("");
  const [accountDeletePassword, setAccountDeletePassword] =
    useState("");

  const [isLoggingOut, setIsLoggingOut] = useState(false);

  //Variables for reporting bugs
  const [isReportingBug, setIsReportingBug] = useState(false);
  const [bug, setBug] = useState("");

  //Variables for contacting support
  const [isContactingSupport, setIsContactingSupport] =
    useState(false);
  const [contactInquiry, setContactInquiry] = useState("");

  //Variable for changing major / campus
  const [isChangingMajor, setIsChangingMajor] = useState(false);
  const [major, setMajor] = useState("");

  const [isChangingCampus, setIsChangingCampus] =
    useState(false);
  const [campus, setCampus] = useState("");

  const handleLogout = async () => {
    const authToken = getToken();

    if (!authToken) {
      toast.error("Please login");
      navigate("/login");
      return;
    }

    try {
      const response = await fetch(API_ENDPOINTS.auth.logout, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      });

      if (response.ok) {
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_role");
        localStorage.removeItem(USER_ID_KEY);
        toast.success("Successfully logged out");
        navigate("/login");
      } else if (response.status === 401) {
        // Backend sends a 401 if the user is not logged in to begin with
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_role");
        localStorage.removeItem(USER_ID_KEY);
        navigate("/login");
      } else {
        toast.error("Could not logout, please try again.");
      }
    } catch (e) {
      console.error("Logout error: ", e);
      toast.warning("A network error occurred.");
    }
  };

  async function handleDeleteAccount() {
    const authToken = getToken();

    if (!authToken) {
      toast.error("Please login");
      navigate("/login");
      return;
    }

    const deleteAccountrequest = {
      email: userEmail,
      password: accountDeletePassword
    };

    try {
      const response = await fetch(API_ENDPOINTS.user.delete, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(deleteAccountrequest)
      });

      if (response.ok) {
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_role");
        localStorage.removeItem(USER_ID_KEY);
        toast.success("Account successfully deleted");
        navigate("/");
      } else if (response.status === 401) {
        toast.error("Invalid credentials");
      } else {
        toast.error("Something went wrong");
      }
    } catch (e) {
      toast.warning("A network error occurred");
      console.error(e);
    }
  }

  async function handleReportBug() {
    const authToken = getToken();

    try {
      const response = await fetch(API_ENDPOINTS.support.bug, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ context: bug })
      });

      if (response.ok) {
        const data = await response.json();

        toast.success(data.message);
        setIsReportingBug(false);
      } else {
        toast.error("Could not report this bug");
      }
    } catch {
      toast.warning("A network error occurred");
    }
  }

  async function handleContactSupport() {
    const authToken = getToken();

    try {
      const response = await fetch(
        API_ENDPOINTS.support.contact,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ context: contactInquiry })
        }
      );

      if (response.ok) {
        const data = await response.json();

        toast.success(data.message);
        setIsContactingSupport(false);
      } else {
        toast.error("Could not contact support");
      }
    } catch {
      toast.warning("A network error occurred");
    }
  }

  async function handleChangePassword() {
    const authToken = getToken();

    if (!authToken) {
      toast.error("Please login");
      navigate("/login");
      return;
    }

    if (newPassword !== confirmNewPassword) {
      toast.error("Passwords do not match!");
      return;
    } else if (newPassword.length < 8) {
      toast.error(
        "Your password must be at least 8 characters"
      );
      return;
    }

    const newPasswordRequest = {
      password: currentPassword,
      newPassword: newPassword
    };

    try {
      const response = await fetch(
        API_ENDPOINTS.user.changePassword,
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify(newPasswordRequest)
        }
      );

      if (response.ok) {
        toast.success("Password changed successfully");
        setIsChangingPassword(false);
      } else if (response.status === 401) {
        toast.error("Invalid password");
      } else if (response.status === 409) {
        toast.error(
          "Your old password and new password cannot match"
        );
      }
    } catch {
      toast.warning(
        "A network error occurred, please try again."
      );
    }
  }

  async function handleChangeMajor() {
    const authToken = getToken();

    if (!authToken) {
      toast.error("Please login");
      navigate("/login");
      return;
    }

    try {
      const response = await fetch(
        API_ENDPOINTS.user.changeMajor,
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ update: major })
        }
      );

      if (response.ok) {
        toast.success("Successfully changed your major");
        setIsChangingMajor(false);
      } else if (response.status === 409) {
        toast.error(
          "Your old major and new major cannot match"
        );
      } else {
        toast.error("Could not change your major");
      }
    } catch {
      toast.warning("A network error occurred");
    }
  }

  async function handleChangeCampus() {
    const authToken = getToken();

    if (!authToken) {
      toast.error("Please login");
      navigate("/login");
      return;
    }

    try {
      const response = await fetch(
        API_ENDPOINTS.user.changeCampus,
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ update: campus })
        }
      );

      if (response.ok) {
        toast.success("Successfully changed your campus");
        setIsChangingCampus(false);
      } else if (response.status === 409) {
        toast.error(
          "Your old campus and new campus cannot match"
        );
      } else {
        toast.error("Could not change your campus");
      }
    } catch {
      toast.warning("A network error occurred");
    }
  }

  function deleteModal() {
    return (
      <div className="modal-style">
        <h1 className="modal-title">Delete your account</h1>
        <InputField
          label=""
          value={userEmail}
          placeHolder="Account email"
          type="text"
          onChange={(e) => setUserEmail(e.target.value)}
        />
        <InputField
          label=""
          placeHolder="Password"
          value={accountDeletePassword}
          onChange={(e) =>
            setAccountDeletePassword(e.target.value)
          }
          type="password"
        />
        <SubmitButton
          label="Delete Account"
          onClick={handleDeleteAccount}
        />
      </div>
    );
  }

  function changePasswordModal() {
    return (
      <div className="modal-style">
        <h1 className="modal-title">Change your password</h1>
        <InputField
          label=""
          value={currentPassword}
          placeHolder="Current Password"
          onChange={(e) => setCurrentPassword(e.target.value)}
          type="password"
        />
        <InputField
          label=""
          placeHolder="New Password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          type="password"
        />
        <InputField
          label=""
          placeHolder="Confirm New Password"
          value={confirmNewPassword}
          onChange={(e) =>
            setConfirmNewPassword(e.target.value)
          }
          type="password"
        />
        <SubmitButton
          label="Change Password"
          onClick={handleChangePassword}
        />
      </div>
    );
  }

  function logoutModal() {
    return (
      <>
        <h4>Are you sure you want to logout?</h4>
        <SubmitButton label="Confirm" onClick={handleLogout} />
      </>
    );
  }

  function reportModal() {
    return (
      <div className="modal-style">
        <h4 className="modal-title">Report a Bug</h4>
        <textarea
          placeholder="Explain the bug..."
          value={bug}
          onChange={(e) => setBug(e.target.value)}
          className="modal-input"
        />
        <SubmitButton
          onClick={handleReportBug}
          label="Submit Bug Report"
        />
      </div>
    );
  }

  function contactSupportModal() {
    return (
      <div className="modal-style">
        <h4 className="modal-title">Contact Support</h4>
        <textarea
          placeholder="Explain your inquiry..."
          value={contactInquiry}
          onChange={(e) => setContactInquiry(e.target.value)}
          className="modal-input"
        />
        <SubmitButton
          label="Contact Support"
          onClick={handleContactSupport}
        />
      </div>
    );
  }

  function changeMajorModal() {
    return (
      <div className="modal-style">
        <h4 className="modal-title">Change your major</h4>
        <MajorComboBox value={major} onChange={setMajor} />
        <SubmitButton
          label="Change Major"
          onClick={handleChangeMajor}
        />
      </div>
    );
  }

  function changeCampusModal() {
    return (
      <div className="modal-style">
        <h4>Change your campus</h4>
        <DropDown
          label="Campus"
          value={campus}
          placeHolder={"Select a Campus"}
          options={
            <>
              <option value="San Luis Obispo">
                San Luis Obispo
              </option>

              <option value="Maritime Academy">
                Maritime Academy
              </option>
            </>
          }
          onChange={(e) => setCampus(e.target.value)}
        />
        <SubmitButton
          label="Change Campus"
          onClick={handleChangeCampus}
        />
      </div>
    );
  }

  return (
    <div className="settings-wrapper">
      <div className="settings-title">
        <h1>Settings</h1>
        <h4>Manage your account and preferences</h4>
      </div>

      <Modal
        isOpen={isDeletingAccount}
        onClose={() => setIsDeletingAccount(false)}
        children={deleteModal()}
      />

      <Modal
        isOpen={isChangingPassword}
        onClose={() => setIsChangingPassword(false)}
        children={changePasswordModal()}
      />

      <Modal
        isOpen={isLoggingOut}
        onClose={() => setIsLoggingOut(false)}
        children={logoutModal()}
      />

      <Modal
        isOpen={isReportingBug}
        onClose={() => setIsReportingBug(false)}
        children={reportModal()}
      />

      <Modal
        isOpen={isContactingSupport}
        onClose={() => setIsContactingSupport(false)}
        children={contactSupportModal()}
      />

      <Modal
        isOpen={isChangingMajor}
        onClose={() => setIsChangingMajor(false)}
        children={changeMajorModal()}
      />

      <Modal
        isOpen={isChangingCampus}
        onClose={() => setIsChangingCampus(false)}
        children={changeCampusModal()}
      />

      <div className="setting-section">
        <h3>Account Settings</h3>
        <div className="setting-container">
          <button onClick={() => setIsChangingPassword(true)}>
            <span>Change Password</span>
            <FaLock />
          </button>

          <button onClick={() => setIsChangingMajor(true)}>
            <span>Change Major</span>
            <FaBook />
          </button>

          <button onClick={() => setIsChangingCampus(true)}>
            <span>Change Campus</span>
            <FaSchool />
          </button>
        </div>
      </div>

      <div className="setting-section">
        <h3>Support</h3>
        <div className="setting-container">
          <button onClick={() => setIsReportingBug(true)}>
            <span>
              Report a Bug <FaBug color="orange" />{" "}
            </span>
            <BsBoxArrowUpRight />
          </button>

          <button onClick={() => setIsContactingSupport(true)}>
            <span>Contact Support</span>
            <BsBoxArrowUpRight />
          </button>

          <button>
            <span>Terms of Service</span>
            <BsBoxArrowUpRight />
          </button>

          <button>
            <span>Privacy Policy</span>
            <BsBoxArrowUpRight />
          </button>
        </div>
      </div>

      <div className="setting-section">
        <h3>Danger Zone</h3>
        <div className="setting-container">
          <button
            className="delete-account-button"
            onClick={() => setIsDeletingAccount(true)}>
            <span>Delete Account</span>
            <FaTrash />
          </button>

          <button
            className="logout-button"
            onClick={() => setIsLoggingOut(true)}>
            <span>Logout</span>
            <FaSignOutAlt />
          </button>
        </div>
      </div>

      <Footer />
    </div>
  );
}
