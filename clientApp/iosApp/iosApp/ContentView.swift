import UIKit
import SwiftUI
import SharedEcom

/// White-label client storefront. The pinned workspace slug and brand color are read from Info.plist
/// so ONE framework/target serves every client — mirrors the Android `-Pclient=<id>` build. Set:
///   AMPAIRS_WORKSPACE_SLUG   (String)  e.g. "ambika"
///   AMPAIRS_THEME_COLOR_ARGB (String)  32-bit ARGB hex, e.g. "FF1B6C4A" (with or without 0x)
/// per client via the target's Info.plist / xcconfig. Missing/blank slug → directory mode fallback;
/// missing color → the default Ampairs green.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let slug = (Bundle.main.object(forInfoDictionaryKey: "AMPAIRS_WORKSPACE_SLUG") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        let workspaceSlug = (slug?.isEmpty == false) ? slug : nil

        let argbString = (Bundle.main.object(forInfoDictionaryKey: "AMPAIRS_THEME_COLOR_ARGB") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "0x", with: "")
            .replacingOccurrences(of: "0X", with: "")
        let seedColorArgb = Int64(argbString ?? "", radix: 16) ?? 0

        return StorefrontViewControllerKt.StorefrontViewController(
            workspaceSlug: workspaceSlug,
            seedColorArgb: seedColorArgb
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        uiViewController.navigationController?.setNavigationBarHidden(true, animated: false)
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose manages all safe areas and keyboard
    }
}
