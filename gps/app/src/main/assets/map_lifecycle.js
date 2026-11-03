/* Startup watchdog: only renderer startup failures request a 2D fallback. */
(function(root){
 'use strict';
 class RoadguardMapLifecycle {
  constructor(notify,set,clear){this.notify=notify;this.set=set.bind(root);this.clear=clear.bind(root);this.timer=null;this.state='idle';this.generation=0;}
  start(){this.cancel();const generation=++this.generation;this.state='loading';this.timer=this.set(()=>{if(this.generation===generation&&this.state==='loading')this.fail('Démarrage du moteur dépassant 15 secondes');},15000);}
  cancel(){if(this.timer!==null)this.clear(this.timer);this.timer=null;}
  ready(){if(this.state!=='loading')return;this.cancel();this.state='ready';this.notify('ready','');}
  fail(message){if(this.state!=='loading')return;this.cancel();this.state='failed';this.notify('failed',String(message||'Erreur de démarrage').slice(0,800));}
  close(){this.cancel();++this.generation;this.state='closed';}
 }
 root.RoadguardMapLifecycle=RoadguardMapLifecycle;
})(typeof module==='object'?module.exports:window);
