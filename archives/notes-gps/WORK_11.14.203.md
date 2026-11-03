# ROADGUARD 11.14.203 - Lufop EU ASC

- Source: user supplied Lufop ZIP (263 ASC files).
- Converted: 80021 unique records from 80035 parsed lines.
- New asset: app/src/main/assets/lufop_europe_asc_overlay.csv.
- New Java: LufopEuropeCatalog.java.
- UpdatedRadarCatalog.active now adds FR and CH Lufop points with coarse coordinate deduplication and historical/mobile filtering.
- All seven embedded country CSVs and other original radar assets retained byte-for-byte.
- Other countries included in supplemental asset but NOT connected to active alert engine.
- No Gradle compile or on-device tests performed.
- No new online download mechanism added.
- TODO: compile, test, connect remaining countries to alert engine, validate duplicate alert behavior and licensing.
