enum AppLocale {
  ru('ru', 'Русский'),
  uz('uz', 'Oʻzbekcha'),
  en('en', 'English'),
  tr('tr', 'Türkçe');

  const AppLocale(this.code, this.nativeName);

  final String code;
  final String nativeName;

  static AppLocale fromCode(String? code) {
    return AppLocale.values.firstWhere((l) => l.code == code, orElse: () => AppLocale.ru);
  }
}
