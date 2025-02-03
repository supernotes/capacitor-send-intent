import Foundation
import Capacitor

public final class ShareStore {
    public static let store = ShareStore()

    public var shareItems: [JSObject]
    public var processed: Bool

    private init() {
        self.shareItems = []
        self.processed = false
    }
}
