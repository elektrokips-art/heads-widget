import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

/// Chip-shaped icon (IC body with legs) with an "i" inside -- GeSys attribution mark,
/// reused from the minilab project's dev_contact_button.dart.
class ChipInfoIcon extends StatelessWidget {
  final double size;
  final Color color;
  const ChipInfoIcon({super.key, this.size = 20, required this.color});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(painter: _ChipPainter(color)),
    );
  }
}

class _ChipPainter extends CustomPainter {
  final Color color;
  _ChipPainter(this.color);

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width;
    final stroke = s * 0.09;
    final p = Paint()
      ..color = color
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    final bodyInset = s * 0.18;
    final body = Rect.fromLTRB(bodyInset, bodyInset, s - bodyInset, s - bodyInset);
    canvas.drawRRect(RRect.fromRectAndRadius(body, Radius.circular(s * 0.08)), p);

    final legOut = s * 0.05;
    final legIn = bodyInset;
    final positions = [0.35, 0.5, 0.65];
    for (final f in positions) {
      final c = s * f;
      canvas.drawLine(Offset(c, legOut), Offset(c, legIn), p);
      canvas.drawLine(Offset(c, s - legOut), Offset(c, s - legIn), p);
      canvas.drawLine(Offset(legOut, c), Offset(legIn, c), p);
      canvas.drawLine(Offset(s - legOut, c), Offset(s - legIn, c), p);
    }

    final fill = Paint()
      ..color = color
      ..style = PaintingStyle.fill;
    final cx = s * 0.5;
    canvas.drawCircle(Offset(cx, s * 0.38), s * 0.055, fill);
    final stem = Rect.fromLTWH(cx - s * 0.045, s * 0.47, s * 0.09, s * 0.18);
    canvas.drawRRect(RRect.fromRectAndRadius(stem, Radius.circular(s * 0.03)), fill);
  }

  @override
  bool shouldRepaint(covariant _ChipPainter old) => old.color != color;
}

/// Amber pill linking to the developer's Telegram -- opens externally, falls back to browser.
class DevContactButton extends StatelessWidget {
  final String label;
  const DevContactButton({super.key, required this.label});

  static final Uri _tg = Uri.parse('https://t.me/qudaro');

  static Future<void> open() async {
    if (!await launchUrl(_tg, mode: LaunchMode.externalApplication)) {
      await launchUrl(_tg);
    }
  }

  @override
  Widget build(BuildContext context) {
    const amber = Colors.amber;
    return InkWell(
      onTap: open,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: amber.withValues(alpha: 0.09),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: amber.withValues(alpha: 0.4)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const ChipInfoIcon(size: 18, color: amber),
            const SizedBox(width: 6),
            Text(label, style: const TextStyle(color: amber, fontSize: 13, fontWeight: FontWeight.w600)),
          ],
        ),
      ),
    );
  }
}
