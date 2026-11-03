/* Speed bands have hysteresis and a dwell period. No zoom changes from an absent speed. */
(function(root){
 class RoadguardAutoZoom {
  constructor(){this.level=null;this.candidate=null;this.since=0;this.changed=0;}
  update(speed,current,now){
   if(!Number.isFinite(speed)||speed<0)return null;
   if(this.level===null)this.level=Math.max(14,Math.min(17,Math.round(current)));
   let next=this.level;
   const up={17:35,16:70,15:105},down={14:92,15:57,16:23};
   if(next>14&&speed>=up[next])next--;
   else if(next<17&&speed<=down[next])next++;
   if(next===this.level){this.candidate=null;return null;}
   if(next!==this.candidate){this.candidate=next;this.since=now;return null;}
   if(now-this.since<3500||now-this.changed<12000)return null;
   this.level=next;this.candidate=null;this.changed=now;
   return Math.abs(current-next)>.2?next:null;
  }
 }
 root.RoadguardAutoZoom=RoadguardAutoZoom;
})(typeof module==='object'?module.exports:window);
