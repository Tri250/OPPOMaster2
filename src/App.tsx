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
import LUTSharePage from './pages/subpages/LUTSharePage';
import HasselbladPage from './pages/subpages/HasselbladPage';
import CloudSyncPage from './pages/subpages/CloudSyncPage';
import UpdateChannelPage from './pages/subpages/UpdateChannelPage';
import TermsPage from './pages/subpages/TermsPage';
import ThemeSettingsPage from './pages/subpages/ThemeSettingsPage';
import DarkModePage from './pages/subpages/DarkModePage';
import NotificationPage from './pages/subpages/NotificationPage';
import PrivacyPage from './pages/subpages/PrivacyPage';
import AndroidShowcasePage from './pages/AndroidShowcasePage';

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
      case 'dark-mode':
        return <DarkModePage />;
      case 'update-channel':
        return <UpdateChannelPage />;
      case 'notification':
        return <NotificationPage />;
      case 'privacy':
        return <PrivacyPage />;
      case 'terms':
        return <TermsPage />;
      case 'android-showcase':
        return <AndroidShowcasePage />;
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
    currentSubPage === 'android-showcase' ? (
      <AndroidShowcasePage />
    ) : (
      <PhoneMockup>
        <div className="h-full w-full relative">
          {currentSubPage ? renderSubPage() : renderMainPage()}
        </div>
      </PhoneMockup>
    )
  );
};

export default App;
