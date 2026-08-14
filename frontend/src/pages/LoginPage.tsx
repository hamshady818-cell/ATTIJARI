import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Input } from '../components/ui/Input';
import { Lock, User, ShieldCheck, AlertCircle, ArrowRight, CheckCircle2, Shield } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import buildingImg from '../assets/attijari-building.jpg';
import logoImg from '../assets/attijari-logo.png';

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
        <div className="min-h-screen w-full flex flex-col lg:flex-row bg-[#F8F9FA] text-slate-800 font-sans">
            {/* PARTIE GAUCHE: Visual & Institutional Branding */}
            <div className="relative w-full lg:w-1/2 min-h-[300px] sm:min-h-[360px] lg:min-h-screen flex flex-col justify-between p-6 sm:p-10 lg:p-14 overflow-hidden bg-[#1A0508]">
                {/* Background Image with Cover */}
                <img
                    src={buildingImg}
                    alt="Siège Attijariwafa bank"
                    className="absolute inset-0 w-full h-full object-cover object-center transform scale-105 transition-transform duration-1000 ease-out"
                />

                {/* Gradient Overlays for High Legibility */}
                <div className="absolute inset-0 bg-gradient-to-t from-[#1a0508] via-[#1a0508]/60 to-[#1a0508]/30" />
                <div className="absolute inset-0 bg-gradient-to-r from-[#1a0508]/80 via-transparent to-transparent hidden lg:block" />

                {/* Top Institutional Badge */}
                <div className="relative z-10 flex items-center justify-between">
                    <div className="flex items-center gap-2.5 bg-white/10 backdrop-blur-md px-3.5 py-1.5 rounded-full border border-white/15">
                        <span className="w-2 h-2 rounded-full bg-[#FDB913] animate-pulse" />
                        <span className="text-[11px] font-semibold tracking-wider text-white uppercase">
                            Attijariwafa bank
                        </span>
                    </div>

                    <div className="hidden sm:flex items-center gap-1.5 text-xs text-white/80 font-medium">
                        <Shield className="w-3.5 h-3.5 text-[#FDB913]" />
                        <span>Portail Sécurisé</span>
                    </div>
                </div>

                {/* Center / Bottom Main Text Overlay */}
                <div className="relative z-10 my-auto pt-8 pb-4 lg:py-0 max-w-xl space-y-4">
                    <div className="inline-flex items-center gap-2 px-3 py-1 rounded-md bg-[#C8102E]/80 border border-white/20 text-white text-xs font-semibold uppercase tracking-widest">
                        <span>Solution Corporate</span>
                    </div>

                    <div className="space-y-2">
                        <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-white tracking-tight leading-tight">
                            Votre espace de gestion documentaire
                        </h1>
                        <p className="text-xl sm:text-2xl font-bold text-[#FDB913] tracking-wide">
                            GED-AWB
                        </p>
                    </div>

                    <p className="text-sm sm:text-base text-gray-200 font-normal leading-relaxed max-w-lg">
                        Centralisez, sécurisez et gérez vos documents en toute simplicité.
                    </p>

                    <div className="pt-4 hidden sm:grid grid-cols-2 gap-3 text-xs text-white/90">
                        <div className="flex items-center gap-2 bg-black/25 backdrop-blur-sm p-2.5 rounded-lg border border-white/10">
                            <CheckCircle2 className="w-4 h-4 text-[#FDB913] shrink-0" />
                            <span>Gouvernance & Conformité</span>
                        </div>
                        <div className="flex items-center gap-2 bg-black/25 backdrop-blur-sm p-2.5 rounded-lg border border-white/10">
                            <CheckCircle2 className="w-4 h-4 text-[#FDB913] shrink-0" />
                            <span>Contrôle d'Accès Sécurisé</span>
                        </div>
                    </div>
                </div>

                {/* Bottom Left Footer Info */}
                <div className="relative z-10 pt-4 border-t border-white/15 flex items-center justify-between text-xs text-white/70">
                    <span>GED-AWB Workstation v0.1.0</span>
                    <span className="font-mono text-[11px]">ISO/IEC 27001 Certified</span>
                </div>
            </div>

            {/* PARTIE DROITE: Modern Bank Login Panel */}
            <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-10 lg:p-16 bg-[#F8F9FA]">
                <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-200/80 p-8 sm:p-10 space-y-7">

                    {/* Header: Logo & Subtitle */}
                    <div className="space-y-4 text-center sm:text-left">
                        <div className="flex items-center justify-center sm:justify-start gap-4">
                            <img
                                src={logoImg}
                                alt="Attijariwafa bank"
                                className="h-12 w-auto object-contain shrink-0"
                            />
                            <div className="h-10 w-px bg-gray-200 hidden sm:block" />
                            <div className="hidden sm:block">
                                <span className="text-xs font-bold uppercase tracking-wider text-[#C8102E] block">
                                    GED-AWB
                                </span>
                                <span className="text-[11px] text-gray-500 font-medium block">
                                    Gestion Électronique de Documents
                                </span>
                            </div>
                        </div>

                        <div className="pt-2 border-t border-gray-100">
                            <h2 className="text-xl sm:text-2xl font-bold text-gray-900 tracking-tight">
                                Bienvenue
                            </h2>
                            <p className="text-xs sm:text-sm text-gray-500 mt-1">
                                Connectez-vous pour accéder à votre espace documentaire.
                            </p>
                        </div>
                    </div>

                    {/* Form Section */}
                    <form onSubmit={handleLogin} className="space-y-5">
                        <div className="space-y-4">
                            <Input
                                label="Identifiant Keycloak / Matricule"
                                leftIcon={<User className="w-4 h-4 text-gray-400" />}
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                                disabled={isSubmitting}
                                placeholder="Entrez votre matricule"
                                className="h-11 text-sm bg-gray-50/50 focus:bg-white border-gray-300 focus:border-[#C8102E] focus:ring-[#C8102E]/20"
                            />

                            <Input
                                label="Mot de passe"
                                type="password"
                                leftIcon={<Lock className="w-4 h-4 text-gray-400" />}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isSubmitting}
                                placeholder="••••••••"
                                className="h-11 text-sm bg-gray-50/50 focus:bg-white border-gray-300 focus:border-[#C8102E] focus:ring-[#C8102E]/20"
                            />
                        </div>

                        {/* Error Alert */}
                        {error && (
                            <div className="p-3.5 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2.5 text-xs text-red-700">
                                <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                                <span className="leading-relaxed">{error}</span>
                            </div>
                        )}

                        {/* Security / SSO status indicator */}
                        <div className="flex items-center justify-between text-xs text-gray-500 bg-gray-50 p-2.5 rounded-lg border border-gray-200/60">
                            <span className="flex items-center gap-1.5 font-medium text-gray-600">
                                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                                Authentification SSO Keycloak
                            </span>
                            <span className="text-[11px] font-mono text-gray-400">Actif</span>
                        </div>

                        {/* Primary Action Button */}
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="w-full h-11 px-6 bg-[#C8102E] hover:bg-[#A00C24] active:bg-[#8B0000] text-white font-semibold text-sm rounded-lg shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 disabled:opacity-60 disabled:cursor-not-allowed group"
                        >
                            {isSubmitting ? (
                                <>
                                    <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                    <span>Vérification en cours...</span>
                                </>
                            ) : (
                                <>
                                    <span>Se connecter au poste GED</span>
                                    <ArrowRight className="w-4 h-4 transition-transform duration-200 group-hover:translate-x-1" />
                                </>
                            )}
                        </button>
                    </form>

                    {/* Disclaimer Footer */}
                    <div className="pt-4 border-t border-gray-100 text-center">
                        <p className="text-[11px] text-gray-400 leading-relaxed">
                            Ce système est réservé à l'usage exclusif du personnel autorisé
                            d'Attijariwafa bank.
                        </p>
                    </div>

                </div>
            </div>
        </div>
    );
};