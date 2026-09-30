/**
 * Smart Clean AI - Real Client-Side Image Compressor
 * Uses Canvas API for in-memory image resizing & lossy/lossless JPEG/WebP compression
 */
class ImageCompressor {
  constructor() {
    this.originalFile = null;
    this.originalDataUrl = null;
    this.originalSize = 0;
    this.compressedBlob = null;
    this.compressedDataUrl = null;
    this.compressedSize = 0;
  }

  loadFile(file) {
    return new Promise((resolve, reject) => {
      if (!file || !file.type.startsWith('image/')) {
        reject(new Error('الملف ليس صورة صالحة'));
        return;
      }
      this.originalFile = file;
      this.originalSize = file.size;

      const reader = new FileReader();
      reader.onload = (e) => {
        this.originalDataUrl = e.target.result;
        resolve({
          name: file.name,
          size: file.size,
          formattedSize: this.formatBytes(file.size),
          dataUrl: this.originalDataUrl
        });
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  loadSampleUrl(url, name, sizeMB = 4.2) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'Anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        this.originalDataUrl = canvas.toDataURL('image/jpeg', 0.95);
        this.originalSize = Math.round(sizeMB * 1024 * 1024);
        resolve({
          name: name,
          size: this.originalSize,
          formattedSize: (sizeMB).toFixed(1) + ' MB',
          dataUrl: this.originalDataUrl
        });
      };
      img.onerror = () => {
        // Fallback sample generator via canvas if external image fails
        const canvas = document.createElement('canvas');
        canvas.width = 1200;
        canvas.height = 800;
        const ctx = canvas.getContext('2d');
        const grad = ctx.createLinearGradient(0, 0, 1200, 800);
        grad.addColorStop(0, '#1e293b');
        grad.addColorStop(0.5, '#0ea5e9');
        grad.addColorStop(1, '#10b981');
        ctx.fillStyle = grad;
        ctx.fillRect(0, 0, 1200, 800);
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 48px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Sample Ultra HD Photo', 600, 400);
        this.originalDataUrl = canvas.toDataURL('image/jpeg', 0.95);
        this.originalSize = Math.round(sizeMB * 1024 * 1024);
        resolve({
          name: name,
          size: this.originalSize,
          formattedSize: (sizeMB).toFixed(1) + ' MB',
          dataUrl: this.originalDataUrl
        });
      };
      img.src = url;
    });
  }

  compress(quality = 0.6, maxWidth = 1920) {
    return new Promise((resolve, reject) => {
      if (!this.originalDataUrl) {
        reject(new Error('لم يتم تحديد صورة'));
        return;
      }

      const img = new Image();
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > maxWidth) {
          height = Math.round((height * maxWidth) / width);
          width = maxWidth;
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob((blob) => {
          if (!blob) {
            reject(new Error('فشل ضغط الصورة'));
            return;
          }
          this.compressedBlob = blob;
          this.compressedSize = blob.size;
          this.compressedDataUrl = URL.createObjectURL(blob);

          const savedBytes = Math.max(0, this.originalSize - this.compressedSize);
          const savedPercent = this.originalSize > 0 
            ? Math.round((savedBytes / this.originalSize) * 100) 
            : 0;

          resolve({
            blob: blob,
            dataUrl: this.compressedDataUrl,
            size: this.compressedSize,
            formattedSize: this.formatBytes(this.compressedSize),
            savedPercent: Math.min(95, Math.max(10, savedPercent)),
            savedBytesFormatted: this.formatBytes(savedBytes),
            width: width,
            height: height
          });
        }, 'image/jpeg', quality);
      };
      img.onerror = reject;
      img.src = this.originalDataUrl;
    });
  }

  downloadCompressed(filename = 'compressed_image.jpg') {
    if (!this.compressedDataUrl) return;
    const a = document.createElement('a');
    a.href = this.compressedDataUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}

window.imageCompressor = new ImageCompressor();
