-- Replace placeholder catalog seeded in V10 with the real indicator set
-- defined in pokazateli.md. Placeholder rows are only safe to delete when
-- nothing references them (production/default has no measurements yet).

delete from examination_characteristics
where characteristic_id in (
    select id from characteristics where code like 'metric_%'
);

delete from characteristics where code like 'metric_%';

insert into characteristics (code, name, unit) values
    ('weight_kg',           'Вес',                                  'кг'),
    ('height_cm',           'Рост',                                 'см'),
    ('exercise_tolerance',  'Переносимость физической нагрузки',    'NYHA'),
    ('rv_size_mm',          'Размер ПЖ',                            'мм'),
    ('contractility_pct',   'Сократимость',                         '%'),
    ('tc_insufficiency',    'Недостаточность ТК',                   'степень'),
    ('rvsp_mmhg',           'СДПЖ',                                 'мм рт. ст.'),
    ('ef_pct',              'ФВ',                                   '%'),
    ('edv_rv_lv_ratio',     'иКДО ПЖ/ЛЖ',                           'индекс'),
    ('rvot_mm',             'ВОПЖ, диаметр',                        'мм'),
    ('rvot_gradient',       'ВОПЖ, ср градиент',                    'мм рт. ст.'),
    ('pa_annulus_mm',       'ФК ЛА, диаметр',                       'мм'),
    ('pa_annulus_gradient', 'ФК ЛА, ср градиент',                   'мм рт. ст.'),
    ('pa_trunk_mm',         'Ствол ЛА, диаметр',                    'мм'),
    ('pa_trunk_gradient',   'Ствол ЛА, ср градиент',                'мм рт. ст.'),
    ('rpa_mm',              'ПВЛА, диаметр',                        'мм'),
    ('rpa_gradient',        'ПВЛА, ср градиент',                    'мм рт. ст.'),
    ('lpa_mm',              'ЛВЛА, диаметр',                        'мм'),
    ('lpa_gradient',        'ЛВЛА, ср градиент',                    'мм рт. ст.'),
    ('ivs_mm',              'МЖП',                                  'мм'),
    ('ias_mm',              'МПП',                                  'мм'),
    ('arch_mm',             'Дуга аорты',                           'мм')
on conflict (code) do nothing;
