ALTER TABLE `devices` ADD COLUMN `signal_hash` text;
--> statement-breakpoint
CREATE TABLE `device_signals` (
 `signal_hash` text NOT NULL,
 `user_id` text NOT NULL,
 `first_seen` integer NOT NULL,
 `last_seen` integer NOT NULL,
 PRIMARY KEY(`signal_hash`,`user_id`)
);
--> statement-breakpoint
CREATE INDEX `device_signals_user` ON `device_signals` (`user_id`);
