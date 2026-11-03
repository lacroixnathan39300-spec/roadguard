(function(root){
 'use strict';
 const labels={graphite:'Graphite ROADGUARD',readable:'Routes plus lisibles',original:'Couleurs OpenStreetMap'};
 function normalize(value){return Object.prototype.hasOwnProperty.call(labels,value)?value:'graphite';}
 root.setRoadguardMapPack=function(value){const selected=normalize(value);document.body.setAttribute('data-rg-map-pack',selected);return selected;};
 root.getRoadguardMapPackStatus=function(){const tile=document.querySelector('#map .leaflet-tile-pane img.leaflet-tile');return {pack:document.body.getAttribute('data-rg-map-pack'),tiles:document.querySelectorAll('#map .leaflet-tile-pane img.leaflet-tile-loaded').length,filter:tile?getComputedStyle(tile).filter:null};};
 if(typeof module!=='undefined')module.exports={normalize,labels};
})(typeof globalThis!=='undefined'?globalThis:this);
