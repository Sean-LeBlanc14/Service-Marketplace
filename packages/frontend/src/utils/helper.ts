
export function getToken(){
    const authToken = window.localStorage.getItem("jwt_token");
    
    return authToken;
}