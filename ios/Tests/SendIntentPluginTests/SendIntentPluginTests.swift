import XCTest
import Capacitor
@testable import SendIntentPlugin

final class SendIntentPluginTests: XCTestCase {
    var plugin: SendIntentPlugin!
    var mockBridge: CAPBridge!
    
    override func setUp() {
        super.setUp()
        plugin = SendIntentPlugin()
        mockBridge = CAPBridge()
        plugin.bridge = mockBridge
    }
    
    override func tearDown() {
        plugin = nil
        mockBridge = nil
        super.tearDown()
    }
    
    func testCheckSendIntentReceivedWithNoItems() {
        let call = CAPPluginCall(callbackId: "test", options: [:], success: { _ in }, error: { _ in })
        plugin.checkSendIntentReceived(call)
        
        XCTAssertEqual(plugin.store.shareItems.count, 0)
        XCTAssertTrue(plugin.store.processed)
    }
    
    func testCheckSendIntentReceivedWithSingleItem() {
        let shareItem = JSObject()
        shareItem["title"] = "Test Title"
        shareItem["description"] = "Test Description"
        shareItem["type"] = "text/plain"
        shareItem["url"] = "https://example.com"
        
        plugin.store.shareItems = [shareItem]
        plugin.store.processed = false
        
        let call = CAPPluginCall(callbackId: "test", options: [:], success: { result in
            XCTAssertEqual(result.get("title") as? String, "Test Title")
            XCTAssertEqual(result.get("description") as? String, "Test Description")
            XCTAssertEqual(result.get("type") as? String, "text/plain")
            XCTAssertEqual(result.get("url") as? String, "https://example.com")
            XCTAssertEqual((result.get("additionalItems") as? [JSObject])?.count, 0)
        }, error: { _ in })
        
        plugin.checkSendIntentReceived(call)
        XCTAssertTrue(plugin.store.processed)
    }
    
    func testCheckSendIntentReceivedWithMultipleItems() {
        let item1 = JSObject()
        item1["title"] = "Item 1"
        item1["type"] = "text/plain"
        item1["url"] = "https://example.com/1"
        
        let item2 = JSObject()
        item2["title"] = "Item 2"
        item2["type"] = "text/plain"
        item2["url"] = "https://example.com/2"
        
        plugin.store.shareItems = [item1, item2]
        plugin.store.processed = false
        
        let call = CAPPluginCall(callbackId: "test", options: [:], success: { result in
            XCTAssertEqual(result.get("title") as? String, "Item 1")
            XCTAssertEqual(result.get("type") as? String, "text/plain")
            XCTAssertEqual(result.get("url") as? String, "https://example.com/1")
            
            let additionalItems = result.get("additionalItems") as? [JSObject]
            XCTAssertEqual(additionalItems?.count, 1)
            XCTAssertEqual(additionalItems?[0].get("title") as? String, "Item 2")
            XCTAssertEqual(additionalItems?[0].get("url") as? String, "https://example.com/2")
        }, error: { _ in })
        
        plugin.checkSendIntentReceived(call)
        XCTAssertTrue(plugin.store.processed)
    }
    
    func testFinish() {
        let call = CAPPluginCall(callbackId: "test", options: [:], success: { _ in }, error: { _ in })
        plugin.finish(call)
        // Finish is a no-op that just resolves, so we just verify it doesn't crash
    }
}