import React, { useRef, useState, useEffect } from 'react';
import { useAppStore, PageType } from '../store/appStore';
import { Home, Star, Grid3X3, Info } from 'lucide-react';

const navItems: { id: PageType; label: string; icon: React.ReactNode }[] = [
  { id: 'home', label: '首页', icon: <Home size={20} /> },
  { id: 'featured', label: '精选', icon: <Star size={20} /> },
  { id: 'features', label: '功能', icon: <Grid3X3 size={20} /> },
  { id: 'about', label: '关于', icon: <Info size={20} /> },
];

const PhoneMockup: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { currentPage, setCurrentPage } = useAppStore();
  const navRef = useRef<HTMLDivElement>(null);
  const [indicatorStyle, setIndicatorStyle] = useState({ left: 0, width: 0 });
  const touchStartX = useRef<number | null>(null);
  const touchStartY = useRef<number | null>(null);

  // 更新滑动指示器位置
  useEffect(() => {
    if (!navRef.current) return;
    const buttons = navRef.current.querySelectorAll('button');
    const activeIndex = navItems.findIndex(item => item.id === currentPage);
    if (activeIndex >= 0 && buttons[activeIndex]) {
      const button = buttons[activeIndex];
      const navRect = navRef.current.getBoundingClientRect();
      const buttonRect = button.getBoundingClientRect();
      setIndicatorStyle({
        left: buttonRect.left - navRect.left,
        width: buttonRect.width,
      });
    }
  }, [currentPage]);

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    touchStartY.current = e.touches[0].clientY;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current == null || touchStartY.current == null) return;
    const endX = e.changedTouches[0].clientX;
    const endY = e.changedTouches[0].clientY;
    const diffX = touchStartX.current - endX;
    const diffY = touchStartY.current - endY;

    //  predominantly horizontal swipe
    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 50) {
      const currentIndex = navItems.findIndex(item => item.id === currentPage);
      if (diffX > 0 && currentIndex < navItems.length - 1) {
        setCurrentPage(navItems[currentIndex + 1].id);
      } else if (diffX < 0 && currentIndex > 0) {
        setCurrentPage(navItems[currentIndex - 1].id);
      }
    }

    touchStartX.current = null;
    touchStartY.current = null;
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black p-4">
      {/* Phone Frame */}
      <div className="relative">
        {/* Phone Body */}
        <div className="relative w-[375px] h-[812px] bg-[#1a1a1a] rounded-[50px] shadow-2xl border-4 border-[#2a2a2a] overflow-hidden">
          {/* Screen Content - 全屏显示，无状态栏 */}
          <div
            className="absolute top-0 left-0 right-0 bottom-20 bg-[#0a0a0a] overflow-hidden"
            onTouchStart={handleTouchStart}
            onTouchEnd={handleTouchEnd}
          >
            {children}
          </div>

          {/* Bottom Navigation */}
          <div
            ref={navRef}
            className="absolute bottom-0 left-0 right-0 h-20 bg-[#0a0a0a] border-t border-white/5 flex items-center justify-around px-4 z-40"
          >
            {/* 滑动指示器 */}
            <div
              className="absolute top-0 h-[2px] bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] rounded-full transition-all duration-300 ease-out"
              style={{
                left: indicatorStyle.left,
                width: indicatorStyle.width,
                transform: 'translateY(-1px)',
              }}
            />

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
