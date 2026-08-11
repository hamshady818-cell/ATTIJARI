import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { AttijariLogo } from '../components/ui/AttijariLogo';
import { Lock, User, ShieldCheck, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LoginPage: React.FC = () => {
    const navigate = useNavigate();
    const { login, isAuthenticated } = useAuth();

    const [username, setUsername] = useState('docuser');
    const [password, setPassword] = useState('password');

    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        if (isAuthenticated) {
            navigate('/', { replace: true });
        }
    }, [isAuthenticated, navigate]);

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setIsSubmitting(true);

        try {
            await login(username, password);
            navigate('/', { replace: true });
        } catch (err: any) {
            console.error('Erreur d\'authentification GED Keycloak:', err);
            setError(err.message || 'Identifiant ou mot de passe incorrect.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-brand-background p-4">
            <div className="w-full max-w-sm bg-brand-surface border border-brand-border rounded-xl shadow-modal overflow-hidden p-6 space-y-6">

                <div className="text-center space-y-3 border-b border-brand-border pb-5">
                    <AttijariLogo className="w-14 h-14 mx-auto" />

                    <div>
                        <h1 className="text-sm font-bold tracking-wider uppercase text-brand-text">
                            GED-AWB Workstation
                        </h1>

                        <p className="text-[11px] font-mono text-brand-muted uppercase mt-0.5">
                            Attijariwafa bank — Portail Interne
                        </p>
                    </div>
                </div>

                <form onSubmit={handleLogin} className="space-y-4">

                    <Input
                        label="Identifiant Keycloak / Matricule"
                        leftIcon={<User className="w-3.5 h-3.5" />}
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />

                    <Input
                        label="Mot de passe"
                        type="password"
                        leftIcon={<Lock className="w-3.5 h-3.5" />}
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />

                    <div className="flex items-center justify-between text-[11px] text-brand-muted">
                        <span className="flex items-center gap-1.5">
                            <ShieldCheck className="w-4 h-4 text-emerald-700" />
                            SSO Authentification active
                        </span>

                        <span className="font-mono">
                            v0.1.0
                        </span>
                    </div>

                    {error && (
                        <div className="p-3 bg-red-50 border border-red-200 rounded-md flex items-start gap-2 text-xs text-red-700">
                            <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                            <span>{error}</span>
                        </div>
                    )}

                    <Button
                        type="submit"
                        variant="primary"
                        className="w-full"
                        size="md"
                        disabled={isSubmitting}
                    >
                        {isSubmitting
                            ? 'Vérification en cours...'
                            : 'Se connecter au poste GED'}
                    </Button>

                </form>

                <div className="text-[10px] text-brand-muted text-center border-t border-brand-border pt-4">
                    Ce système est réservé à l'usage exclusif du personnel autorisé
                    d'Attijariwafa bank.
                </div>

            </div>
        </div>
    );
};