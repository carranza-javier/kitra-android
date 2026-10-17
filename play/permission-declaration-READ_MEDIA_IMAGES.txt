Google Play Console > App content > Photo and video permissions > READ_MEDIA_IMAGES
App: Kitra (com.kettenki.kitra), "full" build

--- Core functionality that requires this permission (paste into "Describe your app's core functionality") ---

Kitra's core and only purpose is to translate screenshots instantly. When the user takes a screenshot with the phone's hardware buttons, Kitra detects that new screenshot and immediately shows a short "Translate" notification. One tap on it opens a translation of the text visible in that screenshot. This "screenshot + one tap" flow is the app's main feature and the reason it exists.

--- How the permission is used ---

- Kitra uses READ_MEDIA_IMAGES only to detect and read each new screenshot right after it is created. A JobScheduler content trigger on MediaStore tells the app that the media store changed; the app then queries only images added in the last 60 seconds whose relative path is a screenshots folder (for example Pictures/Screenshots or DCIM/Screenshots).
- Any other image (camera photos, downloads, messaging images, etc.) is ignored: it is never opened, read, stored or sent anywhere.
- A screenshot is only read when the user taps "Translate". At that point the screenshot is either translated on the device (basic mode) or sent directly from the device to Google's Gemini API with the user's own API key. Kitra has no servers and never uploads, stores, copies, moves or deletes any image.
- The app does not browse, index, back up or display the user's photo library.

--- Why the Android photo picker cannot provide this functionality ---

The photo picker only returns an image after the user opens it and selects that image manually. It cannot notify the app that a new screenshot was just taken, and it cannot hand over that screenshot without user interaction inside the picker. Using the picker would turn "screenshot + one tap" into "screenshot + tap a notification + find and tap the right image in the picker" on every single translation, which defeats the app's core purpose of translating what the user is reading with a single tap. There is no system API that grants access only to newly created screenshots.

The app also offers alternatives that need no photo permission: users can share any image with Kitra, and users who deny the permission (or grant only partial access) are told clearly that automatic screenshot detection is unavailable and are pointed to the share option.
