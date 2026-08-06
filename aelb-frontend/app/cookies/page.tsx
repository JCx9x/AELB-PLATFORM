import type { Metadata } from 'next'
import Navbar from '../components/Navbar'

export const metadata: Metadata = { title: 'Política de cookies | AELB' }

export default function CookiesPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />
      <main className="max-w-4xl mx-auto px-4 lg:px-8 py-8 lg:py-12">
        <h1 className="text-3xl lg:text-4xl font-extrabold" style={{ color: 'var(--color-on-surface)' }}>Política de cookies</h1>
        <div className="mt-8 space-y-8 text-sm leading-7" style={{ color: 'var(--color-on-surface-variant)' }}>
          <Section title="Política de cookies"><p>Una cookie es un pequeño fichero de texto que se almacena en el navegador al visitar casi cualquier página web. Su utilidad es permitir que la web recuerde la visita al volver a navegar por ella. Las cookies suelen almacenar información técnica, preferencias personales, personalización de contenidos, estadísticas de uso, enlaces a redes sociales y acceso a cuentas de usuario.</p><p>El objetivo de una cookie es adaptar el contenido de la web al perfil y necesidades del usuario. Sin cookies, los servicios ofrecidos por cualquier página web se verían mermados notablemente. Para ampliar información sobre qué son, qué almacenan o cómo eliminarlas y desactivarlas, consulte la ayuda de su navegador.</p></Section>
          <Section title="Cookies utilizadas en este sitio web"><p>Siguiendo las directrices de la Agencia Española de Protección de Datos, detallamos el uso de cookies de esta web con la máxima exactitud posible.</p><p><strong>Cookies propias:</strong> cookies de sesión para garantizar que las personas que escriban comentarios en el blog sean humanas y no aplicaciones automatizadas, combatiendo así el spam.</p><p><strong>Cookies de terceros:</strong></p><ul><li><strong>Google Analytics:</strong> almacena cookies para elaborar estadísticas sobre tráfico y volumen de visitas. Al utilizar este sitio web, el usuario consiente el tratamiento de información por Google; el ejercicio de derechos respecto de este tratamiento deberá realizarse directamente ante Google.</li><li><strong>Redes sociales:</strong> cada red social utiliza sus propias cookies para permitir acciones como “Me gusta” o “Compartir”.</li></ul></Section>
          <Section title="Desactivación o eliminación de cookies"><p>En cualquier momento puede ejercer el derecho de desactivación o eliminación de las cookies de este sitio web. Estas acciones se realizan de forma diferente según el navegador utilizado; consulte la guía de ayuda de su navegador para conocer los pasos aplicables.</p></Section>
          <Section title="Notas adicionales"><ul><li>Ni esta web ni sus representantes legales se hacen responsables del contenido o veracidad de las políticas de privacidad de terceros mencionados en esta política.</li><li>Los navegadores web son las herramientas encargadas de almacenar cookies; desde ellos debe ejercer el derecho de eliminación o desactivación. Ni esta web ni sus representantes legales pueden garantizar la correcta o incorrecta manipulación de cookies por los navegadores.</li><li>En algunos casos es necesario instalar cookies para que el navegador no olvide la decisión de no aceptación.</li><li>En el caso de Google Analytics, Google almacena cookies en servidores ubicados en Estados Unidos y se compromete a no compartirlas con terceros salvo cuando sea necesario para el funcionamiento del sistema o exista obligación legal. Según Google, no guarda la dirección IP. Puede consultar información sobre el uso de cookies de Google en su documentación oficial.</li><li>Para dudas o consultas sobre esta política de cookies, puede contactar con AELB a través de <a className="underline" href="mailto:contacto@aelb.es">contacto@aelb.es</a>.</li></ul></Section>
        </div>
      </main>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <section><h2 className="text-lg font-bold mb-3" style={{ color: 'var(--color-on-surface)' }}>{title}</h2><div className="space-y-3">{children}</div></section>
}
