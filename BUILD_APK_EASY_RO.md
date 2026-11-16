# APK pentru Philips 43PUS7303/12 — construire fără Android Studio

Acest proiect include un workflow GitHub Actions care compilează APK-ul în cloud. În mediul curent nu sunt instalate Android SDK și Gradle, deci APK-ul nu a putut fi compilat aici.

## Compatibilitate
- Configurația proiectului: `minSdk = 26` (Android 8.0 sau mai nou), orientare landscape și interfață Android TV.
- Modelul confirmat: Philips 43PUS7303/12. Versiunea de Android/firmware instalată efectiv pe televizor poate varia; verific-o în Setări → Preferințe dispozitiv → Despre.
- APK-ul rezultat este un APK debug pentru instalare manuală, nu o aplicație testată pe televizor.

## Pași
1. Creează un cont GitHub dacă nu ai deja unul.
2. Creează un repository nou, de exemplu `tv-subtitle-translator`.
3. Încarcă toate fișierele și folderele din acest proiect în repository (inclusiv folderul ascuns `.github`).
4. Deschide fila **Actions** a repository-ului.
5. Alege workflow-ul **Build Android TV APK** și apasă **Run workflow**. Alternativ, primul `push` pe ramura `main` pornește automat compilarea.
6. Așteaptă terminarea cu status verde, deschide rularea și descarcă artifact-ul **TV-Subtitle-Translator-APK**.
7. Dezarhivează artifact-ul. Fișierul `app-debug.apk` este installerul pentru Android TV.

## Instalare pe televizor
1. Copiază `app-debug.apk` pe un stick USB formatat FAT32 sau exFAT.
2. Conectează stickul la TV și deschide-l cu un File Manager.
3. Selectează APK-ul. Dacă Android cere permisiune, permite instalarea din acea sursă.
4. Apasă **Install** și apoi **Open**.

Dacă televizorul nu permite instalarea din USB, se poate folosi instalarea prin ADB din rețeaua locală, dar pașii depind de model și versiunea Android TV.

## Important
- Confirmarea modelului Philips 43PUS7303/12 permite țintirea Android TV 8/API 26+, dar nu înlocuiește testarea pe dispozitiv.
- Acesta este un APK de debug, nu o versiune semnată pentru publicare în Play Store.
- Proiectul nu a fost încă testat pe modelul exact al televizorului.
- Pentru transcriere și traducere este necesară o cheie OpenAI API, facturată separat de abonamentul ChatGPT.
- Aplicația captează sunetul prin microfon. Televizorul trebuie să aibă microfon funcțional sau să accepte un microfon USB compatibil.
- Nu toate site-urile/player-ele web vor funcționa în WebView; DRM, codecurile sau restricțiile site-ului pot împiedica redarea.
