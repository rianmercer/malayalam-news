# Malayalam News app: setup (about 15 minutes, ₹0)

## 1. Put it online (GitHub Pages, free)
1. Make a free account at github.com and tap **New repository**. Name it `malayalam-news`, set it to **Public**, and create it.
2. Tap **Add file → Upload files**. Drag in everything from the unzipped folder: `index.html`, `manifest.webmanifest`, `sw.js`, both `icon-*.png` files, and the `logos` folder. Tap **Commit changes**.
3. Go to **Settings → Pages**. Under "Build and deployment", set Source to **Deploy from a branch**, Branch **main**, folder **/ (root)**, and Save.
4. Wait 1-2 minutes. Your address will be `https://YOUR-USERNAME.github.io/malayalam-news/`.

## 2. Install on your father's phone
1. Open that address in **Chrome** on his phone.
2. Tap the three dots (⋮), then **Install app** (or **Add to Home screen**).
3. A "വാർത്ത" icon appears on his home screen. Show him once: tap icon, tap channel.

## 3. Add logos (optional)
Save each logo as a PNG with these exact names and upload them into the `logos` folder on GitHub:
`asianet.png`, `manorama.png`, `mathrubhumi.png`, `twentyfour.png`, `reporter.png`, `kairali.png`, `janam.png`, `mediaone.png`
Any tile without a logo file shows a coloured name badge instead, so nothing breaks.

## 4. Change channels later
Open `index.html` on GitHub, tap the pencil icon, and edit the `CHANNELS` list near the bottom. Each line is one channel.

## If a channel shows a black or error screen
Tap the red **YouTube-ൽ തുറക്കുക** button. It opens that channel's live page in the YouTube app. Some channels restrict embedded playback, so this is expected for a few.
