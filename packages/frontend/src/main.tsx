import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { PublicClientApplication, EventType } from '@azure/msal-browser';
import { msalConfig } from './authConfig';

import 'bootstrap/dist/css/bootstrap.min.css';

// The msal Instance is instantiated outside of the component tree to avoid rerendering

const msalInstance = new PublicClientApplication(msalConfig);

msalInstance.initialize().then(() => {
    
    // Set active account if one exists in cache
    if (!msalInstance.getActiveAccount() && msalInstance.getAllAccounts().length > 0) {
        msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
    }

    // Optional: Event callback to set active account on login success
    msalInstance.addEventCallback((event) => {
        if (event.eventType === EventType.LOGIN_SUCCESS && event.payload.account) {
            msalInstance.setActiveAccount(event.payload.account);
        }
    });

    const root = createRoot(document.getElementById('root'));
    root.render(
        <App instance={msalInstance}/>
    );
}).catch(err => {
    console.error("Failed to initialize MSAL:", err);
});