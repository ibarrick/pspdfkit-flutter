//#import "PspdfkitPlugin.h"
import PSPDFKitSwift;
//import PSPDFKit;
import PSPDFKitUI;

class PspdfkitPlugin : NSObject, FlutterPlugin {
    static func register(with registrar: FlutterPluginRegistrar) {
        let channel:FlutterMethodChannel! = FlutterMethodChannel(name: "pspdfkit",binaryMessenger:registrar.messenger())
        let instance:PspdfkitPlugin! = PspdfkitPlugin()
        registrar.addMethodCallDelegate(instance, channel:channel)
    }
    

    func handleMethodCall(call:FlutterMethodCall!, result:FlutterResult) {
        let dic = call.arguments as! [String: Any]
        if ("frameworkVersion" == call.method) {
            result("iOS " + PSPDFKit.versionNumber)
        } else if ("setLicenseKey" == call.method) {
            let licenseKey = dic["licenseKey"] as! String
            PSPDFKit.setLicenseKey(licenseKey);
        
//        } else if ("present" == call.method) {
//            let documentPath:String! = call.arguments["document"]
//
//            let document:PSPDFDocument! = self.PSPDFDocument(documentPath)
//            let pdfViewController:PSPDFViewController! = PSPDFViewController(document:document)
//
//            let navigationController:UINavigationController! = UINavigationController(rootViewController:pdfViewController)
//            let presentingViewController:UIViewController! = UIApplication.sharedApplication().delegate.window.rootViewController
//            presentingViewController.presentViewController(navigationController, animated:true, completion:nil)
//        } else {
        } else {
                result(FlutterMethodNotImplemented)
            }
    }

//    func PSPDFDocument(string:String!) -> PSPDFDocument! {
//        var url:NSURL!
//
//        if string.hasPrefix("/") {
//            url = NSURL.fileURLWithPath(string)
//        } else {
//            url = NSBundle.mainBundle.URLForResource(string, withExtension:nil)
//        }
//
//        let fileExtension:String! = url.pathExtension.lowercaseString
//        let isImageFile:Bool = (fileExtension == "png") || (fileExtension == "jpeg") || (fileExtension == "jpg")
//        if isImageFile {
//            return PSPDFImageDocument(imageURL:url)
//        } else {
//            return PSPDFDocument(URL:url)
//        }
//    }
}

