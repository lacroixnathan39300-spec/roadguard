import { env } from 'cloudflare:workers';
import { getChatGPTUser } from '../../../chatgpt-auth';
import { ApiError, handleApi } from '../../../../lib/service';
import { phoneApi, nativeAdminApi } from '../../../../lib/phone';
import { nativeAuthApi } from '../../../../lib/native-auth';
export const dynamic='force-dynamic';
async function handler(request:Request){
 try{
  if(!env.DB)throw new ApiError(503,'La base communautaire est indisponible. Réessayez plus tard.');
  const browserUser=await getChatGPTUser();
  const path=new URL(request.url).pathname.replace(/^\/api\/v1\/?/,'').replace(/\/$/,'');
  const native=await nativeAuthApi(env.DB,request,path,{PHONE_HASH_SECRET:env.PHONE_HASH_SECRET,ROADGUARD_ADMIN_BOOTSTRAP_PHONE:env.ROADGUARD_ADMIN_BOOTSTRAP_PHONE,ROADGUARD_ADMIN_BOOTSTRAP_SALT:env.ROADGUARD_ADMIN_BOOTSTRAP_SALT,ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER:env.ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER},Date.now());
  if(native)return native;
  const admin=await nativeAdminApi(env.DB,request,path,{PHONE_HASH_SECRET:env.PHONE_HASH_SECRET},Date.now());
  if(admin)return admin;
  const phone=await phoneApi(env.DB,request,browserUser,path,env as unknown as Parameters<typeof phoneApi>[4],Date.now());
  if(phone)return phone;
  return await handleApi(env.DB,request,browserUser,{tileUrl:env.PUBLIC_MAP_TILE_URL,tileAttribution:env.PUBLIC_MAP_ATTRIBUTION,deviceHashSecret:env.PHONE_HASH_SECRET});
 }catch(error){
  if(error instanceof ApiError)return Response.json({error:error.message},{status:error.status,headers:{'Cache-Control':'no-store'}});
  console.error('ROADGUARD API failure',error instanceof Error?error.message:'unknown');
  return Response.json({error:'La synchronisation a échoué. Vos données locales restent disponibles.'},{status:503,headers:{'Cache-Control':'no-store','Retry-After':'30'}});
 }
}
export {handler as GET,handler as POST,handler as PUT,handler as DELETE,handler as OPTIONS};
