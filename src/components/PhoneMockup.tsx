import React, { useRef, useEffect } from 'react';
import { useAppStore, PageType } from '../store/appStore';
import { Home, Star, Grid3X3, Info } from 'lucide-react';

const PhoneMockup: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { currentPage, setCurrentPage } = useAppStore();
  const navRef = useRef<HTMLDivElement>(null);

  const navItems: { id: PageType; label: string; icon: React.ReactNode }[] = [
    { id: 'home', label: '首页', icon: <Home size={20} /> },
    { id: 'featured', label: '精选', icon: <Star size={20} /> },
    { id: 'features', label: '功能', icon: <Grid3X3 size={20} /> },
    { id: 'about', label: '关于', icon: <Info size={20} /> },
  ];

  // 切换页面时，确保当前激活的导航项始终可见
  useEffect(() => {
    const container = navRef.current;
    if (!container) return;
    const activeButton = container.querySelector(`[data-nav-id="${currentPage}"]`) as HTMLElement | null;
    if (activeButton) {
      activeButton.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    }
  }, [currentPage]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black p-4">
      {/* Phone Frame */}
      <div className="relative">
        {/* Phone Body */}
        <div className="relative w-[375px] h-[812px] bg-[#1a1a1a] rounded-[50px] shadow-2xl border-4 border-[#2a2a2a] overflow-hidden">
          {/* Screen Content - 全屏显示，无状态栏 */}
          <div className="absolute top-0 left-0 right-0 bottom-20 bg-[#0a0a0a] overflow-hidden">
            {children}
          </div>

          {/* Bottom Navigation - 固定在底部，支持横向滚动 */}
          <div
            ref={navRef}
            className="absolute bottom-0 left-0 right-0 h-20 bg-[#0a0a0a] border-t border-white/5 flex items-center px-2 z-40 overflow-x-auto scrollbar-hide"
          >
            {navItems.map((item) => (
              <button
                key={item.id}
                data-nav-id={item.id}
                onClick={() => setCurrentPage(item.id)}
                className={`flex-shrink-0 flex flex-col items-center justify-center gap-1 min-w-[70px] h-full transition-all duration-300 ${
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

      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default PhoneMockup;
