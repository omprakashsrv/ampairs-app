import UIKit
import SwiftUI
import SharedEcom

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Multi-store marketplace: no pinned slug (directory mode → storefront picker), default seed
        // color (each selected store applies its own brand color).
        StorefrontViewControllerKt.StorefrontViewController(workspaceSlug: nil, seedColorArgb: 0)
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
