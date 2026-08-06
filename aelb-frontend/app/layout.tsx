import type { Metadata } from "next";
import Link from "next/link";
import { Lexend } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages, getTranslations } from "next-intl/server";
import "./globals.css";

const lexend = Lexend({
  subsets: ["latin"],
  variable: "--font-lexend",
  weight: ["400", "500", "600", "700", "800"],
});

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
            <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2 px-4 py-5 text-[11px]" style={{ color: 'rgba(255,255,255,0.45)' }}>
              <span>{t("footer", { year: new Date().getFullYear() })}</span>
              <Link href="/privacidad" className="underline hover:text-white">{t("privacy")}</Link>
              <Link href="/cookies" className="underline hover:text-white">{t("cookies")}</Link>
            </div>
          </footer>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
