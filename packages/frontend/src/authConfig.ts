import { LogLevel } from '@azure/msal-browser';


 export const msalConfig = {
     auth: {
         clientId: '2f09ee3f-8678-428c-a18a-6235a39bbb53', 
         authority: 'https://login.microsoftonline.com/common', 
         redirectUri: 'http://localhost:5173', 
         postLogoutRedirectUri: '/', 
         navigateToLoginRequestUrl: false, 
     },
     cache: {
         cacheLocation: 'sessionStorage', 
         storeAuthStateInCookie: false, 
     },
     system: {
         loggerOptions: {
             loggerCallback: (level, message, containsPii) => {
                 if (containsPii) {
                     return;
                 }
                 switch (level) {
                     case LogLevel.Error:
                         console.error(message);
                         return;
                     case LogLevel.Info:
                         console.info(message);
                         return;
                     case LogLevel.Verbose:
                         console.debug(message);
                         return;
                     case LogLevel.Warning:
                         console.warn(message);
                         return;
                     default:
                         return;
                 }
             },
         },
     },
 };

 /**
 * Scopes you add here will be prompted for user consent during sign-in.
 * By default, MSAL.js will add OIDC scopes (openid, profile, email) to any login request.
 * For more information about OIDC scopes, visit: 
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-permissions-and-consent#openid-connect-scopes
 */
 export const loginRequest = {
     scopes: [],
 };

 /**
 * An optional silentRequest object can be used to achieve silent SSO
 * between applications by providing a "login_hint" property.
 */
 // export const silentRequest = {
 //     scopes: ["openid", "profile"],
 //     loginHint: "example@domain.net"
 // };