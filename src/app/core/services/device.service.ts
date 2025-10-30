import {Injectable} from '@angular/core';
import {Platform} from '@angular/cdk/platform';
import {environment} from '../../../environments/environment';

export interface DeviceInfo {
  device_id: string;
  device_name: string;
  device_type: string;
  platform: string;
  browser: string;
  os: string;
  user_agent: string;
}

@Injectable({
  providedIn: 'root'
})
export class DeviceService {
  private deviceId: string | null = null;

  constructor(private platform: Platform) {
    this.deviceId = this.generateOrRetrieveDeviceId();

    // Log device info for debugging (remove in production)
    if (!environment.production) {
      console.log('🔍 Device Detection (Angular CDK):', this.getDeviceInfo());
    }
  }

  /**
   * Get comprehensive device information for authentication
   */
  getDeviceInfo(): DeviceInfo {
    const userAgent = navigator.userAgent;
    const platformInfo = this.detectPlatformUsingCDK();
    const browser = this.detectBrowserUsingCDK();
    const os = this.detectOSUsingCDK();
    const deviceType = this.detectDeviceTypeUsingCDK();
    const deviceName = this.generateDeviceName(browser, os);

    return {
      device_id: this.getDeviceId(),
      device_name: deviceName,
      device_type: deviceType,
      platform: platformInfo,
      browser: browser,
      os: os,
      user_agent: userAgent
    };
  }

  /**
   * Get or generate device ID
   */
  getDeviceId(): string {
    if (!this.deviceId) {
      this.deviceId = this.generateOrRetrieveDeviceId();
    }
    return this.deviceId;
  }

  /**
   * Clear device ID from all storage sources (useful for testing or logout)
   */
  clearDeviceId(): void {
    try {
      // Clear localStorage
      localStorage.removeItem('ampairs_device_id');
      localStorage.removeItem('ampairs_fingerprint_map');

      // Clear sessionStorage
      sessionStorage.removeItem('ampairs_device_id');
      sessionStorage.removeItem('ampairs_fingerprint_map');

      // Clear cookies
      document.cookie = 'ampairs_device_id=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
      document.cookie = 'ampairs_device_backup=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

      // Clear IndexedDB
      this.clearFromIndexedDB();

      // Reset in-memory device ID
      this.deviceId = null;
    } catch (error) {
      console.warn('Some storage clearing failed:', error);
    }
  }

  /**
   * Force regenerate device ID (clears all storage and creates new ID)
   */
  regenerateDeviceId(): string {
    // Clear all existing storage
    this.clearDeviceId();

    // Generate new device ID
    const fingerprint = this.generateFingerprint();
    const newDeviceId = `WEB_${fingerprint}_${Date.now()}`;

    // Store in all locations
    this.storeDeviceIdInMultipleSources(newDeviceId);

    // Update in-memory reference
    this.deviceId = newDeviceId;

    return newDeviceId;
  }

  /**
   * Validate device ID integrity across storage sources
   */
  validateDeviceIdIntegrity(): boolean {
    const localStorage_id = localStorage.getItem('ampairs_device_id');
    const sessionStorage_id = sessionStorage.getItem('ampairs_device_id');
    const cookie_id = this.getDeviceIdFromCookies();

    // Check if stored IDs are consistent
    const ids = [localStorage_id, sessionStorage_id, cookie_id].filter(id => id !== null);

    if (ids.length === 0) return false;

    // All non-null IDs should be the same
    const uniqueIds = [...new Set(ids)];
    return uniqueIds.length === 1;
  }

  /**
   * Repair device ID storage by syncing across all sources
   */
  repairDeviceIdStorage(): void {
    const currentDeviceId = this.getDeviceId();
    if (currentDeviceId) {
      this.storeDeviceIdInMultipleSources(currentDeviceId);
    }
  }

