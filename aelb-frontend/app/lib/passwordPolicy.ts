// Replica de PasswordPolicy del backend (domain/user/valueobject/PasswordPolicy.java):
// si cambian las reglas allí, hay que cambiarlas aquí. El backend es quien decide.

export const PASSWORD_MIN_LENGTH = 8

export type PasswordRule = 'minimo' | 'minuscula' | 'mayuscula' | 'numero'

const RULES: Record<PasswordRule, (password: string) => boolean> = {
  minimo:    (p) => p.length >= PASSWORD_MIN_LENGTH,
  minuscula: (p) => /\p{Ll}/u.test(p),
  mayuscula: (p) => /\p{Lu}/u.test(p),
  numero:    (p) => /\d/.test(p),
}

export function checkPassword(password: string): Record<PasswordRule, boolean> {
  return Object.fromEntries(
    Object.entries(RULES).map(([rule, test]) => [rule, test(password)])
  ) as Record<PasswordRule, boolean>
}

export function isPasswordValid(password: string): boolean {
  return Object.values(checkPassword(password)).every(Boolean)
}
