# TV Subtitle Translator v3 — browser universal Android TV

## Ce include
- Browser WebView integrat cu adresă web editabilă; include scurtături Cinejoy și Cinecat.
- Poate naviga la alte site-uri HTTP/HTTPS introduse de utilizator.
- Bară de control pentru telecomandă.
- Subtitrare română suprapusă peste pagina/video.
- Captură prin microfonul dispozitivului (nu extrage direct pista audio digitală din WebView).
- Transcriere Whisper și traducere în română prin API OpenAI.
- Exportul replicilor cu timestamp în fișier SRT.

## LIMITARE CRITICĂ
Android nu oferă aplicației o metodă universală de a citi direct pista audio din propriul WebView sau din orice player web. `AudioPlaybackCapture` nu rezolvă acest caz în mod universal, iar DRM/EME, CORS și politicile site-ului pot bloca accesul. Din acest motiv, această versiune folosește microfonul TV-ului sau un microfon USB conectat la TV pentru a asculta sunetul difuzat de televizor.

Multe televizoare Sony/Philips Android TV nu au microfon integrat. Dacă televizorul nu are microfon funcțional, conectează un microfon USB compatibil sau folosește o sursă externă de audio/microfon. Calitatea și sincronizarea depind de distanța față de difuzoare și de ecou. Pentru audio digital, este necesar un player/flux pe care utilizatorul are dreptul să îl acceseze și o integrare explicită cu playerul; nu se poate promite compatibilitate cu toate site-urile sau DRM.

Aplicația nu ocolește DRM, autentificări, paywall-uri sau restricțiile site-ului. Folosește serviciul numai în condițiile permise de site și de lege.

## Construire
Cerințe: Android Studio, JDK 17, Android SDK 35, internet.
1. Deschide folderul proiectului în Android Studio.
2. Așteaptă Gradle Sync.
3. `Build > Build APK(s)`.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Instalare
Instalează APK-ul pe Sony/Philips Android TV prin USB sau ADB. Deschide aplicația, configurează cheia API OpenAI, introdu orice adresă HTTP/HTTPS sau folosește scurtăturile Cinejoy/Cinecat, conectează/permite microfonul și apasă START RO.

## Configurare API
Cheia API se introduce în dialogul aplicației la prima apăsare pe START SUBTITRARE și se păstrează local în SharedPreferences. Costurile API sunt separate de ChatGPT. Pentru distribuție publică, nu stoca cheia în clar: folosește backend propriu și autentificare sigură.

## Limitări MVP
- Proiectul este sursă Android, nu APK compilat/testat pe dispozitiv fizic.
- Microfonul captează și sunetul ambiental; replicile pot fi afectate de zgomot și ecou.
- Procesarea folosește blocuri audio de aproximativ 10 secunde; latența include transcrierea și traducerea.
- Timestamp-urile sunt relative la începutul capturii, nu la timpul exact al playerului. Pauzele, seek-ul și buffering-ul pot produce decalaje.
- WebView poate să nu redea anumite codecuri/DRM sau pagini care cer browser certificat.
- Testează pe modelul exact Sony/Philips și firmware.

## Compatibilitate site-uri
Browserul nu este limitat la Cinejoy sau Cinecat. Totuși, faptul că o pagină se deschide nu garantează că video-ul poate fi redat sau că subtitrarea poate fi generată: unele site-uri cer browser certificat, au DRM/EME, codecuri neacceptate pe televizor, ferestre pop-up sau protecții anti-automatizare. Aplicația nu ocolește aceste restricții.
