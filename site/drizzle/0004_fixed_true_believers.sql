CREATE TABLE `phone_accounts` (
	`phone_hash` text PRIMARY KEY NOT NULL,
	`user_id` text NOT NULL,
	`last_four` text NOT NULL,
	`display_name` text DEFAULT '' NOT NULL,
	`unlimited` integer DEFAULT 0 NOT NULL,
	`trial_started` integer NOT NULL,
	`created` integer NOT NULL,
	`updated` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `phone_accounts_user_id_unique` ON `phone_accounts` (`user_id`);--> statement-breakpoint
CREATE INDEX `phone_accounts_created` ON `phone_accounts` (`created`);