import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:provider/provider.dart';

import 'app_settings.dart';
import 'l10n/app_text.dart';
import 'native_bridge.dart';
import 'settings_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final settings = AppSettingsController();
  await settings.load();
  runApp(
    ChangeNotifierProvider.value(
      value: settings,
      child: const LinkBudsDiagnosticApp(),
    ),
  );
}

class LinkBudsDiagnosticApp extends StatelessWidget {
  const LinkBudsDiagnosticApp({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<AppSettingsController>();
    return MaterialApp(
      title: 'Heads Widget',
      debugShowCheckedModeBanner: false,
      themeMode: settings.themeMode,
      theme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: Colors.deepPurple,
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      home: const DiagnosticScreen(),
    );
  }
}

class DiagnosticScreen extends StatefulWidget {
  const DiagnosticScreen({super.key});

  @override
  State<DiagnosticScreen> createState() => _DiagnosticScreenState();
}

class _DiagnosticScreenState extends State<DiagnosticScreen> {
  bool _permissionGranted = false;
  bool _loadingDevices = false;
  String? _error;
  List<BondedDevice> _devices = [];
  BondedDevice? _selected;

  bool _checking = false;
  BatteryStatus? _status;
  bool _saved = false;

  AncMode? _ancMode;
  bool _ancBusy = false;

  // Refreshed at the top of every build() from Provider; cached here (rather than read via
  // context.watch on every access) so it's also safe to use from callbacks/error handlers,
  // where context.watch would throw since Provider only allows watch() during build.
  late AppText t;

  @override
  void initState() {
    super.initState();
    _requestPermissionAndLoad();
  }

  Future<void> _requestPermissionAndLoad() async {
    // bluetoothScan is only actually used for Apple AirPods/Beats (passive BLE beacon
    // scanning); requesting it upfront alongside bluetoothConnect keeps the permission
    // flow to a single prompt instead of a second one appearing later per-brand.
    final statuses = await [
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
    ].request();
    final granted = statuses[Permission.bluetoothConnect]?.isGranted ?? false;
    setState(() => _permissionGranted = granted);
    if (granted) {
      await _loadDevices();
    }
  }

  Future<void> _loadDevices() async {
    setState(() {
      _loadingDevices = true;
      _error = null;
    });
    try {
      final devices = await NativeBridge.getBondedDevices();
      setState(() {
        _devices = devices;
        _loadingDevices = false;
      });
    } catch (e) {
      setState(() {
        _error = t.devicesListError(e);
        _loadingDevices = false;
      });
    }
  }

  Future<void> _saveAndCheck() async {
    final device = _selected;
    if (device == null) return;
    setState(() {
      _checking = true;
      _status = null;
    });
    try {
      final status = await NativeBridge.saveSelectedDevice(
        device.address,
        device.name,
      );
      if (!mounted) return;
      setState(() {
        _status = status;
        _saved = true;
        _checking = false;
      });
      _loadAncMode(device);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = t.checkError(e);
        _checking = false;
      });
    }
  }

  /// Null result means the brand has no known ANC control or the read failed --
  /// either way the section just stays hidden.
  Future<void> _loadAncMode(BondedDevice device) async {
    final mode = await NativeBridge.getAncMode(device.address, device.name);
    if (!mounted || _selected != device) return;
    setState(() => _ancMode = mode);
  }

  Future<void> _setAncMode(AncMode mode) async {
    final device = _selected;
    if (device == null) return;
    setState(() => _ancBusy = true);
    final success = await NativeBridge.setAncMode(
      device.address,
      device.name,
      mode,
    );
    if (!mounted) return;
    setState(() {
      _ancBusy = false;
      if (success) _ancMode = mode;
    });
    if (!success) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(t.ancSetFailed)));
    }
  }

  @override
  Widget build(BuildContext context) {
    t = AppText.of(context.watch<AppSettingsController>().locale);
    return Scaffold(
      appBar: AppBar(
        title: Text(t.appTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.palette_outlined),
            tooltip: t.widgetSettingsTooltip,
            onPressed: () => Navigator.of(
              context,
            ).push(MaterialPageRoute(builder: (_) => const SettingsScreen())),
          ),
        ],
      ),
      body: !_permissionGranted
          ? _buildPermissionRequest()
          : _buildDeviceList(),
    );
  }

  Widget _buildPermissionRequest() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(t.permissionExplanation, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _requestPermissionAndLoad,
              child: Text(t.requestPermission),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: openAppSettings,
              child: Text(t.openAppSettingsAction),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDeviceList() {
    if (_loadingDevices) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Padding(padding: const EdgeInsets.all(24), child: Text(_error!)),
      );
    }
    if (_devices.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(t.noBondedDevices, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: _loadDevices,
                child: Text(t.refreshList),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Text(
            t.selectDevicePrompt,
            style: Theme.of(context).textTheme.titleMedium,
          ),
        ),
        Expanded(
          child: RadioGroup<BondedDevice>(
            groupValue: _selected,
            onChanged: (v) => setState(() {
              _selected = v;
              _status = null;
              _saved = false;
              _ancMode = null;
            }),
            child: ListView.builder(
              itemCount: _devices.length,
              itemBuilder: (context, index) {
                final d = _devices[index];
                return RadioListTile<BondedDevice>(
                  title: Text(d.name),
                  subtitle: Text(d.address),
                  value: d,
                );
              },
            ),
          ),
        ),
        if (_selected != null) _buildCheckPanel(),
      ],
    );
  }

  Widget _buildCheckPanel() {
    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.5,
      ),
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceContainerHighest,
          borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
        ),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              FilledButton.icon(
                onPressed: _checking ? null : _saveAndCheck,
                icon: _checking
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.battery_std),
                label: Text(_checking ? t.connecting : t.saveAndCheck),
              ),
              const SizedBox(height: 12),
              if (_status != null) _buildStatus(_status!),
              if (_ancMode != null) ...[
                const SizedBox(height: 16),
                Text(
                  t.ancSectionTitle,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 8),
                SegmentedButton<AncMode>(
                  segments: [
                    ButtonSegment(
                      value: AncMode.off,
                      label: Text(t.ancModeOff),
                    ),
                    ButtonSegment(
                      value: AncMode.noiseCancelling,
                      label: Text(t.ancModeNoiseCancelling),
                    ),
                    ButtonSegment(
                      value: AncMode.ambientSound,
                      label: Text(t.ancModeAmbientSound),
                    ),
                  ],
                  selected: {_ancMode!},
                  onSelectionChanged: _ancBusy
                      ? null
                      : (selection) => _setAncMode(selection.first),
                ),
              ],
              if (_saved) ...[
                const SizedBox(height: 12),
                const Divider(),
                const SizedBox(height: 4),
                Text(
                  t.widgetSavedInstructions,
                  style: const TextStyle(fontSize: 12),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatus(BatteryStatus status) {
    if (status.hasSonyData) {
      return Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _batteryCell(t.left, status.left),
          _batteryCell(t.right, status.right),
          _batteryCell(t.caseLabel, status.caseLevel),
        ],
      );
    }
    if (status.fallback != null) {
      return Text(
        t.fallbackOnly(status.fallback!),
        textAlign: TextAlign.center,
      );
    }
    return Text(
      t.noDataAtAll,
      textAlign: TextAlign.center,
      style: const TextStyle(color: Colors.orange),
    );
  }

  Widget _batteryCell(String label, int? value) {
    return Column(
      children: [
        Text(
          value != null ? '$value%' : '—',
          style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
        ),
        Text(label, style: const TextStyle(fontSize: 11)),
      ],
    );
  }
}
