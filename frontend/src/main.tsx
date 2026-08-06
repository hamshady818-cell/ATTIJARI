import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import keycloak from './lib/keycloak';

const rootElement = document.getElementById('root')!;
const root = ReactDOM.createRoot(rootElement);

keycloak
    .init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
    })
    .then((authenticated) => {
        if (!authenticated) {
            keycloak.login();
            return;
        }

        setInterval(() => {
            keycloak.updateToken(30).catch(() => keycloak.login());
        }, 20000);

        root.render(
            <React.StrictMode>
                <App />
            </React.StrictMode>
        );
    })
    .catch((error) => {
        console.error("Échec de l'initialisation de Keycloak:", error);
        root.render(
            <div style={{ padding: '2rem', fontFamily: 'sans-serif', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#f9fafb' }}>
                <div style={{ backgroundColor: 'white', padding: '2rem', borderRadius: '0.75rem', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', maxWidth: '540px', width: '100%', borderLeft: '4px solid #ef4444' }}>
                    <h2 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#991b1b', marginBottom: '0.75rem' }}>
                        Erreur d'authentification Keycloak
                    </h2>
                    <p style={{ color: '#374151', fontSize: '0.95rem', marginBottom: '1rem' }}>
                        L'initialisation de la session Keycloak a échoué. Le frontend n'a pas pu échanger le jeton avec le serveur Keycloak.
                    </p>
                    <div style={{ backgroundColor: '#f3f4f6', padding: '0.75rem', borderRadius: '0.375rem', fontSize: '0.85rem', fontFamily: 'monospace', color: '#1f2937', marginBottom: '1.25rem', wordBreak: 'break-all' }}>
                        {String(error || 'Network Error / 403 Forbidden')}
                    </div>
                    <div style={{ fontSize: '0.875rem', color: '#4b5563', lineHeight: '1.5' }}>
                        <strong>Actions recommandées dans Keycloak (Admin Console) :</strong>
                        <ol style={{ margin: '0.5rem 0 0 1.25rem', paddingLeft: 0 }}>
                            <li>Vérifiez que <strong>Client Authentication</strong> est désactivé (Client public).</li>
                            <li>Vérifiez que <strong>Web Origins</strong> contient <code>+</code> ou <code>http://localhost:5173</code> (sans <code>/*</code>).</li>
                            <li>Vérifiez que Keycloak tourne bien sur <code>http://localhost:8081</code>.</li>
                        </ol>
                    </div>
                </div>
            </div>
        );
    });