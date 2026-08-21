import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:linkbuds_widget/app_settings.dart';
import 'package:linkbuds_widget/main.dart';

void main() {
  testWidgets('Diagnostic screen shows its title', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: AppSettingsController(),
        child: const LinkBudsDiagnosticApp(),
      ),
    );
    expect(find.text('Заряд наушников'), findsOneWidget);
  });
}
