# Reflection into the Android framework's BluetoothDevice (getBatteryLevel) targets platform
# classes, which R8 never touches, so no keep rule is needed for that. Nothing else in this
# app relies on reflection into its own code.
