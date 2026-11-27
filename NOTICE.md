# Third-party notices

This project is a 1.7.10 / GT New Horizons 2.9 port that merges the behaviour of three upstream
projects. It is distributed under the GNU General Public License v3.0 (see LICENSE), because
CrosshairBobbing is GPL-3.0 and the combined work must be GPL-3.0.

## DynamicCrosshair

- Upstream: https://github.com/Crendgrim/DynamicCrosshair
- Author: Crendgrim
- License: GNU Lesser General Public License v3.0 (upstream)
- Used for: context classification model, crosshair style set, configuration layout, public API
  concepts, and the 15x15 crosshair sprites in
  `assets/dynamiccrosshair/textures/gui/crosshair/`.

## DynamicCrosshairCompat

- Upstream: https://github.com/Crendgrim/DynamicCrosshairCompat
- Author: Crendgrim
- License: GNU Lesser General Public License v3.0
- Used for: the idea of a central interaction provider API and per-block/item/entity
  classification. The original per-mod mixins were not copied; the 1.7.10 port replaces them with
  generic heuristics plus explicit id lists and a runtime API.

## CrosshairBobbing

- Upstream: https://github.com/Krash220/CrosshairBobbingMod
- Author: Krash220
- License: GNU General Public License v3.0
- Used for: crosshair bobbing behaviour (view bobbing offset, sneak rotation, item switch shift,
  attack kick/scale, eating wobble, bow draw scaling).

## Crosshair-Selection

- Upstream: https://github.com/kanami2024520-jpg/Crosshair-Selection
- Author: kuionc
- License: MIT
- This port does not copy Crosshair-Selection code. The compatibility layer reads its public static
  configuration reflectively, and the colour/flow sampling used when tinting the dynamic shapes is a
  reimplementation of the phase and spatial-band formulas from its MIT-licensed crosshair shader
  (`shaders/crosshair.frag`), so the colours match its flowing rainbow.

The test-only stand-in mod under `probe/xs-stub` (outside this directory) is original code written
for testing and is not part of the shipped jar.
