import { env } from 'cloudflare:workers';
import { notFound, redirect } from 'next/navigation';
import { getChatGPTUser } from './chatgpt-auth';
import Roadguard from './roadguard';

export const dynamic = 'force-dynamic';

/** The browser dashboard is owner-only; the Android API remains a separate route. */
export default async function Home() {
  const user = await getChatGPTUser();
  if (!user) redirect('/signin-with-chatgpt?return_to=%2F');
  const settings = env as unknown as { ROADGUARD_ADMIN_USER_ID?: string; ROADGUARD_ADMIN_EMAIL?: string };
  const isOwner = !!(settings.ROADGUARD_ADMIN_USER_ID && user.userId === settings.ROADGUARD_ADMIN_USER_ID)
    || !!(settings.ROADGUARD_ADMIN_EMAIL && user.email.toLowerCase() === settings.ROADGUARD_ADMIN_EMAIL.toLowerCase());
  if (!isOwner) notFound();
  return <Roadguard />;
}