  /**
   * Get detailed platform information for debugging
   */
  getPlatformDebugInfo(): any {
    return {
      device_storage: {
        current_device_id: this.getDeviceId(),
        localStorage_id: localStorage.getItem('ampairs_device_id'),
        sessionStorage_id: sessionStorage.getItem('ampairs_device_id'),
        cookie_id: this.getDeviceIdFromCookies(),
        fingerprint_map: localStorage.getItem('ampairs_fingerprint_map'),
        storage_integrity: this.validateDeviceIdIntegrity(),
        indexedDB_supported: 'indexedDB' in window
      },
      angular_cdk_detection: {
        ANDROID: this.platform.ANDROID,
        IOS: this.platform.IOS,
        FIREFOX: this.platform.FIREFOX,
        SAFARI: this.platform.SAFARI,
        EDGE: this.platform.EDGE,
        WEBKIT: this.platform.WEBKIT,
        BLINK: this.platform.BLINK,
        TRIDENT: this.platform.TRIDENT
      },
      browser_features: {
        touchSupport: 'ontouchstart' in window,
        maxTouchPoints: navigator.maxTouchPoints || 0,
        deviceOrientation: window.DeviceOrientationEvent !== undefined,
        deviceMotion: window.DeviceMotionEvent !== undefined,
        hardwareConcurrency: navigator.hardwareConcurrency || 0,
        devicePixelRatio: window.devicePixelRatio || 1,
        screenOrientation: screen.orientation?.type || 'unknown'
      },
      navigator_info: {
        userAgent: navigator.userAgent,
        platform: navigator.platform,
        language: navigator.language,
        cookieEnabled: navigator.cookieEnabled,
        onLine: navigator.onLine
      },
      screen_info: {
        width: screen.width,
        height: screen.height,
        colorDepth: screen.colorDepth,
        pixelDepth: screen.pixelDepth
      },
      detected_info: {
        platform: this.detectPlatformUsingCDK(),
        browser: this.detectBrowserUsingCDK(),
        os: this.detectOSUsingCDK(),
        deviceType: this.detectDeviceTypeUsingCDK()
      }
    };
  }

  /**
   * Generate a unique device fingerprint or retrieve existing one using multi-layer storage
   */
  private generateOrRetrieveDeviceId(): string {
    // Try to retrieve device ID from multiple sources
    let deviceId = this.retrieveDeviceIdFromStorage();

    if (!deviceId) {
      // Generate a new device ID based on enhanced browser fingerprinting
      const fingerprint = this.generateFingerprint();
      deviceId = `WEB_${fingerprint}_${Date.now()}`;

      // Store in multiple locations for persistence
      this.storeDeviceIdInMultipleSources(deviceId);
    }

    return deviceId;
  }

  /**
   * Retrieve device ID from multiple storage sources in order of preference
   */
  private retrieveDeviceIdFromStorage(): string | null {

    // 1. Try IndexedDB first (most persistent)
    const indexedDbId = this.getFromIndexedDB();
    if (indexedDbId) {
      // Ensure it's stored in other locations too
      this.storeDeviceIdInMultipleSources(indexedDbId);
      return indexedDbId;
    }

    // 2. Try localStorage
    const localStorageId = localStorage.getItem('ampairs_device_id');
    if (localStorageId) {
      this.storeDeviceIdInMultipleSources(localStorageId);
      return localStorageId;
    }

    // 3. Try sessionStorage
    const sessionStorageId = sessionStorage.getItem('ampairs_device_id');
    if (sessionStorageId) {
      this.storeDeviceIdInMultipleSources(sessionStorageId);
      return sessionStorageId;
    }

    // 4. Try cookies
    const cookieId = this.getDeviceIdFromCookies();
    if (cookieId) {
      this.storeDeviceIdInMultipleSources(cookieId);
      return cookieId;
    }

    // 5. Try to regenerate from consistent fingerprint
    const regeneratedId = this.tryRegenerateFromFingerprint();
    if (regeneratedId) {
      this.storeDeviceIdInMultipleSources(regeneratedId);
      return regeneratedId;
    }

    return null;
  }

