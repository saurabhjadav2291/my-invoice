# My Invoice design system

Status: **adopted 2026-11-07, revised the same evening with an Unslop pass.** The Android app follows this document. The app is light only, and the invoice PDF keeps its own orange design.

Decided so far (2026-11-07): the PDF stays orange; the app is light only; text fields and form buttons have 8 dp corners; empty states have no background block; content fades into a progressive blur at the status bar and behind the bottom navigation. Saurabh then chose the full Unslop pass: the liquid glass, gloss and skeuomorphic styling were replaced with plain surfaces, hairlines and one blue accent. The blur bands stayed.

Unslop is by Damir Sokolovsky ([@heydamir](https://x.com/heydamir)), CC BY 4.0.

---

## 1. Direction

- **Product.** Invoices for a one-person studio that bills Indian and overseas clients, and keeps track of what is still owed.
- **Character.** Exact, unhurried, a little warm.
- **World.** A studio desk: paper invoices, bank slips, ink.
- **Moments.** The "Still to collect" figure on the home screen, with a small paper invoice beside it, and the invoice details card, drawn like the paper invoice with its orange rule.
- **Signature.** Invoice numbers like `RS/26-27/031` are set in IBM Plex Mono everywhere, like a serial stamp.

Principles:

1. **Quiet chrome.** Navigation, buttons and text use the neutral ramp. Nothing glows, and nothing has a gradient.
2. **One accent.** Blue marks the main action, the + button and the selection. Everything else is ink, white or grey.
3. **The paper carries the colour.** The only other colour is the PDF's orange, and it appears only where the app shows the invoice itself.
4. **Depth from surface steps.** White cards on a pale canvas, a hairline around each, and a soft neutral shadow only on things that float (the tab bar, the hero card, menus).

---

## 2. Colour

### 2.1 Neutral ramp (cool ink)

| Token | Hex | Role |
|---|---|---|
| `background` | `#F5F7FA` | Screen canvas |
| `surface` | `#FFFFFF` | Cards, fields, the tab bar, dialogs, menus |
| `surfaceRaised` | `#ECEFF4` | Selected tab, segmented-control track, tags, pressed cards |
| `ink` | `#111827` | Primary text |
| `greyText` | `#4B5565` | Secondary text and icons (7.5:1 on white) |
| `grey` | `#6B7487` | Muted text, meta and resting icons (4.7:1 on white) |
| `hairline` | `ink` at 8% | Card and tab bar borders, dividers |
| field border | `ink` at 14% | Text field, picker and switch track borders |

### 2.2 Accent and paper

| Token | Hex | Role |
|---|---|---|
| `primary` | `#2F6FD6` | Primary button, the + button, focused field border. White text on it is 4.8:1. |
| `primaryText` | `#2563C9` | Blue text on light surfaces, such as Save in a top bar (5.7:1) |
| `primarySoft` | `#EAF1FC` | The Sent status fill |
| `inverse` | `#111827` | Selected filter chip, snackbar, dark button |
| `paperAccent` | `#FF5400` | The PDF orange. Used only on the paper invoice: the rule on the hero card, the paper sheet and the empty-state illustration. |

### 2.3 Status colours

Used as a small dot plus a soft tag, never as large fills.

| Status | Text and dot | Tag fill |
|---|---|---|
| Draft | `#4B5565` | `#ECEFF4` |
| Sent | `#2559B8` | `#EAF1FC` |
| Partly paid | `#9A5B00` | `#FBF1E0` |
| Paid | `#15803D` | `#E6F4EA` |
| Overdue | `#C62828` | `#FCEAEA` |
| Error text | `#DC2626` | |

### 2.4 Rules

- One filled blue element per screen region.
- No hue gradients, gradient text, glows or coloured shadows. Shadows are ink at 6 to 10%.
- Screens never use raw hex values. Everything comes from `Ui.colors` in `Theme.kt`.
- **Light only.** The app ignores the phone's dark setting. System bar icons are always dark.

### 2.5 The invoice PDF

The PDF keeps its orange accent (`#FF5400`) and Inter, so new invoices match the ones already sent. The app theme never changes it.

---

## 3. Typography

**Interface:** [Outfit](https://fonts.google.com/specimen/Outfit), Regular 400 and Medium 500. SemiBold is bundled but not used in the scale. Hierarchy comes from size and colour, not weight.

**Serial:** [IBM Plex Mono](https://fonts.google.com/specimen/IBM+Plex+Mono) Regular, only for invoice numbers (`TextStyle.serial()`).

### 3.1 Scale (sp)

| Material slot | Size / line | Weight | Tracking | Use |
|---|---|---|---|---|
| `displaySmall` | 32 / 36 | Medium | -0.5 | Key figures: still to collect, invoice total |
| `headlineLarge` | 28 / 32 | Medium | -0.3 | Page titles: Invoices, Clients, Settings |
| `headlineMedium` | 24 / 28 | Medium | 0 | Editor total |
| `headlineSmall` | 20 / 28 | Medium | 0 | Empty-state titles |
| `titleLarge` | 18 / 24 | Medium | 0 | Section headings, top bar titles, dialog titles |
| `titleMedium` | 16 / 24 | Medium | 0 | Client name on a card, amounts on rows |
| `bodyLarge` | 15 / 22 | Regular | 0 | Body text, field text |
| `bodySmall` | 13 / 20 | Regular | 0 | Captions, labels above fields, meta |
| `labelLarge` | 15 / 20 | Medium | 0 | Button and chip labels |
| `labelMedium` | 12 / 16 | Medium | 0 | Status tags |
| `labelSmall` | 12 / 16 | Regular | 0.1 | Small meta |

- Negative tracking only at 28 sp and above.
- Amounts use tabular figures (`TextStyle.tabular()`), in one format per currency.
- Sentence case everywhere, no uppercase labels, no italics.

---

## 4. Spacing, radii and elevation

### 4.1 Spacing

Everything sits on a 4 dp grid.

| Use | Value |
|---|---|
| Screen side margin | 20 dp |
| Card padding | 16 dp (hero card 20 dp) |
| Between fields in a card | 16 dp |
| Between cards in a list | 8 dp |
| Between sections | 16 dp above a section heading |
| Label above a field | 8 dp gap, 4 dp inset |
| Header | 24 dp top, 16 dp bottom |

### 4.2 Corner radii

| Element | Radius |
|---|---|
| Tags (status, currency, number preview), segmented-control segments | 6 dp |
| Text fields, pickers, buttons, filter chips, tabs, menus items | 8 dp |
| Segmented-control track | 10 dp (6 + 4 padding) |
| Logo tile, avatar, menus | 12 dp |
| Cards, the tab bar, the + button | 16 dp |
| Hero card, dialogs, date picker | 20 dp |
| Round icon buttons (back, delete) | circle |

### 4.3 Elevation

- Cards on the canvas: hairline, no shadow.
- Floating things (tab bar, + button, hero card, paper sheet): shadow 12 dp, ink at 6% ambient and 10 to 12% spot.
- Menus: 8 dp shadow plus a hairline.

---

## 5. Backgrounds and blur

### 5.1 Canvas

Every screen sits on the plain `background` colour. There is no header gradient.

### 5.2 Progressive blur at the top and bottom

Content never ends in a hard cut where it meets the status bar or the tab bar.

| Edge | Band | Blur | Tint |
|---|---|---|---|
| Top (tab screens) | The status bar only | 0 at its lower edge, rising to 20 dp at the top | `background`, 0% to 55% towards the edge |
| Bottom (tab screens) | Behind the tab bar, about 100 dp including the gesture bar | 0 at the top, rising to 24 dp at the bottom | `background`, 0% to 60% towards the edge |
| Inner screens | The pinned back bar | 20 dp, masked out over its lower 30% | `background`, 90% fading to 0 |

- The editor's total bar is a plain white sheet with a hairline along the top. The form scrolls under it.
- Lists add bottom padding equal to the band, so the last row can rest above the tab bar.
- Android 12 and newer blur with Haze 1.7.0. Older phones get the tint alone.

---

## 6. Components

### 6.1 Screen header (tab screens)

Page title in `headlineLarge` `ink` ("Invoices", "Clients", "Settings"), with one line of `bodySmall` `greyText` under it: the studio name, the client count, or what the page holds. If a logo is set, it appears in a 44 dp tile with 12 dp corners on the left. Actions on the right: a round + on Clients, a blue "Save" text button on Settings. The header scrolls with the content.

### 6.2 Hero card

White, 20 dp corners, floating shadow, with a 4 dp `paperAccent` rule along the top edge, like the paper invoice.

- **Home:** "Still to collect" in `bodySmall`, the amount per currency in `displaySmall`, then "4 unpaid invoices" with an overdue tag. On the right, the paper invoice drawn in code: two small sheets, the front one with the orange rule, the last three digits of an invoice number in mono, and grey item lines.
- **Invoice details:** invoice number in mono and the status tag, then the client name, dates, and the total or amount still owed.

### 6.3 Buttons

All text buttons have 8 dp corners and are 48 dp tall. Press is a 0.98 scale over 120 ms.

| Kind | Look | Use |
|---|---|---|
| Primary | `primary` fill, white label | One per screen: Save, Create invoice, View or share PDF |
| Secondary | White, hairline, `ink` label, `greyText` icon | Add item, Record payment, Mark as sent, Choose logo |
| Dark | `inverse` fill | Kept for a strong second action; not used now |
| Text | `primaryText` label, no fill | Save in top bars, dialog buttons |
| Round icon | 40 dp circle, white with hairline (or `surfaceRaised` for the quiet style), 18 dp `greyText` icon | Back, add client, delete item or payment |

### 6.4 Tags and chips

- **Status tag:** 6 dp corners, status fill, a 6 dp dot and the label in `labelMedium`.
- **Quiet tag:** `surfaceRaised`, `greyText`, 6 dp corners. Currency codes, "Default", and the next-number preview (in mono).
- **Filter chip:** 36 dp tall, 8 dp corners, white with hairline; selected is `inverse` with white text.
- **Segmented control:** `surfaceRaised` track with 4 dp padding; the selected segment is white with a hairline.

### 6.5 Cards and rows

White, 16 dp corners, hairline, 16 dp padding. A tappable card steps to `surfaceRaised` while pressed; there is no open-arrow button, the whole card is the target.

- **Invoice row:** number in mono and the status tag on the first line; client name (`titleMedium`) and "Issued 7 Nov 2026" (`grey`) on the left, the amount on the right.
- **Client row:** avatar tile with the first letter, name and email, currency tag.

### 6.6 Tab bar

A white floating bar with 16 dp corners, 64 dp tall, hairline and floating shadow, with the blue + tile (64 dp, 16 dp corners) beside it. The selected tab is a `surfaceRaised` segment with 8 dp corners, its icon and label in `ink`. Other tabs show only their icon in `grey`.

### 6.7 Forms

- Fields: 8 dp corners, white, field border at rest, `primary` border when focused. Label above in `bodySmall` `greyText`.
- Pickers (dropdowns and dates) look like fields and open a menu or the date picker.
- Menus: white, 12 dp corners, hairline, 8 dp shadow.
- Dialogs and the date picker: white, 20 dp corners.
- Snackbar: `inverse`, 12 dp corners, light blue action text.

### 6.8 Icons

The project's own line icon set (`AppIcons.kt`), 18 dp in buttons and rows, 20 dp in the tab bar, coloured with the text role (`greyText` or `grey`). No emoji.

---

## 7. Empty states

No background block. The illustration, title, one line of help and an optional button sit on the canvas, centred.

- Illustration: two flat paper sheets drawn in code, the front one with the orange rule, the screen's icon in `grey` and two item lines.
- Then 24 dp, the title in `headlineSmall`, 8 dp, help in `bodyLarge` `greyText`, 24 dp, an optional secondary button.
- Used for: no invoices, no clients, no invoices matching a filter, no payments yet.

---

## 8. Motion

- One easing curve, `CubicBezierEasing(0.22, 1, 0.36, 1)` (`AppEasing`).
- Press: 0.98 scale over 120 ms. Colour changes (pressed card, selected chip or tab): 150 ms.
- No looping animation, no springs, nothing animates layout size.

---

## 9. Where it lives in the code

| File | What |
|---|---|
| `ui/Theme.kt` | `AppColors` tokens, Outfit and IBM Plex Mono, type scale, `tabular()` and `serial()` |
| `ui/Components.kt` | `Modifier.surface`, `progressiveBlur`, `ScreenHeader`, `AppCard`, `HeroCard`, buttons, chips, `FloatingNavBar`, `PaperInvoice`, empty states |
| `ui/Common.kt` | `InnerScaffold` (pinned back bar with blur), fields, pickers, dialogs, status tags |
| `ui/InvoiceAppUi.kt` | Blur source, status-bar and tab-bar blur bands, snackbar |
| `res/font/` | `outfit_*.ttf`, `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf` |
