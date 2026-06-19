import React, { Suspense } from 'react';
import { useAppStore } from './store/appStore';
import PhoneMockup from './components/PhoneMockup';
import HomeScreen from './pages/HomeScreen';
import FeaturedScreen from './pages/FeaturedScreen';
import FeaturesScreen from './pages/FeaturesScreen';
import AboutScreen from './pages/AboutScreen';

// Sub pages - 动态加载，减少首屏 bundle
const AISceneRecognitionPage = React.lazy(() => import('./pages/subpages/AISceneRecognitionPage'));
const AIFineTunePage = React.lazy(() => import('./pages/subpages/AIFineTunePage'));
const SmartOptimizePage = React.lazy(() => import('./pages/subpages/SmartOptimizePage'));
const WatermarkPage = React.lazy(() => import('./pages/subpages/WatermarkPage'));
const PresetManagerPage = React.lazy(() => import('./pages/subpages/PresetManagerPage'));
const ParamAdjustPage = React.lazy(() => import('./pages/subpages/ParamAdjustPage'));
const LUTSharePage = React.lazy(() => import('./pages/subpages/LUTSharePage'));
const HasselbladPage = React.lazy(() => import('./pages/subpages/HasselbladPage'));
const CloudSyncPage = React.lazy(() => import('./pages/subpages/CloudSyncPage'));
const GeneralSettingsPage = React.lazy(() => import('./pages/subpages/GeneralSettingsPage'));
const LegalInfoPage = React.lazy(() => import('./pages/subpages/LegalInfoPage'));
const PresetSourceManager = React.lazy(() => import('./pages/subpages/PresetSourceManager'));

const SubPageFallback: React.FC = () => (
  <div className="h-full w-full bg-[#0a0a0a] flex flex-col items-center justify-center">
    <div className="w-10 h-10 rounded-full border-4 border-white/20 border-t-[#FF6B35] animate-spin" />
    <span className="mt-4 text-white/60 text-sm">加载中...</span>
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
      case 'general-settings':
        return <GeneralSettingsPage />;
      case 'legal-info':
        return <LegalInfoPage />;
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
        <Suspense fallback={<SubPageFallback />}>
          {currentSubPage ? renderSubPage() : renderMainPage()}
        </Suspense>
      </div>
    </PhoneMockup>
  );
};

export default App;
