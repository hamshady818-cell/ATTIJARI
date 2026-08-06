import React from 'react';
import logoImg from '../../assets/attijari-logo.png';

interface AttijariLogoProps {
  className?: string;
}

export const AttijariLogo: React.FC<AttijariLogoProps> = ({ className = 'w-8 h-8' }) => {
  return (
    <div className={`overflow-hidden shrink-0 ${className}`} title="Attijariwafa bank">
      <img
        src={logoImg}
        alt="Attijariwafa bank"
        className="w-full h-full object-cover block"
      />
    </div>
  );
};
