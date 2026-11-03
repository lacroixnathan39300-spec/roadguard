CREATE TABLE `native_credentials` (
	`phone_hash` text PRIMARY KEY NOT NULL,
	`user_id` text NOT NULL,
	`salt` text NOT NULL,
	`verifier` text NOT NULL,
	`created` integer NOT NULL,
	`updated` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `native_credentials_user_id_unique` ON `native_credentials` (`user_id`);--> statement-breakpoint
