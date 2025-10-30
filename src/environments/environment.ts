export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  recaptcha: {
    // Using a working development site key - you should replace this with your actual site key
    // For now, using a dummy token approach for development
    siteKey: '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI', // Test site key (returns null)
    enabled: false // Disable reCAPTCHA for development
  },
  firebase: {
    apiKey: "AIzaSyARVXwwTDBi9XRf1bsCQVVVr2qRWwbL46Q",
    authDomain: "ampairs.firebaseapp.com",
    databaseURL: "https://ampairs-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "ampairs",
    storageBucket: "ampairs.firebasestorage.app",
    messagingSenderId: "682032206651",
    appId: "1:682032206651:web:176f7967f3515d2aa55d1e",
    measurementId: "G-LZ0PZSNL9H"
  },
  deepLink: {
    scheme: 'ampairs',
    host: 'auth'
  }
};
