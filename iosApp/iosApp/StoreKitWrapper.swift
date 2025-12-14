//
//  StoreKitWrapper.swift
//  iosApp
//
//  Created for Ampairs
//  StoreKit 2 wrapper for Kotlin Multiplatform integration
//

import Foundation
import StoreKit

@objc public class StoreKitWrapper: NSObject {

    @objc public static let shared = StoreKitWrapper()

    private var products: [String: Product] = [:]
    private var transactionListener: Task<Void, Error>?
    private var purchaseCallback: ((String, String?, String?) -> Void)?

    private override init() {
        super.init()
        startTransactionListener()
    }

    // MARK: - Product Loading

    @objc public func loadProducts(productIds: [String], completion: @escaping ([String: [String: Any]]?, String?) -> Void) {
        Task {
            do {
                let products = try await Product.products(for: Set(productIds))

                var productMap: [String: [String: Any]] = [:]
                for product in products {
                    self.products[product.id] = product
                    productMap[product.id] = self.productToDictionary(product)
                }

                DispatchQueue.main.async {
                    completion(productMap, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(nil, error.localizedDescription)
                }
            }
        }
    }

    // MARK: - Purchase Flow

    @objc public func purchase(productId: String, completion: @escaping ([String: Any]?, String?) -> Void) {
        Task {
            guard let product = products[productId] else {
                DispatchQueue.main.async {
                    completion(nil, "Product not found: \(productId)")
                }
                return
            }

            do {
                let result = try await product.purchase()

                switch result {
                case .success(let verification):
                    switch verification {
                    case .verified(let transaction):
                        // Finish the transaction
                        await transaction.finish()

                        let purchaseData = self.transactionToDictionary(transaction)
                        DispatchQueue.main.async {
                            completion(purchaseData, nil)
                        }

                    case .unverified(_, let error):
                        DispatchQueue.main.async {
                            completion(nil, "Transaction unverified: \(error.localizedDescription)")
                        }
                    }

                case .userCancelled:
                    DispatchQueue.main.async {
                        completion(nil, "USER_CANCELLED")
                    }

                case .pending:
                    DispatchQueue.main.async {
                        completion(["status": "PENDING"], nil)
                    }

                @unknown default:
                    DispatchQueue.main.async {
                        completion(nil, "Unknown purchase result")
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    completion(nil, error.localizedDescription)
                }
            }
        }
    }

    // MARK: - Current Entitlements

    @objc public func queryPurchases(completion: @escaping ([[String: Any]]?, String?) -> Void) {
        Task {
            var purchases: [[String: Any]] = []

            for await result in Transaction.currentEntitlements {
                switch result {
                case .verified(let transaction):
                    purchases.append(self.transactionToDictionary(transaction))
                case .unverified(_, let error):
                    print("Unverified transaction: \(error.localizedDescription)")
                }
            }

            DispatchQueue.main.async {
                completion(purchases, nil)
            }
        }
    }

    // MARK: - Restore Purchases

    @objc public func restorePurchases(completion: @escaping (Bool, String?) -> Void) {
        Task {
            do {
                try await AppStore.sync()
                DispatchQueue.main.async {
                    completion(true, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(false, error.localizedDescription)
                }
            }
        }
    }

    // MARK: - Transaction Listener

    private func startTransactionListener() {
        transactionListener = Task {
            for await result in Transaction.updates {
                switch result {
                case .verified(let transaction):
                    await transaction.finish()
                    // Notify Kotlin layer about the transaction
                    self.notifyTransactionUpdate(transaction)

                case .unverified(_, let error):
                    print("Unverified transaction update: \(error.localizedDescription)")
                }
            }
        }
    }

    @objc public func setPurchaseCallback(_ callback: @escaping (String, String?, String?) -> Void) {
        self.purchaseCallback = callback
    }

    private func notifyTransactionUpdate(_ transaction: Transaction) {
        if let callback = purchaseCallback {
            let purchaseToken = String(transaction.id)
            let productId = transaction.productID
            let orderId = String(transaction.originalID)
            callback(purchaseToken, productId, orderId)
        }
    }

    @objc public func stopTransactionListener() {
        transactionListener?.cancel()
        transactionListener = nil
    }

    // MARK: - Helper Methods

    private func productToDictionary(_ product: Product) -> [String: Any] {
        var dict: [String: Any] = [
            "productId": product.id,
            "displayName": product.displayName,
            "description": product.description,
            "price": product.displayPrice,
            "type": product.type == .autoRenewable ? "SUBSCRIPTION" : "CONSUMABLE"
        ]

        if let subscription = product.subscription {
            var subscriptionInfo: [String: Any] = [:]

            // Get subscription period
            if let period = subscription.subscriptionPeriod {
                subscriptionInfo["period"] = "\(period.value) \(period.unit)"
            }

            // Get subscription offers
            var offers: [[String: Any]] = []
            for offer in subscription.promotionalOffers {
                offers.append([
                    "id": offer.id,
                    "displayPrice": offer.displayPrice
                ])
            }
            subscriptionInfo["offers"] = offers

            dict["subscription"] = subscriptionInfo
        }

        return dict
    }

    private func transactionToDictionary(_ transaction: Transaction) -> [String: Any] {
        var dict: [String: Any] = [
            "purchaseToken": String(transaction.id),
            "productId": transaction.productID,
            "orderId": String(transaction.originalID),
            "purchaseTime": transaction.purchaseDate.timeIntervalSince1970 * 1000, // milliseconds
            "isAcknowledged": true // StoreKit auto-finishes
        ]

        // Add subscription info if available
        if let expirationDate = transaction.expirationDate {
            dict["expirationTime"] = expirationDate.timeIntervalSince1970 * 1000
        }

        if let revocationDate = transaction.revocationDate {
            dict["revocationTime"] = revocationDate.timeIntervalSince1970 * 1000
        }

        dict["isAutoRenewing"] = transaction.expirationDate != nil

        return dict
    }
}
