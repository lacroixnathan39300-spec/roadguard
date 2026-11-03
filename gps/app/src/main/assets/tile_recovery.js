/* Bounded retries for failed visible tiles. No cache clearing or area download. */
(function(root){
 'use strict';
 function Recovery(options){
  this.options=options;this.failed=new Set();this.round=0;this.timer=null;
  this.online=true;this.visible=true;this.closed=false;
  this.delays=[2000,5000,15000,32000];
 }
 Recovery.prototype.cancel=function(){if(this.timer!==null)this.options.clear(this.timer);this.timer=null;};
 Recovery.prototype.schedule=function(){
  if(this.closed||!this.online||!this.visible||!this.failed.size||this.timer!==null||this.round>=this.delays.length)return;
  this.timer=this.options.set(()=>{this.timer=null;if(this.closed||!this.online||!this.visible)return;
   const keys=Array.from(this.failed).filter(key=>this.options.current(key));
   this.failed=new Set(keys);if(!keys.length){this.round=0;return;}
   this.round++;this.options.retry(keys);this.schedule();
  },this.delays[this.round]);
 };
 Recovery.prototype.failure=function(key){if(this.closed)return;this.failed.add(key);this.schedule();};
 Recovery.prototype.success=function(key){this.failed.delete(key);if(!this.failed.size){this.cancel();this.round=0;}};
 Recovery.prototype.remove=function(key){this.success(key);};
 Recovery.prototype.network=function(online){const changed=this.online!==!!online;this.online=!!online;if(!this.online)this.cancel();else {if(changed)this.round=0;this.schedule();}};
 Recovery.prototype.visibility=function(visible){this.visible=!!visible;if(!this.visible)this.cancel();else this.schedule();};
 Recovery.prototype.manual=function(){this.cancel();this.round=0;this.schedule();};
 Recovery.prototype.close=function(){this.closed=true;this.cancel();this.failed.clear();};
 root.RoadguardTileRecovery=Recovery;
 if(typeof module!=='undefined')module.exports=Recovery;
})(typeof globalThis!=='undefined'?globalThis:this);
