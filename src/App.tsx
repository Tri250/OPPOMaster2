import React from 'react';
import { useAppStore } from './store/appStore';
import PhoneMockup from './components/PhoneMockup';
import HomeScreen from './pages/HomeScreen';
import FeaturedScreen from './pages/FeaturedScreen';
import FeaturesScreen from './pages/FeaturesScreen';
import AboutScreen from './pages/AboutScreen';

// Sub pages
import AISceneRecognitionPage from './pages/subpages/AISceneRecognitionPage';
import AIFineTunePage from './pages/subpages/AIFineTunePage';
import SmartOptimizePage from './pages/subpages/SmartOptimizePage';
import WatermarkPage from './pages/subpages/WatermarkPage';
import PresetManagerPage from './pages/subpages/PresetManagerPage';
import ParamAdjustPage from './pages/subpages/ParamAdjustPage';
import CloudSyncPage from './pages/subpages/CloudSyncPage';
import ThemeSettingsPage from './pages/subpages/ThemeSettingsPage';
import DarkModePage from './pages/subpages/DarkModePage';
import NotificationPage from './pages/subpages/NotificationPage';
import PrivacyPage from './pages/subpages/PrivacyPage';

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
      case 'cloud-sync':
        return <CloudSyncPage />;
      case 'theme-settings':
        return <ThemeSettingsPage />;
      case 'dark-mode':
        return <DarkModePage />;
      case 'notification':
        return <NotificationPage />;
      case 'privacy':
        return <PrivacyPage />;
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
        {currentSubPage ? renderSubPage() : renderMainPage()}
      </div>
    </PhoneMockup>
  );
};

export default App;