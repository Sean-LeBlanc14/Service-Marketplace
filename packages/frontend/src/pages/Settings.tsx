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

export default function Settings() {

  const navigate = useNavigate();

  const isDarkMode = useState(false);

  const [ userEmail, setUserEmail ] = useState("");

  const [ currentPassword, setCurrentPassword] = useState("");

  const [ newPassword, setNewPassword ] = useState("");

  const [ confirmNewPassword, setConfirmNewPassowrd ] = useState("");

  const [ newFirstName, setNewFirstName ] = useState("");
  const [ newLastName, setNewLastName ] = useState("");


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

  async function handleNameChange(){

  }

  return (
    <div className="settings-wrapper">
      <h2>Settings</h2>

      <div className="account-settings-wrapper">
        <h3>Account Settings</h3>
        <div className="change-name-container">
          <h4>Change Account Name</h4>

          <div className="name-container">
            <InputField
              value={newFirstName}
              placeHolder="New First Name"
              onChange={(e) => setNewFirstName(e.target.value)}
              type="text"
            />
            <InputField
              value={newLastName}
              placeHolder="New Last Name"
              onChange={(e) => setNewLastName(e.target.value)}
              type="text"
            />

          </div>

          <InputField 
            value={currentPassword} 
            placeHolder="Current Password" 
            onChange={(e) => setCurrentPassword(e.target.value)}
            type="password"
            />

            <SubmitButton
              label="Set New Name"
              onClick={handleNameChange}
            />

        </div>

        <div className="change-password-container">
          <h4>Change Password</h4>

          <InputField 
            value={currentPassword} 
            placeHolder="Current Password" 
            onChange={(e) => setCurrentPassword(e.target.value)}
            type="password"
            />
          <InputField
            value={newPassword}
            placeHolder="New Password"
            onChange={(e) => setNewPassword(e.target.value)}
            type="password"
          />
          <InputField
            value={confirmNewPassword}
            placeHolder="Confirm New Password"
            onChange={(e) => setConfirmNewPassowrd(e.target.value)}
            type="password"
          />

          <SubmitButton
            label="Set new Password"
            onClick={handleChangePassword}
          />

        </div>
        <div className="delete-account-container">
          <h4>Delete Account</h4>

          <InputField
            value={userEmail}
            placeHolder="Account Email"
            onChange={(e) => setUserEmail(e.target.value)}
            type="text"
          />

          <InputField 
            value={currentPassword} 
            placeHolder="Current Password" 
            onChange={(e) => setCurrentPassword(e.target.value)}
            type="password"
            />

          <SubmitButton
            label="Delete Account"
            onClick={handleDeleteAccount}
          />
        </div>
      </div>

      <div className="support-container">
        <div className="report-bug-container">'
          <span>Report a Bug <FaBug color="orange"/></span>
          <button className="contact-button"><BsBoxArrowUpRight/></button>
        </div>

        <div className="contact-support-container">
          <span>Contact Support</span>
          <button className="contact-button"><BsBoxArrowUpRight/></button>
        </div>
      </div>

    </div>
  );
}
