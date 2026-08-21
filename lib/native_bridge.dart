import 'package:flutter/services.dart';

class BondedDevice {
  final String name;
  final String address;

  const BondedDevice({required this.name, required this.address});
}

class BatteryStatus {
  final int? left;
  final int? right;
  final int? caseLevel;
  final int? fallback;

  const BatteryStatus({this.left, this.right, this.caseLevel, this.fallback});

  bool get hasSonyData => left != null || right != null || caseLevel != null;
}

enum AncMode { off, noiseCancelling, ambientSound }

String _ancModeToWire(AncMode mode) => switch (mode) {
      AncMode.off => 'OFF',
      AncMode.noiseCancelling => 'NOISE_CANCELLING',
      AncMode.ambientSound => 'AMBIENT_SOUND',
    };

AncMode? _ancModeFromWire(String? name) => switch (name) {
      'OFF' => AncMode.off,
      'NOISE_CANCELLING' => AncMode.noiseCancelling,
      'AMBIENT_SOUND' => AncMode.ambientSound,
      _ => null,
    };

class WidgetSettings {
  final int color; // ARGB
  final int opacityPercent;
  final int cornerRadiusDp;
  final int refreshIntervalMin;

  const WidgetSettings({
    required this.color,
    required this.opacityPercent,
    required this.cornerRadiusDp,
    required this.refreshIntervalMin,
  });
}

/// Thin wrapper over the native Bluetooth code in
/// android/app/src/main/kotlin/com/gesys/linkbuds_widget/. All Bluetooth
/// logic lives natively so the background widget updater can reuse the exact
/// same code path as this app screen.
class NativeBridge {
  static const _channel = MethodChannel('linkbuds_widget/native');

  static Future<List<BondedDevice>> getBondedDevices() async {
    final result = await _channel.invokeMethod<List<Object?>>('getBondedDevices');
    return (result ?? [])
        .cast<Map<Object?, Object?>>()
        .map((m) => BondedDevice(
              name: m['name'] as String,
              address: m['address'] as String,
            ))
        .toList();
  }

  /// Saves the selected device for the background widget updater, then
  /// immediately polls both battery sources (Sony protocol first, hidden
  /// system API as fallback) and returns the result.
  static Future<BatteryStatus> saveSelectedDevice(String address, String name) async {
    final result = await _channel.invokeMethod<Map<Object?, Object?>>(
      'saveSelectedDevice',
      {'address': address, 'name': name},
    );
    return BatteryStatus(
      left: result?['left'] as int?,
      right: result?['right'] as int?,
      caseLevel: result?['case'] as int?,
      fallback: result?['fallback'] as int?,
    );
  }

  static Future<WidgetSettings> getWidgetSettings() async {
    final result = await _channel.invokeMethod<Map<Object?, Object?>>('getWidgetSettings');
    // Native returns the color as a signed 32-bit int (e.g. 0xFF4527A0 comes back negative
    // because alpha=FF sets the sign bit) -- mask back to the unsigned form so it compares
    // equal to the positive 0xFFxxxxxx literals used for the preset swatches.
    final rawColor = result?['color'] as int? ?? 0xFF4527A0;
    return WidgetSettings(
      color: rawColor & 0xFFFFFFFF,
      opacityPercent: result?['opacityPercent'] as int? ?? 100,
      cornerRadiusDp: result?['cornerRadiusDp'] as int? ?? 16,
      refreshIntervalMin: result?['refreshIntervalMin'] as int? ?? 30,
    );
  }

  static Future<void> saveWidgetSettings(WidgetSettings settings) {
    return _channel.invokeMethod('saveWidgetSettings', {
      'color': settings.color,
      'opacityPercent': settings.opacityPercent,
      'cornerRadiusDp': settings.cornerRadiusDp,
      'refreshIntervalMin': settings.refreshIntervalMin,
    });
  }

  /// Null means either the brand has no known ANC control (BBK/Apple aren't wired up) or the
  /// read failed -- both cases should just hide the ANC control in the UI.
  static Future<AncMode?> getAncMode(String address, String name) async {
    final result = await _channel.invokeMethod<String>('getAncMode', {'address': address, 'name': name});
    return _ancModeFromWire(result);
  }

  static Future<bool> setAncMode(String address, String name, AncMode mode) async {
    final result = await _channel.invokeMethod<bool>('setAncMode', {
      'address': address,
      'name': name,
      'mode': _ancModeToWire(mode),
    });
    return result ?? false;
  }
}
