# My Invoice design system

Status: **adopted 2026-11-07**. The Android app is styled from this document. It replaces the warm orange theme from the "glossy theme" restyle (PR #4) with a cool blue one. The app is light only.

Decided so far (2026-11-07): the invoice PDF stays orange; the app is light only, with no dark mode; the New invoice button is blue; text fields and form buttons have 8 dp corners; every surface is liquid glass (section 5.3); primary buttons, the tab bar and the + button are skeuomorphic (5.4); screens fade into a progressive blur at the top and bottom (5.5); empty states have no background block.

The Android app follows every section, including the 2026-11-07 evening revision (sections 4.2, 5.3–5.5, 6.8–6.10 and 7.1).

This document is drawn from five reference images Saurabh shared on 2026-11-07: a "Font & Colors" sheet, two phone mockups (a task dashboard and an assistant home screen), a poster with a glossy 3D illustration, and a long overview board of the same app. The references are productivity apps, so the second half of this document translates their patterns onto the invoice screens we already have (Invoices, Clients, Settings, editor, details, payments).

---

## 1. Look and feel in one paragraph

Calm, airy and light. Screens open on a soft blue sky gradient that fades into a pale cool grey canvas. Content sits on frosted **liquid glass** cards with large rounded corners, a bright specular rim and a soft shadow, and scrolls under the top and bottom edges into a **progressive blur** instead of being cut off. The few things you press most (the main button, the tab bar, the blue +) are **skeuomorphic**: they look like real, slightly raised, glossy objects. Black is used sparingly but boldly: headings, the small round "go" buttons on cards, and the floating pill tab bar. Blue is the single accent and appears in only a few places per screen (the main action, the selected state, the header wash). Greys carry secondary text. A few glossy, toy-like 3D illustrations in blue and white add personality on empty states and onboarding.

Principles:

1. **One accent.** Blue marks the one thing to do next. Everything else is black, white or grey.
2. **Soft surfaces, hard ink.** Surfaces are pale and rounded; text and key controls are near black for contrast.
3. **Soft rectangles for forms, pills for navigation.** Text inputs and form buttons use a tight 8 dp corner. Chips, the tab bar and round icon buttons stay full pills or circles. Cards keep large radii.
4. **Glass everywhere, depth where you press.** Every surface is liquid glass. Only the primary actions get skeuomorphic depth, so they stand out as the things to tap.
5. **Space over lines.** Separate content with whitespace and card edges, not dividers.

---

## 2. Colour

### 2.1 Core palette (from the reference sheet)

| Token | Hex | Role |
|---|---|---|
| `Ink` | `#000203` | Headings, body text, dark buttons, tab bar background |
| `White` | `#FFFFFF` | Cards, inputs, text on dark or blue |
| `Primary` | `#438BEF` | The accent: primary button, selected chip, send/FAB, header gradient, links |
| `Canvas` | `#F2F6F9` | Screen background below the header, secondary chips, icon wells |
| `Grey` | `#808081` | Secondary text, icons at rest, placeholders |

### 2.2 Derived tokens (needed for a working app, not in the sheet)

| Token | Hex | Why |
|---|---|---|
| `PrimaryDeep` | `#2F6FD6` | Blue **text** on white. `#438BEF` on white is only 3.4:1, which fails for small text; this shade is 4.8:1. |
| `PrimarySoft` | `#E3EEFD` | Selected chip fill, info banners, the tinted "SemiBold" style label in the sheet |
| `SkyTop` | `#3F86EC` | Top stop of the header gradient |
| `SkyMid` | `#C9DDFB` | Middle stop of the header gradient |
| `GreyText` | `#6B6B6C` | Secondary text at 12 to 14 sp. `#808081` is 3.9:1 on white, fine for icons and 16 sp+ but not for small labels; this is 5.3:1. |
| `Hairline` | `#E6ECF1` | Input borders and the rare divider |
| `Scrim` | `#000203` at 40% | Bottom sheets and dialogs |

### 2.3 Status colours for invoices

The references have no status colours, so these are new. They are used only as a small dot plus a soft-tinted pill, never as large fills, so the screen stays blue/black/white.

| Status | Text / dot | Pill fill |
|---|---|---|
| Draft | `#6B6B6C` | `#F2F6F9` |
| Sent | `#2F6FD6` | `#E3EEFD` |
| Partly paid | `#B26B00` | `#FFF3DC` |
| Paid | `#1E8E4E` | `#E2F5EA` |
| Overdue | `#D23B3B` | `#FDE6E6` |

### 2.4 Usage rules

- At most **one** filled blue element per screen region (the main button, or the selected chip, or the FAB).
- Body text is always `Ink`. Never put body text in blue or grey on the gradient.
- Text on the blue header gradient is `White` at the top and switches to `Ink` once the gradient has faded past `SkyMid`.
- White text on `#438BEF` is 3.4:1, which is fine for icons and large text. For buttons with small labels, use labels of at least 16 sp SemiBold, or fill the button with `PrimaryDeep` instead.
- **Light only (decided 2026-11-07).** The app has no dark mode and ignores the phone's dark setting. Code still refers to tokens, never raw hex, so a dark theme could be added later.

### 2.5 The invoice PDF

**Decided (2026-11-07): the PDF stays orange.** It keeps its own accent (`#FF5400`) and Inter, so new invoices match the ones already sent to clients. The app theme never changes the PDF.

---

## 3. Typography

**Font:** [Outfit](https://fonts.google.com/specimen/Outfit) (SIL Open Font License, free to bundle). Three weights only, as in the sheet: Regular 400, Medium 500, SemiBold 600. No Bold.

Outfit is geometric with round "o"s and short descenders, which suits the round shapes elsewhere. It is used for all UI text including amounts.

### 3.1 Type scale (sp)

| Style | Weight | Size / line height | Tracking | Used for |
|---|---|---|---|---|
| `display` | SemiBold | 32 / 38 | -0.5 | The big outstanding total on the home hero card |
| `headline` | SemiBold | 26 / 32 | -0.4 | Hero headlines ("Your Work, On Autopilot" style), screen titles on the gradient |
| `title` | SemiBold | 20 / 26 | -0.2 | Section headings ("Upcoming Events" → "Due this week") |
| `cardTitle` | SemiBold | 17 / 22 | -0.1 | Client name on an invoice card |
| `body` | Regular | 15 / 22 | 0 | Default text, form values |
| `bodyMedium` | Medium | 15 / 22 | 0 | Amounts inside lists, button labels |
| `caption` | Regular | 13 / 18 | 0 | Secondary lines ("Google Meet / Room 4B" → "RS/26-27/031 · Due 22 Nov") |
| `chip` | Medium | 12 / 16 | 0.1 | Chip and pill labels |
| `micro` | Regular | 11 / 14 | 0.2 | Time stamps, tab bar labels, the small "3 scheduled" style counters |

### 3.2 The two-weight headline

The hero headline in the reference mixes weights in one line: **Your** Work, On **Autopilot**, with the plain words in Regular grey and the key words in SemiBold black. Use this for the home hero only, for example "**₹1,24,500** still to collect" or "**3 invoices** due this week". Do not use it elsewhere; it loses its effect if repeated.

### 3.3 Numbers

Amounts use tabular figures (`fontFeatureSettings = "tnum"`) so columns of totals line up. The currency symbol is the same size and weight as the number.

---

## 4. Spacing, radii and elevation

### 4.1 Spacing

A 4 dp base grid. Common steps: 4, 8, 12, 16, 20, 24, 32.

| Use | Value |
|---|---|
| Screen side margin | 20 dp |
| Gap between cards in a list | 12 dp |
| Card inner padding | 18 dp |
| Gap between a section title and its first card | 12 dp |
| Gap between sections | 28 dp |
| Space reserved below content for the floating tab bar | 112 dp |

### 4.2 Corner radii

| Element | Radius |
|---|---|
| Large card / hero card | 28 dp |
| Standard card, grid tile | 24 dp |
| Text input, search bar, dropdown, date picker field, multi-line text box | **8 dp** |
| Form buttons: primary, secondary, dark and text buttons (Save, Create invoice, Add item, Record payment, Share PDF, dialog buttons) | **8 dp** |
| Chip, filter pill, segmented control, tab bar, selected tab | full pill |
| Icon well, round buttons, avatars | circle |
| Bottom sheet top corners | 32 dp |

### 4.3 Elevation

Glass surfaces separate from what is behind them by blur, a bright rim and a soft shadow (section 5.3). Skeuomorphic controls (section 5.4) have their own layered shadows. The table below is the base shadow scale those build on.

| Level | Shadow | Used for |
|---|---|---|
| 0 | none | Cards on `Canvas` |
| 1 | y 4, blur 16, `#0B2A5B` at 6% | Cards that sit on the blue gradient (the hero card) |
| 2 | y 10, blur 24, `#000203` at 18% | Floating tab bar, FAB, dark round buttons |
| Glow | y 8, blur 20, `#438BEF` at 35% | The blue primary/send button only |

---

## 5. Backgrounds and materials

### 5.1 Sky header gradient

Every top-level screen opens with a vertical gradient that covers the status bar and roughly the top 40% of the screen, then disappears into the canvas so there is no visible edge.

```
0%    #3F86EC
32%   Primary  #438BEF
68%   SkyMid   #C9DDFB
100%  Canvas   #F2F6F9   (reached at ~42% of screen height)
```

(Changed while building: the top is a deeper blue than the first draft's `#6FA6F3`, so the white header text stays readable.)

In Compose: a `Brush.verticalGradient` drawn behind the scaffold, with the status bar drawn edge-to-edge over it. The header scrolls with the content (it is not pinned), so long lists end on plain canvas.

On the assistant-style screen the gradient is stronger (fully saturated `Primary` at the top). Reserve that stronger version for one special moment, such as the "Invoice sent" or "Paid in full" confirmation.

### 5.2 Canvas

Below the gradient, the screen background is `Canvas #F2F6F9`. Glass cards sit on it; over the plain canvas they read as soft white, and over the sky gradient or scrolling content their blur shows.

### 5.3 Liquid glass

Every UI surface follows the iOS 27 "liquid glass" material: a translucent layer that blurs and slightly brightens whatever is behind it, with a light-catching rim, so content seems to sit under a piece of polished glass.

**Anatomy of a glass surface (light theme):**

| Layer | Spec |
|---|---|
| Backdrop blur | 24 dp radius on what is behind the surface |
| Vibrancy | Saturation of the blurred backdrop ×1.3, brightness +4% |
| Tint | `White` at the variant's opacity (table below) |
| Specular rim | 1 dp stroke, linear gradient from `White` 85% at top left to `White` 15% at bottom right |
| Inner highlight | 1 dp `White` 60% line along the inside of the top edge, fading out over the first and last 20% of the width |
| Refraction (where supported) | The backdrop bends slightly within 6 dp of the edge, like the edge of a lens |
| Shadow | y 8, blur 24, `#0B2A5B` at 8% |

**Variants:**

| Variant | Tint | Used for |
|---|---|---|
| Glass regular | `White` 68% | Cards, hero card, list rows, text fields, dropdown and date fields, menus, dialogs, bottom sheets, the editor's total bar, snackbars |
| Glass clear | `White` 22% | Things that sit on the blue sky: header icon buttons, the avatar ring, the header "Save" pill |
| Glass smoked | `Ink` 62% | The tab bar (combined with skeuomorphic depth, section 5.4) and the dark round "open" buttons |

**Rules:**

- Text on glass regular is always `Ink` or `GreyText`. The 68% tint keeps contrast above 4.5:1 even over the blue sky.
- Glass never sits on glass more than two layers deep (for example a menu over a card is fine; a chip inside a card inside a sheet uses a flat tint instead of blur).
- Status chips and outline chips inside cards are flat tints, not blurred, so lists stay fast to scroll.
- On a press, glass surfaces brighten (tint +10%) and scale to 98% for 120 ms.

**On Android:** backdrop blur needs Android 12 (API 31) or newer. Refraction needs a custom AGSL shader on Android 13 (API 33) or newer. On older phones the surface falls back to the same tint at 92% opacity, plus the rim and shadow, with no blur. Compose has no built-in backdrop blur, so the app would use the open-source Haze library (`dev.chrisbanes.haze`), which also does the progressive blur in 5.5.

### 5.4 Skeuomorphic controls

The primary buttons, the tab bar and the blue + button are the only skeuomorphic elements. They look like real, slightly raised objects with a glossy surface, so the eye goes to them first. Everything else stays glass.

**Primary button** (Save, Create invoice, Save settings and similar; 8 dp corners, 54 dp tall):

- Fill: vertical gradient `#5A9BF2` → `#438BEF` (50%) → `#2F6FD6`.
- Gloss: the top half carries a sheen, `White` 28% fading to 0 at the middle.
- Bevel: 1 dp inner highlight `White` 55% on the top edge, 1 dp inner shade `#1F55B0` 45% on the bottom edge.
- Shadows: a soft one (y 6, blur 14, `#2F6FD6` 40%) and a tight contact one (y 1, blur 2, `#0B2A5B` 25%).
- Label: `White`, 16 sp SemiBold, with a 1 dp `#1F55B0` 45% text shadow below it so it looks pressed into the surface.
- Pressed: the gradient flips (dark at the top), an inner shadow appears at the top (y 2, blur 4, `#0B2A5B` 30%), the button moves 1 dp down, and the soft shadow shrinks to y 2, blur 6.

**Floating tab bar** (smoked glass plus skeuomorphic depth):

- Body: glass smoked (blurred backdrop, `Ink` 62%), full pill, 64 dp tall.
- Bevel: 1 dp inner highlight `White` 22% on the top edge, inner shadow on the bottom edge (y -2, blur 4, `#000000` 35%).
- Shadow: y 12, blur 28, `#000203` 28%, plus a contact shadow (y 2, blur 4, `#000203` 20%).
- Selected tab: a raised white "key": vertical gradient `#FFFFFF` → `#E9EEF3`, 1 dp top highlight `White`, 1 dp bottom edge `#C9D3DD`, shadow y 2, blur 6, `#000203` 25%. Icon and label in `Ink`.
- Unselected icons: `White` 72%, with a 1 dp `#000000` 40% shadow below so they look engraved.
- Changing tab: the white key slides to the new tab over 220 ms with a slight overshoot.

**Blue + button** (New invoice, 64 dp circle), a glossy blue gem:

- Fill: radial gradient centred at the top left, `#8DBAF6` → `#438BEF` (55%) → `#2559B8`.
- Specular highlight: an ellipse across the top 40%, `White` 70% fading to 0.
- Bounce light: a crescent of `#8DBAF6` 45% inside the bottom edge.
- Rim: 1 dp `#2559B8` 60% outside, and a 1 dp `White` 35% ring just inside.
- Shadows: y 10, blur 20, `#438BEF` 45%, plus a contact shadow (y 2, blur 4, `#0B2A5B` 30%).
- Icon: white "+", 2.4 dp stroke, with a 1 dp `#1F55B0` 50% shadow below.
- Pressed: scales to 94%, the highlight dims to 45% and the shadow tightens.

### 5.5 Progressive blur at the top and bottom

Content never ends in a hard cut where it meets the status bar or the floating tab bar. Instead it slides under a band that blurs more and more towards the screen edge, so it fades out smoothly and the bars stay readable.

| Edge | Band height | Blur | Tint for readability |
|---|---|---|---|
| Top | The status bar only | 0 at the bottom of the status bar, rising to 20 dp at the top edge | The colour at the top of the screen (sky blue on sky screens, `Canvas` on forms), 0% → 55% towards the edge |
| Bottom (tab screens) | The bottom navigation only: from the screen's bottom edge up to the top of the floating tab bar (about 100 dp including the gesture bar) | 0 at the top of the band, rising to 24 dp at the bottom edge | `Canvas`, 0% → 60% towards the edge |
| Bottom (editor) | Behind the sticky total bar only | Same as the tab screens | Same |

**Decided (2026-11-07):** the blur covers only the status bar at the top and only the bottom navigation at the bottom, with the soft strength above. The screen title is not pinned; it scrolls away under the status-bar blur.

Inner screens (invoice details and the forms) keep their back button and title pinned, as they did before. The form scrolls under that bar, which is blurred glass of the same tint, solid behind the title and fading out at its lower edge, so there is no hard cut there either.

- The ramp follows an ease-in curve, so text is still crisp in the upper part of the bottom band and only blurs close to the edge.
- The bands are always on, not only while scrolling. When a list is short and nothing sits behind a band, the band is invisible.
- Lists add padding equal to the band height, so the last item can scroll fully clear of the tab bar.
- On Android 12 (API 31) and newer, Haze's progressive blur does this. On older phones the band is the tint gradient alone, with no blur.

---

## 6. Components

Each component lists what it looks like in the references and what it becomes in the invoice app.

### 6.1 Greeting header

- Left: 40 dp circular avatar (the Revishing Studio logo), then two lines: "Good morning" in `bodyMedium` white, and a `micro` line below ("Today · 3 due").
- Right: two 44 dp circular icon buttons, `White` at 20% fill with a 1 dp `White` at 40% border, white 20 dp line icons. In the app these are **Search** and **Notifications** (overdue reminders).

### 6.2 Hero card

A glass card (glass regular, radius 28) overlapping the bottom of the gradient. Contains the two-weight headline, an optional 3D illustration on the right (about 96 dp), and a single input row at the bottom.

In the app: the **outstanding summary**. Headline "**₹1,24,500** still to collect", a caption with the overdue count, and a row of two pills: "+ New invoice" (blue primary) and "Record payment" (secondary).

### 6.3 Search input with send button

Field with 8 dp corners, glass regular, 52 dp tall, placeholder in `Grey`. Left: small circular "+" and mic buttons (36 dp, white, hairline border). Right: a 40 dp **blue circular send button** with a white paper-plane icon and the blue glow.

In the app: the **search bar** on Invoices and Clients (no mic; the right-hand button becomes a filter icon in a white circle, not blue, so blue stays reserved for creating).

### 6.4 Chips

Small full pills, 28 to 32 dp tall, `chip` text, 12 dp horizontal padding.

| Variant | Fill | Text |
|---|---|---|
| Neutral (the "Today / 1h" chip) | `Canvas` | `Ink` |
| Outline (the time-range chip) | `White`, 1 dp `Hairline` border | `GreyText` |
| Selected filter | `Ink` | `White` |
| Accent badge (the "TRY MINDLY →" pill) | `Primary` | `White`, uppercase, 0.5 tracking |
| Status | see section 2.3 | see section 2.3, with a 6 dp dot |

In the app: the status filter row on Invoices (All · Draft · Sent · Overdue · Paid), currency chips in the editor (INR · USD · AUD · EUR), and the status pill on each invoice.

### 6.5 Dark circular action button

44 dp circle, `Ink` fill, white 20 dp "arrow up-right" icon, elevation 2. Sits in the top-right corner of a card and means "open this".

In the app: on each invoice card, opens the invoice details. Also used as the **back** button (white arrow on black) on detail screens.

### 6.6 List card (the event card)

Glass regular, radius 24, padding 18. Layout from the reference, mapped to an invoice:

```
┌──────────────────────────────────────────┐
│ (RS/26-27/031)                       (↗) │  outline chip + dark round button
│ Acme Pty Ltd                             │  cardTitle
│ AUD 2,400.00 · Due 22 Nov                │  caption, GreyText
│                                          │
│ (● Overdue)                    ₹1,31,520 │  status pill   amount in INR
└──────────────────────────────────────────┘
```

The reference shows overlapping avatars at bottom right; in the app that slot holds the amount instead (we have no people to show).

### 6.7 Grid tile (the quick-action tile)

Square-ish white tile, radius 24, padding 16. A 36 dp circular icon well (`Canvas` fill, `Ink` 18 dp line icon) at top left, then a two-line `cardTitle` label at bottom left.

In the app: a 2×2 grid on the home screen or in Settings: "New invoice", "Add client", "Record payment", "Export backup".

### 6.8 Buttons

Form buttons have **8 dp corners**. In Compose, use `RoundedCornerShape(8.dp)` for every button with a text label, including Material buttons, dialog buttons and snackbar actions. Round icon buttons stay circles.

| Type | Spec |
|---|---|
| Primary | 8 dp corners, 54 dp tall, skeuomorphic blue (section 5.4) |
| Secondary | 8 dp corners, 54 dp, glass regular with the specular rim, `Ink` text |
| Dark | 8 dp corners, 54 dp, glass smoked with `White` text. For "Share PDF" on the details screen |
| Text | `PrimaryDeep` `bodyMedium`, no fill. "View all" links next to section titles |
| Icon (light) | 44 dp circle, glass regular, `Ink` icon |
| Icon (dark) | see 6.5 |

### 6.9 Floating pill tab bar

Floats 16 dp above the bottom edge and 20 dp from the sides. 64 dp tall, full pill, smoked glass with skeuomorphic depth (section 5.4). Content scrolls under it into the bottom progressive blur (section 5.5).

- The **selected** tab is a raised white key inside the bar (52 dp tall, 6 dp inset) with an `Ink` icon and label side by side ("⌂ Home").
- Other tabs are icon-only, `White` at 70%.
- To the right of the bar, separated by 10 dp, sits a 64 dp circular **New invoice** button.

In the app: tabs **Invoices · Clients · Settings**.

**New invoice button: blue (decided 2026-11-07).** A 64 dp skeuomorphic glossy blue circle with a white "+" (section 5.4). Why:

1. Creating an invoice is the app's main action, and section 2.4 reserves blue for the main action. Next to a black tab bar, a black button would read as just another tab.
2. It stays the most visible thing on the screen, even next to the black tab bar.
3. The second reference uses exactly this: a glossy blue circle at the centre of the bottom bar.

The alternative considered was a black circle matching the first reference's send button; blue was chosen to keep the main action in the accent colour.

### 6.10 Forms (invoice editor)

The references have no forms, so these follow the same language:

- Every field has **8 dp corners**: 56 dp tall, glass regular with the specular rim, 16 dp horizontal padding, label above in `caption` `GreyText`. This includes single-line fields, multi-line boxes (notes, addresses), amount, rate and quantity fields, dropdowns (client, currency) and date fields.
- In Compose, pass `shape = RoundedCornerShape(8.dp)` to every `OutlinedTextField` / `TextField`, and hide the default underline indicator.
- Focused field: the rim becomes 1.5 dp `Primary`, with a soft outer glow (blur 8, `Primary` 25%).
- Line items are white cards (radius 20) with a dark round "×" to remove.
- The running total sits in a sticky glass bar at the bottom with the skeuomorphic primary "Save" button. Content scrolls under it into the bottom progressive blur.

### 6.11 Icons

Thin line icons, 1.75 dp stroke, rounded caps and joins, 20 to 24 dp. Material Symbols Rounded at weight 300 is a close match and already available in Compose.

---

## 7. Illustration style

The poster and the hero card use **glossy 3D "clay" illustrations**:

- Soft, inflated shapes with rounded edges, like toys.
- Mostly `Primary` blue and white, with small warm yellow sparkle accents (`#FFC233`).
- A strong top-left specular highlight and soft ambient occlusion underneath.
- Floating paper cards with skeleton text lines orbiting a central object, joined by a thin glowing ring.
- Four-point sparkles scattered around.

Compose cannot render this live, so illustrations are **pre-rendered transparent WebP files** (at 1x, 2x, 3x) bundled in the app. Suggested set, all in the same style, with invoice subjects (not copies of the reference artwork):

| Illustration | Where |
|---|---|
| A glossy invoice sheet with a ₹ coin and a pen | Home hero card, onboarding |
| A paper plane leaving a document | "Invoice sent" confirmation |
| A stack of coins with a green tick | "Paid in full" confirmation |
| An empty folder with a sparkle | Empty Invoices and Clients lists |
| A cloud with a padlock | Google Drive backup screen |

### 7.1 Empty states

Empty states have **no background block**: no card, no fill, no border. The illustration, title, one line of help and an optional button sit directly on the canvas (or the sky), centred in the free space.

- Illustration about 200 dp wide, then 28 dp gap, title in `headline` `Ink`, 10 dp gap, help text in `body` `GreyText`, 24 dp gap, optional secondary button.
- Used for: no invoices, no clients, no invoices matching a filter, no payments yet.

Until the WebP illustrations exist, the Compose-drawn layered card from PR #4 (`EmptyIllustration` in `Glass.kt`) can be recoloured to blue as a placeholder.

The glossy 3D sphere in the second reference is a nice single "mascot" object; a glossy blue ₹ coin could play the same role on the home screen.

---

## 8. Screen mapping

### Invoices (home)

1. Sky gradient with greeting header (6.1).
2. Hero card: outstanding total, overdue count, "+ New invoice" and "Record payment" (6.2).
3. Search pill (6.3) and status filter chips (6.4).
4. Section "Due this week" with "View all" link, then invoice cards (6.6).
5. Section "Recent" with the remaining invoices.
6. Floating tab bar with the New invoice FAB (6.9).

### Invoice details

- Gradient header with a dark back button (6.5), invoice number as `headline`, status pill below.
- White card: client, issue and due dates, currency.
- White card: line items and total, with amount in words in `caption`.
- Payments card: each recorded payment as a row, "Record payment" secondary button.
- Bottom bar: "Share PDF" dark button and "Edit" secondary button.

### Invoice editor

- Plain `Canvas` background (no gradient, so the form feels calm).
- Sections as white cards: Client, Dates, Currency chips, Line items, Notes.
- Sticky total bar with the primary "Save" button (6.10).

### Clients

- Gradient header with title "Clients" and a client count.
- Search pill.
- Client cards: name (`cardTitle`), email (`caption`), outstanding amount at bottom right, dark round button to open.

### Settings

- Gradient header with the studio logo and name.
- 2×2 quick tiles (6.7): Business profile, Bank details, Numbering, Backup.
- A white card list for the remaining toggles (show due date on PDF, GST note, default due days).

---

## 9. Implementing it in Compose (if adopted)

The restyle in PR #4 already has the right structure, so adopting this is mostly a reskin:

| PR #4 piece (`androidApp/.../ui/`) | Change |
|---|---|
| `Theme.kt` `Palette` (orange/peach) | Replace with the tokens in section 2 |
| `Theme.kt` Inter font family | Swap to Outfit Regular/Medium/SemiBold (`res/font/outfit_*.ttf`), type scale from section 3 |
| `Theme.kt` `Shapes` | Radii from section 4.2 |
| `Glass.kt` `AppBackground` | Sky gradient from section 5.1 |
| `Glass.kt` `GlassCard` | Flatter: white, radius 24, elevation 0 on canvas |
| `Glass.kt` `HeroCard` | White hero with two-weight headline instead of an orange glossy card |
| `Glass.kt` `FloatingNavBar` | Black pill with white selected pill, plus separate blue New invoice button |
| `Glass.kt` `GlossyFab`, `PrimaryButton` | Blue glow version; add `DarkRoundButton` and `StatusChip` |
| `Glass.kt` `EmptyIllustration` | Recolour now; replace with WebP illustrations later |
| Text fields across the screens | `RoundedCornerShape(8.dp)` on every field (section 6.10) |
| `Theme.kt` colour scheme | `lightColorScheme` from section 2, plus `LocalAppColors` for the tokens Material has no slot for (sky gradient, status colours) |

Status colours (section 2.3) are used as proposed.

### 9.1 Changes needed for the 2026-11-07 evening update

| Piece | Change |
|---|---|
| `build.gradle` | Add Haze (`dev.chrisbanes.haze:haze` and `haze-materials`) for backdrop and progressive blur |
| `Components.kt` | New `GlassSurface` modifier (blur, tint, rim, inner highlight, shadow) used by `AppCard`, `HeroCard`, fields, menus and dialogs |
| `Components.kt` `PrimaryButton`, `FloatingNavBar`, `NewInvoiceButton` | Skeuomorphic drawing from section 5.4, with pressed states |
| `Common.kt` fields and all text buttons | `RoundedCornerShape(8.dp)` |
| `IllustratedEmptyState` callers | Remove the wrapping `AppCard` |
| `InvoiceAppUi.kt` | Top and bottom progressive blur bands (section 5.5) over the screen content |

