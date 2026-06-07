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
import ThemeSettingsPage from './pages/subpages/ThemeSettingsPage';
import DarkModePage from './pages/subpages/DarkModePage';
import NotificationPage from './pages/subpages/NotificationPage';
import PrivacyPage from './pages/subpages/PrivacyPage';
import HSLAdjustmentPage from './pages/subpages/HSLAdjustmentPage';
import BatchProcessingPage from './pages/subpages/BatchProcessingPage';
import RAWProcessingPage from './pages/subpages/RAWProcessingPage';
import ToneCurvePage from './pages/subpages/ToneCurvePage';
import HistogramPage from './pages/subpages/HistogramPage';
import FavoritesPage from './pages/subpages/FavoritesPage';
import Trend2026Page from './pages/subpages/Trend2026Page';
import SceneDetailPage from './pages/subpages/SceneDetailPage';

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
      case 'theme-settings':
        return <ThemeSettingsPage />;
      case 'dark-mode':
        return <DarkModePage />;
      case 'notification':
        return <NotificationPage />;
      case 'privacy':
        return <PrivacyPage />;
      case 'hsl-adjustment':
        return <HSLAdjustmentPage />;
      case 'batch-processing':
        return <BatchProcessingPage />;
      case 'raw-processing':
        return <RAWProcessingPage />;
      case 'tone-curve':
        return <ToneCurvePage />;
      case 'histogram':
        return <HistogramPage />;
      case 'favorites':
        return <FavoritesPage />;
      case 'trend-2026':
        return <Trend2026Page />;
      case 'scene-detail':
        return <SceneDetailPage />;
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
