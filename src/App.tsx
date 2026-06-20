import React, { Suspense, lazy } from 'react';
import { useAppStore } from './store/appStore';
import PhoneMockup from './components/PhoneMockup';
import HomeScreen from './pages/HomeScreen';
import FeaturedScreen from './pages/FeaturedScreen';
import FeaturesScreen from './pages/FeaturesScreen';
import AboutScreen from './pages/AboutScreen';
import { tokens } from './styles/designTokens';

// 主屏保持即时加载；子页面按路由懒加载，降低首屏包体积
const AISceneRecognitionPage = lazy(() => import('./pages/subpages/AISceneRecognitionPage'));
const AIFineTunePage = lazy(() => import('./pages/subpages/AIFineTunePage'));
const SmartOptimizePage = lazy(() => import('./pages/subpages/SmartOptimizePage'));
const WatermarkPage = lazy(() => import('./pages/subpages/WatermarkPage'));
const PresetManagerPage = lazy(() => import('./pages/subpages/PresetManagerPage'));
const ParamAdjustPage = lazy(() => import('./pages/subpages/ParamAdjustPage'));
const LUTSharePage = lazy(() => import('./pages/subpages/LUTSharePage'));
const HasselbladPage = lazy(() => import('./pages/subpages/HasselbladPage'));
const CloudSyncPage = lazy(() => import('./pages/subpages/CloudSyncPage'));
const UpdateChannelPage = lazy(() => import('./pages/subpages/UpdateChannelPage'));
const TermsPage = lazy(() => import('./pages/subpages/TermsPage'));
const ThemeSettingsPage = lazy(() => import('./pages/subpages/ThemeSettingsPage'));
const NotificationPage = lazy(() => import('./pages/subpages/NotificationPage'));
const PrivacyPage = lazy(() => import('./pages/subpages/PrivacyPage'));
const PresetSourceManager = lazy(() => import('./pages/subpages/PresetSourceManager'));

/**
 * 子页面懒加载占位
 */
const PageSkeleton: React.FC = () => (
  <div
    className="h-full w-full flex items-center justify-center"
    style={{ background: tokens.colors.background }}
  >
    <div
      className="w-10 h-10 rounded-full border-2 border-t-transparent animate-spin"
      style={{ borderColor: tokens.colors.accent, borderTopColor: 'transparent' }}
    />
  </div>
);

const App: React.FC = () => {
  const { currentPage, currentSubPage } = useAppStore();

  const renderSubPage = () => {
    switch (currentSubPage) {
      case 'ai-scene':
        return <AISceneRecognitionPage />;
      case 'ai-fine-tune':
        return <AIFineTunePage />;
      case 'smart-optimize':
        return <SmartOptimizePage />;
      case 'watermark':
        return <WatermarkPage />;
      case 'preset-manager':
        return <PresetManagerPage />;
      case 'param-adjust':
        return <ParamAdjustPage />;
      case 'lut-share':
        return <LUTSharePage />;
      case 'hasselblad':
        return <HasselbladPage />;
      case 'cloud-sync':
        return <CloudSyncPage />;
      case 'theme-settings':
        return <ThemeSettingsPage />;
      case 'update-channel':
        return <UpdateChannelPage />;
      case 'notification':
        return <NotificationPage />;
      case 'privacy':
        return <PrivacyPage />;
      case 'terms':
        return <TermsPage />;
      case 'preset-sources':
        return <PresetSourceManager />;
      default:
        return null;
    }
  };

  const renderMainPage = () => {
    switch (currentPage) {
      case 'home':
        return <HomeScreen />;
      case 'featured':
        return <FeaturedScreen />;
      case 'features':
        return <FeaturesScreen />;
      case 'about':
        return <AboutScreen />;
      default:
        return <HomeScreen />;
    }
  };

  return (
    <PhoneMockup>
      <div className="h-full w-full relative">
        {currentSubPage ? (
          <Suspense fallback={<PageSkeleton />}>{renderSubPage()}</Suspense>
        ) : (
          renderMainPage()
        )}
      </div>
    </PhoneMockup>
  );
};

export default App;
