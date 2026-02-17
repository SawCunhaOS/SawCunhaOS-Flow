INSERT INTO scos_profile_permission (profile_id, permission)
SELECT (SELECT profile_id FROM scos_profile WHERE code = 'ADMIN'), permission
FROM scos_permission;
