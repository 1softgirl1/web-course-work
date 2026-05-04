-- Local/dev only demo measurements.
insert into examination_characteristics (examination_id, characteristic_id, value, comment)
select e.id, c.id, v.measurement_value, v.measurement_comment
from (
    values
        ('PT-DEMO-001', 'Initial check', date '2026-02-15', 'metric_01', 72, 'Measured at rest'),
        ('PT-DEMO-001', 'Initial check', date '2026-02-15', 'metric_02', 120, 'Systolic pressure'),
        ('PT-DEMO-001', 'Quarterly follow-up', date '2026-04-10', 'metric_01', 68, 'Measured at rest'),
        ('PT-DEMO-001', 'Quarterly follow-up', date '2026-04-10', 'metric_02', 118, 'Systolic pressure'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'metric_01', 80, 'Measured at rest'),
        ('PT-DEMO-002', 'Initial check', date '2025-10-01', 'metric_02', 135, 'Systolic pressure'),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'metric_01', 82, 'Measured at rest'),
        ('PT-DEMO-002', 'Control visit', date '2025-12-10', 'metric_02', 132, 'Systolic pressure'),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'metric_01', 78, 'Measured at rest'),
        ('PT-DEMO-003', 'Initial check', date '2025-07-01', 'metric_02', 140, 'Systolic pressure'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'metric_01', 85, 'Measured at rest'),
        ('PT-DEMO-003', 'Control visit', date '2025-09-01', 'metric_02', 145, 'Systolic pressure'),
        ('PT-DEMO-004', 'Initial check', date '2026-01-15', 'metric_01', 76, 'Measured at rest'),
        ('PT-DEMO-004', 'Initial check', date '2026-01-15', 'metric_02', 128, 'Systolic pressure'),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'metric_01', 70, 'Measured at rest'),
        ('PT-DEMO-004', 'Quarterly follow-up', date '2026-03-25', 'metric_02', 122, 'Systolic pressure'),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'metric_01', 88, 'Measured at rest'),
        ('PT-DEMO-005', 'Initial check', date '2025-11-20', 'metric_02', 138, 'Systolic pressure'),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'metric_01', 84, 'Measured at rest'),
        ('PT-DEMO-005', 'Control visit', date '2026-01-05', 'metric_02', 134, 'Systolic pressure'),
        ('PT-DEMO-006', 'Initial check', date '2026-02-05', 'metric_01', 73, 'Measured at rest'),
        ('PT-DEMO-006', 'Initial check', date '2026-02-05', 'metric_02', 124, 'Systolic pressure'),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'metric_01', 69, 'Measured at rest'),
        ('PT-DEMO-006', 'Quarterly follow-up', date '2026-04-15', 'metric_02', 119, 'Systolic pressure'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'metric_01', 77, 'Measured at rest'),
        ('PT-DEMO-007', 'Initial check', date '2025-12-18', 'metric_02', 130, 'Systolic pressure'),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'metric_01', 79, 'Measured at rest'),
        ('PT-DEMO-007', 'Control visit', date '2026-02-20', 'metric_02', 128, 'Systolic pressure'),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'metric_01', 83, 'Measured at rest'),
        ('PT-DEMO-008', 'Initial check', date '2025-09-12', 'metric_02', 142, 'Systolic pressure'),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'metric_01', 81, 'Measured at rest'),
        ('PT-DEMO-008', 'Control visit', date '2025-11-30', 'metric_02', 136, 'Systolic pressure'),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'metric_01', 74, 'Measured at rest'),
        ('PT-DEMO-009', 'Initial check', date '2026-03-03', 'metric_02', 126, 'Systolic pressure'),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'metric_01', 71, 'Measured at rest'),
        ('PT-DEMO-009', 'Control visit', date '2026-04-20', 'metric_02', 121, 'Systolic pressure'),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'metric_01', 86, 'Measured at rest'),
        ('PT-DEMO-010', 'Initial check', date '2025-08-05', 'metric_02', 146, 'Systolic pressure'),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'metric_01', 84, 'Measured at rest'),
        ('PT-DEMO-010', 'Control visit', date '2026-01-18', 'metric_02', 139, 'Systolic pressure'),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'metric_01', 75, 'Measured at rest'),
        ('PT-DEMO-011', 'Initial check', date '2026-02-02', 'metric_02', 122, 'Systolic pressure'),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'metric_01', 72, 'Measured at rest'),
        ('PT-DEMO-011', 'Control visit', date '2026-03-12', 'metric_02', 120, 'Systolic pressure'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'metric_01', 90, 'Measured at rest'),
        ('PT-DEMO-012', 'Initial check', date '2025-06-22', 'metric_02', 150, 'Systolic pressure'),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'metric_01', 87, 'Measured at rest'),
        ('PT-DEMO-012', 'Control visit', date '2025-10-15', 'metric_02', 144, 'Systolic pressure')
) as v(username, title, exam_date, characteristic_code, measurement_value, measurement_comment)
join users u on u.username = v.username
join patient_profiles p on p.user_id = u.id
join examinations e on e.patient_id = p.id and e.title = v.title and e.exam_date = v.exam_date
join characteristics c on c.code = v.characteristic_code;
