import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import SignupPage from "./pages/SignupPage";
import {
  MsalProvider,
  AuthenticatedTemplate,
  useMsal,
  UnauthenticatedTemplate
} from "@azure/msal-react";
import { Container, Button } from "react-bootstrap";
import { loginRequest } from "./authConfig";
import type { IPublicClientApplication } from "@azure/msal-browser";

const MainContent = () => {
  /**
   * useMsal is hook that returns the PublicClientApplication instance,
   * that tells you what msal is currently doing. For more, visit:
   * https://github.com/AzureAD/microsoft-authentication-library-for-js/blob/dev/lib/msal-react/docs/hooks.md
   */
  const { instance } = useMsal();
  const activeAccount = instance.getActiveAccount();

  const handleRedirect = () => {
    instance
      .loginRedirect({
        ...loginRequest,
        prompt: "create"
      })
      .catch((error) => console.log(error));
  };
  return (
    <div className="App">
      <AuthenticatedTemplate>
        {activeAccount ? (
          <Container>
            <IdTokenData
              idTokenClaims={activeAccount.idTokenClaims}
            />
          </Container>
        ) : null}
      </AuthenticatedTemplate>
      <UnauthenticatedTemplate>
        <Button
          className="signInButton"
          onClick={handleRedirect}
          variant="primary">
          Sign up
        </Button>
      </UnauthenticatedTemplate>
    </div>
  );
};

interface AppProps {
  instance: IPublicClientApplication;
}

function App({ instance }: AppProps) {
  return (
    <MsalProvider instance={instance}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route path="signup" element={<SignupPage />} />
            <Route index element={<HomePage />} />
            <Route
              path="test"
              element={<MainContent />}></Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </MsalProvider>
  );
}

export default App;
