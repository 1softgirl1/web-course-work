-- Local/dev only demo examinations.
insert into examinations (patient_id, title, exam_date, comment)
select p.id, 'Initial check', date '2026-02-15', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-001'
union all
select p.id, 'Quarterly follow-up', date '2026-04-10', 'Stable postoperative condition'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-001'
union all
select p.id, 'Initial check', date '2025-10-01', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-002'
union all
select p.id, 'Control visit', date '2025-12-10', 'Moderate symptoms remain'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-002'
union all
select p.id, 'Initial check', date '2025-07-01', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-003'
union all
select p.id, 'Control visit', date '2025-09-01', 'Long interval without updates'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-003'
union all
select p.id, 'Initial check', date '2026-01-15', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-004'
union all
select p.id, 'Quarterly follow-up', date '2026-03-25', 'Condition improving'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-004'
union all
select p.id, 'Initial check', date '2025-11-20', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-005'
union all
select p.id, 'Control visit', date '2026-01-05', 'Requires continued monitoring'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-005'
union all
select p.id, 'Initial check', date '2026-02-05', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-006'
union all
select p.id, 'Quarterly follow-up', date '2026-04-15', 'Good exercise tolerance'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-006'
union all
select p.id, 'Initial check', date '2025-12-18', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-007'
union all
select p.id, 'Control visit', date '2026-02-20', 'Requires pressure monitoring'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-007'
union all
select p.id, 'Initial check', date '2025-09-12', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-008'
union all
select p.id, 'Control visit', date '2025-11-30', 'Stable with mild symptoms'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-008'
union all
select p.id, 'Initial check', date '2026-03-03', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-009'
union all
select p.id, 'Control visit', date '2026-04-20', 'Rhythm stabilized'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-009'
union all
select p.id, 'Initial check', date '2025-08-05', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-010'
union all
select p.id, 'Control visit', date '2026-01-18', 'Needs closer follow-up'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-010'
union all
select p.id, 'Initial check', date '2026-02-02', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-011'
union all
select p.id, 'Control visit', date '2026-03-12', 'Stable measurements'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-011'
union all
select p.id, 'Initial check', date '2025-06-22', 'Baseline values'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-012'
union all
select p.id, 'Control visit', date '2025-10-15', 'Long interval without updates'
from patient_profiles p join users u on u.id = p.user_id where u.username = 'PT-DEMO-012';
