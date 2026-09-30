/**
 * Smart Clean AI - Main Application Controller
 * Handles Navigation, Empty Folders, dHash, Code Studio, View Rendering, State Reactivity, Modals, Audio & Localization
 */

class SmartCleanApp {
  constructor() {
    this.currentLang = 'ar';
    this.currentTheme = 'dark';
    this.isMobileSimulator = false;
    this.radarController = null;
    this.activeTab = 'dashboard';
    this.activePhotoSubtab = 'duplicates';
    this.activeLargeFileFilter = 'all';
    this.activeCodeSnippetId = 'empty_scanner';

    this.init();
  }

  init() {
    this.bindEvents();
    this.renderAll();
    this.applyLocalization();
    
    // Subscribe to store updates
    window.cleanerStore.subscribe(() => {
      this.renderDashboardStats();
      this.renderQuickCategories();
      this.renderEmptyFolders();
      this.renderRamStats();
      this.renderDuplicatePhotos();
      this.renderLargeFiles();
      this.renderRecycleBin();
      this.renderSocialStats();
      this.refreshLucideIcons();
    });

    this.refreshLucideIcons();
  }

  refreshLucideIcons() {
    if (window.lucide) {
      window.lucide.createIcons();
    }
  }

  bindEvents() {
    // Navigation tabs
    document.querySelectorAll('.nav-tab').forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        if (tab) {
          if (window.soundEngine) window.soundEngine.playClick();
          this.switchTab(tab);
        }
      });
    });

    // Goto tab shortcut buttons
    document.querySelectorAll('.btn-goto-tab').forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        if (tab) {
          if (window.soundEngine) window.soundEngine.playClick();
          this.switchTab(tab);
        }
      });
    });

    // Photo Subtabs
    document.querySelectorAll('.photo-subtab').forEach(btn => {
      btn.addEventListener('click', () => {
        const subtab = btn.dataset.subtab;
        if (subtab) {
          if (window.soundEngine) window.soundEngine.playClick();
          this.switchPhotoSubtab(subtab);
        }
      });
    });

    // Large file filters
    document.querySelectorAll('.largefile-filter').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.largefile-filter').forEach(b => {
          b.classList.remove('bg-sky-500', 'text-white', 'active');
          b.classList.add('bg-slate-800', 'text-slate-400');
        });
        btn.classList.add('bg-sky-500', 'text-white', 'active');
        btn.classList.remove('bg-slate-800', 'text-slate-400');
        this.activeLargeFileFilter = btn.dataset.filter;
        this.renderLargeFiles();
      });
    });

    // Search large files
    const largeFileSearch = document.getElementById('largeFileSearchInput');
    if (largeFileSearch) {
      largeFileSearch.addEventListener('input', (e) => {
        this.renderLargeFiles(e.target.value.toLowerCase());
      });
    }

    // Toggle Mobile / Fullscreen simulator
    const btnToggleSim = document.getElementById('btnToggleSimulator');
    if (btnToggleSim) {
      btnToggleSim.addEventListener('click', () => {
        this.toggleSimulatorMode();
      });
    }

    // Toggle Sound FX
    const btnToggleSound = document.getElementById('btnToggleSound');
    if (btnToggleSound) {
      btnToggleSound.addEventListener('click', () => {
        const enabled = window.soundEngine.toggleSound();
        const icon = document.getElementById('soundIcon');
        if (icon) {
          icon.setAttribute('data-lucide', enabled ? 'volume-2' : 'volume-x');
          btnToggleSound.className = enabled 
            ? 'p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-emerald-400 border border-white/10 transition'
            : 'p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-500 border border-white/10 transition';
          this.refreshLucideIcons();
        }
        this.showToast(enabled ? 'تم تفعيل المؤثرات الصوتية' : 'تم كتم الصوت');
      });
    }

    // Toggle Theme
    const btnToggleTheme = document.getElementById('btnToggleTheme');
    if (btnToggleTheme) {
      btnToggleTheme.addEventListener('click', () => {
        this.toggleTheme();
      });
    }

    // Toggle Language
    const btnToggleLang = document.getElementById('btnToggleLang');
    if (btnToggleLang) {
      btnToggleLang.addEventListener('click', () => {
        this.toggleLanguage();
      });
    }

    // 1-Click Clean Trigger
    const btnQuickClean = document.getElementById('btnQuickCleanPrimary');
    if (btnQuickClean) {
      btnQuickClean.addEventListener('click', () => {
        this.executeQuickClean();
      });
    }

    // Delete empty folders action
    const btnDeleteEmptyFolders = document.getElementById('btnDeleteEmptyFoldersAction');
    if (btnDeleteEmptyFolders) {
      btnDeleteEmptyFolders.addEventListener('click', () => {
        const count = window.cleanerStore.deleteSelectedEmptyFolders();
        if (count > 0) {
          if (window.soundEngine) window.soundEngine.playCleanLaser();
          this.showToast(`تم حذف ${count} مجلدات فارغة بنجاح!`);
        } else {
          this.showToast('لا توجد مجلدات فارغة محددة للحذف');
        }
      });
    }

    // Select all categories
    const btnSelectAllCat = document.getElementById('btnSelectAllCategories');
    if (btnSelectAllCat) {
      btnSelectAllCat.addEventListener('click', () => {
        const state = window.cleanerStore.state;
        const allSelected = state.quickScanCategories.every(c => c.selected);
        state.quickScanCategories.forEach(c => c.selected = !allSelected);
        window.cleanerStore.notify();
      });
    }

    // Deep scan trigger
    const btnDeepScan = document.getElementById('btnStartDeepScan');
    if (btnDeepScan) {
      btnDeepScan.addEventListener('click', () => {
        this.executeDeepScan();
      });
    }

    // Execute RAM Boost
    const btnBoostRam = document.getElementById('btnExecuteRamBoost');
    if (btnBoostRam) {
      btnBoostRam.addEventListener('click', () => {
        this.executeRamBoost();
      });
    }

    // Delete selected duplicates
    const btnDeleteDups = document.getElementById('btnDeleteDuplicatesAction');
    if (btnDeleteDups) {
      btnDeleteDups.addEventListener('click', () => {
        const freedMB = window.cleanerStore.deleteSelectedDuplicates();
        if (freedMB > 0) {
          if (window.soundEngine) window.soundEngine.playCleanLaser();
          this.showToast(`تم حذف النسخ المكررة وتوفير ${freedMB.toFixed(1)} ميجابايت بنجاح!`);
        } else {
          this.showToast('لا توجد عناصر مكررة محددة للحذف');
        }
      });
    }

    // Delete selected large files
    const btnDeleteLarge = document.getElementById('btnDeleteSelectedLargeFiles');
    if (btnDeleteLarge) {
      btnDeleteLarge.addEventListener('click', () => {
        const selectedIds = window.cleanerStore.state.largeFiles.filter(f => f.selected).map(f => f.id);
        if (selectedIds.length === 0) {
          this.showToast('يرجى تحديد ملف واحد على الأقل للحذف');
          return;
        }
        const freedMB = window.cleanerStore.deleteSelectedLargeFiles(selectedIds);
        if (window.soundEngine) window.soundEngine.playCleanLaser();
        this.showToast(`تم نقل ${selectedIds.length} ملفات إلى سلة المحذوفات (${freedMB} MB)`);
      });
    }

    // Clean WhatsApp voice notes
    const btnCleanWA = document.getElementById('btnCleanWhatsAppVoice');
    if (btnCleanWA) {
      btnCleanWA.addEventListener('click', () => {
        const freedMB = window.cleanerStore.cleanWhatsAppVoiceNotes();
        if (window.soundEngine) window.soundEngine.playCleanLaser();
        this.showToast(`تم تنظيف الرسائل الصوتية القديمة وتوفير ${(freedMB/1024).toFixed(1)} جيجابايت!`);
      });
    }

    // Empty Recycle Bin
    const btnEmptyBin = document.getElementById('btnEmptyBinAction');
    if (btnEmptyBin) {
      btnEmptyBin.addEventListener('click', () => {
        window.cleanerStore.emptyRecycleBin();
        if (window.soundEngine) window.soundEngine.playCleanLaser();
        this.showToast('تم إفراغ سلة المحذوفات نهائياً');
      });
    }

    // Copy Code Button
    const btnCopyCode = document.getElementById('btnCopyActiveCode');
    if (btnCopyCode) {
      btnCopyCode.addEventListener('click', () => {
        const codePre = document.getElementById('codeSnippetBody');
        if (codePre && codePre.innerText) {
          navigator.clipboard.writeText(codePre.innerText).then(() => {
            if (window.soundEngine) window.soundEngine.playSuccessChime();
            this.showToast(translations[this.currentLang].codeCopied || 'تم نسخ الكود البرمجي بنجاح!');
          });
        }
      });
    }

    // Close success modal
    const btnCloseSuccess = document.getElementById('btnCloseSuccessModal');
    if (btnCloseSuccess) {
      btnCloseSuccess.addEventListener('click', () => {
        document.getElementById('cleanSuccessModal').classList.add('hidden');
        this.switchTab('dashboard');
      });
    }

    // Close preview modal
    const btnClosePreview = document.getElementById('btnClosePreviewModal');
    if (btnClosePreview) {
      btnClosePreview.addEventListener('click', () => {
        document.getElementById('mediaPreviewModal').classList.add('hidden');
      });
    }

    // Image Compressor Bindings
    this.bindCompressorEvents();

    // Real File Scanner Bindings
    this.bindRealScannerEvents();
  }

  // Switch between tabs
  switchTab(tabId) {
    this.activeTab = tabId;

    document.querySelectorAll('.nav-tab').forEach(b => {
      if (b.dataset.tab === tabId) {
        b.classList.add('active', 'text-sky-400');
        b.classList.remove('text-slate-300', 'text-slate-400');
      } else {
        b.classList.remove('active', 'text-sky-400');
        b.classList.add('text-slate-300');
      }
    });

    document.querySelectorAll('.tab-pane').forEach(p => {
      p.classList.add('hidden');
      p.classList.remove('block');
    });

    const activePane = document.getElementById(`tab-${tabId}`);
    if (activePane) {
      activePane.classList.remove('hidden');
      activePane.classList.add('block');
    }

    if (tabId === 'deepScan') {
      if (!this.radarController) {
        this.radarController = window.scannerEngine.initRadarCanvas('deepScanRadarCanvas');
      }
    }

    if (tabId === 'codeStudio') {
      this.renderCodeStudio();
    }

    this.refreshLucideIcons();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // Switch photo subtabs
  switchPhotoSubtab(subtabId) {
    this.activePhotoSubtab = subtabId;
    document.querySelectorAll('.photo-subtab').forEach(b => {
      if (b.dataset.subtab === subtabId) {
        b.className = 'photo-subtab active px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-sky-500/20 text-sky-400 border border-sky-500/30 transition';
      } else {
        b.className = 'photo-subtab px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-slate-800 text-slate-400 hover:text-white border border-transparent transition';
      }
    });

    document.querySelectorAll('.photo-pane').forEach(p => {
      p.classList.add('hidden');
      p.classList.remove('block');
    });

    const pane = document.getElementById(`subpane-${subtabId}`);
    if (pane) {
      pane.classList.remove('hidden');
      pane.classList.add('block');
    }
    this.refreshLucideIcons();
  }

  // Toggle phone simulator frame
  toggleSimulatorMode() {
    this.isMobileSimulator = !this.isMobileSimulator;
    const container = document.getElementById('appContainer');
    const notch = document.getElementById('phoneNotch');
    
    if (this.isMobileSimulator) {
      container.classList.add('phone-frame-active');
      notch.classList.remove('hidden');
      this.showToast('تم تفعيل وضع محاكي الهاتف الذكي');
    } else {
      container.classList.remove('phone-frame-active');
      notch.classList.add('hidden');
      this.showToast('تم العودة لوضع العرض الكامل');
    }
  }

  // Toggle Theme
  toggleTheme() {
    const html = document.documentElement;
    const themeIcon = document.getElementById('themeIcon');
    if (this.currentTheme === 'dark') {
      this.currentTheme = 'light';
      html.classList.remove('dark');
      html.setAttribute('data-theme', 'light');
      if (themeIcon) themeIcon.setAttribute('data-lucide', 'sun');
    } else if (this.currentTheme === 'light') {
      this.currentTheme = 'oled';
      html.classList.add('dark');
      html.setAttribute('data-theme', 'oled');
      if (themeIcon) themeIcon.setAttribute('data-lucide', 'moon-star');
    } else {
      this.currentTheme = 'dark';
      html.classList.add('dark');
      html.removeAttribute('data-theme');
      if (themeIcon) themeIcon.setAttribute('data-lucide', 'moon');
    }
    this.refreshLucideIcons();
  }

  // Toggle Language
  toggleLanguage() {
    this.currentLang = this.currentLang === 'ar' ? 'en' : 'ar';
    const html = document.documentElement;
    const label = document.getElementById('langLabel');
    if (this.currentLang === 'en') {
      html.setAttribute('lang', 'en');
      html.setAttribute('dir', 'ltr');
      if (label) label.textContent = 'العربية';
    } else {
      html.setAttribute('lang', 'ar');
      html.setAttribute('dir', 'rtl');
      if (label) label.textContent = 'English';
    }
    this.applyLocalization();
    this.renderAll();
  }

  applyLocalization() {
    const dict = translations[this.currentLang];
    if (!dict) return;

    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.getAttribute('data-i18n');
      if (dict[key]) {
        el.textContent = dict[key];
      }
    });
  }

  // Renders
  renderAll() {
    this.renderDashboardStats();
    this.renderQuickCategories();
    this.renderEmptyFolders();
    this.renderRamStats();
    this.renderDuplicatePhotos();
    this.renderBlurryPhotos();
    this.renderScreenshots();
    this.renderLargeFiles();
    this.renderSocialStats();
    this.renderRecycleBin();
    this.renderWhitelist();
    this.renderCodeStudio();
    this.refreshLucideIcons();
  }

  renderDashboardStats() {
    const state = window.cleanerStore.state;
    const pct = Math.round((state.usedCapacityGB / state.totalCapacityGB) * 100);

    // Header pills
    document.getElementById('headerUsedStorage').textContent = state.usedCapacityGB.toFixed(1);
    document.getElementById('headerTotalStorage').textContent = state.totalCapacityGB.toFixed(0);
    document.getElementById('headerRamUsage').textContent = Math.round((state.ramUsedGB / state.ramTotalGB) * 100);
    document.getElementById('headerTemp').textContent = state.batteryTempC.toFixed(1);
    document.getElementById('headerBattery').textContent = `${state.batteryLevel}%`;

    // Circular gauge
    const ring = document.getElementById('storageRingGauge');
    if (ring) {
      ring.setAttribute('stroke-dasharray', `${pct}, 100`);
      if (pct > 85) {
        ring.setAttribute('class', 'circle-progress stroke-rose-500');
      } else if (pct > 70) {
        ring.setAttribute('class', 'circle-progress stroke-amber-400');
      } else {
        ring.setAttribute('class', 'circle-progress stroke-emerald-400');
      }
    }
    document.getElementById('storagePercentText').textContent = `${pct}%`;
    document.getElementById('storageUsedGB').textContent = state.usedCapacityGB.toFixed(1);
    document.getElementById('storageTotalGB').textContent = state.totalCapacityGB.toFixed(0);
    document.getElementById('freeStorageGB').textContent = state.freeCapacityGB.toFixed(1);
    document.getElementById('healthScoreValue').textContent = state.healthScore;

    // Badges
    const emptyBadge = document.getElementById('dashEmptyCountBadge');
    if (emptyBadge) emptyBadge.textContent = `${state.emptyFolders.length} مجلد`;

    // Health badge
    const healthText = document.getElementById('healthStatusText');
    const dict = translations[this.currentLang];
    if (state.healthScore > 85) {
      healthText.className = 'text-xl font-black text-emerald-400 flex items-center gap-2';
      healthText.innerHTML = `<i data-lucide="shield-check" class="w-5 h-5"></i> <span>${dict.healthStatusGood}</span>`;
    } else if (state.healthScore > 60) {
      healthText.className = 'text-xl font-black text-amber-400 flex items-center gap-2';
      healthText.innerHTML = `<i data-lucide="alert-triangle" class="w-5 h-5"></i> <span>${dict.healthStatusWarning}</span>`;
    } else {
      healthText.className = 'text-xl font-black text-rose-400 flex items-center gap-2';
      healthText.innerHTML = `<i data-lucide="alert-octagon" class="w-5 h-5"></i> <span>${dict.healthStatusCritical}</span>`;
    }

    // Mini cards
    const ramPct = Math.round((state.ramUsedGB / state.ramTotalGB) * 100);
    const miniRamP = document.getElementById('miniRamPercent');
    if (miniRamP) miniRamP.textContent = ramPct;

    // Total cleanable badge
    const cleanableMB = state.quickScanCategories.filter(c => !c.cleaned).reduce((sum, c) => sum + c.sizeMB, 0);
    const cleanableGB = (cleanableMB / 1024).toFixed(1);
    document.getElementById('cleanableTotalBadge').textContent = `${cleanableGB} GB`;
  }

  renderEmptyFolders() {
    const container = document.getElementById('emptyFoldersListContainer');
    if (!container) return;
    const state = window.cleanerStore.state;

    if (state.emptyFolders.length === 0) {
      container.innerHTML = `
        <div class="text-center py-10 glass-panel border border-white/10 rounded-2xl">
          <i data-lucide="check-circle" class="w-10 h-10 text-emerald-400 mx-auto mb-2"></i>
          <h4 class="text-base font-bold text-white">لا توجد مجلدات فارغة!</h4>
          <p class="text-xs text-slate-400">ذاكرة التخزين منظمة ومفهرسة بنقاء تام.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = state.emptyFolders.map(f => `
      <div class="flex items-center justify-between bg-slate-900/80 p-3.5 rounded-xl border border-white/5 hover:border-amber-500/20 transition">
        <div class="flex items-center gap-3 min-w-0">
          <input type="checkbox" ${f.selected ? 'checked' : ''} data-folder-id="${f.id}" class="empty-folder-cb w-4 h-4 accent-amber-500 rounded cursor-pointer">
          <div class="p-2 rounded-lg bg-amber-500/10 text-amber-400 shrink-0">
            <i data-lucide="folder-open" class="w-4 h-4"></i>
          </div>
          <div class="min-w-0">
            <h5 class="text-xs font-bold text-white truncate">${f.name}</h5>
            <div class="text-[10px] text-slate-400 font-mono truncate">${f.path}</div>
          </div>
        </div>
        <span class="text-[10px] font-bold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full">0 B (فارغ)</span>
      </div>
    `).join('');

    container.querySelectorAll('.empty-folder-cb').forEach(cb => {
      cb.addEventListener('change', () => {
        const fId = cb.dataset.folderId;
        const target = state.emptyFolders.find(item => item.id === fId);
        if (target) target.selected = cb.checked;
      });
    });
  }

  renderCodeStudio() {
    const tabsContainer = document.getElementById('codeSnippetsTabs');
    const snippetPre = document.getElementById('codeSnippetBody');
    const fileNameEl = document.getElementById('codeFileNameDisplay');
    if (!tabsContainer || !snippetPre) return;

    const catalog = window.cleanerStore.state.codeCatalog;
    tabsContainer.innerHTML = catalog.map(item => `
      <button class="code-tab-btn ${item.id === this.activeCodeSnippetId ? 'bg-amber-500 text-slate-950 font-bold' : 'bg-slate-800 text-slate-300'} px-3 py-1.5 rounded-xl text-xs transition" data-code-id="${item.id}">
        ${this.currentLang === 'ar' ? item.titleAr : item.titleEn}
      </button>
    `).join('');

    const activeItem = catalog.find(i => i.id === this.activeCodeSnippetId) || catalog[0];
    snippetPre.textContent = activeItem.code;
    if (fileNameEl) fileNameEl.textContent = activeItem.titleAr.split('(')[1]?.replace(')', '') || activeItem.id;

    tabsContainer.querySelectorAll('.code-tab-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.activeCodeSnippetId = btn.dataset.codeId;
        if (window.soundEngine) window.soundEngine.playClick();
        this.renderCodeStudio();
      });
    });
  }

  renderQuickCategories() {
    const list = document.getElementById('quickCategoriesList');
    if (!list) return;
    const state = window.cleanerStore.state;
    const dict = translations[this.currentLang];

    list.innerHTML = state.quickScanCategories.map(cat => {
      const isCleaned = cat.cleaned;
      const sizeFormatted = isCleaned ? '0 MB (تم التنظيف)' : (cat.sizeMB > 1024 ? (cat.sizeMB/1024).toFixed(1) + ' GB' : cat.sizeMB + ' MB');
      const title = dict[cat.key] || cat.key;
      const desc = dict[cat.descKey] || cat.descKey;

      return `
        <div class="bg-slate-900/70 border ${cat.selected ? 'border-sky-500/40 bg-sky-500/5' : 'border-white/5'} rounded-2xl p-4 flex items-start gap-3 transition">
          <input type="checkbox" ${cat.selected ? 'checked' : ''} ${isCleaned ? 'disabled' : ''} data-cat-id="${cat.id}" class="cat-checkbox mt-1 w-5 h-5 accent-sky-500 rounded cursor-pointer">
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2">
              <h4 class="text-sm font-bold text-white truncate ${isCleaned ? 'line-through text-slate-500' : ''}">${title}</h4>
              <span class="text-xs font-mono font-bold px-2 py-0.5 rounded-full ${isCleaned ? 'bg-emerald-500/20 text-emerald-400' : 'bg-sky-500/20 text-sky-300'}">${sizeFormatted}</span>
            </div>
            <p class="text-xs text-slate-400 mt-0.5">${desc}</p>
          </div>
        </div>
      `;
    }).join('');

    list.querySelectorAll('.cat-checkbox').forEach(cb => {
      cb.addEventListener('change', (e) => {
        const catId = cb.dataset.catId;
        const targetCat = state.quickScanCategories.find(c => c.id === catId);
        if (targetCat) {
          targetCat.selected = cb.checked;
          window.cleanerStore.notify();
        }
      });
    });
  }

  renderRamStats() {
    const state = window.cleanerStore.state;
    const pct = Math.round((state.ramUsedGB / state.ramTotalGB) * 100);
    const ring = document.getElementById('ramRingGauge');
    if (ring) {
      ring.setAttribute('stroke-dasharray', `${pct}, 100`);
    }
    const mainPercent = document.getElementById('ramMainPercent');
    if (mainPercent) mainPercent.textContent = `${pct}%`;

    const tasksContainer = document.getElementById('runningTasksList');
    const taskCountBadge = document.getElementById('runningTasksCount');
    if (taskCountBadge) taskCountBadge.textContent = state.runningTasks.length;

    if (tasksContainer) {
      if (state.runningTasks.length === 0) {
        tasksContainer.innerHTML = `
          <div class="text-center py-6 text-emerald-400 text-sm font-bold">
            <i data-lucide="check-circle" class="w-8 h-8 mx-auto mb-2"></i>
            تم تحرير كافة العمليات النشطة في الخلفية! الذاكرة في أعلى درجات الكفاءة.
          </div>
        `;
      } else {
        tasksContainer.innerHTML = state.runningTasks.map(t => `
          <div class="flex items-center justify-between bg-slate-900/80 p-3 rounded-xl border border-white/5 hover:border-amber-500/20 transition">
            <div class="flex items-center gap-3">
              <div class="p-2 rounded-lg bg-amber-500/10 text-amber-400">
                <i data-lucide="${t.icon || 'cpu'}" class="w-4 h-4"></i>
              </div>
              <div>
                <h5 class="text-xs font-bold text-white">${t.name}</h5>
                <span class="text-[10px] text-slate-400">${t.app} • استهلاك المعالج: ${t.cpuPercent}%</span>
              </div>
            </div>
            <div class="text-xs font-mono font-bold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-lg">
              ${t.ramMB} MB
            </div>
          </div>
        `).join('');
      }
    }
  }

  renderDuplicatePhotos() {
    const container = document.getElementById('duplicateGroupsContainer');
    if (!container) return;
    const state = window.cleanerStore.state;
    const dict = translations[this.currentLang];

    if (state.duplicateGroups.length === 0) {
      container.innerHTML = `
        <div class="text-center py-12 glass-panel border border-white/10 rounded-2xl">
          <i data-lucide="check-circle" class="w-12 h-12 text-emerald-400 mx-auto mb-2"></i>
          <h4 class="text-base font-bold text-white">معرض الصور نقي تماماً!</h4>
          <p class="text-xs text-slate-400">لا توجد صور مكررة متبقية في هاتفك.</p>
        </div>
      `;
      return;
    }

    container.innerHTML = state.duplicateGroups.map(grp => `
      <div class="glass-panel p-4 border border-white/10 rounded-2xl space-y-3">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="p-1.5 rounded-lg bg-purple-500/20 text-purple-400"><i data-lucide="fingerprint" class="w-4 h-4"></i></span>
            <div>
              <h4 class="text-sm font-bold text-white">${this.currentLang === 'ar' ? grp.titleAr : grp.titleEn}</h4>
              <span class="text-[10px] text-slate-400 font-mono">dHash: ${grp.dHashVal} (مسافة هامينغ: ${grp.hammingDistance})</span>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs font-mono font-bold text-purple-300 bg-purple-500/10 px-2 py-0.5 rounded-full border border-purple-500/20">تطابق ${grp.similarity}%</span>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          ${grp.photos.map(p => `
            <div class="relative bg-slate-900 rounded-xl overflow-hidden border ${p.isBest ? 'border-emerald-500/50' : 'border-rose-500/50'} p-2.5 flex gap-3">
              <div class="w-24 h-24 rounded-lg overflow-hidden shrink-0 bg-slate-950 relative">
                <img src="${p.previewUrl}" alt="${p.name}" class="w-full h-full object-cover">
                <button class="btn-preview-photo absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 flex items-center justify-center text-white transition" data-url="${p.previewUrl}" data-title="${p.name}">
                  <i data-lucide="zoom-in" class="w-5 h-5"></i>
                </button>
              </div>

              <div class="flex-1 min-w-0 flex flex-col justify-between">
                <div>
                  <div class="flex items-center justify-between">
                    <span class="text-[10px] font-bold px-2 py-0.5 rounded-md ${p.isBest ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'}">
                      ${p.isBest ? dict.bestShot : dict.duplicateMark}
                    </span>
                    <input type="checkbox" ${p.selected ? 'checked' : ''} data-photo-id="${p.id}" class="photo-dup-cb w-4 h-4 accent-purple-500 rounded cursor-pointer">
                  </div>
                  <h5 class="text-xs font-bold text-white truncate mt-1.5">${p.name}</h5>
                  <div class="text-[10px] text-slate-400 font-mono">${p.resolution} • ${p.size}</div>
                </div>
                <div class="text-[10px] text-slate-500">${p.date}</div>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `).join('');

    container.querySelectorAll('.photo-dup-cb').forEach(cb => {
      cb.addEventListener('change', () => {
        const pId = cb.dataset.photoId;
        state.duplicateGroups.forEach(g => {
          const found = g.photos.find(p => p.id === pId);
          if (found) found.selected = cb.checked;
        });
      });
    });

    container.querySelectorAll('.btn-preview-photo').forEach(btn => {
      btn.addEventListener('click', () => {
        this.openMediaPreview('image', btn.dataset.url, btn.dataset.title);
      });
    });
  }

  renderBlurryPhotos() {
    const container = document.getElementById('blurryPhotosContainer');
    if (!container) return;
    const state = window.cleanerStore.state;

    container.innerHTML = state.blurryPhotos.map(p => `
      <div class="glass-panel p-3 border border-white/10 rounded-2xl flex gap-3 bg-slate-900/80">
        <div class="w-20 h-20 rounded-xl overflow-hidden shrink-0 bg-slate-950">
          <img src="${p.previewUrl}" alt="${p.name}" class="w-full h-full object-cover blur-[1.5px]">
        </div>
        <div class="flex-1 min-w-0 flex flex-col justify-between">
          <div>
            <div class="flex items-center justify-between">
              <span class="text-[10px] font-bold text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full">${p.blurScore}</span>
              <input type="checkbox" checked class="w-4 h-4 accent-rose-500 rounded">
            </div>
            <h5 class="text-xs font-bold text-white truncate mt-1">${p.name}</h5>
            <span class="text-[10px] text-slate-400 font-mono">${p.size} • ${p.date}</span>
          </div>
        </div>
      </div>
    `).join('');
  }

  renderScreenshots() {
    const container = document.getElementById('screenshotsContainer');
    if (!container) return;
    const state = window.cleanerStore.state;

    container.innerHTML = state.screenshots.map(s => `
      <div class="glass-panel p-3 border border-white/10 rounded-2xl flex flex-col justify-between bg-slate-900/80 space-y-2">
        <div class="h-28 rounded-xl overflow-hidden bg-slate-950 relative">
          <img src="${s.previewUrl}" alt="${s.name}" class="w-full h-full object-cover">
          <span class="absolute top-2 start-2 text-[10px] font-bold text-amber-300 bg-black/70 backdrop-blur px-2 py-0.5 rounded-md">${s.age}</span>
        </div>
        <div>
          <div class="flex items-center justify-between">
            <h5 class="text-xs font-bold text-white truncate max-w-[150px]">${s.name}</h5>
            <input type="checkbox" checked class="w-4 h-4 accent-sky-500 rounded">
          </div>
          <span class="text-[10px] text-slate-400 font-mono">${s.size} • ${s.date}</span>
        </div>
      </div>
    `).join('');
  }

  renderLargeFiles(searchQuery = '') {
    const container = document.getElementById('largeFilesList');
    if (!container) return;
    const state = window.cleanerStore.state;

    let filtered = state.largeFiles;
    if (this.activeLargeFileFilter !== 'all') {
      filtered = filtered.filter(f => f.type === this.activeLargeFileFilter);
    }
    if (searchQuery) {
      filtered = filtered.filter(f => f.name.toLowerCase().includes(searchQuery) || f.path.toLowerCase().includes(searchQuery));
    }

    if (filtered.length === 0) {
      container.innerHTML = `
        <div class="text-center py-8 text-slate-400 text-xs">لا توجد ملفات تطابق الفلتر الحالي</div>
      `;
      return;
    }

    container.innerHTML = filtered.map(f => `
      <div class="flex items-center justify-between bg-slate-900/70 p-3 rounded-xl border ${f.selected ? 'border-sky-500/40 bg-sky-500/5' : 'border-white/5'} hover:border-sky-500/30 transition">
        <div class="flex items-center gap-3 min-w-0">
          <input type="checkbox" ${f.selected ? 'checked' : ''} data-file-id="${f.id}" class="large-file-cb w-4 h-4 accent-sky-500 rounded cursor-pointer">
          <div class="p-2 rounded-lg bg-sky-500/10 text-sky-400 shrink-0">
            <i data-lucide="${f.icon || 'file'}" class="w-4 h-4"></i>
          </div>
          <div class="min-w-0">
            <h5 class="text-xs font-bold text-white truncate">${f.name}</h5>
            <div class="text-[10px] text-slate-400 font-mono truncate">${f.path} • ${f.date}</div>
          </div>
        </div>

        <div class="flex items-center gap-2 shrink-0">
          <span class="text-xs font-mono font-bold text-sky-300 bg-sky-500/10 px-2.5 py-1 rounded-lg">
            ${f.sizeMB > 1024 ? (f.sizeMB/1024).toFixed(1) + ' GB' : f.sizeMB + ' MB'}
          </span>
          <button class="btn-preview-largefile p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300" data-type="${f.type}" data-name="${f.name}">
            <i data-lucide="eye" class="w-3.5 h-3.5"></i>
          </button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('.large-file-cb').forEach(cb => {
      cb.addEventListener('change', () => {
        const fId = cb.dataset.fileId;
        const target = state.largeFiles.find(f => f.id === fId);
        if (target) target.selected = cb.checked;
      });
    });

    container.querySelectorAll('.btn-preview-largefile').forEach(btn => {
      btn.addEventListener('click', () => {
        this.openMediaPreview(btn.dataset.type, '', btn.dataset.name);
      });
    });
  }

  renderSocialStats() {
    const waVoice = document.getElementById('waVoiceNotesSize');
    if (waVoice) {
      const stats = window.cleanerStore.state.socialMediaStats.whatsapp;
      waVoice.textContent = `${(stats.voiceNotesMB/1024).toFixed(2)} GB (${stats.voiceCount.toLocaleString('ar-EG')} ملف)`;
    }
  }

  renderRecycleBin() {
    const container = document.getElementById('recycleBinList');
    const badge = document.getElementById('recycleCountBadge');
    if (!container) return;
    const state = window.cleanerStore.state;
    const dict = translations[this.currentLang];

    if (badge) badge.textContent = state.recycleBin.length;

    if (state.recycleBin.length === 0) {
      container.innerHTML = `
        <div class="text-center py-10 glass-panel border border-white/10 rounded-2xl">
          <i data-lucide="trash" class="w-10 h-10 text-slate-500 mx-auto mb-2"></i>
          <h4 class="text-sm font-bold text-slate-300" data-i18n="emptyBinNotice">${dict.emptyBinNotice}</h4>
        </div>
      `;
      return;
    }

    container.innerHTML = state.recycleBin.map(item => `
      <div class="flex items-center justify-between bg-slate-900/80 p-3.5 rounded-xl border border-white/5">
        <div class="flex items-center gap-3 min-w-0">
          <div class="p-2 rounded-lg bg-rose-500/10 text-rose-400">
            <i data-lucide="file" class="w-4 h-4"></i>
          </div>
          <div class="min-w-0">
            <h5 class="text-xs font-bold text-white truncate">${item.name}</h5>
            <div class="text-[10px] text-slate-400">حُذف في: ${item.deletedAt} • يتبقى ${item.daysLeft} يوماً على الحذف الدائم</div>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <span class="text-xs font-mono font-bold text-rose-300 bg-rose-500/10 px-2 py-0.5 rounded-md">${item.size}</span>
          <button class="btn-restore-item py-1 px-2.5 rounded-lg bg-sky-500/20 hover:bg-sky-500/30 text-sky-400 text-xs font-bold transition flex items-center gap-1" data-id="${item.id}">
            <i data-lucide="rotate-ccw" class="w-3 h-3"></i>
            <span data-i18n="btnRestore">${dict.btnRestore}</span>
          </button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('.btn-restore-item').forEach(btn => {
      btn.addEventListener('click', () => {
        window.cleanerStore.restoreRecycleItem(btn.dataset.id);
        if (window.soundEngine) window.soundEngine.playSuccessChime();
        this.showToast('تم استعادة الملف بنجاح!');
      });
    });
  }

  renderWhitelist() {
    const container = document.getElementById('whitelistItemsList');
    if (!container) return;
    const state = window.cleanerStore.state;

    container.innerHTML = state.whitelist.map(w => `
      <div class="flex items-center justify-between bg-slate-900/60 p-3 rounded-xl border border-white/5">
        <div class="flex items-center gap-2.5">
          <span class="p-1.5 rounded-md bg-emerald-500/20 text-emerald-400"><i data-lucide="${w.type === 'folder' ? 'folder' : 'file-code'}" class="w-4 h-4"></i></span>
          <div>
            <div class="text-xs font-mono font-bold text-white">${w.pattern}</div>
            <div class="text-[10px] text-slate-400">${this.currentLang === 'ar' ? w.descAr : w.descEn}</div>
          </div>
        </div>
        <span class="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">محمي ✓</span>
      </div>
    `).join('');
  }

  // Execution Handlers
  executeQuickClean() {
    const state = window.cleanerStore.state;
    const selectedIds = state.quickScanCategories.filter(c => c.selected && !c.cleaned).map(c => c.id);

    if (selectedIds.length === 0) {
      this.showToast('يرجى اختيار عنصر واحد على الأقل للتنظيف');
      return;
    }

    const modal = document.getElementById('cleanProgressModal');
    const bar = document.getElementById('cleanModalProgressBar');
    const textPercent = document.getElementById('cleanModalPercentText');
    const stageText = document.getElementById('cleanModalStageText');

    modal.classList.remove('hidden');
    bar.style.width = '0%';
    textPercent.textContent = '0%';

    let progress = 0;
    if (window.soundEngine) window.soundEngine.playCleanLaser();

    const stages = [
      'مسح الذاكرة المؤقتة للتطبيقات (App Cache)...',
      'حذف المجلدات الفارغة المعزولة...',
      'إزالة حزم التثبيت والمخلفات القديمة...',
      'تنظيف نسخ الصور والوسائط المكررة (dHash)...',
      'تفريغ سجلات النظام وتحديث جداول التخزين...'
    ];

    const timer = setInterval(() => {
      progress += 5;
      bar.style.width = `${progress}%`;
      textPercent.textContent = `${progress}%`;

      const stageIndex = Math.min(stages.length - 1, Math.floor((progress / 100) * stages.length));
      stageText.textContent = stages[stageIndex];

      if (progress >= 100) {
        clearInterval(timer);
        modal.classList.add('hidden');

        const result = window.cleanerStore.performQuickClean(selectedIds);

        document.getElementById('successFreedGB').textContent = `${result.freedGB.toFixed(1)} GB`;
        document.getElementById('cleanSuccessModal').classList.remove('hidden');

        if (window.soundEngine) window.soundEngine.playSuccessChime();

        if (window.confetti) {
          window.confetti({
            particleCount: 80,
            spread: 70,
            origin: { y: 0.6 }
          });
        }
      }
    }, 50);
  }

  executeDeepScan() {
    const btn = document.getElementById('btnStartDeepScan');
    btn.disabled = true;
    btn.classList.add('opacity-50');

    const progressBar = document.getElementById('deepScanProgressBar');
    const percentText = document.getElementById('deepScanPercentText');
    const stageLabel = document.getElementById('deepScanCurrentStage');
    const statusLabel = document.getElementById('deepScanStatusLabel');

    statusLabel.textContent = 'جارٍ الفحص الذكي...';

    window.scannerEngine.startSimulatedScan(
      (prog) => {
        progressBar.style.width = `${prog}%`;
        percentText.textContent = `${prog}%`;
      },
      (stage) => {
        stageLabel.textContent = this.currentLang === 'ar' ? stage.textAr : stage.textEn;
      },
      () => {
        btn.disabled = false;
        btn.classList.remove('opacity-50');
        statusLabel.textContent = 'اكتمل الفحص!';
        this.showToast('اكتمل الفحص الذكي الشامل بنجاح! تم العثور على 14.2 GB جاهزة للتحرير.');
      }
    );
  }

  executeRamBoost() {
    const btn = document.getElementById('btnExecuteRamBoost');
    btn.disabled = true;
    btn.innerHTML = `<i data-lucide="loader" class="w-5 h-5 animate-spin"></i> <span>جارٍ التسريع والتبريد...</span>`;
    this.refreshLucideIcons();

    if (window.soundEngine) window.soundEngine.playBoostSwoosh();

    setTimeout(() => {
      const freed = window.cleanerStore.performRamBoost();
      btn.disabled = false;
      btn.innerHTML = `<i data-lucide="check" class="w-5 h-5"></i> <span>تم التسريع بنجاح!</span>`;
      this.refreshLucideIcons();
      this.showToast(`تم إيقاف العمليات الخفية وتحرير ${freed} GB من RAM!`);

      if (window.confetti) {
        window.confetti({ particleCount: 50, spread: 60, origin: { y: 0.5 } });
      }

      setTimeout(() => {
        btn.innerHTML = `<i data-lucide="zap" class="w-5 h-5"></i> <span>تسريع وتحرير الذاكرة فوراً</span>`;
        this.refreshLucideIcons();
      }, 3000);
    }, 1200);
  }

  // Image Compressor bindings
  bindCompressorEvents() {
    const dropZone = document.getElementById('compressDropZone');
    const fileInput = document.getElementById('compressFileInput');
    const sampleBtn = document.getElementById('btnLoadSampleImage');
    const qualityRange = document.getElementById('compressQualityRange');
    const qualityLabel = document.getElementById('compressQualityLabel');
    const maxSelect = document.getElementById('compressMaxWidthSelect');
    const maxWidthLabel = document.getElementById('compressMaxWidthLabel');
    const downloadBtn = document.getElementById('btnDownloadCompressed');

    if (dropZone && fileInput) {
      dropZone.addEventListener('click', (e) => {
        if (e.target !== sampleBtn) fileInput.click();
      });

      dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('border-sky-400', 'bg-sky-500/20');
      });

      dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('border-sky-400', 'bg-sky-500/20');
      });

      dropZone.addEventListener('drop', async (e) => {
        e.preventDefault();
        dropZone.classList.remove('border-sky-400', 'bg-sky-500/20');
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
          await this.processCompressionFile(e.dataTransfer.files[0]);
        }
      });

      fileInput.addEventListener('change', async (e) => {
        if (e.target.files && e.target.files[0]) {
          await this.processCompressionFile(e.target.files[0]);
        }
      });
    }

    if (sampleBtn) {
      sampleBtn.addEventListener('click', async (e) => {
        e.stopPropagation();
        this.showToast('جارٍ تحميل صورة عينة فائقة الدقة...');
        await window.imageCompressor.loadSampleUrl(
          'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1920&q=95',
          'Sample_Ultra_HD_Beach.jpg',
          5.8
        );
        this.runImageCompression();
      });
    }

    if (qualityRange) {
      qualityRange.addEventListener('input', (e) => {
        qualityLabel.textContent = `${e.target.value}%`;
        this.runImageCompression();
      });
    }

    if (maxSelect) {
      maxSelect.addEventListener('change', (e) => {
        maxWidthLabel.textContent = `${e.target.value} px`;
        this.runImageCompression();
      });
    }

    if (downloadBtn) {
      downloadBtn.addEventListener('click', () => {
        window.imageCompressor.downloadCompressed('optimized_image.jpg');
        this.showToast('تم تحميل الصورة المضغوطة بنجاح!');
      });
    }
  }

  async processCompressionFile(file) {
    try {
      this.showToast('جارٍ قراءة الصورة...');
      await window.imageCompressor.loadFile(file);
      this.runImageCompression();
    } catch (err) {
      this.showToast('تعذر قراءة ملف الصورة');
    }
  }

  async runImageCompression() {
    const quality = (parseInt(document.getElementById('compressQualityRange').value, 10) || 65) / 100;
    const maxWidth = parseInt(document.getElementById('compressMaxWidthSelect').value, 10) || 1920;

    const res = await window.imageCompressor.compress(quality, maxWidth);
    
    document.getElementById('compressResultSection').classList.remove('hidden');
    document.getElementById('origImagePreview').src = window.imageCompressor.originalDataUrl;
    document.getElementById('origImageSizeBadge').textContent = window.imageCompressor.formatBytes(window.imageCompressor.originalSize);
    
    document.getElementById('compressedImagePreview').src = res.dataUrl;
    document.getElementById('compressedImageSizeBadge').textContent = `${res.formattedSize} (-${res.savedPercent}%)`;

    if (window.soundEngine) window.soundEngine.playClick();
  }

  // Real Local File Scanner Bindings
  bindRealScannerEvents() {
    const dropZone = document.getElementById('realScanDropZone');
    const folderInput = document.getElementById('realFolderInput');
    const fileInput = document.getElementById('realFileInput');
    const btnFolder = document.getElementById('btnTriggerFolderSelect');
    const btnFile = document.getElementById('btnTriggerFileSelect');

    if (btnFolder && folderInput) {
      btnFolder.addEventListener('click', () => folderInput.click());
      folderInput.addEventListener('change', (e) => this.handleRealLocalFiles(e.target.files));
    }

    if (btnFile && fileInput) {
      btnFile.addEventListener('click', () => fileInput.click());
      fileInput.addEventListener('change', (e) => this.handleRealLocalFiles(e.target.files));
    }

    if (dropZone) {
      dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('border-sky-400', 'bg-sky-500/20');
      });

      dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('border-sky-400', 'bg-sky-500/20');
      });

      dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('border-sky-400', 'bg-sky-500/20');
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
          this.handleRealLocalFiles(e.dataTransfer.files);
        }
      });
    }
  }

  async handleRealLocalFiles(fileList) {
    if (!fileList || fileList.length === 0) return;
    this.showToast(`جارٍ فحص ${fileList.length} ملفاً حقيقياً محلياً...`);

    const results = await window.scannerEngine.analyzeLocalFiles(fileList);

    const container = document.getElementById('realScanResultsContainer');
    container.classList.remove('hidden');

    document.getElementById('realScanFilesCount').textContent = results.totalCount;
    document.getElementById('realScanTotalSize').textContent = results.formattedTotalSize;
    document.getElementById('realScanDupCount').textContent = results.duplicates.length;
    document.getElementById('realScanDupSize').textContent = results.formattedDuplicateSize;

    const list = document.getElementById('realScanLargestFilesList');
    list.innerHTML = results.largestFiles.map(f => `
      <div class="flex items-center justify-between bg-slate-900/80 p-2.5 rounded-xl border border-white/5 text-xs">
        <div class="flex items-center gap-2 truncate">
          <i data-lucide="file" class="w-3.5 h-3.5 text-sky-400 shrink-0"></i>
          <span class="text-white font-medium truncate">${f.path}</span>
        </div>
        <span class="font-mono text-sky-300 font-bold bg-sky-500/10 px-2 py-0.5 rounded-md shrink-0">${f.formattedSize}</span>
      </div>
    `).join('');

    this.refreshLucideIcons();
    this.showToast('اكتمل فحص الملفات المحلية بنجاح!');
  }

  // Open generic media preview modal
  openMediaPreview(type, url, title) {
    const modal = document.getElementById('mediaPreviewModal');
    const body = document.getElementById('previewModalBody');
    const titleEl = document.getElementById('previewModalTitle');

    titleEl.textContent = title || 'معاينة الوسائط';

    if (type === 'image' && url) {
      body.innerHTML = `<img src="${url}" class="max-h-[60vh] max-w-full object-contain rounded-lg">`;
    } else if (type === 'video') {
      body.innerHTML = `
        <div class="text-center p-8 text-sky-400 space-y-3">
          <i data-lucide="play-circle" class="w-16 h-16 mx-auto animate-pulse"></i>
          <h4 class="text-sm font-bold text-white">${title}</h4>
          <p class="text-xs text-slate-400">مشغل الفيديو عالي الدقة (4K 60fps)</p>
        </div>
      `;
    } else if (type === 'audio') {
      body.innerHTML = `
        <div class="text-center p-8 text-emerald-400 space-y-3">
          <i data-lucide="music" class="w-16 h-16 mx-auto animate-bounce"></i>
          <h4 class="text-sm font-bold text-white">${title}</h4>
          <p class="text-xs text-slate-400">مشغل الملفات الصوتية والملاحظات</p>
        </div>
      `;
    } else {
      body.innerHTML = `
        <div class="text-center p-8 text-slate-300 space-y-3">
          <i data-lucide="file-text" class="w-16 h-16 mx-auto text-sky-400"></i>
          <h4 class="text-sm font-bold text-white">${title}</h4>
          <p class="text-xs text-slate-400">مستند مشفر ومحمي</p>
        </div>
      `;
    }

    modal.classList.remove('hidden');
    this.refreshLucideIcons();
  }

  showToast(message) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'glass-panel px-4 py-2.5 border border-sky-500/40 text-xs font-bold text-white shadow-xl flex items-center gap-2 transform transition-all duration-300 translate-y-2 opacity-0 pointer-events-auto';
    toast.innerHTML = `<i data-lucide="info" class="w-4 h-4 text-sky-400"></i> <span>${message}</span>`;

    container.appendChild(toast);
    this.refreshLucideIcons();

    setTimeout(() => {
      toast.classList.remove('translate-y-2', 'opacity-0');
    }, 10);

    setTimeout(() => {
      toast.classList.add('translate-y-2', 'opacity-0');
      setTimeout(() => toast.remove(), 300);
    }, 3200);
  }
}

// Instantiate on DOM load
window.addEventListener('DOMContentLoaded', () => {
  window.app = new SmartCleanApp();
});
