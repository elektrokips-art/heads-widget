import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'l10n/app_locale.dart';

const _keyThemeMode = 'app_theme_mode';
const _keyLocale = 'app_locale';

/// Holds the app's theme and language, persisted locally. Deliberately not a
/// state-management package -- the app is small enough that a single
/// ChangeNotifier threaded through constructors is simpler than a dependency.
class AppSettingsController extends ChangeNotifier {
  ThemeMode themeMode = ThemeMode.light;
  AppLocale locale = AppLocale.ru;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    themeMode = prefs.getString(_keyThemeMode) == 'dark' ? ThemeMode.dark : ThemeMode.light;
    locale = AppLocale.fromCode(prefs.getString(_keyLocale));
    notifyListeners();
  }

  Future<void> setDarkMode(bool dark) async {
    themeMode = dark ? ThemeMode.dark : ThemeMode.light;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyThemeMode, dark ? 'dark' : 'light');
  }

  Future<void> setLocale(AppLocale newLocale) async {
    locale = newLocale;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyLocale, newLocale.code);
  }
}
