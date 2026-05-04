-- Local/dev only. Keeps repeated local runs predictable by removing demo and Bruno runtime users.
delete from users
where username like 'bruno.%'
   or username like 'PT-%'
   or username in (
    'doctor.demo@example.com',
    'head.doctor@example.com',
    'outsider.doctor@example.com',
    'kemerovo.doctor@example.com',
    'novosibirsk.doctor@example.com',
    'tomsk.doctor@example.com',
    'perm.doctor@example.com',
    'moscow.doctor@example.com'
);
