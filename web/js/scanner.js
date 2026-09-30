/**
 * Smart Clean AI - Deep Scan Engine & Real Local File Analyzer
 */
class ScannerEngine {
  constructor() {
    this.isScanning = false;
    this.scanProgress = 0;
    this.currentScanStage = '';
    this.scanInterval = null;
  }

  // Draw futuristic radar scan on canvas
  initRadarCanvas(canvasId) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return null;
    const ctx = canvas.getContext('2d');
    let angle = 0;
    let animId = null;

    const render = () => {
      const width = canvas.width;
      const height = canvas.height;
      const centerX = width / 2;
      const centerY = height / 2;
      const radius = Math.min(centerX, centerY) - 8;

      ctx.clearRect(0, 0, width, height);

      // Outer rings
      ctx.strokeStyle = 'rgba(14, 165, 233, 0.25)';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
      ctx.stroke();

      ctx.strokeStyle = 'rgba(14, 165, 233, 0.15)';
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius * 0.65, 0, Math.PI * 2);
      ctx.stroke();

      ctx.strokeStyle = 'rgba(14, 165, 233, 0.1)';
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius * 0.35, 0, Math.PI * 2);
      ctx.stroke();

      // Crosshairs
      ctx.strokeStyle = 'rgba(14, 165, 233, 0.15)';
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.moveTo(centerX - radius, centerY);
      ctx.lineTo(centerX + radius, centerY);
      ctx.moveTo(centerX, centerY - radius);
      ctx.lineTo(centerX, centerY + radius);
      ctx.stroke();
      ctx.setLineDash([]);

      // Rotating radar beam
      const beamGrad = ctx.createConicGradient(angle, centerX, centerY);
      beamGrad.addColorStop(0, 'rgba(16, 185, 129, 0.4)');
      beamGrad.addColorStop(0.15, 'rgba(14, 165, 233, 0.05)');
      beamGrad.addColorStop(1, 'transparent');

      ctx.fillStyle = beamGrad;
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
      ctx.fill();

      // Radar sweep line
      const sweepX = centerX + radius * Math.cos(angle);
      const sweepY = centerY + radius * Math.sin(angle);
      ctx.strokeStyle = '#10B981';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.lineTo(sweepX, sweepY);
      ctx.stroke();

      // Pulsing center dot
      ctx.fillStyle = '#38BDF8';
      ctx.beginPath();
      ctx.arc(centerX, centerY, 4, 0, Math.PI * 2);
      ctx.fill();

      angle += 0.05;
      animId = requestAnimationFrame(render);
    };

    render();
    return {
      stop: () => {
        if (animId) cancelAnimationFrame(animId);
      }
    };
  }

  // Execute Simulated Deep Scan with callbacks
  startSimulatedScan(onProgress, onStageChange, onComplete) {
    if (this.isScanning) return;
    this.isScanning = true;
    this.scanProgress = 0;

    const stages = [
      { textAr: "فحص ذاكرة التخزين المؤقت للتطبيقات (App Cache)...", textEn: "Scanning Application Caches...", duration: 800 },
      { textAr: "فحص وتحليل الصور المتشابهة والمكررة بالذكاء الاصطناعي...", textEn: "Analyzing Duplicate Photos via AI...", duration: 900 },
      { textAr: "اكتشاف بقايا التطبيقات المحذوفة وحزم APK...", textEn: "Finding Residual Files & Leftover APKs...", duration: 750 },
      { textAr: "تصفية وفحص مقاطع فيديو ورسائل واتساب...", textEn: "Scanning WhatsApp & Social Media Storage...", duration: 850 },
      { textAr: "فحص الفيديوهات والملفات الكبيرة المتراكمة...", textEn: "Detecting Large Videos & Forgotten Archives...", duration: 800 },
      { textAr: "تحليل استهلاك ذاكرة RAM وسجلات الأخطاء...", textEn: "Analyzing RAM Usage & System Dump Logs...", duration: 700 }
    ];

    let currentStageIndex = 0;
    const totalDuration = stages.reduce((acc, s) => acc + s.duration, 0);
    const startTime = Date.now();

    if (window.soundEngine) window.soundEngine.playScanTick(500);

    this.scanInterval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      this.scanProgress = Math.min(100, Math.round((elapsed / totalDuration) * 100));

      // Calculate stage
      let accumulated = 0;
      for (let i = 0; i < stages.length; i++) {
        accumulated += stages[i].duration;
        if (elapsed < accumulated || i === stages.length - 1) {
          if (currentStageIndex !== i) {
            currentStageIndex = i;
            if (window.soundEngine) window.soundEngine.playScanTick(600 + i * 80);
            if (onStageChange) onStageChange(stages[i]);
          }
          break;
        }
      }

      if (onProgress) onProgress(this.scanProgress);

      if (this.scanProgress >= 100) {
        clearInterval(this.scanInterval);
        this.isScanning = false;
        if (window.soundEngine) window.soundEngine.playSuccessChime();
        if (onComplete) onComplete();
      }
    }, 40);
  }

  stopScan() {
    if (this.scanInterval) clearInterval(this.scanInterval);
    this.isScanning = false;
  }

  // Real Local File Analysis (Local browser File API)
  async analyzeLocalFiles(fileList, onProgressUpdate) {
    const files = Array.from(fileList);
    const totalCount = files.length;
    let totalBytes = 0;
    const typeDistribution = {
      images: { count: 0, bytes: 0, label: 'صور' },
      videos: { count: 0, bytes: 0, label: 'فيديوهات' },
      audio: { count: 0, bytes: 0, label: 'صوتيات' },
      docs: { count: 0, bytes: 0, label: 'مستندات' },
      archives: { count: 0, bytes: 0, label: 'أرشيفات' },
      others: { count: 0, bytes: 0, label: 'أخرى' }
    };

    const parsedFiles = [];
    const sizeMap = new Map();
    const potentialDuplicates = [];

    for (let i = 0; i < totalCount; i++) {
      const file = files[i];
      totalBytes += file.size;
      const ext = file.name.split('.').pop().toLowerCase();
      let cat = 'others';

      if (['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg', 'heic', 'bmp'].includes(ext) || file.type.startsWith('image/')) {
        cat = 'images';
      } else if (['mp4', 'mov', 'avi', 'mkv', 'webm', '3gp'].includes(ext) || file.type.startsWith('video/')) {
        cat = 'videos';
      } else if (['mp3', 'wav', 'm4a', 'aac', 'ogg', 'flac'].includes(ext) || file.type.startsWith('audio/')) {
        cat = 'audio';
      } else if (['pdf', 'docx', 'doc', 'xlsx', 'pptx', 'txt'].includes(ext)) {
        cat = 'docs';
      } else if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) {
        cat = 'archives';
      }

      typeDistribution[cat].count++;
      typeDistribution[cat].bytes += file.size;

      const fileObj = {
        name: file.name,
        size: file.size,
        formattedSize: this.formatBytes(file.size),
        type: cat,
        mimeType: file.type || 'application/octet-stream',
        lastModified: new Date(file.lastModified).toLocaleDateString('ar-EG'),
        path: file.webkitRelativePath || file.name,
        rawFile: file
      };
      parsedFiles.push(fileObj);

      // Simple duplicate detection by size + name or size similarity
      if (file.size > 1024) { // Only for files > 1KB
        const sizeKey = `${file.size}_${file.name.replace(/\(\d+\)/, '').trim()}`;
        if (sizeMap.has(sizeKey)) {
          const original = sizeMap.get(sizeKey);
          potentialDuplicates.push({
            original: original,
            duplicate: fileObj,
            size: file.size,
            formattedSize: this.formatBytes(file.size)
          });
        } else {
          sizeMap.set(sizeKey, fileObj);
        }
      }

      if (onProgressUpdate && i % 25 === 0) {
        onProgressUpdate(Math.round(((i + 1) / totalCount) * 100));
        await new Promise(r => setTimeout(r, 0));
      }
    }

    // Sort largest files
    parsedFiles.sort((a, b) => b.size - a.size);
    const largestFiles = parsedFiles.slice(0, 15);

    return {
      totalCount,
      totalBytes,
      formattedTotalSize: this.formatBytes(totalBytes),
      typeDistribution,
      largestFiles,
      duplicates: potentialDuplicates,
      duplicateWastedBytes: potentialDuplicates.reduce((sum, d) => sum + d.size, 0),
      formattedDuplicateSize: this.formatBytes(potentialDuplicates.reduce((sum, d) => sum + d.size, 0)),
      allFiles: parsedFiles
    };
  }

  formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}

window.scannerEngine = new ScannerEngine();
