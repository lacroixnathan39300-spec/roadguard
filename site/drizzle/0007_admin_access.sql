ALTER TABLE `phone_accounts` ADD COLUMN `admin` integer DEFAULT 0 NOT NULL;
--> statement-breakpoint
CREATE TABLE `access_grants` (
 `phone_hash` text PRIMARY KEY NOT NULL,
 `last_four` text NOT NULL,
 `unlimited` integer NOT NULL,
 `updated` integer NOT NULL
);
