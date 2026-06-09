import React, { useState, useMemo } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Camera, Zap, Check, Target, Wand2, Layers,
  Sun, Moon, Mountain, Users, Utensils, Building,
  Flower, Sparkles, Leaf, Coffee, Eye,
  ChevronRight, X, Image, Download, Share2, RefreshCw,
  Aperture, Plus, Minus, Sliders, CheckCircle,
  Heart, Bookmark, Copy, QrCode, Film, Trash2, Search,
  Library, Star, TrendingUp, Clock
} from 'lucide-react';
import {
  SCENE_PROFILES,
  FILM_PRESETS,
  SceneCategoryInfo,
  HasselbladParams,
  SoftLightMode,
  SoftLightModeInfo,
  FilmSeriesInfo,
  SceneProfile,
  FilmPreset,
  RecipeProfile,
  HASSELBLAD_ORANGE
} from '../../lib/hasselbladModels';

// 模拟配方数据
const MOCK_RECIPES: RecipeProfile[] = [
  {
    id: 'recipe_1',
    name: '日落风光大片',
    description: '适合日落时分拍摄，暖色调浓郁',
    author: { name: '摄影大师' },
    scene: { id: 'landscape-sunset', displayName: '日落', category: '风景', icon: '🌅' },
    film: { id: 'rdp3', displayName: 'RDP3', series: '情绪与表达', matchScore: 0.93 },
    hasselbladParams: { tone: -5, saturation: 25, contrast: 10, colorTemp: 20, sharpness: 12, vignette: 0, cyanMagenta: 5, softLight: SoftLightMode.NONE },
    masterTips: ['增强暖色调表现', '寻找有层次的天空', '注意曝光控制'],
    createdAt: Date.now() - 100000,
    updatedAt: Date.now() - 100000,
    usageCount: 15,
    isFavorite: true,
    tags: ['日落', '风景', '暖调']
  },
  {
    id: 'recipe_2',
    name: '人像柔美风格',
    description: '适合人像拍摄，柔美肤色',
    author: { name: '用户' },
    scene: { id: 'portrait-standard', displayName: '标准人像', category: '人像', icon: '👤' },
    film: { id: 'portra', displayName: 'Portra 400', series: '情绪与表达', matchScore: 0.85 },
    hasselbladParams: { tone: -3, saturation: 10, contrast: -15, colorTemp: -5, sharpness: -15, vignette: 20, cyanMagenta: -5, softLight: SoftLightMode.SOFT },
    masterTips: ['使用柔光模式营造自然肤色', '降低对比度保持皮肤质感'],
    createdAt: Date.now() - 200000,
    updatedAt: Date.now() - 200000,
    usageCount: 8,
    isFavorite: false,
    tags: ['人像', '柔美', '肤色']
  },
  {
    id: 'recipe_3',
    name: '城市夜景霓虹',
    description: '适合城市夜景拍摄，霓虹灯效果',
    author: { name: '街拍达人' },
    scene: { id: 'night-neon', displayName: '霓虹灯', category: '夜景', icon: '🌃' },
    film: { id: '800t', displayName: '800T', series: '结构与时间', matchScore: 0.95 },
    hasselbladParams: { tone: 0, saturation: 20, contrast: 15, colorTemp: -10, sharpness: 10, vignette: 5, cyanMagenta: 3, softLight: SoftLightMode.SOFT },
    masterTips: ['增强色彩饱和度', '柔光营造梦幻感'],
    createdAt: Date.now() - 300000,
    updatedAt: Date.now() - 300000,
    usageCount: 12,
    isFavorite: true,
    tags: ['夜景', '霓虹', '城市']
  }
];

const RecipeManagementPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();

  const [recipes, setRecipes] = useState<RecipeProfile[]>(MOCK_RECIPES);
  const [selectedTab, setSelectedTab] = useState<'all' | 'favorites' | 'frequent'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [showImportDialog, setShowImportDialog] = useState(false);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeProfile | null>(null);
  const [importCode, setImportCode] = useState('');

  // 统计信息
  const stats = useMemo(() => ({
    total: recipes.length,
    favorites: recipes.filter(r => r.isFavorite).length,
    totalUsage: recipes.reduce((sum, r) => sum + r.usageCount, 0),
    mostUsed: recipes.sort((a, b) => b.usageCount - a.usageCount)[0]
  }), [recipes]);

  // 过滤配方
  const filteredRecipes = useMemo(() => {
    let list = recipes;

    // 搜索过滤
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      list = list.filter(r =>
        r.name.toLowerCase().includes(query) ||
        r.scene.displayName.toLowerCase().includes(query) ||
        r.film.displayName.toLowerCase().includes(query) ||
        r.tags.some(t => t.toLowerCase().includes(query))
      );
    }

    // 分类过滤
    switch (selectedTab) {
      case 'favorites':
        list = list.filter(r => r.isFavorite);
        break;
      case 'frequent':
        list = list.sort((a, b) => b.usageCount - a.usageCount);
        break;
    }

    return list;
  }, [recipes, searchQuery, selectedTab]);

  // 收藏/取消收藏
  const toggleFavorite = (recipeId: string) => {
    setRecipes(prev => prev.map(r =>
      r.id === recipeId ? { ...r, isFavorite: !r.isFavorite } : r
    ));
  };

  // 删除配方
  const deleteRecipe = (recipeId: string) => {
    setRecipes(prev => prev.filter(r => r.id !== recipeId));
  };

  // 使用配方
  const useRecipe = (recipeId: string) => {
    setRecipes(prev => prev.map(r =>
      r.id === recipeId ? { ...r, usageCount: r.usageCount + 1 } : r
    ));
  };

  // 分享配方
  const shareRecipe = (recipe: RecipeProfile) => {
    setSelectedRecipe(recipe);
    setShowShareDialog(true);
  };

  // 查看详情
  const viewDetail = (recipe: RecipeProfile) => {
    setSelectedRecipe(recipe);
    setShowDetailDialog(true);
  };

  // 导入配方
  const handleImport = () => {
    if (importCode.trim()) {
      // 模拟导入成功
      setShowImportDialog(false);
      setImportCode('');
    }
  };

  return (
    <div className="h-full w-full bg-[#0a0a0a] flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-[#0a0a0a] border-b border-white/5 px-4 py-3 flex items-center gap-3">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <div className="flex items-center gap-2">
          <Film size={20} className="text-orange-500" />
          <h1 className="text-lg font-semibold text-white">哈苏配方库</h1>
        </div>
        <div className="flex-1" />
        <button
          onClick={() => setShowImportDialog(true)}
          className="p-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <Download size={20} className="text-orange-500" />
        </button>
        <button
          onClick={() => setShowImportDialog(true)}
          className="p-2 hover:bg-white/10 rounded-full transition-colors"
        >
          <Plus size={20} className="text-orange-500" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* 统计卡片 */}
        <div className="bg-white/5 rounded-2xl p-4 border border-white/10">
          <div className="flex justify-around">
            <div className="text-center">
              <span className="text-2xl">📚</span>
              <p className="text-orange-500 font-bold mt-1">{stats.total}</p>
              <p className="text-white/50 text-xs">配方总数</p>
            </div>
            <div className="text-center">
              <span className="text-2xl">⭐</span>
              <p className="text-orange-500 font-bold mt-1">{stats.favorites}</p>
              <p className="text-white/50 text-xs">收藏数</p>
            </div>
            <div className="text-center">
              <span className="text-2xl">🎯</span>
              <p className="text-orange-500 font-bold mt-1">{stats.totalUsage}</p>
              <p className="text-white/50 text-xs">使用次数</p>
            </div>
          </div>
        </div>

        {/* 搜索框 */}
        <div className="relative">
          <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/30" />
          <input
            type="text"
            placeholder="搜索配方..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white text-sm placeholder:text-white/30 focus:outline-none focus:border-orange-500/50"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-white/30"
            >
              <X size={18} />
            </button>
          )}
        </div>

        {/* 分类标签 */}
        <div className="flex gap-2">
          <button
            onClick={() => setSelectedTab('all')}
            className={`px-4 py-2 rounded-lg text-sm transition-all ${
              selectedTab === 'all'
                ? 'bg-orange-500/20 text-orange-400 border border-orange-500/50'
                : 'bg-white/5 text-white/50'
            }`}
          >
            全部 ({recipes.length})
          </button>
          <button
            onClick={() => setSelectedTab('favorites')}
            className={`px-4 py-2 rounded-lg text-sm transition-all ${
              selectedTab === 'favorites'
                ? 'bg-orange-500/20 text-orange-400 border border-orange-500/50'
                : 'bg-white/5 text-white/50'
            }`}
          >
            收藏 ({stats.favorites})
          </button>
          <button
            onClick={() => setSelectedTab('frequent')}
            className={`px-4 py-2 rounded-lg text-sm transition-all ${
              selectedTab === 'frequent'
                ? 'bg-orange-500/20 text-orange-400 border border-orange-500/50'
                : 'bg-white/5 text-white/50'
            }`}
          >
            常用
          </button>
        </div>

        {/* 配方列表 */}
        {filteredRecipes.length === 0 ? (
          <div className="text-center py-12">
            <Library size={48} className="mx-auto text-white/20 mb-4" />
            <p className="text-white/50">暂无配方</p>
            <p className="text-white/30 text-xs mt-2">点击右上角 + 创建你的第一个哈苏配方</p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredRecipes.map(recipe => (
              <div
                key={recipe.id}
                onClick={() => viewDetail(recipe)}
                className="bg-white/5 rounded-2xl p-4 border border-white/10 hover:border-orange-500/30 transition-all cursor-pointer"
              >
                {/* 标题行 */}
                <div className="flex items-center gap-3 mb-3">
                  <span className="text-2xl">{recipe.scene.icon}</span>
                  <div className="flex-1">
                    <h3 className="text-white font-medium">{recipe.name}</h3>
                    <p className="text-white/50 text-xs">{recipe.description}</p>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavorite(recipe.id);
                    }}
                    className="p-2"
                  >
                    <Heart
                      size={18}
                      className={recipe.isFavorite ? 'text-orange-500 fill-orange-500' : 'text-white/30'}
                    />
                  </button>
                </div>

                {/* 场景和胶片 */}
                <div className="flex gap-2 mb-3">
                  <span className="px-2 py-1 bg-white/10 rounded text-white/60 text-xs">
                    {recipe.scene.displayName}
                  </span>
                  <span className="px-2 py-1 bg-orange-500/20 rounded text-orange-400 text-xs">
                    🎞️ {recipe.film.displayName}
                  </span>
                </div>

                {/* 参数摘要 */}
                <div className="flex gap-2 overflow-hidden">
                  {Object.entries(recipe.hasselbladParams)
                    .filter(([key, value]) => key !== 'softLight' && (value as number) !== 0)
                    .slice(0, 4)
                    .map(([key, value]) => (
                      <span key={key} className="px-2 py-1 bg-white/10 rounded text-orange-400 text-xs">
                        {key}: {(value as number) > 0 ? `+${value}` : value}
                      </span>
                    ))}
                </div>

                {/* 底部信息 */}
                <div className="flex items-center justify-between mt-3 pt-3 border-t border-white/5">
                  <span className="text-white/40 text-xs">使用 {recipe.usageCount} 次</span>
                  <div className="flex gap-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        shareRecipe(recipe);
                      }}
                      className="p-1 text-white/40 hover:text-orange-500"
                    >
                      <Share2 size={16} />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteRecipe(recipe.id);
                      }}
                      className="p-1 text-white/40 hover:text-red-500"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 底部间距 */}
        <div className="h-20" />
      </div>

      {/* 分享弹窗 */}
      {showShareDialog && selectedRecipe && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
          <div className="w-[90%] max-w-sm bg-[#0a0a0a] rounded-2xl p-5 border border-white/10">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-white font-bold">分享配方</h3>
              <button onClick={() => setShowShareDialog(false)} className="text-white/50">
                <X size={20} />
              </button>
            </div>

            {/* 二维码区域 */}
            <div className="flex justify-center mb-4">
              <div className="w-48 h-48 bg-white rounded-xl flex items-center justify-center">
                <QrCode size={120} className="text-orange-500" />
              </div>
            </div>

            <p className="text-white/50 text-xs text-center mb-4">扫描二维码或复制配方代码</p>

            {/* 配方代码 */}
            <div className="bg-white/5 rounded-xl p-3 mb-4">
              <p className="text-white/30 text-xs truncate">
                recipe_{selectedRecipe.id}_code_{Date.now()}
              </p>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => setShowShareDialog(false)}
                className="flex-1 py-3 bg-white/10 text-white rounded-xl"
              >
                关闭
              </button>
              <button
                onClick={() => {
                  navigator.clipboard.writeText(`recipe_${selectedRecipe.id}_code_${Date.now()}`);
                  setShowShareDialog(false);
                }}
                className="flex-1 py-3 bg-orange-500 text-white rounded-xl flex items-center justify-center gap-2"
              >
                <Copy size={18} />
                复制代码
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 导入弹窗 */}
      {showImportDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
          <div className="w-[90%] max-w-sm bg-[#0a0a0a] rounded-2xl p-5 border border-white/10">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-white font-bold">导入配方</h3>
              <button onClick={() => setShowImportDialog(false)} className="text-white/50">
                <X size={20} />
              </button>
            </div>

            <textarea
              placeholder="粘贴配方代码..."
              value={importCode}
              onChange={(e) => setImportCode(e.target.value)}
              className="w-full h-20 p-3 bg-white/5 border border-white/10 rounded-xl text-white text-sm placeholder:text-white/30 resize-none focus:outline-none focus:border-orange-500/50"
            />

            <div className="flex gap-3 mt-4">
              <button
                onClick={() => setShowImportDialog(false)}
                className="flex-1 py-3 bg-white/10 text-white rounded-xl"
              >
                取消
              </button>
              <button
                onClick={handleImport}
                className="flex-1 py-3 bg-orange-500 text-white rounded-xl flex items-center justify-center gap-2"
              >
                <Download size={18} />
                导入
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 详情弹窗 */}
      {showDetailDialog && selectedRecipe && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm">
          <div className="w-[90%] max-w-sm bg-[#0a0a0a] rounded-2xl p-5 border border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{selectedRecipe.scene.icon}</span>
                <h3 className="text-white font-bold">{selectedRecipe.name}</h3>
              </div>
              <button onClick={() => setShowDetailDialog(false)} className="text-white/50">
                <X size={20} />
              </button>
            </div>

            <p className="text-white/50 text-sm mb-4">{selectedRecipe.description}</p>

            {/* 场景和胶片 */}
            <div className="flex gap-2 mb-4">
              <span className="px-3 py-2 bg-white/10 rounded-lg text-white/60 text-sm">
                场景: {selectedRecipe.scene.displayName}
              </span>
              <span className="px-3 py-2 bg-orange-500/20 rounded-lg text-orange-400 text-sm">
                胶片: {selectedRecipe.film.displayName} ({(selectedRecipe.film.matchScore * 100).toFixed(0)}%)
              </span>
            </div>

            {/* 哈苏参数 */}
            <div className="bg-white/5 rounded-xl p-3 mb-4">
              <h4 className="text-orange-500 text-sm font-medium mb-2">哈苏大师参数</h4>
              <div className="grid grid-cols-2 gap-2">
                {Object.entries(selectedRecipe.hasselbladParams)
                  .filter(([key]) => key !== 'softLight')
                  .map(([key, value]) => {
                    const numValue = value as number;
                    if (numValue === 0) return null;
                    const labelMap: Record<string, string> = {
                      tone: '影调', saturation: '饱和度', contrast: '对比度',
                      colorTemp: '色温', sharpness: '锐度', vignette: '暗角', cyanMagenta: '青品调'
                    };
                    return (
                      <div key={key} className="flex justify-between text-xs">
                        <span className="text-white/50">{labelMap[key]}</span>
                        <span className="text-orange-400">{numValue > 0 ? `+${numValue}` : numValue}</span>
                      </div>
                    );
                  })}
              </div>
            </div>

            {/* 大师建议 */}
            {selectedRecipe.masterTips.length > 0 && (
              <div className="mb-4">
                <h4 className="text-orange-500 text-sm font-medium mb-2">大师建议</h4>
                {selectedRecipe.masterTips.map((tip, i) => (
                  <div key={i} className="flex items-start gap-2 py-1">
                    <span className="text-orange-400">💡</span>
                    <p className="text-white/60 text-xs">{tip}</p>
                  </div>
                ))}
              </div>
            )}

            <div className="flex gap-3">
              <button
                onClick={() => setShowDetailDialog(false)}
                className="flex-1 py-3 bg-white/10 text-white rounded-xl"
              >
                关闭
              </button>
              <button
                onClick={() => {
                  useRecipe(selectedRecipe.id);
                  setShowDetailDialog(false);
                }}
                className="flex-1 py-3 bg-orange-500 text-white rounded-xl flex items-center justify-center gap-2"
              >
                <Check size={18} />
                应用配方
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RecipeManagementPage;