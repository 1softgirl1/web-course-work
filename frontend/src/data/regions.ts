export interface Clinic {
    name: string
    phone: string
    email: string
    address: string
}

export interface Region {
    id: string
    name: string
    clinics: Clinic[]
}

export const REGIONS: Region[] = [
    {
        id: "moscow",
        name: "Москва",
        clinics: [
            {
                name: "НМИЦ сердечно-сосудистой хирургии им. А.Н. Бакулева",
                phone: "+7 (495) 414-75-90",
                email: "info@bakulev.ru",
                address: "Рублёвское шоссе, 135, Москва",
            },
            {
                name: "НМИЦ здоровья детей",
                phone: "+7 (495) 967-14-20",
                email: "info@nczd.ru",
                address: "Ломоносовский пр-т, 2, стр. 1, Москва",
            },
        ],
    },
    {
        id: "spb",
        name: "Санкт-Петербург",
        clinics: [
            {
                name: "НМИЦ им. В.А. Алмазова",
                phone: "+7 (812) 702-37-06",
                email: "info@almazovcentre.ru",
                address: "ул. Аккуратова, 2, Санкт-Петербург",
            },
        ],
    },
    {
        id: "novosibirsk",
        name: "Новосибирск",
        clinics: [
            {
                name: "НМИЦ им. Е.Н. Мешалкина",
                phone: "+7 (383) 347-60-00",
                email: "info@meshalkin.ru",
                address: "ул. Речкуновская, 15, Новосибирск",
            },
        ],
    },
    {
        id: "tomsk",
        name: "Томск",
        clinics: [
            {
                name: "НИИ кардиологии Томского НИМЦ",
                phone: "+7 (3822) 55-81-97",
                email: "cardio@tnimc.ru",
                address: "ул. Киевская, 111а, Томск",
            },
        ],
    },
    {
        id: "kazan",
        name: "Казань",
        clinics: [
            {
                name: "Межрегиональный клинико-диагностический центр",
                phone: "+7 (843) 291-11-01",
                email: "info@icdc.ru",
                address: "ул. Карбышева, 12а, Казань",
            },
        ],
    },
]

export const CITY_TO_REGION: Record<string, string> = {
    "moscow": "moscow",
    "москва": "moscow",
    "saint petersburg": "spb",
    "санкт-петербург": "spb",
    "novosibirsk": "novosibirsk",
    "новосибирск": "novosibirsk",
    "tomsk": "tomsk",
    "томск": "tomsk",
    "kazan": "kazan",
    "казань": "kazan",
}