  /**
   * Store device ID in multiple storage sources for redundancy
   */
  private storeDeviceIdInMultipleSources(deviceId: string): void {
    try {
      // 1. localStorage (primary)
      localStorage.setItem('ampairs_device_id', deviceId);

      // 2. sessionStorage (session backup)
      sessionStorage.setItem('ampairs_device_id', deviceId);

      // 3. IndexedDB (most persistent)
      this.storeInIndexedDB(deviceId);

      // 4. Secure cookie (httpOnly not possible from JS, but secure flag)
      this.storeDeviceIdInCookies(deviceId);

      // 5. Store fingerprint mapping for regeneration
      this.storeFingerprint(deviceId);

    } catch (error) {
      console.warn('Some storage methods failed:', error);
    }
  }

  /**
   * Store device ID in IndexedDB for long-term persistence
   */
  private storeInIndexedDB(deviceId: string): void {
    if (!('indexedDB' in window)) return;

    try {
      const request = indexedDB.open('AmpairsDeviceDB', 1);

      request.onupgradeneeded = (event: any) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains('device')) {
          db.createObjectStore('device', {keyPath: 'key'});
        }
      };

      request.onsuccess = (event: any) => {
        const db = event.target.result;
        const transaction = db.transaction(['device'], 'readwrite');
        const store = transaction.objectStore('device');
        store.put({
          key: 'device_id',
          value: deviceId,
          timestamp: Date.now(),
          fingerprint: this.generateFingerprint()
        });
      };
    } catch (error) {
      console.warn('IndexedDB storage failed:', error);
    }
  }

  /**
   * Retrieve device ID from IndexedDB (synchronous fallback)
   * Note: This is a simplified sync approach. In production, consider making the entire
   * device ID retrieval async for proper IndexedDB support.
   */
  private getFromIndexedDB(): string | null {
    // For now, we skip IndexedDB in the sync flow and rely on other storage methods
    // IndexedDB will be used for storage and can be retrieved on next session
    return null;
  }

  /**
   * Async method to retrieve from IndexedDB (for future enhancement)
   */
  private async getFromIndexedDBAsync(): Promise<string | null> {
    if (!('indexedDB' in window)) return null;

    return new Promise((resolve) => {
      try {
        const request = indexedDB.open('AmpairsDeviceDB', 1);

        request.onerror = () => resolve(null);

        request.onsuccess = (event: any) => {
          const db = event.target.result;
          const transaction = db.transaction(['device'], 'readonly');
          const store = transaction.objectStore('device');
          const getRequest = store.get('device_id');

          getRequest.onsuccess = () => {
            const result = getRequest.result;
            resolve(result ? result.value : null);
          };

          getRequest.onerror = () => resolve(null);
        };
      } catch (error) {
        resolve(null);
      }
    });
  }

  /**
   * Store device ID in cookies with security settings
   */
  private storeDeviceIdInCookies(deviceId: string): void {
    try {
      const expires = new Date();
      expires.setFullYear(expires.getFullYear() + 1); // 1 year expiry

      document.cookie = `ampairs_device_id=${deviceId}; expires=${expires.toUTCString()}; path=/; secure; samesite=strict`;

      // Backup cookie with different name
      document.cookie = `ampairs_device_backup=${deviceId}; expires=${expires.toUTCString()}; path=/; secure; samesite=strict`;
    } catch (error) {
      console.warn('Cookie storage failed:', error);
    }
  }

  /**
   * Retrieve device ID from cookies
   */
  private getDeviceIdFromCookies(): string | null {
    try {
      const cookies = document.cookie.split(';');

      for (const cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if ((name === 'ampairs_device_id' || name === 'ampairs_device_backup') && value) {
          return decodeURIComponent(value);
        }
      }
    } catch (error) {
      console.warn('Cookie retrieval failed:', error);
    }
    return null;
  }

  /**
   * Store fingerprint mapping for device ID regeneration
   */
  private storeFingerprint(deviceId: string): void {
    try {
      const fingerprint = this.generateFingerprint();
      const mapping = {
        fingerprint,
        deviceId,
        timestamp: Date.now()
      };

      localStorage.setItem('ampairs_fingerprint_map', JSON.stringify(mapping));
      sessionStorage.setItem('ampairs_fingerprint_map', JSON.stringify(mapping));
    } catch (error) {
      console.warn('Fingerprint mapping storage failed:', error);
    }
  }

  /**
   * Try to regenerate device ID from stored fingerprint mapping
   */
  private tryRegenerateFromFingerprint(): string | null {
    try {
      const currentFingerprint = this.generateFingerprint();

      // Check localStorage first
      let mappingStr = localStorage.getItem('ampairs_fingerprint_map');
      if (!mappingStr) {
        // Fallback to sessionStorage
        mappingStr = sessionStorage.getItem('ampairs_fingerprint_map');
      }

      if (mappingStr) {
        const mapping = JSON.parse(mappingStr);

        // If fingerprint matches and mapping is not too old (30 days)
        if (mapping.fingerprint === currentFingerprint &&
          Date.now() - mapping.timestamp < 30 * 24 * 60 * 60 * 1000) {
          return mapping.deviceId;
        }
      }
    } catch (error) {
      console.warn('Fingerprint regeneration failed:', error);
    }

    return null;
  }

  /**
   * Generate a browser fingerprint based on available characteristics using Angular CDK
   */
  private generateFingerprint(): string {
    const characteristics = [
      navigator.userAgent,
      navigator.language,
      screen.width + 'x' + screen.height,
      screen.colorDepth.toString(),
      new Date().getTimezoneOffset().toString(),
      navigator.platform,
      navigator.cookieEnabled.toString(),
      navigator.onLine.toString()
    ];

    // Add Angular CDK platform information
    characteristics.push(
      this.platform.ANDROID.toString(),
      this.platform.IOS.toString(),
      this.platform.FIREFOX.toString(),
      this.platform.SAFARI.toString(),
      this.platform.EDGE.toString(),
      this.platform.WEBKIT.toString(),
      this.platform.BLINK.toString(),
      this.platform.TRIDENT.toString()
    );

    // Add additional browser features
    characteristics.push(
      ('ontouchstart' in window).toString(), // Touch support
      (window.DeviceOrientationEvent !== undefined).toString(), // Device orientation
      (window.DeviceMotionEvent !== undefined).toString(), // Device motion
      navigator.maxTouchPoints?.toString() || '0', // Touch points
      navigator.hardwareConcurrency?.toString() || '0' // CPU cores
    );

    // Add screen and display information
    if (screen.orientation) {
      characteristics.push(screen.orientation.type);
    }
    if (window.devicePixelRatio) {
      characteristics.push(window.devicePixelRatio.toString());
    }

    // Add canvas fingerprinting if available
    try {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.textBaseline = 'top';
        ctx.font = '14px Arial';
        ctx.fillText('Ampairs Device Fingerprint', 2, 2);
        characteristics.push(canvas.toDataURL());
      }
    } catch (e) {
      // Canvas fingerprinting not available
    }

    // Create hash from characteristics
    const combinedString = characteristics.join('|');
    return this.simpleHash(combinedString).substring(0, 12).toUpperCase();
  }

  /**
   * Simple hash function for fingerprinting
   */
  private simpleHash(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString(16);
  }

  /**
   * Detect device platform using Angular CDK
   */
  private detectPlatformUsingCDK(): string {
    if (this.platform.ANDROID) return 'Android';
    if (this.platform.IOS) return 'iOS';
    if (this.platform.SAFARI) return 'macOS'; // Safari typically means macOS
    if (this.platform.WEBKIT && navigator.userAgent.includes('Mac')) return 'macOS';

    // Fall back to user agent detection for other platforms
    const userAgent = navigator.userAgent.toLowerCase();
    if (userAgent.includes('windows')) return 'Windows';
    if (userAgent.includes('linux')) return 'Linux';
    if (userAgent.includes('cros')) return 'ChromeOS';

    return 'Unknown';
  }

  /**
   * Detect browser information using Angular CDK
   */
  private detectBrowserUsingCDK(): string {
    // Use Angular CDK Platform detection
    if (this.platform.EDGE) return 'Microsoft Edge';
    if (this.platform.FIREFOX) return 'Mozilla Firefox';
    if (this.platform.SAFARI) return 'Safari';
    if (this.platform.TRIDENT) return 'Internet Explorer';

    // Additional checks for browsers not covered by CDK
    const userAgent = navigator.userAgent;

    // Chrome detection (must be after Edge check)
    if (userAgent.includes('Chrome/')) {
      if (userAgent.includes('OPR/')) return 'Opera';
      if (userAgent.includes('Brave/')) return 'Brave';
      return 'Google Chrome';
    }

    if (userAgent.includes('MSIE')) return 'Internet Explorer';

    return 'Unknown Browser';
  }

  /**
   * Detect operating system using Angular CDK
   */
  private detectOSUsingCDK(): string {
    const userAgent = navigator.userAgent;

    // Debug log for troubleshooting
    if (!environment.production) {
      console.log('🔍 OS Detection Debug:', {
        userAgent,
        platform: navigator.platform,
        cdkFlags: {
          IOS: this.platform.IOS,
          ANDROID: this.platform.ANDROID,
          WEBKIT: this.platform.WEBKIT,
          SAFARI: this.platform.SAFARI,
          EDGE: this.platform.EDGE,
          TRIDENT: this.platform.TRIDENT
        }
      });
    }

    // Use Angular CDK Platform service for comprehensive OS detection
    if (this.platform.IOS) {
      const match = userAgent.match(/OS (\d+_\d+_?\d*)/);
      return match && match[1] ? `iOS ${match[1].replace(/_/g, '.')}` : 'iOS';
    }

    if (this.platform.ANDROID) {
      const match = userAgent.match(/Android (\d+\.?\d*\.?\d*)/);
      return match ? `Android ${match[1]}` : 'Android';
    }

    // Primary OS detection using user agent patterns (more reliable than CDK for desktop)
    if (userAgent.includes('Windows')) {
      let windowsOS = this.getWindowsVersion(userAgent);

      // Add browser context if available from CDK
      if (this.platform.EDGE) {
        windowsOS += ' (Edge)';
      } else if (this.platform.TRIDENT) {
        windowsOS += ' (IE)';
      }

      return windowsOS;
    }

    // macOS detection - check user agent first, then CDK
    if (userAgent.includes('Mac OS X') || userAgent.includes('Macintosh')) {
      const match = userAgent.match(/Mac OS X (\d+_\d+_?\d*)/);
      return match && match[1] ? `macOS ${match[1].replace(/_/g, '.')}` : 'macOS';
    }

    // Additional CDK-based detection for Edge cases
    if (this.platform.WEBKIT && userAgent.includes('Mac')) {
      return 'macOS';
    }

    // Linux variants detection
    if (userAgent.includes('CrOS')) return 'ChromeOS';
    if (userAgent.includes('Ubuntu')) return 'Ubuntu';
    if (userAgent.includes('Fedora')) return 'Fedora';
    if (userAgent.includes('Debian')) return 'Debian';
    if (userAgent.includes('Red Hat')) return 'Red Hat Linux';
    if (userAgent.includes('SUSE')) return 'SUSE Linux';
    if (userAgent.includes('Linux')) return 'Linux';

    // Other Unix-like systems
    if (userAgent.includes('FreeBSD')) return 'FreeBSD';
    if (userAgent.includes('OpenBSD')) return 'OpenBSD';
    if (userAgent.includes('NetBSD')) return 'NetBSD';
    if (userAgent.includes('SunOS') || userAgent.includes('Solaris')) return 'Solaris';

    // Mobile OS fallback checks
    if (userAgent.includes('iPhone') || userAgent.includes('iPad')) return 'iOS';
    if (userAgent.includes('Android')) return 'Android';

    // Use navigator.platform as additional fallback
    const platform = navigator.platform.toLowerCase();
    if (platform.includes('win')) return 'Windows';
    if (platform.includes('mac')) return 'macOS';
    if (platform.includes('linux')) return 'Linux';
    if (platform.includes('freebsd')) return 'FreeBSD';
    if (platform.includes('openbsd')) return 'OpenBSD';

    return 'Unknown OS';
  }

  private getWindowsVersion(userAgent: string): string {
    if (userAgent.includes('Windows NT 10.0')) {
      // More specific Windows 10/11 detection
      if (userAgent.includes('Edg/') || userAgent.includes('Edge/')) {
        return 'Windows 11'; // Edge on Windows 11 has specific indicators
      }
      return 'Windows 10/11';
    }
    if (userAgent.includes('Windows NT 6.3')) return 'Windows 8.1';
    if (userAgent.includes('Windows NT 6.2')) return 'Windows 8';
    if (userAgent.includes('Windows NT 6.1')) return 'Windows 7';
    if (userAgent.includes('Windows NT 6.0')) return 'Windows Vista';
    if (userAgent.includes('Windows NT 5.2')) return 'Windows XP x64';
    if (userAgent.includes('Windows NT 5.1')) return 'Windows XP';
    if (userAgent.includes('Windows NT 5.0')) return 'Windows 2000';
    return 'Windows';
  }

  /**
   * Detect device type using Angular CDK
   */
  private detectDeviceTypeUsingCDK(): string {
    // Use Angular CDK Platform detection for mobile and tablet
    if (this.platform.IOS) {
      // iPad detection
      if (navigator.userAgent.includes('iPad') ||
        (navigator.userAgent.includes('Macintosh') && 'ontouchend' in document)) {
        return 'Tablet';
      }
      return 'Mobile';
    }

    if (this.platform.ANDROID) {
      // Android tablet detection (screen size heuristic)
      const screenSize = Math.min(screen.width, screen.height);
      if (screenSize >= 600) { // Typical tablet breakpoint
        return 'Tablet';
      }
      return 'Mobile';
    }

    // Use additional checks for other devices
    const userAgent = navigator.userAgent.toLowerCase();

    // Check for tablets that might not be caught by CDK
    if (/tablet|ipad|kindle|silk|gt-|sm-t|nexus [7-9]/.test(userAgent)) {
      return 'Tablet';
    }

    // Check for mobile devices that might not be caught by CDK
    if (/mobile|blackberry|opera mini|iemobile/.test(userAgent)) {
      return 'Mobile';
    }

    // Default to desktop
    return 'Desktop';
  }

  /**
   * Generate a human-readable device name
   */
  private generateDeviceName(browser: string, os: string): string {
    return `${browser} on ${os}`;
  }

  /**
   * Clear device ID from IndexedDB
   */
  private clearFromIndexedDB(): void {
    if (!('indexedDB' in window)) return;

    try {
      const request = indexedDB.open('AmpairsDeviceDB', 1);

      request.onsuccess = (event: any) => {
        const db = event.target.result;
        const transaction = db.transaction(['device'], 'readwrite');
        const store = transaction.objectStore('device');
        store.delete('device_id');
      };
    } catch (error) {
      console.warn('IndexedDB clearing failed:', error);
    }
  }
}
