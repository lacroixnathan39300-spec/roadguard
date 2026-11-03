// k6 profile for an isolated staging service. Never point this at production or OSM.
import http from 'k6/http';
import {check,sleep} from 'k6';
import {SharedArray} from 'k6/data';
import exec from 'k6/execution';
const target=__ENV.TARGET_URL||'http://127.0.0.1:8787';
if(target.includes('roadguard-community.lacroixnathan39300.chatgpt.site'))throw Error('Production load is forbidden by this profile.');
if(!/^http:\/\/127\.0\.0\.1(?::\d+)?$/.test(target)&&(__ENV.ALLOW_STAGING_LOAD!=='1'||!/^https:\/\/roadguard-staging[\w.-]*(?::\d+)?$/.test(target)))throw Error('Use a dedicated roadguard-staging origin and ALLOW_STAGING_LOAD=1.');
const actors=new SharedArray('synthetic staging actors',()=>JSON.parse(open(__ENV.ACTORS_FILE)));
const total=Number(__ENV.USERS||500000),sharing=__ENV.SHARE_POSITIONS==='1';
export const options={scenarios:{drivers:{executor:'ramping-vus',startVUs:0,stages:[{duration:'20m',target:Math.ceil(total*.01)},{duration:'20m',target:Math.ceil(total*.1)},{duration:'30m',target:Math.ceil(total*.5)},{duration:'30m',target:total},{duration:'30m',target:total},{duration:'10m',target:0}],gracefulRampDown:'2m'}},thresholds:{http_req_failed:['rate<0.01'],'http_req_duration{kind:reports}':['p(95)<500'],'http_req_duration{kind:presence}':['p(95)<800']}};
export default function(){
 const actor=actors[(exec.vu.idInTest-1)%actors.length];if(!actor||!actor.token)throw Error('Missing synthetic staging actor.');
 if(exec.vu.iterationInScenario===0)sleep((exec.vu.idInTest*0.61803398875%1)*120);
 const began=Date.now(),headers={Authorization:'Bearer '+actor.token,'Content-Type':'application/json'};
 const response=http.get(target+'/api/v1/reports?bbox='+actor.bbox,{headers,tags:{kind:'reports'}});
 check(response,{'report page succeeds':r=>r.status===200});
 if(sharing&&exec.vu.iterationInScenario%5===0){const presence=http.put(target+'/api/v1/presence',JSON.stringify({lat:actor.lat,lng:actor.lng,accuracy:20,consent:true,start:false,lease:actor.lease}),{headers,tags:{kind:'presence'}});check(presence,{'position renewal succeeds':r=>r.status===200});}
 sleep(Math.max(0,120-(Date.now()-began)/1000));
}
