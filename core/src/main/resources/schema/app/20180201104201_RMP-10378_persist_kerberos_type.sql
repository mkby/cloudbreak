-- // RMP-10378_persist_kerberos_type
-- Migration SQL that makes the change goes here.

ALTER TABLE kerberosconfig ADD COLUMN type varchar(255);

UPDATE kerberosconfig SET type = 'CB_MANAGED' where kerberosmasterkey IS NOT NULL AND kerberosmasterkey <> '';
UPDATE kerberosconfig SET type = 'ACTIVE_DIRECTORY' where kerberoscontainerdn IS NOT NULL AND kerberoscontainerdn <> '';
UPDATE kerberosconfig SET type = 'MIT' where type IS NULL;

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE kerberosconfig DROP COLUMN type;