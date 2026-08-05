import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Lock, User, ShieldCheck } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('docuser');
  const [password, setPassword] = useState('password');
  const navigate = useNavigate();

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    // Simulate Keycloak SSO authentication redirect / token login
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-brand-bg flex flex-col justify-center items-center p-4">
      {/* Top Red Bar Accent */}
      <div className="fixed top-0 left-0 right-0 h-1.5 bg-brand-primary" />

      <div className="w-full max-w-sm bg-brand-surface border border-brand-border shadow-popover p-6 space-y-6">
        {/* Attijari Header Branding */}
        <div className="text-center space-y-2 border-b border-brand-border pb-4">
          <div className="w-10 h-10 bg-brand-primary mx-auto flex items-center justify-center text-white font-bold text-base rounded-none">
            AW
          </div>
          <div>
            <h1 className="text-sm font-bold tracking-wider uppercase text-brand-text">
              GED-AWB Workstation
            </h1>
            <p className="text-[11px] font-mono text-brand-muted uppercase">
              Attijariwafa bank — Portail Interne
            </p>
          </div>
        </div>

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4">
          <Input
            label="Identifiant Keycloak / Matricule"
            leftIcon={<User className="w-3.5 h-3.5" />}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <Input
            label="Mot de passe"
            type="password"
            leftIcon={<Lock className="w-3.5 h-3.5" />}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <div className="flex items-center justify-between text-[11px] text-brand-muted">
            <span className="flex items-center gap-1">
              <ShieldCheck className="w-3 h-3 text-emerald-700" />
              SSO Authentification active
            </span>
            <span className="font-mono">v0.1.0</span>
          </div>

          <Button type="submit" variant="primary" className="w-full" size="md">
            Se connecter au poste GED
          </Button>
        </form>

        <div className="text-[10px] text-brand-muted text-center border-t border-brand-border pt-3">
          Ce système est réservé à l'usage exclusif du personnel autorisé d'Attijariwafa bank.
        </div>
      </div>
    </div>
  );
};
