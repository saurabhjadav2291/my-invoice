# My Invoice design system

Status: **adopted 2026-11-07, revised the same evening with an Unslop pass, and on 2026-11-08 with Saurabh's references for the buttons, bottom navigation, segmented tabs and alert banner.** The Android app follows this document. The app is light only, and the invoice PDF keeps its own orange design.

Decided so far (2026-11-07): the PDF stays orange; the app is light only; text fields have 8 dp corners (form buttons did too until the 2026-11-08 pill buttons); empty states have no background block; content fades into a progressive blur at the status bar and behind the bottom navigation. Saurabh then chose the full Unslop pass: the liquid glass, gloss and skeuomorphic styling were replaced with plain surfaces, hairlines and one blue accent. The blur bands stayed.

Update 2026-11-08: buttons follow Saurabh's button spec (section 6.3). They are pill shaped in four variants with soft, lightly neumorphic surfaces, and focus is a crisp blue outline, never a glow. The tab bar, segmented tabs and alert banner follow his reference images. Text fields keep their 8 dp corners.

Unslop is by Damir Sokolovsky ([@heydamir](https://x.com/heydamir)), CC BY 4.0.

---

## 1. Direction

- **Product.** Invoices for a one-person studio that bills Indian and overseas clients, and keeps track of what is still owed.
- **Character.** Exact, unhurried, a little warm.
- **World.** A studio desk: paper invoices, bank slips, ink.
- **Moments.** The "Still to collect" figure on the home screen, with a small paper invoice beside it, and the invoice details card, drawn like the paper invoice with its orange rule.
- **Signature.** Invoice numbers like `RS/26-27/031` are set in Geist Mono everywhere, like a serial stamp.

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
| `primary` | `#155EEF` | Primary button, the + button, selected tab, focus rings, link text. White text on it is 5.4:1. |
| `primaryHover` | `#0B4FD6` | Primary button on hover, link text on hover |
| `primaryPressed` | `#0A45BD` | Primary button and + button while pressed |
| `primaryTint` | `#EEF3FE` | Secondary and link buttons on hover (`#E1EAFD` pressed) |
| `primaryText` | `#155EEF` | Blue text on light surfaces |
| `primarySoft` | `#EAF1FC` | The Sent status fill |
| `frosted` | `#FBFCFD` | Secondary buttons, the tab bar and banners (with a white-to-frosted gradient) |
| `surfacePressed` | `#E2E6EC` | Tertiary buttons while pressed |
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

The PDF keeps its orange accent (`#FF5400`). Since 2026-11-09 it is set in Geist, like the app (it used Inter before). The app theme never changes it.

---

## 3. Typography

**Interface:** [Geist](https://vercel.com/font) (decided 2026-11-09), Regular 400, Medium 500 and SemiBold 600, all bundled. SemiBold for page titles, key figures and section headings (decided 2026-11-09, Medium read too light there); Medium for card titles, labels and buttons; Regular for body text.

**Serial:** [Geist Mono](https://vercel.com/font) Regular, only for invoice numbers (`TextStyle.serial()`).

### 3.1 Scale (sp)

| Material slot | Size / line | Weight | Tracking | Use |
|---|---|---|---|---|
| `displaySmall` | 32 / 36 | SemiBold | -0.5 | Key figures: still to collect, invoice total |
| `headlineLarge` | 28 / 32 | SemiBold | -0.3 | Page titles: Invoices, Clients, Settings |
| `headlineMedium` | 24 / 28 | SemiBold | 0 | Editor total |
| `headlineSmall` | 20 / 28 | SemiBold | 0 | Empty-state titles |
| `titleLarge` | 18 / 24 | SemiBold | 0 | Section headings, top bar titles, dialog titles |
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
| Text fields, pickers, menu items | 8 dp |
| Every button with a text label, segmented tabs and their track, the tab bar | pill (fully round) |
| Logo tile, avatar, menus | 12 dp |
| Cards | 16 dp |
| Hero card, dialogs, date picker, alert banner | 20 dp |
| Round icon buttons (back, delete) | circle |

Every corner with a radius uses **70% corner smoothing** (decided 2026-11-09), the same squircle curve as Figma's corner smoothing: `SmoothShape(radius)` in `ui/SmoothShape.kt`. Pills and circles stay fully round. Material's own fallback shapes (`AppShapes` in `Theme.kt`) stay plain rounded because Material requires that type; every shape the app sets itself is smoothed.

### 4.3 Elevation

- Cards on the canvas: hairline, no shadow.
- Floating things (hero card, paper sheet): shadow 12 dp, ink at 6% ambient and 10 to 12% spot.
- Menus: 8 dp shadow plus a hairline.

---

## 5. Backgrounds and blur

### 5.1 Canvas

Every screen sits on a sky-to-canvas background (decided 2026-11-09, from Saurabh's final background image): `#B6D4F9` at the top, through `#C9DFF9` (15%), `#DFECF9` (25%) and `#EFF4FA` (35%), reaching the `background` canvas `#F5F7FA` at 45% of the screen height and staying there. It is drawn in code against the window height (`rememberBackgroundBrush`) and does not scroll. The pinned back bar of inner screens and the status-bar blur band use the same gradient, so they blend into it.

### 5.2 Progressive blur at the top and bottom

Content never ends in a hard cut where it meets the status bar or the tab bar.

| Edge | Band | Blur | Tint |
|---|---|---|---|
| Top (tab screens) | The status bar only | 0 at its lower edge, rising to 20 dp at the top | `background`, 0% to 55% towards the edge |
| Bottom (tab screens) | Behind the tab bar, about 108 dp plus the gesture bar | 0 at the top, rising to 24 dp at the bottom | `background`, 0% to 60% towards the edge |
| Inner screens | The pinned back bar | 20 dp, masked out over its lower 20% | `background`, 97% (near solid, so scrolled text never reads through the title) fading to 0 over the last 20% |

- The editor's total bar is a plain white sheet with a hairline along the top. The form scrolls under it.
- Lists add bottom padding equal to the band, so the last row can rest above the tab bar.
- Android 12 and newer blur with Haze 1.7.0. Older phones get the tint alone.

---

### 5.3 Soft keys

**Decided 2026-11-08:** buttons looked flat, so every control takes the look of the active tab in the segmented selector. One shared modifier, `Modifier.softKey()` in `Components.kt`, draws it:

- A vertical gradient from a lighter top to the base colour.
- A soft shade just under the top edge: ink at 8 to 18%, fading out over 8 dp. It deepens while pressed.
- A 1.5 dp white rim, 50% white along the top and full white along the bottom.
- Raised keys add a small neutral lift (`keyShadow`). Nothing is ever coloured or glowing.

| Element | Top | Bottom | Rim | Lift |
|---|---|---|---|---|
| Neutral key (secondary button, icon buttons) | `#F9FAFB` | `#E4E7EC` | full | 2 to 4 dp |
| Primary button and the + disc | `primary` mixed with 18% white | `primary` | 55% to 60% | 2 dp, 6 dp on hover |
| Selected segmented tab, selected nav tab | `#DFE2E7` | `#D5D9DF` | full | none (pressed in) |
| Tab bar and segmented track | `#F9FAFB` / `#ECEEF1` | `#ECEEF1` | full | 20 dp for the tab bar, 4 dp for the track |
| Text field and picker well (inset) | `#E9ECF0` | `#F7F8FA` | full | none |

### 5.4 Liquid glass

Decided 2026-11-09. Round icon buttons (back, add client, edit, delete), the Settings option icons and secondary buttons are translucent glass, so the sky gradient or any coloured surface shows through them (`Modifier.glass` in `Components.kt`):

It keeps the depth of the soft keys (5.3), only the body turns clear (revised 2026-11-09 after the first, flat version lost the glass feel):

- Lift: a soft ink shadow 3 dp below (16%, 6 dp blur), drawn only outside the shape so it never smudges through the clear body. Android 9 and later; older phones skip it.
- Body: white at 78% at the top to cool grey `#E4E7EC` at 42% at the bottom.
- Top shade: a faint ink shade (6%) inside the top 8 dp, as on the soft keys.
- Rim: 1.5 dp, bright white at the top (95%) and bottom (90%) edges where light catches the glass, clearer (35%) at the sides.
- Never a glow. Pressed: half the lift, a clearer body (50% / 50%), a deeper top shade, and the usual scale.

The page background is a smooth gradient, so a translucent fill reads the same as a live backdrop blur; a real blur can be added later if glass ever sits over busy content.

## 6. Components

### 6.1 Screen header (tab screens)

Page title in `headlineLarge` `ink` ("Invoices", "Clients", "Settings"), with one line of `bodySmall` `greyText` under it: the studio name, the client count, or what the page holds. If a logo is set, it appears in a 44 dp tile with 12 dp corners on the left. Actions on the right: a round + on Clients, a blue "Save" text button on Settings. The header scrolls with the content.

### 6.2 Hero card

White, 20 dp corners, floating shadow, with a 4 dp `paperAccent` rule along the top edge, like the paper invoice.

- **Home:** "Still to collect" in `bodySmall`, the amount per currency in `displaySmall`, then "4 unpaid invoices" with an overdue tag. On the right, the paper invoice drawn in code: two small sheets, the front one with the orange rule, the last three digits of an invoice number in mono, and grey item lines.
- **Invoice details:** invoice number in mono and the status tag, then the client name, dates, and the total or amount still owed.

### 6.3 Buttons

One component, `AppButton` in `ui/Buttons.kt`, with four variants. Screens never style their own buttons.

Global rules:

- Pill shaped. Height 48 dp (link 40 dp), horizontal padding 20 dp (link 12 dp), 18 dp icons with an 8 dp gap.
- Transitions take 200 ms on the app curve.
- Focus is never a glow or a blur. It is a crisp 2 dp `primary` outline.
- Pressed: scale 0.98 and a darker fill. Disabled: 40% opacity, no hover or press.

| Variant | Default | Hover | Focused | Use |
|---|---|---|---|---|
| Primary | `primary` soft key (5.3), white label and icon, 2 dp soft neutral lift | `primaryHover`, lift rises to 6 dp | Default fill, 3 dp white gap, then a 2 dp blue ring outside | One per screen: Save, Create invoice, View or share PDF, Save settings |
| Secondary | Liquid glass (5.4), `ink` label, blue icon, no lift | Glass | Glass with a 2 dp blue border | Record payment, Mark as sent, Add item, Add bank account, Choose logo, Preview a sample invoice |
| Tertiary | Transparent, `greyText` label and icon | `surfaceRaised` fill | Transparent with a 2 dp blue border | Cancel in dialogs; destructive actions in red (Archive client, Delete this account, Remove) |
| Link | `primary` text, optional trailing arrow | `primaryHover` text on a `primaryTint` wash | 2 dp blue border | Save in top bars, OK and Save in dialogs, inline actions |

Round icon buttons (back, add client, edit, delete) stay 40 dp circles in liquid glass (5.4), with an 18 dp `greyText` icon. Pressed: the glass clears a little and scales to 0.96.

Tertiary and link buttons stay flat until hovered, as the spec asks for no unnecessary shadows.

### 6.4 Tags and segmented tabs

- **Status tag:** 6 dp corners, status fill, a 6 dp dot and the label in `labelMedium`.
- **Quiet tag:** `surfaceRaised`, `greyText`, 6 dp corners. Currency codes, "Default", and the next-number preview (in mono).
- **Segmented tabs:** a pale pill track (`#ECEEF1`) with a white top rim, a faint lift and 4 dp padding. The chosen option is pressed into it: a 36 dp grey pill (`#DFE2E7` to `#D5D9DF`), a soft shade under its top edge and a white 1.5 dp rim. Labels are `labelLarge`, `ink` when chosen and `greyText` otherwise. Used for the invoice filters on the home screen (sized to their labels, scrolling sideways) and for currency and "Amount / Rate × qty" choices (equal widths).

### 6.5 Cards and rows

White, 16 dp corners, hairline, 16 dp padding. A tappable card steps to `surfaceRaised` while pressed; there is no open-arrow button, the whole card is the target.

- **Invoice row:** number in mono and the status tag on the first line; client name (`titleMedium`) and "Issued 7 Nov 2026" (`grey`) on the left, the amount on the right.
- **Client row:** avatar tile with the first letter, name and email, currency tag.
- **Settings (decided 2026-11-09):** the tab is a short list, not a form. Three rows split by hairlines, each a 40 dp liquid-glass circle (as the round icon buttons) with a `greyText` icon, a `titleMedium` title, one `bodySmall` grey line of what is inside, and a chevron:
  - **Account details** opens a screen with your name, email and phone (sign-in and backup will go here when they exist).
  - **Business info** opens a screen with studio name, address, PAN, logo, invoice numbers, on-the-invoice options and bank accounts.
    Once saved it opens read-only (label over value, "Not added" in grey) so nothing changes by accident. **Edit info** (primary, also an Edit link in the top bar) turns the fields on, with Save and Cancel. **Remove business details** (tertiary, red, with a confirm) clears studio name, address, PAN and logo; name, numbering and bank accounts stay.
  - **Preview invoice** opens a sample PDF built from the saved details.
  The two screens use no cards (nor do the client form, decided 2026-11-09: Client, Contact and tax, Usual currency; and the invoice form: Client and currency, Dates, Items, On the invoice, with a hairline between items and an "Item 1" heading carrying the remove button): sections are a `titleLarge` heading over their fields, split by a `hairline` divider with 20 dp above and below, fields 12 dp apart.

### 6.6 Bottom navigation

Two separate pieces floating 20 dp from the screen sides (decided 2026-11-09): the tab pill, 72 dp tall and filling the width, then a 12 dp gap, then the + disc, also 72 dp. The pill is drawn as a soft key from `#F9FAFB` to the track colour `#ECEEF1`, with a white rim and a subtle ink drop shadow (decided 2026-11-09): 24 dp blur at 10% offset 8 dp down, plus a 3 dp contact shadow at 8% offset 1 dp. Never a coloured glow. The + disc casts the same shadow.

- Only the selected tab shows its label: a pressed-in grey pill (the same key as the selected segmented tab) holding the 24 dp icon in `primary` blue and a `labelLarge` label in `ink` (decided 2026-11-08), side by side. The other tabs are a `grey` 24 dp icon only, with no fill. Switching tabs takes 260 ms (decided 2026-11-09): every slot animates its width together, the new pill fades and stretches in while the old one fades and narrows, and the label slides open inside it. The pill is always drawn as a full pill at its current width, never a square or clipped shape mid-transition.
- The **+ button** stands on its own to the right of the pill, as tall as it: a 72 dp blue soft-key disc inside a 4 dp neutral key ring, with a white + icon and a soft neutral shadow, no glow. Pressed: `primaryPressed` and scale 0.96.
- Invoices, Clients and Settings share the pill. The first and last tabs sit 8 dp from its ends, the same gap as above and below, so the selected pill is evenly inset. The selected pill takes the width it needs; the other tabs split the remaining space equally, each icon centred in its slot (decided 2026-11-09). Icon-only tabs are at least 48 dp wide; the selected pill has 14 dp side padding.

### 6.7 Forms

- **Every full-screen form uses one layout (decided 2026-11-10):** no cards. Sections are a `titleLarge` heading over their fields (`SettingsSection`), split by a `hairline` divider with 20 dp above and below, fields 12 dp apart. A final hairline sits above the full-width Save button; a destructive action (Delete, Archive) is a red tertiary button under it. This covers Account details, Business info, Add/edit bank account (Account, Bank codes, Default), the client form and the invoice form. Short dialogs (Record payment) keep their fields in a single column with no sections.
- Fields: 48 dp tall (12 dp vertical, 16 dp horizontal padding), 8 dp corners, drawn as an inset soft-key well (`Modifier.fieldWell()`): darker at the top, a shade under the top edge and a white rim along the bottom. No border at rest; a 2 dp `primary` border when focused. Label above in `bodySmall` `greyText`.
- Every blank field shows a guided placeholder in `grey`: an example of what goes there ("e.g. ABCD0123456", "Street, city, PIN code", "0.00"), never a repeat of the label.
- Pickers (dropdowns and dates) use the same 48 dp well and open a menu or the date picker.
- Menus: white, 12 dp corners, hairline, 8 dp shadow.
- Dialogs and the date picker: white, 20 dp corners.
- Messages (saved, errors, hints) appear as the alert banner (6.8) above the tab bar or the screen's bottom edge.

### 6.8 Alert banner

A frosted white card with 20 dp corners, a hairline and a soft neutral shadow, with 20 dp side padding and 16 dp vertical padding.

- Left: a 32 dp line icon in the status colour. Success is a ticked circle in the Paid green, errors a circle with "!" in the error red, hints the same in `primary` blue.
- Then 16 dp, the message in `titleMedium` `ink`, and under it the time in `bodySmall` `grey`.
- Used for every confirmation ("Payment recorded"), error and hint. It replaces the old dark snackbar.

### 6.9 Icons

Every icon comes from the free **Hugeicons** set, stroke rounded style (decided 2026-11-10; https://hugeicons.com, npm `@hugeicons/core-free-icons`, MIT). They are copied into `AppIcons.kt` as vectors, each naming its Hugeicons source, so there is no icon library dependency and no Material icons. New icons must also come from the free Hugeicons stroke rounded set. Sizes: 18 dp in buttons and rows, 24 dp in the tab bar, 32 dp in banners, coloured with the text role (`greyText` or `grey`). No emoji.

---

## 7. Empty states

No background block. The illustration, title, one line of help and an optional button sit on the canvas, centred.

- Illustration: two flat paper sheets drawn in code, the front one with the orange rule, the screen's icon in `grey` and two item lines.
- Then 24 dp, the title in `headlineSmall`, 8 dp, help in `bodyLarge` `greyText`, 24 dp, an optional secondary button.
- Used for: no invoices, no clients, no invoices matching a filter, no payments yet.

---

## 8. Motion

- One easing curve, `CubicBezierEasing(0.22, 1, 0.36, 1)` (`AppEasing`).
- Buttons, tabs and the + button: 200 ms for colour, lift and the 0.98 press scale. Pressed cards: 150 ms.
- No looping animation, no springs, nothing animates layout size.

---

## 9. Where it lives in the code

| File | What |
|---|---|
| `ui/Theme.kt` | `AppColors` tokens, Geist and Geist Mono, type scale, `tabular()` and `serial()` |
| `ui/Buttons.kt` | `AppButton` with `ButtonVariant.Primary`, `Secondary`, `Tertiary` and `Link` |
| `ui/Components.kt` | `Modifier.surface`, `progressiveBlur`, `ScreenHeader`, `AppCard`, `HeroCard`, icon buttons, `PillChoice` (segmented tabs), `AlertBanner`, `FloatingNavBar`, `PaperInvoice`, empty states |
| `ui/Common.kt` | `InnerScaffold` (pinned back bar with blur), fields, pickers, dialogs, status tags |
| `ui/InvoiceAppUi.kt` | Blur source, status-bar and tab-bar blur bands, alert banners (`BannerVisuals`) |
| `res/font/` | `outfit_*.ttf`, `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf` |
