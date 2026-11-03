ROADGUARD offline MapLibre glyph subset

The MapLibre style requests local Noto Sans Regular glyph PBF ranges through
{fontstack}/{range}.pbf. The bundled ranges 0-255, 256-511 and 512-767 cover
ASCII and common Latin/Western-European accents needed by the French map.
Add more licensed ranges before using countries or scripts outside that set.

These small glyph subsets are copied from MapLibre demo glyphs:
https://demotiles.maplibre.org/font/Noto%20Sans%20Regular/
Noto Sans copyright and SIL Open Font License 1.1 are included in OFL-Noto-Sans.txt.
The PMTiles dataset is separate and contains map features, not glyph data.
