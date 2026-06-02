import { toast } from "react-toastify";
import "../styles/Settings.css";
import { getToken } from "../utils/helper";
import { useNavigate } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import { useState } from "react";
import SubmitButton from "../components/SubmitButton";
import InputField from "../components/InputField";
import { FaBug, FaSignOutAlt, FaTrash, FaLock, FaSchool, FaBook } from "react-icons/fa";
import { BsBoxArrowUpRight } from "react-icons/bs";
import Modal from "../components/Modal";
import Footer from "../components/Footer";

export default function Settings() {

  const navigate = useNavigate();


  // Variables to handle password changes
  const [ isChangingPassword, setIsChangingPassword ] = useState(false);
  const [ currentPassword, setCurrentPassword ] = useState("");
  const [ newPassword, setNewPassword ] = useState("");
  const [ confirmNewPassword, setConfirmNewPassowrd ] = useState("");

  //Variables to handle account deletion + reUse currentPassword for confirmation
  const [ isDeletingAccount, setIsDeletingAccount ] = useState(false);
  const[ userEmail, setUserEmail ] = useState("");

  const [ isLoggingOut, setIsLoggingOut ] = useState(false);

  //Variables for reporting bugs
  const [ isReportingBug, setIsReportingBug ] = useState(false);
  const [ bug, setBug ] = useState("");

  //Variables for contacting support
  const [ isContactingSupport, setIsContactingSupport ] = useState(false);
  const [ contactInquiry, setContactInquiry ] = useState("");

  

  const handleLogout = async () => {
  
      try {
        const authToken = getToken();
        const response = await fetch(API_ENDPOINTS.auth.logout, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${authToken}`
          }
        });
  
        if (response.ok) {
          localStorage.removeItem("jwt_token");
          localStorage.removeItem("user_role");
          localStorage.removeItem("user_id");
          toast.success("Successfully logged out");
          navigate("/login");
        } else if (response.status === 401) {
          // Backend sends a 401 if the user is not logged in to begin with
          localStorage.removeItem("jwt_token");
          localStorage.removeItem("user_role");
          localStorage.removeItem("user_id");
          navigate("/login");
        } else {
          toast.error("Could not logout, please try again.");
        }
      } catch (e) {
        console.error("Logout error: ", e);
        toast.warning("A network error occurred.");
      }
    };


  async function handleDeleteAccount(){

    const authToken = getToken();

    if (!authToken){
      toast.error("Please login");
      navigate("/login");
    }

    try{
      
      const response = await fetch(API_ENDPOINTS.user.delete, {
        headers: {Authorization: `Bearer ${authToken}`,}});

      if (response.ok){
        toast.success("Account successfully deleted");
        navigate("/");
      }

    }catch(e){
      toast.warning("A network error occurred");
      console.error(e);
    }
  }

  async function handleReportBug(){

  }

  async function handleContactSupport(){

  }

  async function handleChangePassword(){

    const authToken = getToken();

    if (newPassword !== confirmNewPassword){
      toast.error("Passwords do not match!");
      return;
    }

    const newPasswordRequest = {
      password: currentPassword,
      newPassword: newPassword
    };

    try{
      const response = await fetch(API_ENDPOINTS.user.changePassword, {
        method: "PATCH",
        headers: {Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json"},
        body: JSON.stringify(newPasswordRequest)
      });

      if (response.ok){
        toast.success("Password changed successfully");
      }
      else{
        toast.error("Could not change your password.");
      }

    }catch{
      toast.warning("A network error occurred, please try again.");
    }
    
  }

  function deleteModal(){
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
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
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
            onChange={(e) => setConfirmNewPassowrd(e.target.value)}
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
        <SubmitButton
          label="Confirm"
          onClick={handleLogout}
        />
      </>
    )
  }


  function reportModal(){
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

  function contactSupportModal(){
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


      <div className="setting-section">
        <h3>Account Settings</h3>
      <div className="setting-container">
        
        <button className="change-password-button" onClick={() => setIsChangingPassword(true)}>
          <span>Change Password</span>
          <FaLock/>
        </button>

        <button>
          <span>Change Major</span>
          <FaBook/>
        </button>

        <button>
          <span>Change Campus</span>
          <FaSchool/>
        </button>

      </div>
      </div>
      
      <div className="setting-section">
        <h3>Support</h3>
      <div className="setting-container">
        <button onClick={() => setIsReportingBug(true)}>
          <span>Report a Bug <FaBug color="orange"/> </span>
          <BsBoxArrowUpRight/>
        </button>

        <button onClick={() => setIsContactingSupport(true)}>
          <span>Contact Support</span>
          <BsBoxArrowUpRight/>
        </button>

        <button>
          <span>Terms of Service</span>
          <BsBoxArrowUpRight/>
        </button>

        <button>
          <span>Privacy Policy</span>
          <BsBoxArrowUpRight/>
        </button>

      </div>
      </div>

      <div className="setting-section">

        <h3>Danger Zone</h3>
        <div className="setting-container">
          
           <button className="delete-account-button" onClick={() => setIsDeletingAccount(true)}>
          <span>Delete Account</span>
          <FaTrash/>
        </button>

        <button className="logout-button" onClick={() => setIsLoggingOut(true)}>
          <span>Logout</span>
          <FaSignOutAlt/>
        </button>

        </div>

      </div>

      <Footer/>

      </div>
  );
}
