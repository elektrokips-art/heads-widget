import 'app_locale.dart';

abstract class AppText {
  const AppText();

  static AppText of(AppLocale locale) {
    switch (locale) {
      case AppLocale.ru:
        return const AppTextRu();
      case AppLocale.uz:
        return const AppTextUz();
      case AppLocale.en:
        return const AppTextEn();
      case AppLocale.tr:
        return const AppTextTr();
    }
  }

  // Main screen
  String get appTitle;
  String get widgetSettingsTooltip;
  String get permissionExplanation;
  String get requestPermission;
  String get openAppSettingsAction;
  String devicesListError(Object error);
  String get noBondedDevices;
  String get refreshList;
  String get selectDevicePrompt;
  String get saveAndCheck;
  String get connecting;
  String get widgetSavedInstructions;
  String checkError(Object error);
  String get left;
  String get right;
  String get caseLabel;
  String fallbackOnly(int percent);
  String get noDataAtAll;
  String get ancSectionTitle;
  String get ancModeOff;
  String get ancModeNoiseCancelling;
  String get ancModeAmbientSound;
  String get ancSetFailed;

  // Settings screen
  String get widgetSettingsTitle;
  String get settingsSaved;
  String settingsSaveFailed(Object error);
  String get refreshInterval;
  String get refreshIntervalNote;
  String minutesShort(int minutes);
  String hoursShort(int hours);
  String get colorLabel;
  String opacityLabel(int percent);
  String cornerRadiusLabel(int dp);
  String get saveButton;
  String get previewText;
  String get themeLabel;
  String get darkModeLabel;
  String get languageLabel;
}

class AppTextRu extends AppText {
  const AppTextRu();

  @override
  String get appTitle => 'Заряд наушников';
  @override
  String get widgetSettingsTooltip => 'Настройки виджета';
  @override
  String get permissionExplanation =>
      'Нужно разрешение "Устройства поблизости" (BLUETOOTH_CONNECT), '
      'чтобы увидеть привязанные наушники и прочитать заряд.';
  @override
  String get requestPermission => 'Запросить разрешение';
  @override
  String get openAppSettingsAction => 'Открыть настройки приложения';
  @override
  String devicesListError(Object error) => 'Не удалось получить список устройств: $error';
  @override
  String get noBondedDevices =>
      'Привязанных Bluetooth-устройств не найдено. '
      'Сначала соедините наушники с телефоном в системных настройках Bluetooth.';
  @override
  String get refreshList => 'Обновить список';
  @override
  String get selectDevicePrompt => 'Выберите наушники из привязанных устройств:';
  @override
  String get saveAndCheck => 'Сохранить и проверить заряд';
  @override
  String get connecting => 'Подключаюсь к наушникам…';
  @override
  String get widgetSavedInstructions =>
      'Устройство сохранено для виджета. Долгое нажатие на рабочем столе → '
      'Виджеты → Heads Widget, чтобы добавить его на экран.';
  @override
  String checkError(Object error) => 'Ошибка при проверке заряда: $error';
  @override
  String get left => 'L';
  @override
  String get right => 'R';
  @override
  String get caseLabel => 'Кейс';
  @override
  String fallbackOnly(int percent) =>
      'Раздельно L/R/кейс получить не удалось, но общий заряд: $percent%';
  @override
  String get noDataAtAll => 'Не удалось получить заряд ни одним из способов. Убедитесь, что наушники подключены.';
  @override
  String get ancSectionTitle => 'Шумоподавление';
  @override
  String get ancModeOff => 'Выкл.';
  @override
  String get ancModeNoiseCancelling => 'Шумоподавление';
  @override
  String get ancModeAmbientSound => 'Прозрачность';
  @override
  String get ancSetFailed => 'Не удалось переключить режим';

  @override
  String get widgetSettingsTitle => 'Настройки виджета';
  @override
  String get settingsSaved => 'Настройки виджета сохранены';
  @override
  String settingsSaveFailed(Object error) => 'Не удалось сохранить: $error';
  @override
  String get refreshInterval => 'Интервал обновления';
  @override
  String get refreshIntervalNote =>
      'Android не позволяет виджетам обновляться чаще, чем раз в 15 минут — '
      'это системное ограничение, не наше.';
  @override
  String minutesShort(int minutes) => '$minutes мин';
  @override
  String hoursShort(int hours) => '$hours ч';
  @override
  String get colorLabel => 'Цвет';
  @override
  String opacityLabel(int percent) => 'Прозрачность: $percent%';
  @override
  String cornerRadiusLabel(int dp) => 'Скругление углов: $dp dp';
  @override
  String get saveButton => 'Сохранить';
  @override
  String get previewText => 'L 85%   R 90%   Кейс 60%';
  @override
  String get themeLabel => 'Тема';
  @override
  String get darkModeLabel => 'Тёмная тема';
  @override
  String get languageLabel => 'Язык';
}

class AppTextEn extends AppText {
  const AppTextEn();

