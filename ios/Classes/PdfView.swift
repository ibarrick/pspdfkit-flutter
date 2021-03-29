import Flutter
import UIKit
import PSPDFKit
import PSPDFKitUI

func ScaleForSizeWithinSize(targetSize: CGSize, boundsSize:CGSize) -> CGFloat {
    let xScale = boundsSize.width / targetSize.width;
    let yScale = boundsSize.height / targetSize.height;
    let minScale = min(xScale, yScale);
    return minScale > 1.0 ? 1.0 : minScale;
}

var defaultFormColor: UIColor? = nil;

func fBuild(viewController: PDFViewController, signatureDelegate: SignatureViewControllerDelegate, documentName: String) -> FlutterMethodCallHandler {
    return {
        (call: FlutterMethodCall, result: FlutterResult) in
            let dic = call.arguments as? [String: Any];
            switch call.method {
            case "reloadDocument":
                viewController.document = SwiftPspdfkitFlutterPlugin.openPdfs[documentName];
                viewController.reloadData();
                result(nil);
            case "toggleFormEditing":
                let isEnabled:Bool = viewController.configuration.editableAnnotationTypes.contains(.widget);
                viewController.updateConfiguration(builder: {builder in
                    var editableAnnotationTypes = builder.editableAnnotationTypes
                    if (isEnabled) {
                        editableAnnotationTypes?.remove(Annotation.Tool.widget);
                    } else {
                        editableAnnotationTypes?.insert(Annotation.Tool.widget);
                    }
                    builder.editableAnnotationTypes = editableAnnotationTypes;
                });
                let doc = viewController.document!;
                if (isEnabled) {
                    doc.updateRenderOptions(for: RenderType.all, with: { (options :RenderOptions) -> Void in options.interactiveFormFillColor = UIColor.clear; })
                } else {
                    doc.updateRenderOptions(for: RenderType.all, with: { (options :RenderOptions) -> Void in options.interactiveFormFillColor = UIColor.blue.withAlphaComponent(0.2); })
                }
                viewController.reloadPage(at: viewController.pageIndex, animated: false)
                result(nil)
            case "incrementPage":
                viewController.setPageIndex((viewController.pageIndex + 1) % (viewController.document?.pageCount)!, animated: true);
                result(nil);
            case "decrementPage":
               
                if (viewController.pageIndex == 0) {
                    var tmp = viewController.document!.pageCount
                    tmp = tmp - PageCount(1);
                    let tempIndex = tmp;
                    viewController.setPageIndex(PageIndex(tempIndex), animated: true);
                } else {
                    let tempIndex = viewController.pageIndex - PageIndex(1);
                    viewController.setPageIndex(PageIndex(tempIndex), animated: true);
                }
                
                result(nil);
            case "setPage":
                let index = dic!["page"]
                viewController.setPageIndex(index as! PageIndex, animated: true);
                result(nil);
            case "collectSignature":
                let signatureController: SignatureViewController = SignatureViewController.init();
                signatureController.delegate = signatureDelegate;
                signatureController.savingStrategy = SignatureSavingStrategy.neverSave;
//                let signatureContainer:FlutterViewController = FlutterViewController.init();
//                signatureContainer.addChildViewController(viewController)
//                signatureController.modalPresentationStyle = .overCurrentContext;
                var options:[PresentationOption:Any] = [:];
                options[PresentationOption.inNavigationController] = true;
                signatureController.modalPresentationCapturesStatusBarAppearance = true;
                viewController.present(signatureController, options: options, animated: true, sender: nil, completion: nil);
                result(nil);
            default:
                result(FlutterMethodNotImplemented);
            }
    };
}


public class PdfView : NSObject, FlutterPlatformView {
    let frame: CGRect
    let viewId: Int64
    let viewController: PDFViewController
    var signatureDelegate: SignatureViewControllerDelegate
    var viewDelegate: PDFDocumentViewControllerDelegate
    
