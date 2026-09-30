/**
 * Smart Clean AI - Translation Dictionary
 * Arabic (Default) & English
 */
const translations = {
  ar: {
    appTitle: "منظف التخزين الذكي",
    appSubtitle: "تحسين الأداء وإدارة المساحة بالذكاء الاصطناعي وهندسة أندرويد الحديثة",
    versionBadge: "الإصدار 3.5 برو",
    
    // Header & Stats
    storageUsed: "المساحة المستخدمة",
    storageFree: "المساحة الفارغة",
    storageTotal: "السعة الإجمالية",
    ramUsage: "ذاكرة RAM",
    deviceTemp: "درجة الحرارة",
    batteryStatus: "البطارية",
    healthScore: "صحة التخزين",
    healthStatusGood: "ممتاز ومثالي",
    healthStatusWarning: "يحتاج إلى تنظيف",
    healthStatusCritical: "مساحة منخفضة جداً!",
    
    // View Switcher
    viewDesktop: "شاشة كاملة",
    viewMobile: "محاكي الهاتف",
    soundOn: "الصوت مفعّل",
    soundOff: "الصوت مكتوم",
    
    // Navigation Tabs
    navDashboard: "لوحة التحكم",
    navAppGallery: "صور وموك آب التطبيق",
    navEmptyFolders: "المجلدات الفارغة",
    navDeepScan: "الفحص الذكي الشامل",
    navRamBooster: "تسريع الذاكرة RAM",
    navAiPhotos: "منظف الصور والبصمة dHash",
    navLargeFiles: "الملفات الكبيرة والوسائط",
    navSocialCleaner: "منظف واتساب والتواصل",
    navRealScanner: "فاحص ملفاتك الحقيقي",
    navRecycleBin: "سلة المحذوفات",
    navCodeStudio: "دليل أكواد أندرويد 2026",
    navSettings: "الإعدادات والحماية",

    // Dashboard Quick Clean
    quickCleanTitle: "تنظيف ذكي سريع بضغطة زر",
    quickCleanSubtitle: "يمكنك تحرير مساحة فورية تصل إلى",
    btnQuickClean: "تنظيف فوري الآن",
    btnScanning: "جارٍ الفحص...",
    btnDeepScanNow: "بدء الفحص الشامل",
    cleanComplete: "تم التنظيف بنجاح!",
    spaceFreed: "تم توفير",
    boostFreed: "وتسريع الذاكرة بمقدار",

    // 4 Core Cards
    cardEmptyFoldersTitle: "المجلدات الفارغة",
    cardEmptyFoldersDesc: "18 مجلداً فارغاً مستهلكاً للفهرسة",
    cardPhotosTitle: "مكررات الصور والمتشابهات",
    cardPhotosDesc: "42 صورة (3.1 GB) ببصمة dHash",
    cardVideosTitle: "الفيديوهات المكررة",
    cardVideosDesc: "8 مقاطع فيديو ضخمة (4.8 GB)",
    cardFilesTitle: "الملفات المتطابقة 100%",
    cardFilesDesc: "24 ملفاً متطابقاً بهاش SHA-256",

    // Gallery
    galleryTitle: "معرض صور وموك آب التطبيق لمتجر Google Play",
    gallerySubtitle: "تصاميم وأيقونات ثلاثية الأبعاد احترافية وجاهزة للترويج والنشر على المتجر",
    btnDownloadImage: "تحميل الصورة بجودة عالية",

    // Categories
    catEmptyDirs: "المجلدات الفارغة المعزولة",
    catEmptyDirsDesc: "مجلدات لا تحتوي على أي ملفات وتبطئ فهرسة النظام",
    catAppCache: "مخلفات وذاكرة التطبيقات المؤقتة",
    catAppCacheDesc: "ملفات كاش مؤقتة غير ضرورية لتطبيقات النظام والمثبتة",
    catResidual: "حزم التثبيت والمخلفات المتبقية",
    catResidualDesc: "ملفات APK قديمة ومتبقيات تطبيقات محذوفة",
    catDuplicates: "الصور والوسائط المكررة",
    catDuplicatesDesc: "صور وفيديوهات متطابقة تشغل مساحة مضاعفة",
    catLargeMedia: "مقاطع الفيديو والملفات الضخمة",
    catLargeMediaDesc: "فيديوهات وملفات يتجاوز حجمها 100 ميجابايت",
    catSocialJunk: "مخلفات المحادثات والرسائل الصوتية",
    catSocialJunkDesc: "رسائل صوتية وملصقات وصور قديمة من واتساب وتليجرام",
    catTempLogs: "سجلات النظام والملفات المؤقتة",
    catTempLogsDesc: "تقارير الأخطاء، المصغرات التالفة، وسجلات النظام القديمة",

    // Empty Folders Module
    emptyFoldersTitle: "منظف المجلدات الفارغة الآمن (Empty Folders Cleaner)",
    emptyFoldersSubtitle: "فحص تعاقبي ذكي للمجلدات الفارغة مع استثناء صارم لمجلدات النظام الحساسة",
    btnDeleteEmptyFolders: "حذف المجلدات الفارغة المحددة",
    protectedPathsNotice: "المسارات المحمية (/Android/data, /system, .thumbnails) مستثناة بالكامل ومحصنة من الحذف.",

    // RAM Booster
    ramTitle: "معزز ومسرع الذاكرة العشوائية RAM",
    ramSubtitle: "إيقاف العمليات الخفية في الخلفية لتسريع أداء الهاتف والألعاب",
    btnBoostRam: "تسريع وتحرير الذاكرة فوراً",
    runningApps: "التطبيقات والعمليات النشطة في الخلفية",
    processCount: "عملية قيد التشغيل",
    cpuUsage: "استهلاك المعالج",
    coolDownCpu: "تبريد المعالج",
    batteryOptimization: "أوضاع توفير الطاقة",
    modeNormal: "الوضع العادي",
    modeUltraSaver: "التوفير الفائق (+4.5 ساعات)",
    modeGaming: "وضع الألعاب التوربيني",

    // AI Photo Cleaner
    photoCleanerTitle: "منظف الصور الذكي وبصمة الاختلاف dHash",
    photoTabDuplicates: "الصور المكررة (dHash)",
    photoTabBlurry: "صور مشوشة وضعيفة الجودة",
    photoTabScreenshots: "لقطات الشاشة القديمة",
    photoTabCompressor: "ضاغط الصور الذكي",
    similarity: "نسبة التطابق",
    bestShot: "أعلى جودة (موصى بالاحتفاظ بها)",
    duplicateMark: "نسخة مكررة (موصى بالحذف)",
    btnKeepBest: "الاحتفاظ بالأفضل وحذف الباقي",
    btnCompressImage: "ضغط وتصغير حجم الصورة",
    compressOriginal: "الحجم الأصلي",
    compressNew: "الحجم بعد الضغط",
    compressSaved: "وفرت",
    dropPhotoHere: "اسحب صورة من جهازك هنا أو انقر للاختيار",

    // Android Code Studio
    codeStudioTitle: "استوديو ومستكشف أكواد أندرويد (Kotlin & Compose 2026)",
    codeStudioSubtitle: "تصفح وانسخ الأكواد المصدرية الكاملة للخوارزميات والواجهات وخدمات الخلفية الجاهزة للإنتاج",
    btnCopyCode: "نسخ الكود البرمجي",
    codeCopied: "تم نسخ الكود البرمجي إلى الحافظة بنجاح!",

    // General Actions
    selectAll: "تحديد الكل",
    deselectAll: "إلغاء التحديد",
    delete: "حذف",
    cancel: "إلغاء",
    confirm: "تأكيد",
    items: "عناصر",
    sizeGB: "جيجابايت",
    sizeMB: "ميجابايت",
    sizeKB: "كيلوبايت",
    close: "إغلاق"
  },
  en: {
    appTitle: "Smart Storage Cleaner AI",
    appSubtitle: "AI-Powered Storage Optimization & Android 2026 Engineering Architecture",
    versionBadge: "v3.5 Pro",
    
    // Header & Stats
    storageUsed: "Storage Used",
    storageFree: "Storage Free",
    storageTotal: "Total Capacity",
    ramUsage: "RAM Usage",
    deviceTemp: "Device Temp",
    batteryStatus: "Battery",
    healthScore: "Storage Health",
    healthStatusGood: "Optimal & Healthy",
    healthStatusWarning: "Cleanup Recommended",
    healthStatusCritical: "Critically Low Space!",
    
    // View Switcher
    viewDesktop: "Full Screen",
    viewMobile: "Phone Frame",
    soundOn: "Sound On",
    soundOff: "Sound Muted",
    
    // Navigation Tabs
    navDashboard: "Dashboard",
    navAppGallery: "App Images & Mockups",
    navEmptyFolders: "Empty Folders",
    navDeepScan: "Smart Deep Scan",
    navRamBooster: "RAM Booster",
    navAiPhotos: "AI Photos & dHash",
    navLargeFiles: "Large Files & Media",
    navSocialCleaner: "Social & WhatsApp Cleaner",
    navRealScanner: "Real Local File Scanner",
    navRecycleBin: "Recycle Bin",
    navCodeStudio: "Android 2026 Code Studio",
    navSettings: "Settings & Security",

    // Dashboard Quick Clean
    quickCleanTitle: "1-Tap Smart Storage Cleaner",
    quickCleanSubtitle: "You can instantly reclaim up to",
    btnQuickClean: "Clean Now",
    btnScanning: "Scanning...",
    btnDeepScanNow: "Run Deep Scan",
    cleanComplete: "Cleanup Completed Successfully!",
    spaceFreed: "Freed up",
    boostFreed: "and boosted RAM by",

    // 4 Core Cards
    cardEmptyFoldersTitle: "Empty Folders",
    cardEmptyFoldersDesc: "18 empty directories slowing indexing",
    cardPhotosTitle: "Duplicate & Similar Photos",
    cardPhotosDesc: "42 photos (3.1 GB) with dHash",
    cardVideosTitle: "Duplicate Videos",
    cardVideosDesc: "8 heavy videos (4.8 GB)",
    cardFilesTitle: "Exact Duplicate Files",
    cardFilesDesc: "24 identical files (SHA-256)",

    // Gallery
    galleryTitle: "App Media & Google Play 3D Mockups Gallery",
    gallerySubtitle: "High-resolution 3D renders, app icon and presentation screenshots ready for marketing",
    btnDownloadImage: "Download High-Res Image",

    // Categories
    catEmptyDirs: "Empty Orphan Folders",
    catEmptyDirsDesc: "Directories containing zero files that slow file indexing",
    catAppCache: "App & System Cache",
    catAppCacheDesc: "Unnecessary temporary cache from apps and system",
    catResidual: "Residual Files & Leftover APKs",
    catResidualDesc: "Old installer APKs and uninstalled app leftovers",
    catDuplicates: "Duplicate Photos & Media",
    catDuplicatesDesc: "Exact replica photos and videos consuming space",
    catLargeMedia: "Large Videos & Big Files",
    catLargeMediaDesc: "Videos and files exceeding 100 MB each",
    catSocialJunk: "Social Media & Voice Notes",
    catSocialJunkDesc: "Old voice notes, stickers and statuses from WhatsApp/Telegram",
    catTempLogs: "System Logs & Temp Crash Dumps",
    catTempLogsDesc: "Crash logs, broken thumbnails, and legacy temp files",

    // Empty Folders Module
    emptyFoldersTitle: "Safe Empty Folders Cleaner",
    emptyFoldersSubtitle: "Recursive scanner with strict blacklisting of system & app containers",
    btnDeleteEmptyFolders: "Delete Selected Empty Folders",
    protectedPathsNotice: "Protected system paths (/Android/data, /system, .thumbnails) are strictly immune.",

    // RAM Booster
    ramTitle: "RAM Speed Booster & Task Killer",
    ramSubtitle: "Terminate background hidden tasks to boost gaming & phone speed",
    btnBoostRam: "Boost RAM & Free Memory Now",
    runningApps: "Background Active Apps & Processes",
    processCount: "active processes",
    cpuUsage: "CPU Load",
    coolDownCpu: "Cool Down CPU",
    batteryOptimization: "Power Saving Profiles",
    modeNormal: "Normal Balanced",
    modeUltraSaver: "Ultra Power Saver (+4.5 hrs)",
    modeGaming: "Turbo Gaming Mode",

    // AI Photo Cleaner
    photoCleanerTitle: "AI Smart Photo & Difference Hash (dHash) Engine",
    photoTabDuplicates: "Duplicate Photos (dHash)",
    photoTabBlurry: "Blurry & Low Quality",
    photoTabScreenshots: "Old Screenshots",
    photoTabCompressor: "Smart Image Compressor",
    similarity: "Similarity",
    bestShot: "Best Shot (Recommended to keep)",
    duplicateMark: "Duplicate (Recommended to remove)",
    btnKeepBest: "Keep Best Shot & Select Others",
    btnCompressImage: "Compress & Reduce File Size",
    compressOriginal: "Original Size",
    compressNew: "Compressed Size",
    compressSaved: "Saved",
    dropPhotoHere: "Drag a photo from your device here or click to select",

    // Android Code Studio
    codeStudioTitle: "Android 2026 Production Code Studio",
    codeStudioSubtitle: "Explore and copy complete production Kotlin algorithms, Compose screens, and Services",
    btnCopyCode: "Copy Source Code",
    codeCopied: "Source code copied to clipboard successfully!",

    // General Actions
    selectAll: "Select All",
    deselectAll: "Deselect All",
    delete: "Delete",
    cancel: "Cancel",
    confirm: "Confirm",
    items: "items",
    sizeGB: "GB",
    sizeMB: "MB",
    sizeKB: "KB",
    close: "Close"
  }
};