  @override
  String get appTitle => 'Earbuds Battery';
  @override
  String get widgetSettingsTooltip => 'Widget settings';
  @override
  String get permissionExplanation =>
      'The "Nearby devices" permission (BLUETOOTH_CONNECT) is needed to see '
      'paired earbuds and read their battery.';
  @override
  String get requestPermission => 'Request permission';
  @override
  String get openAppSettingsAction => 'Open app settings';
  @override
  String devicesListError(Object error) => 'Failed to get device list: $error';
  @override
  String get noBondedDevices =>
      'No paired Bluetooth devices found. '
      'Pair your earbuds with this phone in system Bluetooth settings first.';
  @override
  String get refreshList => 'Refresh list';
  @override
  String get selectDevicePrompt => 'Select your earbuds from paired devices:';
  @override
  String get saveAndCheck => 'Save and check battery';
  @override
  String get connecting => 'Connecting to earbuds…';
  @override
  String get widgetSavedInstructions =>
      'Device saved for the widget. Long-press the home screen → '
      'Widgets → Heads Widget to add it.';
  @override
  String checkError(Object error) => 'Error checking battery: $error';
  @override
  String get left => 'L';
  @override
  String get right => 'R';
  @override
  String get caseLabel => 'Case';
  @override
  String fallbackOnly(int percent) => 'Could not get L/R/case separately, overall battery: $percent%';
  @override
  String get noDataAtAll => 'Could not read battery any way. Make sure the earbuds are connected.';
  @override
  String get ancSectionTitle => 'Noise control';
  @override
  String get ancModeOff => 'Off';
  @override
  String get ancModeNoiseCancelling => 'Noise cancelling';
  @override
  String get ancModeAmbientSound => 'Transparency';
  @override
  String get ancSetFailed => 'Failed to switch mode';

  @override
  String get widgetSettingsTitle => 'Widget settings';
  @override
  String get settingsSaved => 'Widget settings saved';
  @override
  String settingsSaveFailed(Object error) => 'Failed to save: $error';
  @override
  String get refreshInterval => 'Refresh interval';
  @override
  String get refreshIntervalNote =>
      'Android doesn\'t let widgets refresh more often than every 15 minutes — '
      'that\'s a system limit, not ours.';
  @override
  String minutesShort(int minutes) => '${minutes}m';
  @override
  String hoursShort(int hours) => '${hours}h';
  @override
  String get colorLabel => 'Color';
  @override
  String opacityLabel(int percent) => 'Opacity: $percent%';
  @override
  String cornerRadiusLabel(int dp) => 'Corner radius: ${dp}dp';
  @override
  String get saveButton => 'Save';
  @override
  String get previewText => 'L 85%   R 90%   Case 60%';
  @override
  String get themeLabel => 'Theme';
  @override
  String get darkModeLabel => 'Dark theme';
  @override
  String get languageLabel => 'Language';
}

class AppTextUz extends AppText {
  const AppTextUz();

  @override
  String get appTitle => 'Quloqchin quvvati';
  @override
  String get widgetSettingsTooltip => 'Vidjet sozlamalari';
  @override
  String get permissionExplanation =>
      'Ulangan quloqchinlarni koʻrish va quvvatini oʻqish uchun '
      '"Yaqin atrofdagi qurilmalar" (BLUETOOTH_CONNECT) ruxsati kerak.';
  @override
  String get requestPermission => 'Ruxsat soʻrash';
  @override
  String get openAppSettingsAction => 'Ilova sozlamalarini ochish';
  @override
  String devicesListError(Object error) => 'Qurilmalar roʻyxatini olib boʻlmadi: $error';
  @override
  String get noBondedDevices =>
      'Ulangan Bluetooth qurilmalari topilmadi. '
      'Avval quloqchinlaringizni telefon Bluetooth sozlamalarida ulang.';
  @override
  String get refreshList => 'Roʻyxatni yangilash';
  @override
  String get selectDevicePrompt => 'Ulangan qurilmalar orasidan quloqchiningizni tanlang:';
  @override
  String get saveAndCheck => 'Saqlash va quvvatni tekshirish';
  @override
  String get connecting => 'Quloqchinlarga ulanmoqda…';
  @override
  String get widgetSavedInstructions =>
      'Qurilma vidjet uchun saqlandi. Uni ekranga qoʻshish uchun bosh ekranda '
      'uzoq bosing → Vidjetlar → Heads Widget.';
  @override
  String checkError(Object error) => 'Quvvatni tekshirishda xatolik: $error';
  @override
  String get left => 'L';
  @override
  String get right => 'R';
  @override
  String get caseLabel => 'Futlyar';
  @override
  String fallbackOnly(int percent) => 'L/R/futlyar alohida olinmadi, umumiy quvvat: $percent%';
  @override
  String get noDataAtAll => 'Quvvatni hech qanday usulda olib boʻlmadi. Quloqchinlar ulanganiga ishonch hosil qiling.';
  @override
  String get ancSectionTitle => 'Shovqin nazorati';
  @override
  String get ancModeOff => 'Oʻchiq';
  @override
  String get ancModeNoiseCancelling => 'Shovqinni bostirish';
  @override
  String get ancModeAmbientSound => 'Shaffoflik';
  @override
  String get ancSetFailed => 'Rejimni almashtirib boʻlmadi';

