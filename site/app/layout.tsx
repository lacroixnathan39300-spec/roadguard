import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "ROADGUARD — La route, ensemble",
  description: "Carte communautaire, radars et signalements partagés.",
  manifest: "/manifest.webmanifest",
  referrer: "no-referrer",
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <head><link rel="stylesheet" href="/vendor/leaflet/leaflet.css" /></head>
      <body className="antialiased">{children}</body>
    </html>
  );
}
