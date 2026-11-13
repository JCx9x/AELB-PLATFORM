import type { Metadata } from "next";
import Link from "next/link";
import { Lexend } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages, getTranslations } from "next-intl/server";
import "./globals.css";
import AelbLogo from "./components/AelbLogo";

const lexend = Lexend({
  subsets: ["latin"],
  variable: "--font-lexend",
  weight: ["400", "500", "600", "700", "800"],
});

const INSTAGRAM_HANDLE = "aelb_luchadebrazos";
const INSTAGRAM_URL = `https://www.instagram.com/${INSTAGRAM_HANDLE}/`;

function InstagramIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Layout");
  return {
    title: t("title"),
    description: t("description"),
  };
}

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const locale = await getLocale();
  const messages = await getMessages();
  const t = await getTranslations("Layout");

  return (
    <html lang={locale} className={`${lexend.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">
        <NextIntlClientProvider messages={messages}>
          {children}
          <footer
            className="mt-auto border-t"
            style={{ backgroundColor: 'var(--color-dark)', borderColor: 'rgba(255,255,255,0.08)' }}
          >
            <div className="max-w-7xl mx-auto flex flex-col items-center gap-4 px-4 py-6 text-[11px] md:flex-row md:justify-between" style={{ color: 'rgba(255,255,255,0.45)' }}>
              <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2">
                <AelbLogo height={28} className="h-7 w-auto" />
                <span>{t("footer", { year: new Date().getFullYear() })}</span>
                <Link href="/privacidad" className="underline hover:text-white">{t("privacy")}</Link>
                <Link href="/cookies" className="underline hover:text-white">{t("cookies")}</Link>
              </div>
              <div className="flex items-center gap-3">
                <span className="font-bold uppercase tracking-widest">{t("contacto")}</span>
                <a
                  href={INSTAGRAM_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={t("instagram")}
                  className="flex items-center gap-2 text-white/70 transition-colors hover:text-white"
                >
                  <InstagramIcon className="h-5 w-5" />
                  <span className="text-xs">@{INSTAGRAM_HANDLE}</span>
                </a>
              </div>
            </div>
          </footer>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
