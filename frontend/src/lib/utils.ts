import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs))
}

/**
 * Возвращает правильную форму русского существительного для числительного.
 * forms = [одна форма, две формы, пять форм] — например ['год', 'года', 'лет'].
 */
export function pluralizeRu(count: number, forms: [string, string, string]): string {
    const absolute = Math.abs(Math.trunc(count))
    const mod10 = absolute % 10
    const mod100 = absolute % 100
    if (mod100 >= 11 && mod100 <= 14) return forms[2]
    if (mod10 === 1) return forms[0]
    if (mod10 >= 2 && mod10 <= 4) return forms[1]
    return forms[2]
}

export function formatYearsRu(years: number): string {
    return `${years} ${pluralizeRu(years, ['год', 'года', 'лет'])}`
}
