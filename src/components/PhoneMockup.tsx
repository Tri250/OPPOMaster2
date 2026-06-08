import React from 'react';
import { useAppStore, PageType } from '../store/appStore';
import { Home, Star, Grid3X3, Info } from 'lucide-react';

const PhoneMockup: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { currentPage, setCurrentPage } = useAppStore();

  const navItems: { id: PageType; label: string; icon: React.ReactNode; ariaLabel: string }[] = [
    { id: 'home', label: '首页', icon: <Home size={20} />, ariaLabel: '首页' },
    { id: 'featured', label: '精选', icon: <Star size={20} />, ariaLabel: '精选' },
    { id: 'features', label: '功能', icon: <Grid3X3 size={20} />, ariaLabel: '功能' },
    { id: 'about', label: '关于', icon: <Info size={20} />, ariaLabel: '关于' },
  ];

  return (
    <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black p-4">
      {/* Phone Frame */}
      <div className="relative animate-spring-in">
        {/* Phone Body - 液态玻璃外壳 */}
        <div 
          className="relative w-[375px] h-[812px] rounded-[50px] overflow-hidden"
          style={{
            background: 'linear-gradient(135deg, rgba(42, 42, 42, 0.9) 0%, rgba(26, 26, 26, 0.95) 100%)',
            boxShadow: '0 25px 50px rgba(0, 0, 0, 0.5), inset 0 1px 1px rgba(255, 255, 255, 0.1)',
            border: '4px solid rgba(60, 60, 60, 0.8)'
          }}
        >
          {/* Screen Content - 全屏显示 */}
          <div 
            className="absolute top-0 left-0 right-0 bottom-20 overflow-hidden"
            style={{ background: 'var(--color-bg-primary)' }}
          >
            {children}
          </div>

          {/* Bottom Navigation - 液态玻璃效果 */}
          <div 
            className="absolute bottom-0 left-0 right-0 h-20 flex items-center justify-around px-4 z-40 glass-bottom-bar"
            role="navigation"
            aria-label="主导航"
          >
            {navItems.map((item, index) => (
              <button
                key={item.id}
                onClick={() => setCurrentPage(item.id)}
                aria-label={item.ariaLabel}
                aria-current={currentPage === item.id ? 'page' : undefined}
                className={`flex flex-col items-center gap-1 transition-all duration-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#FF6B35] focus-visible:ring-offset-2 focus-visible:ring-offset-[#0a0a0a] rounded-lg px-3 py-2 ripple-container ${
                  currentPage === item.id
                    ? 'text-[#FF6B35]'
                    : 'text-white/50 hover:text-white/70'
                }`}
                style={{
                  transform: currentPage === item.id ? 'scale(1.1)' : 'scale(1)',
                  transition: 'transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), color 0.2s ease'
                }}
              >
                <div 
                  className="transition-transform duration-300"
                  style={{
                    transform: currentPage === item.id ? 'scale(1.15)' : 'scale(1)',
                    animation: currentPage === item.id ? 'liquid-pulse 1.5s ease infinite' : 'none'
                  }}
                >
                  {item.icon}
                </div>
                <span className="text-[10px] font-medium">{item.label}</span>
                {/* 活动指示器 */}
                {currentPage === item.id && (
                  <div 
                    className="absolute -bottom-1 w-1 h-1 rounded-full bg-[#FF6B35]"
                    style={{
                      boxShadow: '0 0 6px rgba(255, 107, 53, 0.6)',
                      animation: 'liquid-breathe 2s ease infinite'
                    }}
                  />
                )}
              </button>
            ))}
          </div>

          {/* Home Indicator - 液态玻璃效果 */}
          <div 
            className="absolute bottom-2 left-1/2 -translate-x-1/2 w-[134px] h-[5px] rounded-full z-50"
            style={{
              background: 'linear-gradient(90deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.35) 50%, rgba(255, 255, 255, 0.2) 100%)',
              boxShadow: '0 1px 2px rgba(0, 0, 0, 0.3)'
            }}
          />
        </div>

        {/* Side Buttons - 液态玻璃质感 */}
        <div 
          className="absolute top-[120px] -left-[6px] w-[6px] h-[30px] rounded-l-md"
          style={{
            background: 'linear-gradient(180deg, rgba(60, 60, 60, 0.9) 0%, rgba(42, 42, 42, 0.95) 100%)',
            boxShadow: 'inset -1px 0 2px rgba(255, 255, 255, 0.1)'
          }}
        />
        <div 
          className="absolute top-[170px] -left-[6px] w-[6px] h-[60px] rounded-l-md"
          style={{
            background: 'linear-gradient(180deg, rgba(60, 60, 60, 0.9) 0%, rgba(42, 42, 42, 0.95) 100%)',
            boxShadow: 'inset -1px 0 2px rgba(255, 255, 255, 0.1)'
          }}
        />
        <div 
          className="absolute top-[250px] -left-[6px] w-[6px] h-[60px] rounded-l-md"
          style={{
            background: 'linear-gradient(180deg, rgba(60, 60, 60, 0.9) 0%, rgba(42, 42, 42, 0.95) 100%)',
            boxShadow: 'inset -1px 0 2px rgba(255, 255, 255, 0.1)'
          }}
        />
        <div 
          className="absolute top-[170px] -right-[6px] w-[6px] h-[90px] rounded-r-md"
          style={{
            background: 'linear-gradient(180deg, rgba(60, 60, 60, 0.9) 0%, rgba(42, 42, 42, 0.95) 100%)',
            boxShadow: 'inset 1px 0 2px rgba(255, 255, 255, 0.1)'
          }}
        />
      </div>
    </div>
  );
};

export default PhoneMockup;