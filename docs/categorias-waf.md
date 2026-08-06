# Categorías WAF de competición

Las categorías de edad se modelan en el enum de dominio `CompetitionAgeCategory` y se guardan en `categories.age_category`. Una categoría de competición sigue siendo la combinación reutilizable de sexo, brazo, peso y categoría de edad.

| Identificador | Nombre | Denominación | Edades | Turno |
|---|---|---|---|---|
| `SUB_JUNIOR` | Sub Junior | U15 | 14–15 | `MORNING` |
| `JUNIOR` | Junior | U18 | 16–18 | `MORNING` |
| `YOUTH` | Youth | U23 | 19–23 | `MORNING` |
| `AMATEUR` | Amateur | — | sin límite | `MORNING` |
| `SENIOR` | Senior | — | sin límite | `AFTERNOON` |
| `MASTER` | Master | — | desde 40 | `MORNING` |
| `GRAND_MASTER` | Grand Master | — | desde 50 | `MORNING` |
| `SENIOR_GRAND_MASTER` | Senior Grand Master | — | desde 60 | `MORNING` |
| `SUPER_SENIOR_GRAND_MASTER` | Super Senior Grand Master | — | desde 70 | `MORNING` |

El turno se toma exclusivamente del enum. Al crear o editar una categoría, si se informa `ageCategory`, el backend ignora cualquier turno recibido y asigna el de dicha categoría. El título también se genera en el backend a partir de categoría de edad, sexo, brazo y límite de peso; no se puede editar manualmente. La API conserva `ageGroup` como identificador heredado para la configuración de precios, sincronizado con el identificador WAF.

La elegibilidad se comprueba al inscribirse cuando la categoría tiene `ageCategory`: `año del evento - año de nacimiento`. No depende de que el deportista haya cumplido años en la fecha concreta del campeonato.

## Migración

En instalaciones existentes, ejecutar también `database/migrations/20260731_amateur_competition_category.sql` antes de desplegar el backend. Amplía los enums de MySQL sin modificar datos. La migración WAF añade `age_category`, convierte los turnos heredados `TURNO_1` y `TURNO_2` a `MORNING` y `AFTERNOON`, y asigna los turnos de las categorías ya clasificadas. Las actualizaciones de datos son repetibles y no insertan filas, por lo que no pueden generar duplicados.
