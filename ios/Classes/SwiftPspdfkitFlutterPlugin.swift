import Flutter
import UIKit
import PSPDFKit

func getDocumentsDirectory() -> URL {
    let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
    let documentsDirectory = paths[0]
    return documentsDirectory
}

public class SwiftPspdfkitFlutterPlugin: NSObject, FlutterPlugin {
    static var openPdfs: [String: Document] = [:]
    
    static var tempDocuments: [String: String] = [:]
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "pspdfkit", binaryMessenger: registrar.messenger())
        let instance = SwiftPspdfkitFlutterPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        let viewFactory = PdfViewFactory.init(messenger: registrar.messenger());
        registrar.register(viewFactory, withId: "PdfView");
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let dic = call.arguments as? [String: Any];
        switch call.method {
        case "setLicenseKey":
            let licenseKey:String = dic?["licenseKey"] as! String;
            PSPDFKit.SDK.setLicenseKey(licenseKey);
            result(nil);
        case "openPdfDocument":
            var documentPath:String = dic!["uri"] as! String;
            let name:String = dic!["name"] as! String;
            let document = Document.init(url: URL.init(fileURLWithPath: documentPath));
			document.clearCache();
            SwiftPspdfkitFlutterPlugin.openPdfs[name] = document;
            result(nil);
        case "checkPdf":
            let name:String = dic!["name"] as! String;
            result(SwiftPspdfkitFlutterPlugin.openPdfs[name] != nil);
        case "getPageCount":
            let name:String = dic!["name"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            result(doc.pageCount);
        case "fillPdfForm":
            let name:String = dic!["name"] as! String;
            let fields:[String:Any] = dic!["fields"] as! [String:Any];
            let document:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            for i in 0..<(document.pageCount) {
                let annotations = document.annotationsForPage(at: i, type: .widget);
                for annotation in annotations {
                    switch annotation {
                    case let textField as TextFieldFormElement:
                        let fieldName:String! = textField.fieldName;
                        if (fields[fieldName] != nil) {
                            let value = fields[fieldName] as? String;
                            textField.contents = value;
                        } else {
                            continue;
                        }
                    case let checkbox as ButtonFormElement:
                        let fieldName:String! = checkbox.fieldName;
                        if (fields[fieldName] != nil) {
                            let checked:Bool = fields[fieldName] as? Bool ?? false;
                            if (checked) {
                                checkbox.toggleButtonSelectionState();
                            }
                        } else {
                            continue;
                        }
                    default:
                        break;
                    }
                }
            }
            result(nil);
        case "fillInvoiceNum":
            let name:String = dic!["name"] as! String;
            let invoiceNumber:String = dic!["invoiceNumber"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            for i in 0..<doc.pageCount {
                let annotations = doc.annotationsForPage(at: i, type: .widget);
                for annotation in annotations {
                    if (annotation is TextFieldFormElement) {
                        let textField:TextFieldFormElement = annotation as! TextFieldFormElement;
                        if (textField.fieldName!.contains("Invoice-Num")) {
                            annotation.contents = invoiceNumber;
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
            result(nil);
        case "flattenPdfForm":
            let name:String = dic!["name"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            let configuration = Processor.Configuration(document: doc);
            configuration?.modifyForms(of: PDFFormField.Kind.text, change: AnnotationChange.flatten);
            configuration?.modifyForms(of: PDFFormField.Kind.checkBox, change: AnnotationChange.flatten);
            configuration?.modifyForms(of: PDFFormField.Kind.radioButton, change: AnnotationChange.flatten);
            configuration?.modifyForms(of: PDFFormField.Kind.listBox, change: AnnotationChange.flatten);
            do {
                let processor = Processor(configuration: configuration!, securityOptions: nil);
                var documents = getDocumentsDirectory();
                let uuid = UUID().uuidString
                let fileName = "name" + uuid + "-temp.pdf";
                documents.appendPathComponent(fileName);
                try processor.write(toFileURL: documents);
                let newDoc:Document = Document.init(url: documents);
                SwiftPspdfkitFlutterPlugin.openPdfs[name] = newDoc;
                SwiftPspdfkitFlutterPlugin.tempDocuments[fileName] = name;
                newDoc
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "clearFiles":
            do {
                let documents = getDocumentsDirectory();
                let documentsPath = documents.path;
                let fileManager = FileManager.default;
                let fileNames = try fileManager.contentsOfDirectory(atPath: documentsPath);
                for fileName in fileNames {
                    if (fileName.hasSuffix("-temp.pdf")) {
                        let docName = SwiftPspdfkitFlutterPlugin.tempDocuments[fileName];
                        if (docName != nil) {
                            // this should trigger removal of document since this will be last reference (I hope)
                            SwiftPspdfkitFlutterPlugin.openPdfs[docName] = nil;
                        }
                        let filePath = "\(documentsPath)/\(fileName)";
                        if (fileManager.fileExists(atPath: filePath)) {
                            try fileManager.removeItem(atPath: filePath);
                        }
                    }
                }
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "flattenSignatures":
            let name:String = dic!["name"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            let configuration = Processor.Configuration(document: doc);
            configuration?.modifyForms(of: PDFFormField.Kind.signature, change: AnnotationChange.flatten);
            do {
                let processor = Processor(configuration: configuration!, securityOptions: nil);
                var documents = getDocumentsDirectory();
                let uuid = UUID().uuidString
                documents.appendPathComponent("name" + uuid + "-temp.pdf");
                try processor.write(toFileURL: documents);
                let newDoc:Document = Document.init(url: documents);
                SwiftPspdfkitFlutterPlugin.openPdfs[name] = newDoc;
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "mergePdfs":
            let names:[String] = dic!["names"] as! [String];
            let outputPath:String = dic!["outputPath"] as! String;
            let firstDoc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[names.first!]!;
            var index:Int = Int(firstDoc.pageCount);
            let configuration = Processor.Configuration(document: firstDoc);
            if (names.count > 1) {
                for i in 1..<(names.count) {
                    let tempDoc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[names[i]]!;
                    for j in 0..<tempDoc.pageCount {
                        configuration?.addNewPage(at: PageIndex(index), configuration: PDFNewPageConfiguration(pageTemplate: PageTemplate.init(document: tempDoc, sourcePageIndex: UInt(j)), builderBlock: nil));
                    }
                    index = index + Int(tempDoc.pageCount);
                }
            }
            do {
                let processor:Processor = Processor(configuration: configuration!, securityOptions: nil);
                try processor.write(toFileURL: URL.init(fileURLWithPath: outputPath));
                let mergeDoc = Document.init(url: URL.init(fileURLWithPath: outputPath));
                SwiftPspdfkitFlutterPlugin.openPdfs["merged"] = mergeDoc;
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "savePdf":
            do {
                let name:String = dic!["name"] as! String;
                let outputPath:String = dic!["outputPath"] as! String;
                let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
                let configuration = Processor.Configuration(document: doc);
                let processor:Processor = Processor(configuration: configuration!, securityOptions: nil);
                try processor.write(toFileURL: URL.init(fileURLWithPath: outputPath));
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "checkSignature":
            let name:String = dic!["name"] as! String;
            let doc = SwiftPspdfkitFlutterPlugin.openPdfs[name];
            var signed:Bool = false;
            if (doc == nil) {
                result(false);
                return;
            }
            for i in 0..<(doc?.pageCount)! {
                let annotations = doc?.annotationsForPage(at: i, type: .widget);
                for annotation in annotations! {
                    if (annotation is SignatureFormElement) {
                        let sig:SignatureFormElement = annotation as! SignatureFormElement;
                        let ink = sig.overlappingInkSignature;
                        if(ink != nil && (ink?.isSignature)!) {
                            signed = true;
                        }
                    }
                }
            }
            result(signed);
        case "getSignatures":
            let name:String = dic!["name"] as! String;
            let doc = SwiftPspdfkitFlutterPlugin.openPdfs[name];
            var ret:[String:Any] = [:];
            for i in 0..<(doc?.pageCount)! {
                let annotations = doc?.annotationsForPage(at: i, type: .widget);
                
                for annotation in annotations! {
                    if (annotation is SignatureFormElement) {
                        let sig:SignatureFormElement = annotation as! SignatureFormElement;
                        let ink = sig.overlappingInkSignature;
                        ret[sig.fieldName!] = ink != nil && (ink?.isSignature)!;
                    }
                }
            }
            result(ret);
        case "renameEstimateFields":
            let name:String = dic!["name"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            let index:String = dic!["index"] as! String;
            var mapping:[String:String] = [:];
            mapping["Signature"] = "EstimateSignature-" + index + "-";
            let configuration = Processor.Configuration(document: doc);
            configuration?.formFieldNameMappings = mapping;
            do {
                let processor = Processor(configuration: configuration!, securityOptions: nil);
                var documents = getDocumentsDirectory();
                let uuid = UUID().uuidString
                documents.appendPathComponent("name" + uuid + "-renamed-temp.pdf");
                try processor.write(toFileURL: documents);
                let newDoc:Document = Document.init(url: documents);
                SwiftPspdfkitFlutterPlugin.openPdfs[name] = newDoc;
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "renameInvoiceFields":
            let name:String = dic!["name"] as! String;
            let doc:Document = SwiftPspdfkitFlutterPlugin.openPdfs[name] as! Document;
            let uuid = UUID().uuidString
            var mapping:[String:String] = [:];
            for i in 0..<doc.pageCount {
                let annotations = doc.annotationsForPage(at: i, type: .widget);
                for annotation in annotations {
                    if (annotation is FormElement) {
                        let field:FormElement = annotation as! FormElement;
                        mapping[field.fieldName!] = field.fieldName! + uuid;
                    }
                }
            }
            let configuration = Processor.Configuration(document: doc);
            configuration?.formFieldNameMappings = mapping;
            do {
                let processor = Processor(configuration: configuration!, securityOptions: nil);
                var documents = getDocumentsDirectory();
                
                documents.appendPathComponent("name" + uuid + "-renamed-temp.pdf");
                try processor.write(toFileURL: documents);
                let newDoc:Document = Document.init(url: documents);
                SwiftPspdfkitFlutterPlugin.openPdfs[name] = newDoc;
                result(nil);
            } catch {
                result(FlutterError());
            }
        case "printDocument":
            result(nil);
        default:
            result(FlutterMethodNotImplemented);
        }
    }
}
