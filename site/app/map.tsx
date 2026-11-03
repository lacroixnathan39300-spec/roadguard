'use client';
/* eslint-disable @typescript-eslint/no-explicit-any */
// Leaflet is a pinned, local vendor asset. Its imperative boundary is isolated here.
import {useEffect,useRef,useState} from 'react';
import type {Radar,Report,Member,Cluster,Prefs} from '../lib/types';
import {labels} from '../lib/types';
declare global {interface Window{L:any}}
export type Area={west:number;south:number;east:number;north:number;zoom:number;lat:number;lng:number};
type Props={radars:Radar[];reports:Report[];members:Member[];clusters:Cluster[];prefs:Prefs;filter:string;position:{lat:number;lng:number}|null;focus:{lat:number;lng:number;nonce:number}|null;onArea:(a:Area)=>void;onSelect:(r:Radar|Report)=>void};
let library:Promise<void>|undefined;
async function tileConfig(){try{const r=await fetch('/api/v1/config',{signal:AbortSignal.timeout(5000)});if(!r.ok)throw Error();return await r.json() as {tileUrl:string;tileAttribution:string;osm:boolean};}catch{return {tileUrl:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',tileAttribution:'© OpenStreetMap contributors',osm:true};}}
function load(){if(window.L)return Promise.resolve();if(!library)library=new Promise((resolve,reject)=>{const s=document.createElement('script');s.src='/vendor/leaflet/leaflet.js';s.onload=()=>resolve();s.onerror=()=>{library=undefined;reject(Error('Impossible de charger la carte.'));};document.head.appendChild(s);});return library;}
export default function MapView(p:Props){
 const el=useRef<HTMLDivElement>(null),map=useRef<any>(null),layer=useRef<any>(null),people=useRef<any>(null),cursor=useRef<any>(null),latest=useRef(p),[ready,setReady]=useState(false),[error,setError]=useState(''),[retry,setRetry]=useState(0);
 latest.current=p;
 useEffect(()=>{let stopped=false;Promise.all([load(),tileConfig()]).then(([,config])=>{
  if(stopped||!el.current||map.current)return;
  const L=window.L,m=L.map(el.current,{zoomControl:false,attributionControl:true,minZoom:8,maxZoom:19}).setView([46.746,5.908],12);
  map.current=m;L.tileLayer(config.tileUrl,{maxZoom:19,attribution:config.osm?'&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>':config.tileAttribution.replace(/[<>"&]/g,'')}).addTo(m);
  layer.current=L.layerGroup().addTo(m);people.current=L.layerGroup().addTo(m);
  const change=()=>{const b=m.getBounds(),c=m.getCenter();latest.current.onArea({west:b.getWest(),south:b.getSouth(),east:b.getEast(),north:b.getNorth(),zoom:m.getZoom(),lat:c.lat,lng:c.lng});};
  m.on('moveend',change);change();setReady(true);
  const ro=new ResizeObserver(()=>m.invalidateSize());ro.observe(el.current);(m as any)._rgResize=ro;
 }).catch(e=>setError(e.message));return()=>{stopped=true;if(map.current){map.current._rgResize?.disconnect();map.current.remove();map.current=null;}setReady(false);};},[retry]);
 useEffect(()=>{if(!ready)return;const L=window.L,m=map.current;layer.current.clearLayers();
  const bounds=m.getBounds().pad(.12),data=[...p.radars,...p.reports].filter(r=>bounds.contains([r.lat,r.lng])&&(p.filter==='all'||p.filter==='community'&&'expires'in r||r.kind===p.filter));
  if(m.getZoom()<12){const grid=.04,groups=new Map<string,{lat:number;lng:number;count:number}>();for(const r of data){const key=Math.round(r.lat/grid)+','+Math.round(r.lng/grid);const g=groups.get(key)||{lat:0,lng:0,count:0};g.lat+=r.lat;g.lng+=r.lng;g.count++;groups.set(key,g);}for(const g of groups.values())L.marker([g.lat/g.count,g.lng/g.count],{icon:L.divIcon({className:'map-cluster',html:String(g.count),iconSize:[40,40]})}).on('click',()=>m.setView([g.lat/g.count,g.lng/g.count],13)).addTo(layer.current);
  }else for(const r of data.slice(0,300)){
   const community='expires'in r,shape=r.kind==='redlight'?'●':r.kind==='fixed'||r.kind==='mobile'?'▣':'!';
   const icon=L.divIcon({className:'radar-pin '+(community?'community-pin':''),html:'<span>'+shape+'</span>',iconSize:[32,36],iconAnchor:[16,34]});
   const marker=L.marker([r.lat,r.lng],{icon,title:labels[r.kind],keyboard:true}).addTo(layer.current);marker.on('click',()=>latest.current.onSelect(r));
  }
 },[ready,p.radars,p.reports,p.filter,p.focus]);
 useEffect(()=>{if(!ready)return;people.current.clearLayers();if(!p.prefs.members)return;const L=window.L;
  for(const person of p.members.filter(x=>x.expires>Date.now())){const m=L.circleMarker([person.lat,person.lng],{radius:7,color:'#0b1118',weight:3,fillColor:'#20cba0',fillOpacity:1}).addTo(people.current);const node=document.createElement('div');node.textContent=person.alias+' · position approximative · '+new Date(person.updated).toLocaleTimeString('fr-FR',{hour:'2-digit',minute:'2-digit'});m.bindPopup(node);}
  for(const g of p.clusters)L.marker([g.lat,g.lng],{icon:L.divIcon({className:'member-cluster',html:String(g.count),iconSize:[32,32]})}).addTo(people.current);
 },[ready,p.members,p.clusters,p.prefs.members]);
 useEffect(()=>{if(!ready)return;if(cursor.current){cursor.current.remove();cursor.current=null;}if(!p.position)return;const L=window.L,shape=p.prefs.cursor==='car'?'🚘':p.prefs.cursor==='dot'?'●':'➤';cursor.current=L.marker([p.position.lat,p.position.lng],{icon:L.divIcon({className:'user-cursor '+p.prefs.cursor,html:'<span style="color:'+p.prefs.accent+'">'+shape+'</span>',iconSize:[44,44],iconAnchor:[22,22]})}).addTo(map.current);},[ready,p.position,p.prefs.cursor,p.prefs.accent]);
 useEffect(()=>{if(ready&&p.focus)map.current.setView([p.focus.lat,p.focus.lng],Math.max(14,map.current.getZoom()));},[ready,p.focus]);
 useEffect(()=>{if(!ready)return;const on=(e:Event)=>{const detail=(e as CustomEvent).detail;if(detail==='in')map.current.zoomIn();else if(detail==='out')map.current.zoomOut();};window.addEventListener('rg-zoom',on);return()=>window.removeEventListener('rg-zoom',on);},[ready]);
 return <><div ref={el} className={'leaflet-map '+(p.prefs.theme==='day'||p.prefs.theme==='auto'&&new Date().getHours()>=7&&new Date().getHours()<20?'map-day':'map-night')} aria-label="Carte interactive des radars et signalements"/>{error&&<div className="map-error"><p>{error}</p><button onClick={()=>{setError('');setRetry(x=>x+1);}}>Réessayer</button></div>}</>;
}
