export type Kind='fixed'|'mobile'|'control'|'redlight'|'danger'|'accident'|'stopped'|'traffic'|'obstacle'|'works'|'other';
export type Radar={id:string;kind:Kind;lat:number;lng:number;label:string;category:string;country:string;speed:number|null;sources:string};
export type Report={id:string;kind:Kind;lat:number;lng:number;note:string;created:number;updated:number;expires:number;status:string;confirmations:number;cleared:number;flags?:number;lastConfirmed?:number;presenceCurrent?:boolean;reporterName?:string};
export type Member={id:string;alias:string;lat:number;lng:number;updated:number;expires:number};
export type Cluster={lat:number;lng:number;count:number;expires:number};
export type Prefs={accent:string;theme:string;cursor:string;voice:string;speech:boolean;members:boolean;routeColor:string;menuPhoto:boolean;displayName:string};
export const defaults:Prefs={accent:'#ff304f',theme:'night',cursor:'arrow',voice:'',speech:false,members:true,routeColor:'#329dff',menuPhoto:true,displayName:''};
export const labels:Record<Kind,string>={fixed:'Radar fixe',mobile:'Radar mobile',control:'Contrôle routier',redlight:'Feu rouge',danger:'Zone dangereuse',accident:'Accident',stopped:'Véhicule arrêté',traffic:'Bouchon',obstacle:'Obstacle',works:'Travaux',other:'Autre'};
export function distance(a:{lat:number;lng:number},b:{lat:number;lng:number}){const r=Math.PI/180,dlat=(b.lat-a.lat)*r,dlng=(b.lng-a.lng)*r;return 6371000*2*Math.asin(Math.sqrt(Math.sin(dlat/2)**2+Math.cos(a.lat*r)*Math.cos(b.lat*r)*Math.sin(dlng/2)**2));}
export const distanceText=(n:number)=>n<1000?Math.round(n)+' m':(n/1000).toLocaleString('fr-FR',{maximumFractionDigits:1})+' km';
