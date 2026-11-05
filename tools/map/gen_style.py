import json, sys
def W(base, z):  # largeur de route selon le zoom (exponentiel)
    return ["interpolate", ["exponential", 1.5], ["zoom"], 5, base*0.15, 10, base*0.6, 14, base*2.2, 18, base*9]
def cls(*c): return ["match", ["get", "class"], list(c), True, False]
P = {
 "day": dict(bg="#F4F1EA", wood="#CFE6C1", grass="#DDEECF", resid="#EEEAE2", indus="#E9E5EE", park="#C6E5B5", water="#9CD2F0",
   bld="#E2DCD2", bld3="#E8E2D8", bound="#B39BCB", label="#3A332C", halo="#FFFFFF", waterLabel="#2F78AE", roadLabel="#5A5047",
   roads={"motorway":("#FFA24A","#DC7414"),"trunk":("#FFC94D","#D29A1C"),"primary":("#FFE07A","#CFAE3C"),
          "secondary":("#FFFFFF","#C8C0B4"),"tertiary":("#FFFFFF","#CEC6BB"),"minor":("#FFFFFF","#D7D0C5"),"service":("#FBFAF7","#DCD6CC")},
   path="#BCA98F", rail="#B7B0A8"),
 "night": dict(bg="#0D1520", wood="#112319", grass="#132419", resid="#121C28", indus="#151B27", park="#10261C", water="#0A2740",
   bld="#18222E", bld3="#1C2734", bound="#6E5A8A", label="#D3DCE6", halo="#0D1520", waterLabel="#5E9BD0", roadLabel="#A9B6C4",
   roads={"motorway":("#FF8A3D","#5A2A0C"),"trunk":("#E8A33A","#4A3210"),"primary":("#C9A44E","#2E2614"),
          "secondary":("#55667C","#0D1520"),"tertiary":("#4A5A6E","#0D1520"),"minor":("#38475A","#0D1520"),"service":("#2E3B4C","#0D1520")},
   path="#4A4438", rail="#3C4654"),
}
WID = {"motorway":3.2,"trunk":2.8,"primary":2.5,"secondary":2.0,"tertiary":1.7,"minor":1.25,"service":0.8}
MINZ = {"motorway":5,"trunk":6,"primary":7,"secondary":9,"tertiary":10,"minor":12,"service":14}
def style(mode):
    p = P[mode]; L = []
    L.append({"id":"bg","type":"background","paint":{"background-color":p["bg"]}})
    L.append({"id":"landuse-res","type":"fill","source":"o","source-layer":"landuse","filter":cls("residential","suburb","neighbourhood"),"paint":{"fill-color":p["resid"]}})
    L.append({"id":"landuse-ind","type":"fill","source":"o","source-layer":"landuse","filter":cls("industrial","commercial","retail"),"paint":{"fill-color":p["indus"]}})
    L.append({"id":"wood","type":"fill","source":"o","source-layer":"landcover","filter":cls("wood","forest"),"paint":{"fill-color":p["wood"]}})
    L.append({"id":"grass","type":"fill","source":"o","source-layer":"landcover","filter":cls("grass","farmland","scrub"),"paint":{"fill-color":p["grass"],"fill-opacity":0.7}})
    L.append({"id":"park","type":"fill","source":"o","source-layer":"park","paint":{"fill-color":p["park"]}})
    L.append({"id":"water","type":"fill","source":"o","source-layer":"water","paint":{"fill-color":p["water"]}})
    L.append({"id":"waterway","type":"line","source":"o","source-layer":"waterway","paint":{"line-color":p["water"],"line-width":["interpolate",["linear"],["zoom"],8,0.5,14,2,18,5]}})
    L.append({"id":"building","type":"fill","source":"o","source-layer":"building","minzoom":14,"maxzoom":15.5,"paint":{"fill-color":p["bld"]}})
    L.append({"id":"building-3d","type":"fill-extrusion","source":"o","source-layer":"building","minzoom":15.5,
              "paint":{"fill-extrusion-color":p["bld3"],"fill-extrusion-height":["coalesce",["get","render_height"],6],
                       "fill-extrusion-base":["coalesce",["get","render_min_height"],0],"fill-extrusion-opacity":0.75}})
    L.append({"id":"boundary","type":"line","source":"o","source-layer":"boundary","filter":["<=",["get","admin_level"],4],
              "paint":{"line-color":p["bound"],"line-width":["interpolate",["linear"],["zoom"],3,0.6,10,1.8],"line-dasharray":[3,2]}})
    L.append({"id":"rail","type":"line","source":"o","source-layer":"transportation","minzoom":11,"filter":cls("rail","transit"),
              "paint":{"line-color":p["rail"],"line-width":1.2,"line-dasharray":[4,3]}})
    L.append({"id":"path","type":"line","source":"o","source-layer":"transportation","minzoom":14,"filter":cls("path","track"),
              "paint":{"line-color":p["path"],"line-width":1,"line-dasharray":[2,2]}})
    order = ["service","minor","tertiary","secondary","primary","trunk","motorway"]
    for kind in ("casing","fill"):
        for c in order:
            fill, casing = p["roads"][c]
            base = WID[c]
            f = ["all", cls(c), ["!=", ["get","brunnel"], "tunnel"]]
            if kind == "casing":
                L.append({"id":f"road-{c}-casing","type":"line","source":"o","source-layer":"transportation","minzoom":MINZ[c],"filter":f,
                    "layout":{"line-cap":"round","line-join":"round"},
                    "paint":{"line-color":casing,"line-width":["interpolate",["exponential",1.5],["zoom"],5,base*0.3,10,base*1.0,14,base*3.2,18,base*12]}})
            else:
                L.append({"id":f"road-{c}","type":"line","source":"o","source-layer":"transportation","minzoom":MINZ[c],"filter":f,
                    "layout":{"line-cap":"round","line-join":"round"},"paint":{"line-color":fill,"line-width":W(base,0)}})
    for c in ("minor","secondary","primary","trunk","motorway"):
        fill, casing = p["roads"][c]
        L.append({"id":f"tunnel-{c}","type":"line","source":"o","source-layer":"transportation","minzoom":MINZ[c],
                  "filter":["all",cls(c),["==",["get","brunnel"],"tunnel"]],
                  "paint":{"line-color":fill,"line-opacity":0.45,"line-width":W(WID[c],0),"line-dasharray":[1.5,1]}})
    L.append({"id":"water-name","type":"symbol","source":"o","source-layer":"water_name","minzoom":10,
              "layout":{"text-field":["get","name"],"text-font":["Noto Sans Italic"],"text-size":13},
              "paint":{"text-color":p["waterLabel"],"text-halo-color":p["halo"],"text-halo-width":1.5}})
    L.append({"id":"road-name","type":"symbol","source":"o","source-layer":"transportation_name","minzoom":13,
              "filter":cls("motorway","trunk","primary","secondary","tertiary","minor"),
              "layout":{"symbol-placement":"line","text-field":["get","name"],"text-font":["Noto Sans Regular"],
                        "text-size":["interpolate",["linear"],["zoom"],13,11,17,15],"text-max-angle":30},
              "paint":{"text-color":p["roadLabel"],"text-halo-color":p["halo"],"text-halo-width":2}})
    L.append({"id":"road-ref","type":"symbol","source":"o","source-layer":"transportation_name","minzoom":8,
              "filter":["all",cls("motorway","trunk","primary"),["has","ref"]],
              "layout":{"symbol-placement":"line","symbol-spacing":400,"text-field":["get","ref"],"text-font":["Noto Sans Bold"],
                        "text-size":11,"text-rotation-alignment":"viewport"},
              "paint":{"text-color":"#FFFFFF","text-halo-color":p["roads"]["motorway"][1],"text-halo-width":3}})
    for cl, size, mz, font in (("city",["interpolate",["linear"],["zoom"],5,13,12,22],4,"Noto Sans Bold"),
                               ("town",["interpolate",["linear"],["zoom"],8,12,14,18],7,"Noto Sans Bold"),
                               ("village",["interpolate",["linear"],["zoom"],10,11,15,15],10,"Noto Sans Regular"),
                               ("suburb",13,12,"Noto Sans Regular")):
        L.append({"id":f"place-{cl}","type":"symbol","source":"o","source-layer":"place","minzoom":mz,
                  "filter":["==",["get","class"],cl] if cl!="suburb" else cls("suburb","neighbourhood","hamlet"),
                  "layout":{"text-field":["coalesce",["get","name:fr"],["get","name"]],"text-font":[font],"text-size":size,"text-max-width":8},
                  "paint":{"text-color":p["label"],"text-halo-color":p["halo"],"text-halo-width":2}})
    return {"version":8,"name":"ROADGUARD "+mode,
            "sources":{"o":{"type":"vector","url":"https://tiles.openfreemap.org/planet","attribution":"© OpenFreeMap © OpenMapTiles © OpenStreetMap"}},
            "glyphs":"https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf","layers":L}
day = json.dumps(style("day"), separators=(",",":"), ensure_ascii=False)
night = json.dumps(style("night"), separators=(",",":"), ensure_ascii=False)
assert "$" not in day+night and '"""' not in day+night
out = '''package fr.roadguard.core.map

/**
 * Style de carte ROADGUARD (jour façon Waze, nuit façon tableau de bord) sur les tuiles OpenFreeMap
 * (schéma OpenMapTiles). Généré par tools/map/gen_style.py : modifier le script plutôt que ce fichier.
 */
object RoadguardStyle {
    fun json(night: Boolean): String = if (night) NIGHT else DAY

    private const val DAY = """''' + day + '''"""

    private const val NIGHT = """''' + night + '''"""
}
'''
open(sys.argv[1], "w").write(out)
print(len(day), len(night))
