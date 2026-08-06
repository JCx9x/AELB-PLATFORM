import type { Metadata } from 'next'
import Navbar from '../components/Navbar'

export const metadata: Metadata = { title: 'Política de privacidad | AELB' }

export default function PrivacyPage() {
  return (
    <div style={{ backgroundColor: 'var(--color-surface)', minHeight: '100vh' }}>
      <Navbar />
      <main className="max-w-4xl mx-auto px-4 lg:px-8 py-8 lg:py-12">
        <h1 className="text-3xl lg:text-4xl font-extrabold" style={{ color: 'var(--color-on-surface)' }}>Política de privacidad</h1>
        <div className="mt-8 space-y-8 text-sm leading-7" style={{ color: 'var(--color-on-surface-variant)' }}>
          <PolicySection title="1. ¿QUIÉN ES EL RESPONSABLE DEL TRATAMIENTO?">
            <p>De conformidad con el Reglamento (UE) 2016/679 del Parlamento Europeo y del Consejo de 27 de abril de 2016 relativo a la protección de las personas físicas en lo que respecta al tratamiento de datos personales, y con la Ley Orgánica 3/2018, de 5 de diciembre, de Protección de Datos Personales y garantía de los derechos digitales, los datos facilitados serán tratados por la Asociación Española de Lucha de Brazos (en adelante, “AELB”).</p>
            <ul><li>CIF: G-59050500</li><li>Domicilio social: Calle Larga, 32, 02230, Madrigueras (Albacete)</li><li>Correo electrónico: <a className="underline" href="mailto:contacto@aelb.es">contacto@aelb.es</a></li><li>Teléfono: <a className="underline" href="tel:651901959">651901959</a></li></ul>
          </PolicySection>

          <PolicySection title="2. ¿CON QUÉ FINALIDAD SE TRATARÁN SUS DATOS PERSONALES Y DURANTE CUÁNTO TIEMPO?">
            <p>Cuando los usuarios faciliten datos personales a través de los formularios de la página web de la AELB, sus datos personales serán tratados para las siguientes finalidades:</p>
            <ol className="list-upper-roman pl-6 space-y-3"><li>Gestionar el alta de socio de los usuarios que proporcionen datos mediante el formulario “Hazte socio”. Los datos se tratarán hasta que se comunique la voluntad de darse de baja como socio de AELB.</li><li>Atender las solicitudes enviadas mediante el formulario “Contacto”, incluyendo información sobre campeonatos, creación de asociaciones o cualquier otra información solicitada. Los datos se conservarán hasta que el usuario comunique expresamente su deseo de darse de baja.</li><li>Gestionar y tramitar la participación del atleta en los formularios de inscripción a campeonatos organizados por AELB, así como darle de alta como socio o socia, pues para participar en campeonatos, actividades o eventos organizados por AELB es obligatorio ser socio o socia. Los datos se tratarán hasta que se comunique la voluntad de darse de baja como socio.</li><li>Gestionar la suscripción al Blog por correo electrónico para informar de las publicaciones de la web. Los datos se tratarán hasta que se comunique la voluntad de darse de baja como suscriptor.</li><li><strong>Comentarios.</strong> Cuando los visitantes dejan comentarios en la web, recopilamos los datos mostrados en el formulario, la dirección IP y la cadena de agentes de usuario del navegador para ayudar a la detección de spam. Una cadena anónima creada a partir de la dirección de correo electrónico (hash) puede ser proporcionada a Gravatar para comprobar si se utiliza este servicio. La política de privacidad de Gravatar está disponible en <a className="underline" href="https://automattic.com/privacy/" target="_blank" rel="noreferrer">automattic.com/privacy</a>. Tras aprobar un comentario, la imagen del perfil será visible públicamente en el contexto del comentario.</li></ol>
          </PolicySection>

          <PolicySection title="3. ¿CUÁL ES LA LEGITIMACIÓN DEL TRATAMIENTO DE LOS DATOS?">
            <p>Con carácter general, los datos serán tratados sobre la base del consentimiento expreso, voluntario e inequívoco del usuario, que puede retirar en cualquier momento. La retirada no afectará a la licitud de los tratamientos efectuados con anterioridad.</p>
            <p>Cuando exista una relación jurídica y no se haya manifestado lo contrario, los datos podrán tratarse para enviar información sobre la afiliación a AELB respecto de cuotas y plazos, campeonatos y eventos en los que participe u organice AELB, o cualquier información relacionada con la lucha de brazos. La base legitimadora será el interés legítimo de AELB. El usuario puede oponerse a estas comunicaciones en cualquier momento escribiendo a <a className="underline" href="mailto:contacto@aelb.es">contacto@aelb.es</a>.</p>
          </PolicySection>

          <PolicySection title="4. ¿CUÁLES SON LAS CATEGORÍAS DE DATOS PERSONALES QUE SE TRATARÁN?"><p>En virtud de su consentimiento, AELB podrá tratar datos identificativos (nombre, apellidos, DNI, teléfono, dirección postal y correo electrónico); datos de características personales y sociales (fecha de nacimiento, sexo y nacionalidad); datos bancarios (cuenta bancaria); datos resultantes de informes personalizados sobre su actividad con AELB; y cualesquiera otros datos que los usuarios faciliten mediante los formularios de la web y las inscripciones a campeonatos.</p></PolicySection>

          <PolicySection title="5. DATOS PERSONALES DE MENORES DE EDAD"><p>Para el tratamiento de datos personales de menores de 14 años será necesario el consentimiento de padres, tutores o quienes ostenten la patria potestad. Cuando un menor participe en un campeonato, actividad o evento organizado por AELB, sus colaboradores o socios, será necesario aportar los datos que identifiquen a estas personas con el único fin de recabar el consentimiento necesario. AELB podrá requerir el libro de familia u otro documento que determine la filiación, condición de tutor o patria potestad.</p></PolicySection>

          <PolicySection title="6. DATOS PERSONALES DE TERCEROS"><p>Quienes faciliten datos personales de familiares, amigos, parejas u otros terceros relacionados con el usuario se comprometen a informarles de esta política de privacidad y de los términos aquí expuestos, así como a recabar su consentimiento para el tratamiento de sus datos.</p></PolicySection>

          <PolicySection title="7. SEGURIDAD DE LOS DATOS"><p>AELB tiene implantadas las medidas técnicas y organizativas necesarias para garantizar la seguridad de los datos y evitar su alteración, pérdida, tratamiento o acceso no autorizado, atendiendo al estado de la tecnología, la naturaleza de los datos almacenados y los riesgos a los que están expuestos.</p></PolicySection>

          <PolicySection title="8. ¿QUIÉNES SON LOS DESTINATARIOS DE LOS DATOS DEL INTERESADO?"><p>Los datos personales serán recogidos por AELB y no se compartirán salvo en los siguientes casos:</p><ul><li>Se facilitará a la compañía aseguradora una lista de personas inscritas en el campeonato con nombre, apellidos y DNI, para proteger a los atletas durante el campeonato o evento.</li><li>Se podrá facilitar una lista de socios a interesados u organizadores de campeonatos, actividades o eventos en los que participe AELB, exclusivamente para gestionar la participación.</li><li>A las Administraciones Públicas cuando legalmente corresponda.</li><li>Se podrán ceder datos de contacto para que personas interesadas en practicar el deporte contacten con personas cercanas a su lugar de residencia.</li><li>Al participar en eventos deportivos organizados por AELB, el nombre, apellidos, fotografías y vídeos podrían difundirse en la web de la asociación, redes sociales, entidades colaboradoras y medios de comunicación.</li></ul></PolicySection>

          <PolicySection title="9. ¿QUÉ DERECHOS TIENE EL INTERESADO?"><p>El interesado tiene derecho a obtener confirmación de si AELB trata sus datos; acceder a ellos; rectificar datos inexactos o incompletos; solicitar su cancelación cuando ya no sean necesarios; oponerse o limitar el tratamiento en los supuestos legales; solicitar la portabilidad; y revocar el consentimiento prestado.</p><p>Para ejercer estos derechos puede contactar con el delegado de protección de datos de AELB en Calle Larga, nº 32, CP 02230, Madrigueras (Albacete), o mediante <a className="underline" href="mailto:contacto@aelb.es">contacto@aelb.es</a>. Si considera que no se ha dado efectividad al ejercicio de sus derechos, puede presentar una reclamación ante la Agencia Española de Protección de Datos.</p></PolicySection>
        </div>
      </main>
    </div>
  )
}

function PolicySection({ title, children }: { title: string; children: React.ReactNode }) {
  return <section><h2 className="text-lg font-bold mb-3" style={{ color: 'var(--color-on-surface)' }}>{title}</h2><div className="space-y-3">{children}</div></section>
}
