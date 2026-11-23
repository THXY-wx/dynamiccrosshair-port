# Changelog

## 1.1.14 — 2026-11-16

- Restored the original primary+secondary overlay (removed the held-style exclusive rule).
- Enlarged the bracket-round arcs (6.6 units) so they sit clearly outside the 4.6-unit square,
  matching the original mod's "big brackets around a small square" layering.

## 1.1.10 — 2026-11-15

- Modifier dots are drawn through the same SDF shader as the base crosshair, so the centre dot and
  the square/X share one coordinate system and the dot can no longer sit outside the shape.

## 1.1.8 — 2026-11-15

- Reverted the single-base-style change: drawing primary and secondary together is the original
  behaviour.
- Bracket-round corners are now real quarter-circle arcs plus straight arms (the previous
  rounded-box cut left the four side middles, i.e. straight lines at top/bottom/left/right).
- Suspended Crosshair-Selection's own centre dot while our crosshair is active, so the green dot can
  no longer be drawn at the un-bobbed screen centre and appear offset.

## 1.1.7 — 2026-11-15

- Draw exactly one base crosshair. Previously the primary (e.g. on-block square) and secondary
  (e.g. held ranged X) styles were both rendered, producing an X with four extra straight lines.

## 1.1.6 — 2026-11-15

- Square and diamond outlines now use a rounded-box SDF with a real corner radius (2.0 / 2.6),
  so the corners are actually rounded instead of square.
- Cross-open gap tightened (0.9) and arm caps rounded.
- Verified after a clean build; earlier incremental builds could silently reuse stale classes when
  Gradle's file watcher failed.

## 1.1.4 — 2026-11-15

- Set a GUI orthographic projection before drawing in `RenderTickEvent.END`. Previously the world
  perspective matrix was still active, so sprite modifiers (centre dot) were offset and shapes could
  look skewed.

## 1.1.3 — 2026-11-15

- Cross arms now use rounded SDF caps (larger visible curvature, no jaggies).
- Arm thickness is multiplied by 1.25 for better visibility while remaining identical on both
  axes; bracket elbows are rounded too.

## 1.1.2 — 2026-11-15

- Anti-aliasing width now matches Crosshair-Selection (0.5 physical pixel) instead of two physical
  pixels. The arms use CS's `thickness` (default 0.75 GUI px) and the 0.5 px CS outline, so the
  rendered thickness now matches CS.

## 1.1.1 — 2026-11-15

- Draw the crosshair from `RenderTickEvent.END` like Crosshair-Selection instead of the HUD event.
  This bypasses Angelica's HUD cache, which was resampling the crosshair (jaggies) and updating it
  at a lower rate (stuttering bobbing and bow arrow).
- Arm thickness now follows Crosshair-Selection's `thickness` (quarter GUI pixels); the CS outline
  (faint black 0.5px) is applied in vector mode.
- Vector quad is drawn through a VBO instead of `glBegin`, and shader compile errors are logged.

## 1.1.0 — 2026-11-15

- Optional Crosshair-Selection-style vector renderer (AUTO + CS only): every dynamic style is drawn
  as a procedural SDF in a fragment shader, so shapes are smooth and anti-aliased instead of pixel
  art. Colour, flowing rainbow, outline and the gap animation come from Crosshair-Selection.
  `compat.useSelectionRenderer` (default on, "平滑矢量渲染" in the Compat GUI page).
- Config screen is localised to Chinese when the game language is zh_CN/zh_TW.

## 1.0.9 — 2026-11-14

- Bow/crossbow gap animation is now pixel-stepped: 8 charge stages, each revealing one more sprite
  pixel. The hole follows the crosshair shape (diamond for diagonal sprites, plus for straight
  crosses) instead of a growing square mask.

## 1.0.8 — 2026-11-14

