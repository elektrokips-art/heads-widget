import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';

import 'app_settings.dart';
import 'l10n/app_locale.dart';
import 'l10n/app_text.dart';
import 'native_bridge.dart';
import 'widgets/dev_contact_button.dart';

const _intervalPresets = [15, 30, 60, 120];
const _colorPresets = <int>[
  0xFF4527A0, // deep purple (default)
  0xFF1565C0, // blue
  0xFF00695C, // teal
  0xFF2E7D32, // green
  0xFFC62828, // red
  0xFFE65100, // orange
  0xFF212121, // near-black
];

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _loading = true;
  int _color = 0xFF4527A0;
  int _opacity = 100;
  int _cornerRadius = 16;
  int _refreshInterval = 30;

  late AppText t;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final settings = await NativeBridge.getWidgetSettings();
    setState(() {
      _color = settings.color;
      _opacity = settings.opacityPercent;
      _cornerRadius = settings.cornerRadiusDp;
      _refreshInterval = settings.refreshIntervalMin;
      _loading = false;
    });
  }

  Future<void> _save() async {
    try {
      await NativeBridge.saveWidgetSettings(WidgetSettings(
        color: _color,
        opacityPercent: _opacity,
        cornerRadiusDp: _cornerRadius,
        refreshIntervalMin: _refreshInterval,
      ));
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(t.settingsSaved)));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(t.settingsSaveFailed(e))));
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<AppSettingsController>();
    t = AppText.of(settings.locale);

    return Scaffold(
      appBar: AppBar(title: Text(t.widgetSettingsTitle)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _buildPreview(),
                const SizedBox(height: 24),
                Text(t.refreshInterval, style: Theme.of(context).textTheme.titleMedium),
                Text(
                  t.refreshIntervalNote,
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: _intervalPresets.map((minutes) {
                    return ChoiceChip(
                      label: Text(minutes < 60 ? t.minutesShort(minutes) : t.hoursShort(minutes ~/ 60)),
                      selected: _refreshInterval == minutes,
                      onSelected: (_) => setState(() => _refreshInterval = minutes),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 24),
                Text(t.colorLabel, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: _colorPresets.map((c) {
                    final selected = c == _color;
                    return GestureDetector(
                      onTap: () => setState(() => _color = c),
                      child: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: Color(c),
                          shape: BoxShape.circle,
                          border: selected
                              ? Border.all(color: Theme.of(context).colorScheme.onSurface, width: 3)
                              : null,
                        ),
                      ),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 24),
                Text(t.opacityLabel(_opacity), style: Theme.of(context).textTheme.titleMedium),
                Slider(
                  value: _opacity.toDouble(),
                  min: 20,
                  max: 100,
                  divisions: 16,
                  label: '$_opacity%',
                  onChanged: (v) => setState(() => _opacity = v.round()),
                ),
                const SizedBox(height: 16),
                Text(t.cornerRadiusLabel(_cornerRadius), style: Theme.of(context).textTheme.titleMedium),
                Slider(
                  value: _cornerRadius.toDouble(),
                  min: 0,
                  max: 40,
                  divisions: 20,
                  label: '$_cornerRadius dp',
                  onChanged: (v) => setState(() => _cornerRadius = v.round()),
                ),
                const SizedBox(height: 24),
                FilledButton(onPressed: _save, child: Text(t.saveButton)),
                const SizedBox(height: 32),
                const Divider(),
                const SizedBox(height: 8),
                Text(t.themeLabel, style: Theme.of(context).textTheme.titleMedium),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(t.darkModeLabel),
                  value: settings.themeMode == ThemeMode.dark,
                  onChanged: (dark) => settings.setDarkMode(dark),
                ),
                const SizedBox(height: 16),
                Text(t.languageLabel, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                DropdownButton<AppLocale>(
                  value: settings.locale,
                  isExpanded: true,
                  items: AppLocale.values
                      .map((locale) => DropdownMenuItem(value: locale, child: Text(locale.nativeName)))
                      .toList(),
                  onChanged: (locale) {
                    if (locale != null) settings.setLocale(locale);
                  },
                ),
                const SizedBox(height: 32),
                const Center(child: DevContactButton(label: 'GeSys')),
                const SizedBox(height: 12),
                Center(
                  child: FutureBuilder<PackageInfo>(
                    future: PackageInfo.fromPlatform(),
                    builder: (context, snap) {
                      final info = snap.data;
                      if (info == null) return const SizedBox.shrink();
                      return Text(
                        'Heads Widget v${info.version} (${info.buildNumber})',
                        style: const TextStyle(fontSize: 11, color: Colors.grey),
                      );
                    },
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildPreview() {
    return Center(
      child: Container(
        width: 220,
        height: 100,
        decoration: BoxDecoration(
          color: Color(_color).withValues(alpha: _opacity / 100),
          borderRadius: BorderRadius.circular(_cornerRadius.toDouble()),
        ),
        alignment: Alignment.center,
        child: Text(
          t.previewText,
          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}
