import React from 'react';
import { useAppStore, PageType } from '../store/appStore';
import { Home, Star, Grid3X3, Info } from 'lucide-react';

const PhoneMockup: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { currentPage, setCurrentPage } = useAppStore();

  const navItems: { id: PageType; label: string; icon: React.ReactNode }[] = [
    { id: 'home', label: '首页', icon: <Home size={20} /> },
    { id: 'featured', label: '精选', icon: <Star size={20} /> },
    { id: 'features', label: '功能', icon: <Grid3X3 size={20} /> },
    { id: 'about', label: '关于', icon: <Info size={20} /> },
  ];

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black p-4">
      {/* Phone Frame */}
      <div className="relative">
        {/* Phone Body */}
        <div className="relative w-[375px] h-[812px] bg-[#1a1a1a] rounded-[50px] shadow-2xl border-4 border-[#2a2a2a] overflow-hidden">
          {/* Notch */}
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[160px] h-[34px] bg-black rounded-b-[20px] z-50">
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[80px] h-[4px] bg-[#333] rounded-full" />
          </div>

          {/* Status Bar */}
          <div className="absolute top-2 left-6 right-6 flex justify-between items-center z-40 text-white text-xs font-medium">
            <span>9:41</span>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded-full border border-white/30" />
              <div className="w-4 h-4 rounded-full border border-white/30" />
              <div className="w-6 h-3 border border-white/30 rounded-sm relative">
                <div className="absolute inset-0.5 bg-white rounded-sm" />
              </div>
            </div>
          </div>

          {/* Screen Content */}
          <div className="absolute top-12 left-0 right-0 bottom-20 bg-[#0a0a0a] overflow-hidden">
            {children}
          </div>

          {/* Bottom Navigation */}
          <div className="absolute bottom-0 left-0 right-0 h-20 bg-[#0a0a0a] border-t border-white/5 flex items-center justify-around px-4 z-40">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => setCurrentPage(item.id)}
                className={`flex flex-col items-center gap-1 transition-all duration-300 ${
                  currentPage === item.id
                    ? 'text-[#FF6B35] scale-110'
                    : 'text-white/50 hover:text-white/70'
                }`}
              >
                <div className={`transition-transform duration-300 ${currentPage === item.id ? 'scale-110' : ''}`}>
                  {item.icon}
                </div>
                <span className="text-[10px] font-medium">{item.label}</span>
              </button>
            ))}
          </div>

          {/* Home Indicator */}
          <div className="absolute bottom-2 left-1/2 -translate-x-1/2 w-[134px] h-[5px] bg-white/30 rounded-full z-50" />
        </div>

        {/* Side Buttons */}
        <div className="absolute top-[120px] -left-[6px] w-[6px] h-[30px] bg-[#2a2a2a] rounded-l-md" />
        <div className="absolute top-[170px] -left-[6px] w-[6px] h-[60px] bg-[#2a2a2a] rounded-l-md" />
        <div className="absolute top-[250px] -left-[6px] w-[6px] h-[60px] bg-[#2a2a2a] rounded-l-md" />
        <div className="absolute top-[170px] -right-[6px] w-[6px] h-[90px] bg-[#2a2a2a] rounded-r-md" />
      </div>
    </div>
  );
};

export default PhoneMockup;
