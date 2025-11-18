package com.ampairs.common.localization

/**
 * Interface for all localizable strings in the application
 * This provides type-safe access to translated strings
 */
interface Strings {
    // Common
    val appName: String
    val ok: String
    val cancel: String
    val save: String
    val delete: String
    val edit: String
    val search: String
    val loading: String
    val error: String
    val success: String
    val retry: String
    val close: String
    val yes: String
    val no: String

    // Login Screen
    val loginWelcomeTagline: String
    val loginGetStarted: String
    val loginFeatureCustomerManagement: String
    val loginFeatureCustomerManagementDesc: String
    val loginFeatureInventoryControl: String
    val loginFeatureInventoryControlDesc: String
    val loginFeatureSmartInvoicing: String
    val loginFeatureSmartInvoicingDesc: String
    val loginSelectLanguage: String
    val loginLanguagePrompt: String

    // Phone Number Screen
    val phoneNumberTitle: String
    val phoneNumberEnter: String
    val phoneNumberPlaceholder: String
    val phoneNumberContinue: String
    val phoneNumberInvalid: String
    val phoneUserExists: String
    val phoneUserAlreadyLoggedIn: String
    val phoneUserAlreadyLoggedInDesc: String
    val phoneWouldYouLikeToSwitch: String
    val phoneSwitchToThisUser: String
    val phoneLogin: String

    // OTP Screen
    val otpTitle: String
    val otpEnter: String
    val otpPlaceholder: String
    val otpVerify: String
    val otpResend: String
    val otpInvalid: String
    val otpSent: String
    val otpVerifying: String
    val otpEnterCode: String
    val otpWaitingForAutoVerification: String
    val otpAutoVerificationDesc: String
    val otpEnterManually: String

    // Customer Screen
    val customerTitle: String
    val customerAdd: String
    val customerSearch: String
    val customerName: String
    val customerPhone: String
    val customerEmail: String
    val customerAddress: String
    val customerSave: String
    val customerDelete: String
    val customerEdit: String
    val customerNoCustomers: String
    val customerAddFirst: String
    val customerDetails: String
    val customerList: String
    val customerType: String
    val customerGroup: String
    val customerGSTNumber: String
    val customerPANNumber: String

    // Business/Workspace Screen
    val workspaceTitle: String
    val workspaceSelect: String
    val workspaceCreate: String
    val workspaceName: String
    val workspaceDescription: String
    val workspaceNoWorkspaces: String
    val workspaceAddFirst: String
    val workspaceSwitch: String
    val workspaceSettings: String
    val workspaceModules: String
    val workspaceNoModules: String

    // Settings
    val settingsTitle: String
    val settingsLanguage: String
    val settingsTheme: String
    val settingsAccount: String
    val settingsAbout: String
    val settingsLogout: String
    val settingsVersion: String
    val settingsChangeLanguage: String
    val settingsSelectLanguage: String
}
