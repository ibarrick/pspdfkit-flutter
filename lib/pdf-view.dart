import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/rendering.dart';

class PdfView extends StatefulWidget {
  final String uri;
  final Map<String, num> rect;

  PdfView({this.uri, this.rect});

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
        child: _PdfViewPlaceholder(
            onRectChanged: (Rect value) {
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (_rect == null) {
                  setState(() {
                    _rect = value;
                    print(_rect);
                  });
                }
              });
            },
            child: _rect != null ? AndroidView(
                viewType: 'com.pspdfkit.flutter/pdfview',
                creationParams:
                {
                  'uri': widget.uri,
                  'rect': {
                    'top': 80.0,
                    'left': 40.0,
                    'width': 500.0,
                    'height': 500.0
                  }
                }
                ,
                creationParamsCodec: StandardMessageCodec()
            ) : SizedBox()
        )
    );
  }
}

class _PdfViewPlaceholder extends SingleChildRenderObjectWidget {
  const _PdfViewPlaceholder({
    Key key,
    @required this.onRectChanged,
    Widget child
  }) : super(key: key, child: child);

  final ValueChanged<Rect> onRectChanged;

  @override
  RenderObject createRenderObject(BuildContext context) {
    return _PdfViewPlaceholderRender(
        onRectChanged: onRectChanged
    );
  }

  @override
  void updateRenderObject(BuildContext context,
      _PdfViewPlaceholderRender renderObject) {
    renderObject..onRectChanged = onRectChanged;
  }
}

class _PdfViewPlaceholderRender extends RenderProxyBox {
  _PdfViewPlaceholderRender({
    RenderBox child,
    ValueChanged<Rect> onRectChanged
  })
      : _callback = onRectChanged,
        super(child);

  ValueChanged<Rect> _callback;
  Rect _rect;

  Rect get rect => _rect;

  set onRectChanged(ValueChanged<Rect> callback) {
    if (callback != _callback) {
      _callback = callback;
      notifyRect();
    }
  }

  void notifyRect() {
    if (_callback != null && _rect != null) {
      _callback(_rect);
    }
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    super.paint(context, offset);
    final rect = offset & size;
    if (_rect != rect) {
      _rect = rect;
      notifyRect();
    }
  }
}

