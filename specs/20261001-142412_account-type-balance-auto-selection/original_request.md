# Original Request

Sin ticket de Jira asociado. Surgió en conversación, mientras se trabajaba en VPR-9895 (cash release),
a partir de una pregunta sobre cómo el sistema real (`master-trust-servicer-api`) decide qué campo de
balance (`current_balance` vs `projected_balance`) usar para una cuenta.

## Investigación del usuario contra `master-trust-servicer-api` (rama `develop`)

> Confirmado en el código actual (develop), idéntico a lo que habíamos visto.
>
> Sí existe una clasificación por tipo de cuenta — pero es de 2 niveles, no una sola regla.
>
> **Tipos de cuenta que existen** (`AccountType` enum, `core/domain/account/AccountType.kt`):
> ```kotlin
> enum class AccountType {
>     COLLECTION, INVESTMENT, RESERVE
> }
> ```
> Solo tres valores. No existe un valor literal "DEPOSIT" — el equivalente conceptual a "cuenta de
> depósito" es `COLLECTION`.
>
> **Nivel 1: la clasificación real por tipo** (esto sí está en el código, condicionando todo):
> ```kotlin
> if (it.type == AccountType.INVESTMENT) {
>     // rama especial: calcula investmentGains, hace la excepción WELLI
>     ...
> }
> // rama default (para COLLECTION, RESERVE, y cualquier INVESTMENT no-WELLI):
> AssignmentAccountBalance(
>     usableBalance = balance?.getUsableBalance(),      // projected ?: current
>     reservedBalance = balance?.getReservedBalance(),  // current - projected
> )
> ```
>
> | Tipo de cuenta | Qué pasa |
> |---|---|
> | `COLLECTION` | Rama default: `usableBalance = projectedBalance ?: currentBalance`. Nunca calcula `investmentGains`. |
> | `RESERVE` | Misma rama default que `COLLECTION` — tratamiento idéntico, no hay un branch separado para `RESERVE`. |
> | `INVESTMENT` | Entra a la rama especial: siempre calcula `investmentGains` (vía `getInvestmentGainsBetweenDates`), y requiere que exista una distribución previa (`lastDistribution`) o explota con `IllegalStateException`. Dentro de esta rama, `usableBalance`/`reservedBalance` siguen la regla normal (`projected ?: current`) **excepto** si el código de cuenta es específicamente `WELLI_INVESTMENT`. |
>
> **Nivel 2: la excepción puntual** (no es por tipo, es por instancia de cuenta): dentro de la rama
> `INVESTMENT`, hay un sub-caso keyed por `account.code` (no por tipo) que fuerza `currentBalance` e
> ignora `projectedBalance`, exclusivo de la cuenta de WELLI.
>
> Resumen:
> ```
> AccountType.COLLECTION  → usableBalance = projected ?: current   (rama default)
> AccountType.RESERVE     → usableBalance = projected ?: current   (misma rama default, sin tratamiento propio)
> AccountType.INVESTMENT  → usableBalance = projected ?: current   + investmentGains calculado
>                            EXCEPTO cuenta "WELLI_INVESTMENT" → usableBalance = current (siempre), reservedBalance = 0
> ```
> La única clasificación de "qué campo se usa" que depende realmente del tipo es: `INVESTMENT`
> dispara el cálculo adicional de `investmentGains` (algo que `COLLECTION`/`RESERVE` nunca hacen). La
> elección entre current vs projected en sí misma no está condicionada por tipo en el caso general —
> es el mismo fallback para los tres tipos. Solo se rompe esa uniformidad para una cuenta específica
> de WELLI, identificada por código, no por tipo.

## Conclusión / pedido del usuario

> Es decir, internamente se debería saber qué tipo de campos seleccionar, esos se pueden autosetear
> automáticamente ya que tenemos la deducción lógica... Y en el vaas-backoffice -> distribution v2 ->
> en la sección de pool strategy deberías consultar la company y obtener el tipo de cuenta de la
> misma que se selecciona.. es decir, que no seleccione el campo, sino que se autosete por el tipo de
> cuenta. Eso facilita todo... obvio, se va a recibir por parámetro, pero necesito que el backend
> sepa de esto, para realizar los casos de prueba.

Y en una aclaración posterior:

> Tene en cuenta lo que mencioné sobre tener aunque sea documentado y sobre los campos que se van a
> tomar y como se van a tratar al momento de realizar la distribución, porque eso es importante...
> claramente en config también tiene que estar.

## Alcance confirmado (vía preguntas de aclaración)

1. **Ticket/historia nueva**, separada de VPR-9895 (cash release) — no se toca ese plan/branch.
2. **Alcance de ahora**: modelar `AccountType` (`COLLECTION`/`INVESTMENT`/`RESERVE`) y el mapeo
   tipo→campo de balance (auto-selección en vez de selección manual de `PoolBalanceType`), dejando el
   cálculo real de `investmentGains` como gap explícito **documentado** (no se inventa la fórmula sin
   verificar — requiere `lastDistribution`, sin confirmar en este alcance).
3. **Sí se modela ahora** la excepción puntual de la cuenta `WELLI_INVESTMENT` (por `accountCode`):
   fuerza `currentBalance`, `reservedBalance=0`, sin importar que su tipo sea `INVESTMENT`.
4. El `AccountType` se recibe como parámetro (ya resuelto por el caller/frontend vía Company API) —
   este motor no lo resuelve internamente todavía.
5. `AccountType` (y `accountCode` para la excepción WELLI) tienen que persistirse en la config
   (`PoolConfig`/`BalanceStrategyConfig`), no ser solo un parámetro efímero de cálculo — el usuario
   explícitamente pidió que "en config también tiene que estar".
