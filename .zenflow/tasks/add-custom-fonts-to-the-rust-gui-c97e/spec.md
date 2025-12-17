# Technical Specification: Custom Fonts for the Rust GUI

## Technical Context
- Language: Rust 2021 edition with `gpui = 0.2.2`, `gpui-component = 0.5.0`, `gpui-component-assets = 0.5.0`, `serde`, `tokio`, and GitHub-provided assets for icons/graphics.
- `gpui` exposes `TextStyle`, `Font`, and (per the `text` module source in `docs.rs/crate/gpui/0.2.2/source/gpui/text.rs`) helpers to load TTF data and configure family/weight/style before rendering. GPUI + winit run the window/event loop on the main thread, so font registration and theme application must happen inside `Application::run` (see https://users.rust-lang.org/t/run-on-main-thread-or-equivalent/96066 and the docs for `Context`).
- Theme data is driven through `gpui_component::Theme` (fields such as `font_family`, `mono_font_family`, `font_size`, `mono_font_size`) and the Longbridge theme schema which already exposes `font.family`, `mono_font.family`, `font.size`, etc. We will map our local theme definitions to those fields.
- Shared UI state will remain single-threaded or wrapped in `Arc`/`Mutex` when necessary so we honor Rust’s ownership/memory safety guarantees (see https://doc.rust-lang.org/book/ch04-01-what-is-ownership.html and https://doc.rust-lang.org/std/sync/struct.Arc.html).

## Implementation Approach
1. Extend `moribito-rs/crates/gui/src/theme.rs` with a `FontConfig` (base & monospace family, file path(s), weight/size defaults) and helpers that translate theme JSON data into `gpui_component::Theme` fields plus `gpui::Font`/`TextStyle` overrides. The config will merge values from our theme registry so switching theme also swaps fonts.
2. Create a dedicated `moribito-rs/assets/fonts/` directory and vendor open-source fonts (e.g., Inter + JetBrainsMono) alongside the existing assets. The new `Assets` source (already supplied via `Application::new().with_assets(Assets)`) will expose these paths to GPUI’s asset loader so we can call `Context::load_font` (or the `gpui::text` helper) using either embedded or filesystem data during startup.
3. Update `moribito-rs/crates/gui/src/main.rs` to load and register the font files on the main thread before creating windows, then apply the `ThemeRegistry` configuration (colors + typography) so gpui-component uses the new font families for label/input rendering. The UI will continue to rely on the `Theme` struct (spacing/colors) but now also reads its typography config from the JSON schema’s `font.*` fields.
4. Tie the new configuration into `moribito-rs/themes/gruvbox.json` by setting `font.family`, `mono_font.family`, and/or a `fonts` block that lists the fonts to load (matching the asset filenames). This keeps our theme data self-contained and aligned with Longbridge’s schema while exposing per-theme font choices.

## Source Code Structure Changes
- `crates/gui/src/theme.rs`: add `FontConfig`, parser utilities, and `apply_fonts(cx: &mut App)` helper to register fonts with GPUI/gpui-component.
- `crates/gui/src/main.rs`: call the new loader before window creation and ensure `ThemeRegistry::watch_dir` also updates font settings when a theme file change is detected.
- `themes/gruvbox.json`: extend with the theme font metadata required by the new parser.
- `assets/fonts/`: place the bundled TTF files so they can be referenced by name.

## Data Model / API Changes
- Theme JSON now includes `font.family`, `font.size`, `mono_font.family`, `mono_font.size`, and optionally a `fonts` array that pairs a logical name with an asset path so we can keep fonts grouped with their theme.
- The GUI theme struct now exposes those typography fields to consumers (labels, inputs, scrollbars) so future UI widgets can reference the active font family/size consistently.
- We keep the existing color/spacing/border APIs intact while hoisting typography onto the same shared `Theme` instance for easier consumption.

## Verification Approach
- Run `cargo fmt` in the workspace (Rust fmt) to keep formatting clean.
- Run `cargo test -p moribito-gui` (or `cargo test` in the GUI crate) to cover the new helper logic; existing tests target theme defaults.
- Manually launch the Moribito GUI to confirm the custom fonts (Inter + JetBrainsMono) load and render in tables/inputs during development; we’ll inspect `gpui_component` theme behavior after the changes.