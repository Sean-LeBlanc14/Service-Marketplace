import { toast } from "react-toastify";
import "../styles/Settings.css";
import { getToken } from "../utils/helper";
import { useNavigate } from "react-router-dom";
import { API_ENDPOINTS } from "../utils/api";
import { useState } from "react";
import SubmitButton from "../components/SubmitButton";
import InputField from "../components/InputField";
import { FaBug } from "react-icons/fa";
import { BsBoxArrowUpRight } from "react-icons/bs";
import Modal from "../components/Modal";
import DropDown from "../components/DropDown";

export default function Settings() {

  const navigate = useNavigate();

  const [ userEmail, setUserEmail ] = useState("");

  const [ currentPassword, setCurrentPassword] = useState("");

  const [ newPassword, setNewPassword ] = useState("");

  const [ confirmNewPassword, setConfirmNewPassowrd ] = useState("");

  const [ isDeleting, setIsDeleting ] = useState(false);

  const [ isChangingPassword, setIsChangingPassword ] = useState(false);

  const [ reportingBug, setReportingBug ] = useState(false);

  const [ customerService, setCustomerService ] = useState(false);



  async function handleDeleteAccount(){

    const authToken = getToken();

    if (!authToken){
      toast.error("Please login");
      navigate("/login");
    }

    try{
      
      const response = await fetch(API_ENDPOINTS.user.delete, {
        headers: {"Authentication" : `Bearer: ${authToken}`}
      });

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

    if (newPassword !== confirmNewPassword){
      toast.error("Passwords do not match!");
      return;
    }

    try{
      const response = await fetch();

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

  function reportModal(){
    return (
      <div>
        
        <textarea></textarea>
      </div>
    );
  }

  return (
    <div className="settings-wrapper">
      <h1>Settings</h1>
      <h3>Manage your account and preferences</h3>

      <Modal
        isOpen={isDeleting}
        onClose={() => setIsDeleting(false)}
        children={deleteModal()}
      />

      <Modal
        isOpen={isChangingPassword}
        onClose={() => setIsChangingPassword(false)}
        children={changePasswordModal()}
      />

      <Modal
        isOpen={reportingBug}
        onClose={() => setReportingBug(false)}
        children={reportModal()}
      />

      <div className="setting-container">
        <h4 className="section-title">Account Settings</h4>
      
      <div className="account-settings-wrapper">

        <div className="change-password-container">
          <button className="change-password-button" onClick={() => setIsChangingPassword(true)}>Change Password</button>
        </div>

        <div className="delete-account-container">
          <button className="delete-account-button" onClick={() => setIsDeleting(true)}>Delete Account</button>
        </div>
       
      </div>

      </div>

      <div className="setting-container">
        <h4 className="section-title">Support</h4>
      
      <div className="support-container">
        <div>
          <button className="support-button" onClick={() => setReportingBug(true)}>
          <span>Report a Bug <FaBug color="orange"/> </span> 
          <BsBoxArrowUpRight className="arrow"/>
        </button>
        </div>

        <div>
          <button className="support-button">
          <span>Contact Support</span> 
          <BsBoxArrowUpRight/>
        </button>
        </div>

        <div>
          <button className="support-button">
          <span>Terms of Service</span> 
          <BsBoxArrowUpRight/>
        </button>
        </div>

        <div className="bottom">
          <button className="support-button">
          <span>Privacy Policy</span> 
          <BsBoxArrowUpRight/>
        </button>
        </div>
          
      </div>
      </div>

    </div>
  );
}
