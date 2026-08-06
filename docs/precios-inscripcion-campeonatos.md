# Precios de inscripción por categoría

El precio ya no pertenece al campeonato como importe único. Cada campeonato puede configurar, mediante `PUT /api/championships/{id}/category-prices`, dos valores para cada categoría WAF:

- `pricePerArm`: precio normal por brazo.
- `combinationPrice`: suplemento fijo de esa categoría al combinarla con otra cuando una de las categorías tiene ambos brazos inscritos.

Los valores iniciales si todavía no se han personalizado son 20 € por brazo y 10 € de suplemento. Senior queda inicialmente en 20 € por brazo.

La política de cálculo agrupa las inscripciones por categoría WAF:

- Una categoría sola: precio por brazo.
- Dos categorías con un brazo cada una: ambas a su precio normal. Con 20 € y 20 €, total 40 €.
- Si alguna categoría tiene dos brazos y hay otra categoría: la categoría con dos brazos se cobra normalmente; la otra se sustituye por su suplemento fijo. Con Senior dos brazos (40 €) y Youth, total 50 €.

Si hay varias categorías con dos brazos, Senior se considera la categoría principal; en ausencia de Senior se usa la primera categoría WAF por identificador. El importe de la nueva inscripción se guarda en `registrations.amount` como incremento del total calculado en ese momento.
