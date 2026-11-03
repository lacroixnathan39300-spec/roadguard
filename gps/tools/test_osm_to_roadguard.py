#!/usr/bin/env python3
"""Tests locaux sans dependance externe: python3 tools/test_osm_to_roadguard.py"""
import tempfile,pathlib,json,sys
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from osm_to_roadguard import convert
XML='<osm version="0.6"><node id="1" lat="47.0" lon="6.0"/><node id="2" lat="47.001" lon="6.001"/><node id="3" lat="47.002" lon="6.002"/><node id="4" lat="48.0" lon="7.0"/><way id="10"><nd ref="1"/><nd ref="2"/><nd ref="999"/><nd ref="3"/><nd ref="4"/><tag k="highway" v="residential"/></way><way id="11"><nd ref="1"/><nd ref="2"/><tag k="highway" v="footway"/></way></osm>'
with tempfile.TemporaryDirectory() as d:
    root=pathlib.Path(d);source=root/'test.osm';out=root/'roads.json';source.write_text(XML)
    convert(source,out,3900,(5.9,46.9,6.1,47.1))
    data=json.loads(out.read_text()); assert len(data['features'])==1
    assert len(data['features'][0]['geometry']['coordinates'])==2
    try:convert(source,out,3900,(6.1,47.1,5.9,46.9))
    except ValueError:pass
    else:raise AssertionError('bbox invalide accepte')
    print('OK: filtre zone, segments interrompus, types de voies, bbox invalide')
