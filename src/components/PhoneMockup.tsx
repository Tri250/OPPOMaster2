import React, { useRef, useEffect } from 'react';
import { useAppStore, PageType } from '../store/appStore';
import { Home, Star, Grid3X3, Info } from 'lucide-react';
import { tokens } from '../styles/designTokens';

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
    <div
      className="flex items-center justify-center min-h-screen p-4"
      style={{
        background: `radial-gradient(ellipse at 50% 0%, #1f1f23 0%, ${tokens.colors.background} 60%)`,
      }}
    >
      {/* Phone Frame Container */}
      <div className="relative">
        {/* 机身反光层 */}
        <div
          className="absolute -inset-3 rounded-[64px] opacity-40 blur-2xl pointer-events-none"
          style={{
            background: `linear-gradient(135deg, rgba(255,255,255,0.08) 0%, transparent 50%, ${tokens.colors.accentGlow} 100%)`,
          }}
        />

        {/* Phone Body */}
        <div
          className="relative w-[375px] h-[812px] rounded-[54px] overflow-hidden shadow-medium"
          style={{
            background: '#111111',
            border: '1px solid rgba(255,255,255,0.12)',
            boxShadow: `0 32px 80px rgba(0,0,0,0.6), inset 0 1px 0 rgba(255,255,255,0.1)`,
          }}
        >
          {/* 金属边框高亮 */}
          <div
            className="absolute inset-0 rounded-[54px] pointer-events-none z-20"
            style={{
              boxShadow: 'inset 0 1px 2px rgba(255,255,255,0.18), inset 0 -1px 2px rgba(0,0,0,0.5)',
            }}
          />

          {/* Dynamic Island / 前置摄像头 */}
          <div className="absolute top-3 left-1/2 -translate-x-1/2 z-30">
            <div
              className="h-[34px] w-[118px] rounded-full flex items-center justify-center"
              style={{
                background: '#050505',
                boxShadow: 'inset 0 1px 2px rgba(255,255,255,0.12), 0 1px 2px rgba(0,0,0,0.4)',
              }}
            >
              {/* 镜头反光 */}
              <div className="absolute right-4 w-2.5 h-2.5 rounded-full bg-[#1a1a2e] overflow-hidden">
                <div className="absolute top-0.5 left-0.5 w-1 h-1 rounded-full bg-[#0a3d62] opacity-80" />
              </div>
            </div>
          </div>

          {/* Screen Content */}
          <div
            className="absolute top-0 left-0 right-0 bottom-[76px] overflow-hidden"
            style={{ background: tokens.colors.background }}
          >
            {children}
          </div>

          {/* Bottom Navigation - 液态玻璃导航 */}
          <div
            ref={navRef}
            className="absolute bottom-0 left-0 right-0 h-[76px] flex items-center px-2 z-40 overflow-x-auto scrollbar-hide"
            style={{
              background: 'rgba(10,10,10,0.78)',
              backdropFilter: 'blur(24px) saturate(180%)',
              WebkitBackdropFilter: 'blur(24px) saturate(180%)',
              borderTop: `1px solid ${tokens.colors.glassBorder}`,
            }}
          >
            {navItems.map((item) => {
              const isActive = currentPage === item.id;
              return (
                <button
                  key={item.id}
                  data-nav-id={item.id}
                  onClick={() => setCurrentPage(item.id)}
                  className="flex-shrink-0 flex flex-col items-center justify-center gap-1 min-w-[70px] h-full transition-all duration-normal"
                  style={{
                    color: isActive ? tokens.colors.accent : tokens.colors.textTertiary,
                    transform: isActive ? 'scale(1.08)' : 'scale(1)',
                    transitionTimingFunction: tokens.animation.easing.spring,
                  }}
                >
                  <div
                    className="p-1.5 rounded-full transition-all duration-normal"
                    style={{
                      background: isActive ? `${tokens.colors.accent}20` : 'transparent',
                      transform: isActive ? 'translateY(-2px)' : 'translateY(0)',
                      transitionTimingFunction: tokens.animation.easing.spring,
                    }}
                  >
                    {item.icon}
                  </div>
                  <span className="text-micro font-medium">{item.label}</span>
                </button>
              );
            })}
          </div>

          {/* Home Indicator */}
          <div
            className="absolute bottom-2 left-1/2 -translate-x-1/2 w-[134px] h-[5px] rounded-full z-50"
            style={{ background: 'rgba(255,255,255,0.35)' }}
          />
        </div>

        {/* Side Buttons */}
        <div
          className="absolute top-[118px] -left-[5px] w-[5px] h-[32px] rounded-l-md"
          style={{ background: '#222', boxShadow: 'inset -1px 0 1px rgba(0,0,0,0.5)' }}
        />
        <div
          className="absolute top-[168px] -left-[5px] w-[5px] h-[68px] rounded-l-md"
          style={{ background: '#222', boxShadow: 'inset -1px 0 1px rgba(0,0,0,0.5)' }}
        />
        <div
          className="absolute top-[250px] -left-[5px] w-[5px] h-[68px] rounded-l-md"
          style={{ background: '#222', boxShadow: 'inset -1px 0 1px rgba(0,0,0,0.5)' }}
        />
        <div
          className="absolute top-[178px] -right-[5px] w-[5px] h-[96px] rounded-r-md"
          style={{ background: '#222', boxShadow: 'inset 1px 0 1px rgba(0,0,0,0.5)' }}
        />
      </div>

      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default PhoneMockup;