  @override
  String get widgetSettingsTitle => 'Vidjet sozlamalari';
  @override
  String get settingsSaved => 'Vidjet sozlamalari saqlandi';
  @override
  String settingsSaveFailed(Object error) => 'Saqlab boʻlmadi: $error';
  @override
  String get refreshInterval => 'Yangilanish oraligʻi';
  @override
  String get refreshIntervalNote =>
      'Android vidjetlarni har 15 daqiqada bir martadan tez-tez yangilashga '
      'ruxsat bermaydi — bu tizim cheklovi, bizniki emas.';
  @override
  String minutesShort(int minutes) => '$minutes daq';
  @override
  String hoursShort(int hours) => '$hours soat';
  @override
  String get colorLabel => 'Rang';
  @override
  String opacityLabel(int percent) => 'Shaffoflik: $percent%';
  @override
  String cornerRadiusLabel(int dp) => 'Burchak yumaloqligi: $dp dp';
  @override
  String get saveButton => 'Saqlash';
  @override
  String get previewText => 'L 85%   R 90%   Futlyar 60%';
  @override
  String get themeLabel => 'Mavzu';
  @override
  String get darkModeLabel => 'Qorongʻi mavzu';
  @override
  String get languageLabel => 'Til';
}

class AppTextTr extends AppText {
  const AppTextTr();

  @override
  String get appTitle => 'Kulaklık Bataryası';
  @override
  String get widgetSettingsTooltip => 'Widget ayarları';
  @override
  String get permissionExplanation =>
      'Eşleştirilmiş kulaklıkları görüp bataryasını okumak için '
      '"Yakındaki cihazlar" (BLUETOOTH_CONNECT) izni gerekiyor.';
  @override
  String get requestPermission => 'İzin iste';
  @override
  String get openAppSettingsAction => 'Uygulama ayarlarını aç';
  @override
  String devicesListError(Object error) => 'Cihaz listesi alınamadı: $error';
  @override
  String get noBondedDevices =>
      'Eşleştirilmiş Bluetooth cihazı bulunamadı. '
      'Önce kulaklığınızı telefonun Bluetooth ayarlarından eşleştirin.';
  @override
  String get refreshList => 'Listeyi yenile';
  @override
  String get selectDevicePrompt => 'Eşleştirilmiş cihazlar arasından kulaklığınızı seçin:';
  @override
  String get saveAndCheck => 'Kaydet ve bataryayı kontrol et';
  @override
  String get connecting => 'Kulaklığa bağlanıyor…';
  @override
  String get widgetSavedInstructions =>
      'Cihaz widget için kaydedildi. Ana ekrana eklemek için ana ekranda '
      'uzun basın → Widget\'lar → Heads Widget.';
  @override
  String checkError(Object error) => 'Batarya kontrolünde hata: $error';
  @override
  String get left => 'L';
  @override
  String get right => 'R';
  @override
  String get caseLabel => 'Kılıf';
  @override
  String fallbackOnly(int percent) => 'L/R/kılıf ayrı ayrı alınamadı, toplam batarya: $percent%';
  @override
  String get noDataAtAll => 'Batarya hiçbir yöntemle okunamadı. Kulaklığın bağlı olduğundan emin olun.';
  @override
  String get ancSectionTitle => 'Gürültü kontrolü';
  @override
  String get ancModeOff => 'Kapalı';
  @override
  String get ancModeNoiseCancelling => 'Gürültü engelleme';
  @override
  String get ancModeAmbientSound => 'Şeffaflık';
  @override
  String get ancSetFailed => 'Mod değiştirilemedi';

  @override
  String get widgetSettingsTitle => 'Widget ayarları';
  @override
  String get settingsSaved => 'Widget ayarları kaydedildi';
  @override
  String settingsSaveFailed(Object error) => 'Kaydedilemedi: $error';
  @override
  String get refreshInterval => 'Yenileme aralığı';
  @override
  String get refreshIntervalNote =>
      'Android, widget\'ların 15 dakikadan daha sık yenilenmesine izin vermez — '
      'bu bizim değil, sistemin sınırı.';
  @override
  String minutesShort(int minutes) => '$minutes dk';
  @override
  String hoursShort(int hours) => '$hours sa';
  @override
  String get colorLabel => 'Renk';
  @override
  String opacityLabel(int percent) => 'Saydamlık: %$percent';
  @override
  String cornerRadiusLabel(int dp) => 'Köşe yuvarlaklığı: $dp dp';
  @override
  String get saveButton => 'Kaydet';
  @override
  String get previewText => 'L %85   R %90   Kılıf %60';
  @override
  String get themeLabel => 'Tema';
  @override
  String get darkModeLabel => 'Karanlık tema';
  @override
  String get languageLabel => 'Dil';
}
