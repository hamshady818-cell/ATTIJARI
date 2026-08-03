import React from 'react';
import bankLogo from '../../assets/bank_logo.png';

interface LogoProps {
  className?: string;
}

export const Logo: React.FC<LogoProps> = ({ className = 'h-10' }) => {
  return (
    <div className={`flex items-center gap-2.5 select-none ${className}`}>
      <img src={bankLogo} alt="Attijariwafa Bank" className="h-full w-auto object-contain" />
      <div className="flex flex-col text-left">
        <span className="text-sm font-black text-slate-800 tracking-tight leading-none">
          ATTIJARIWAFA BANK
        </span>
        <span className="text-[9px] font-bold text-brand tracking-widest mt-0.5 leading-none">
          GED INTRA-NET
        </span>
      </div>
    </div>
  );
};
