export const environment = {
  production: true,
  apiBaseUrl: 'https://api.ampairs.com', // Update with your production API URL
  recaptcha: {
    siteKey: 'YOUR_PRODUCTION_RECAPTCHA_SITE_KEY' // Replace with your production site key
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