    init(_ frame: CGRect, viewId: Int64, args: Any?, messenger: FlutterBinaryMessenger) {
        self.frame = frame;
        self.viewId = viewId;
        let dic = args as! [String: Any];
        let configuration:PDFConfiguration = PDFConfiguration { builder in
            builder.shouldHideNavigationBarWithUserInterface = true;
            builder.signatureSavingStrategy = SignatureSavingStrategy.neverSave;
            builder.backgroundColor = UIColor(red: 224.0/255.0, green: 224.0/255.0, blue: 224.0/255.0, alpha: 1.0);
            var editableAnnotationTypes = Set<Annotation.Tool>();
            editableAnnotationTypes.insert(Annotation.Tool.signature);
            editableAnnotationTypes.insert(Annotation.Tool.widget);
            let disableFormEditing:Bool? = dic["disableFormEditing"] as? Bool
            if (disableFormEditing ?? false) {
                editableAnnotationTypes.remove(Annotation.Tool.widget);
            }
            builder.editableAnnotationTypes = editableAnnotationTypes;
            builder.overrideClass(ConflictResolutionManager.self, with: AlwaysReloadConflictResolver.self);
        }
        
        let documentName:String = dic["documentName"] as! String;
        let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[documentName]!;
        let disableFormEditing:Bool? = dic["disableFormEditing"] as? Bool
        if (disableFormEditing ?? false) {
            doc.updateRenderOptions(for: RenderType.all, with: {(options: RenderOptions) -> Void in options.interactiveFormFillColor = UIColor.clear;})
        }
        let messageChannel = FlutterBasicMessageChannel.init(name: "pspdfkit_messages", binaryMessenger: messenger, codec: FlutterJSONMessageCodec.sharedInstance());
        let vController = PDFViewController.init(document: doc, configuration: configuration);
        self.viewController = vController;
        self.signatureDelegate = SignatureDelegate.init(documentName: documentName);
        self.viewDelegate = PageNotifierDelegate.init(messageChannel: messageChannel);
        self.viewController.documentViewController?.delegate = self.viewDelegate;
        self.viewController.setUserInterfaceVisible(false, animated: false);
//        for i in 0...doc.pageCount {
//            for annotation in doc.annotationsForPage(at: i, type: .widget) {
//                if (annotation is PSPDFFormElement) {
//                    (annotation as! PSPDFFormElement).highlightColor = nil;
//                }
//            }
//        }
        let channel = FlutterMethodChannel(name: "com.pspdfkit.flutter/pdfview_" + String(viewId), binaryMessenger: messenger, codec: FlutterStandardMethodCodec.sharedInstance());
        channel.setMethodCallHandler(fBuild(viewController: vController, signatureDelegate: self.signatureDelegate, documentName: documentName));
    }
    
    public func view() -> UIView {
        return viewController.view;
    }
}

public class SignatureDelegate : NSObject, SignatureViewControllerDelegate {
    var documentName:String;
    
    init(documentName: String) {
        self.documentName = documentName;
    }
  
    public func signatureViewControllerDidFinish(_ signatureController: SignatureViewController, with signer: PDFSigner?, shouldSaveSignature: Bool) {
        let lines = signatureController.drawView.pointSequences.map{ $0.map({ NSValue.valueWithDrawingPoint($0) }) }
        //let lines = signatureController.lines;
        let document:Document = SwiftPspdfkitFlutterPlugin.openPdfs[documentName]!;
        for i in 0..<document.pageCount {
            for annotation in document.annotationsForPage(at: i, type: .widget) {
                if (annotation is SignatureFormElement) {
                    let name:String? = (annotation as! SignatureFormElement).fieldName
                    if (name != nil && !(name!.contains("Estimate"))) {
                        let boundingBox = annotation.boundingBox;
                        let pageInfo = document.pageInfoForPage(at: i)!;
                        var rect = CGRect.init();
                        rect.size = pageInfo.size;
                        let newLines = ConvertViewLines(pdfLines: lines, pageInfo: pageInfo, viewBounds: rect);
                        let newLinesPoints = newLines.map { $0.map({ $0.drawingPointValue }) }
                        var annotationSize = BoundingBoxFromLines(newLines, lineWidth: 1.0).size;
                        let maxSize = boundingBox.size;
                        let scale = ScaleForSizeWithinSize(targetSize: annotationSize, boundsSize: maxSize);
                        annotationSize = CGSize.init(width: lround(Double(annotationSize.width * scale)), height: lround(Double(annotationSize.height * scale)));
                        let ink = InkAnnotation.init(lines: newLinesPoints);
                        ink.absolutePageIndex = i;
                        let x = boundingBox.minX + (boundingBox.width - annotationSize.width) / 2.0;
                        let y = boundingBox.minY;
                        ink.boundingBox = CGRect.init(x: x, y: y, width: annotationSize.width, height: annotationSize.height);
                        document.add(annotations: [ink], options: nil);
                    }
                }
            }
        }
        signatureController.dismiss(animated: true, completion: nil);
    }
}

public class PageNotifierDelegate : NSObject, PDFDocumentViewControllerDelegate {
    let messageChannel:FlutterBasicMessageChannel
    
    init(messageChannel: FlutterBasicMessageChannel) {
        self.messageChannel = messageChannel;
    }
    public func documentViewController(_ documentViewController: PDFDocumentViewController, didChangeSpreadIndex oldSpreadIndex: Int) {
        var msg:[String:Int] = [:];
        msg["page"] = documentViewController.spreadIndex;
        messageChannel.sendMessage(msg);
    }
}

public class AlwaysReloadConflictResolver : ConflictResolutionManager {
    override public func controllerForExternalFileChangeResolution(on: Document, dataProvider: CoordinatedFileDataProviding) -> UIViewController? {
        return nil;
    }
}
