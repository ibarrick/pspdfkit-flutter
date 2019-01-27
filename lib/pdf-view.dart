import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/rendering.dart';

class PdfView extends StatefulWidget {
	final String uri;
	final Function onPlatformViewCreated;
	final String documentName;
	final bool disableFormEditing;

	PdfView(
		{this.uri, this.documentName, this.onPlatformViewCreated, this.disableFormEditing});

	@override
	State<StatefulWidget> createState() => _PdfViewState();
}

class _PdfViewState extends State<PdfView> {
	String uri;
	int knowsPosition = 0;
	Rect _rect = null;

	@override
	Widget build(BuildContext context2) {
		return Container(
			child: AndroidView(
				viewType: 'com.pspdfkit.flutter/pdfview',
				onPlatformViewCreated: (int id) {
					if (widget.onPlatformViewCreated != null) {
						widget.onPlatformViewCreated(new PdfViewController._(id));
					}
				},
				creationParams:
				{
					'uri': widget.uri,
					'documentName': widget.documentName,
					'disableFormEditing': widget.disableFormEditing
				}
				,
				creationParamsCodec: StandardMessageCodec()
			)
		);
	}
}

class PdfViewController {
	PdfViewController._(int id)
		: _channel = new MethodChannel('com.pspdfkit.flutter/pdfview_$id');

	final MethodChannel _channel;

	Future<void> toggleFormEditing() async {
		return _channel.invokeMethod('toggleFormEditing');
	}

	Future<void> collectSignature() async {
		return _channel.invokeMethod('collectSignature');
	}

	Future<void> incrementPage() async {
		return _channel.invokeMethod('incrementPage');
	}

	Future<void> decrementPage() async {
		return _channel.invokeMethod('decrementPage');
	}

	Future<void> setPage(int i) async {
		return _channel.invokeMethod('setPage', {'page': i});
	}

	Future<void> reloadDocument() async {
		return _channel.invokeMethod('reloadDocument');
	}
}
