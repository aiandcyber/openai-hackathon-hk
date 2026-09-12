import type { Metadata } from "next";
import { Fraunces, Geist_Mono, Source_Sans_3 } from "next/font/google";
import { Auth0Provider } from "@auth0/nextjs-auth0/client";
import "./globals.css";

const sourceSans = Source_Sans_3({
  variable: "--font-source-sans",
  subsets: ["latin"],
});

const fraunces = Fraunces({
  variable: "--font-fraunces",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "AI For Seniors — Caregiver Console",
  description: "Protect seniors from scams and keep family contacts ready to call",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${sourceSans.variable} ${fraunces.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col font-sans">
        <Auth0Provider>{children}</Auth0Provider>
      </body>
    </html>
  );
}
