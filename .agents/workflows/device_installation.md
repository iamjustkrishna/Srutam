---
description: Build and install Srutam app on an Android emulator (phone, tablet, or custom AVD) or list available emulators.
---

# Device Installation Workflow

This workflow automates listing Android virtual devices, booting emulators if needed, compiling the debug APK, installing it, and launching the app on the target device.

## Parameters

Read the argument passed after the slash command (e.g. `/device_installation [target]`):
- `list`: Show all configured AVDs and all currently connected devices/emulators.
- `phone` / `standard` / `mobile`: Target the standard phone emulator (`Srutam_Phone`).
- `tab` / `tablet`: Target the tablet emulator (`Srutam_Tablet`).
- `<avd_name>`: Target any specific AVD name (e.g. custom user AVD).
- Empty (no parameter): If a device is already running in `adb devices`, use it. If multiple or none, list available devices and ask which one to target.

---

## Execution Steps

### 1. Identify Target & Available Devices

Find the Android SDK tools:
- Emulator: `$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe`
- ADB: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`

Check running devices:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l

& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -list-avds

If the user passed list:

Print the list of installed AVDs (e.g. Srutam_Phone, Srutam_Tablet).
Print the list of currently attached and running devices (online / offline / serial numbers).
Stop here and provide guidance on how to install to one of them (e.g. /device_installation phone or /device_installation tab).

Launch Target Emulator if Not Running
Map aliases to AVD names:

phone, standard, mobile -> Srutam_Phone
tab, tablet -> Srutam_Tablet
Any explicit AVD name -> use as-is
Check if the target emulator is already booted in adb devices:

powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
If not running, launch it in the background:

powershell
Start-Process -FilePath "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -ArgumentList "-avd <AVD_NAME>"
Wait until the device is online and booted:

powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done'
4. Build and Install App
Build the debug APK:
powershell
.\gradlew assembleDebug
Locate the generated APK: app\build\outputs\apk\debug\app-debug.apk

Install on the target device:

powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <device_serial> install -r app\build\outputs\apk\debug\app-debug.apk
5. Launch the App
Start the main Activity:

powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <device_serial> shell am start -n space.iamjustkrishna.srutam/.MainActivity
6. Verify and Report
Verify that the app launched cleanly and report:

Target device / AVD name
Device serial
Build status and installation confirmation
