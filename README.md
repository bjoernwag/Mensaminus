# MensaMinus

Eine Android-App zur Anzeige von Speiseplänen Dresdner Mensen, basierend auf den Speisenplänen des Studentenwerks Dresden.

## Features

- **Aktuelle Speisepläne:** Übersicht über das Angebot ausgewählter Mensen.
- **Favoriten & Smart Recommendations:** Schneller Zugriff auf Deine Lieblingsgerichte mit intelligenter Sortierung.
- **NFC CampusCard Reader:** Direktes Auslesen des Guthabens deiner Mensakarte (NFC).
- **Material 3 Design:** Moderne, intuitive Benutzeroberfläche.
- **100% privat & FOSS:** Keine Tracker, keine Werbung, keine Google Play Services erforderlich (GrapheneOS / CalyxOS kompatibel).
## Download

Bald im Google Play Store verfügbar

## Entwicklung

Die App wird mit **Jetpack Compose** und **Kotlin** entwickelt.

### Voraussetzungen

- Android Studio (aktuellste Version empfohlen)
- Android SDK 30+

### Build

1. Repository klonen.
2. In Android Studio öffnen.
3. Projekt synchronisieren.
4. `Run` klicken.

## Lizenz

Dieses Projekt ist unter der **Apache License 2.0** lizenziert. Weitere Informationen findest Du in der [LICENSE](LICENSE) Datei.

## Datenquelle & Dank

- **Studentenwerk Dresden:** Die Speisepläne werden über die offizielle OpenMensa-Schnittstelle bezogen.

## Verwendete Open-Source-Bibliotheken (Dependencies)

MensaMinus nutzt folgende Open-Source-Bibliotheken:

- [AndroidX & Jetpack Libraries](https://developer.android.com/jetpack/androidx) (Core, AppCompat, Lifecycle, Activity, Navigation, DataStore, Room, WorkManager, SplashScreen, Browser) – Apache 2.0
- [Kotlin Coroutines & Serialization](https://kotlinlang.org/) – Apache 2.0
- [Retrofit & Gson](https://github.com/square/retrofit) – Apache 2.0
- [Coil Compose](https://github.com/coil-kt/coil) – Apache 2.0
- [Jsoup](https://jsoup.org/) – MIT License

