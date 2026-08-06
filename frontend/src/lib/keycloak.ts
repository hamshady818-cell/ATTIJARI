import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
    realm: import.meta.env.VITE_KEYCLOAK_REALM || "ged-awb",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "ged-frontend",
});

export default keycloak;