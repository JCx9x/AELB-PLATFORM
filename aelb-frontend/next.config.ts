import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const nextConfig: NextConfig = {
  // El detalle de campeonato es una ruta dinámica (/campeonatos/[id]).
  // Standalone permite resolverla en tiempo de ejecución dentro del contenedor.
  output: 'standalone',
};

// i18n sin prefijo de idioma en la URL: el locale se resuelve en app/i18n/request.ts
// a partir de una cookie, así que no hace falta tocar el enrutado ni un proxy/middleware.
const withNextIntl = createNextIntlPlugin('./app/i18n/request.ts');

export default withNextIntl(nextConfig);