- Bow/crossbow wind-up now mimics Crosshair-Selection's `uBowOpen` gap animation: the sprite is
  split around a central hole, so the four arms converge towards the centre as the weapon charges.
  The crosshair is no longer scaled or squashed.

## 1.0.7 — 2026-11-14

- Auto mode now calls Crosshair-Selection's own bow/crossbow charge indicator
  (`CrosshairHud.drawArrow` / `drawNumber`) so the arrow/percentage, position, colour and 95%
  threshold match Crosshair-Selection exactly. The local `!` is only a fallback when
  Crosshair-Selection is absent.
- Note: Crosshair-Selection 2.0.8 has no literal `!` renderer; its `BowChargeIndicator` offers
  `NONE`, `NUMBER` (`NN%`) and `ARROW` (`↑`).

## 1.0.6 — 2026-11-14

- Snap crosshair quads to whole screen pixels: an odd 15px sprite on a fractional centre could be
  one pixel wider on the left than on the right.
- Tinkers' Construct bow/crossbow support through `IWindup` (reflection only): wind-up progress
  scales the crosshair, a fully wound crossbow shows `!` beside it.
- New `bobbing.crossbowIndicator` option ("Crossbow !" in the Bobbing GUI page).
- Test harness gains a Tinkers crossbow state, verified with a stand-in implementing the real
  `tconstruct.library.weaponry.IWindup` signature.

## 1.0.5 — 2026-11-14

- Do not apply Crosshair-Selection's outline to the pixel sprites by default: it produced an ugly
  black border around thin, colourful crosshairs. New `compat.adoptSelectionOutline` option
  (default off, "Adopt outline" in the Compat GUI page).
- Test harness disables vanilla's pause-on-focus-loss so automated screenshots are not replaced by
  the pause menu.

## 1.0.4 — 2026-11-14

- Placement feedback is now a damped shake (rotation + small horizontal/vertical offset + slight
  scale, 260 ms) instead of an expanding ring, matching the CrosshairBobbing style. Controlled by
  `bobbing.placeFeedback` ("Place shake" in the GUI).

## 1.0.3 — 2026-11-14

- Restore Crosshair-Selection's block-placement feedback in AUTO mode: after a placement is
  confirmed (clicked face + replaceable-target handling), draw an expanding ring in the current
  crosshair colour / flowing rainbow.
- New `bobbing.placeFeedback` option and "Place pulse" row in the Bobbing GUI page.
- Test harness verifies the pulse rendering; it now refuses to run in any world whose integrated
  server folder is not `dctest`.

## 1.0.2 — 2026-11-14

- Fix a crash on GTNH 2.9.0: the production runtime uses srg method names (`func_149727_a`, ...)
  while the reflection helper only tried MCP names, then cached `null` in a `ConcurrentHashMap`,
  which throws `NullPointerException`.
- Reflection now tries MCP and srg names, caches misses with `Optional.empty()`, and detects
  dig-speed overrides by scanning methods (covers GregTech/Tinkers' tools).
- Guard the crosshair evaluation, bobbing and rendering paths so a misbehaving modded block or
  item cannot crash the game; the vanilla crosshair is kept instead.
- Test harness only modifies worlds it creates itself, and is gated on the deobfuscated
  environment (or an explicit `-Ddynamiccrosshair.test.force=true`).
- Verified in GTNH 2.9.0 with 247 mods, Angelica/NotFine, Java 25: no crash, all 13 states drawn.

## 1.0.1 — 2026-11-14

- AUTO mode now drives Crosshair-Selection's colour, flowing rainbow, outline and dot while using
  this mod's dynamic shapes and bobbing; `YIELD` and `FORCE_OURS` remain available.
- New `compat.adoptSelectionColours` option and Compat GUI row.

## 1.0.0 — 2026-11-14

- Initial 1.7.10 / GTNH 2.9.0 port merging DynamicCrosshair, DynamicCrosshairCompat
  and CrosshairBobbing, with Crosshair-Selection compatibility.
