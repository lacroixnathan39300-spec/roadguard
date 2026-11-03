CREATE TABLE `devices` (
	`id` text PRIMARY KEY NOT NULL,
	`user` text,
	`token_hash` text NOT NULL,
	`name` text NOT NULL,
	`created` integer NOT NULL,
	`pending_until` integer NOT NULL,
	`expires` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `devices_token` ON `devices` (`token_hash`);--> statement-breakpoint
CREATE INDEX `devices_owner` ON `devices` (`user`);--> statement-breakpoint
CREATE INDEX `devices_pending` ON `devices` (`pending_until`);--> statement-breakpoint
CREATE TABLE `geocode_cache` (
	`query` text PRIMARY KEY NOT NULL,
	`value` text NOT NULL,
	`expires` integer NOT NULL
);
--> statement-breakpoint
CREATE TABLE `rate_limits` (
	`key` text PRIMARY KEY NOT NULL,
	`hits` integer NOT NULL,
	`until` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `rate_expiry` ON `rate_limits` (`until`);