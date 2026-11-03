CREATE TABLE `events` (
	`revision` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`report_id` text NOT NULL,
	`lat` real NOT NULL,
	`lng` real NOT NULL,
	`payload` text NOT NULL,
	`created` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `events_created` ON `events` (`created`);--> statement-breakpoint
CREATE TABLE `preferences` (
	`user` text PRIMARY KEY NOT NULL,
	`value` text NOT NULL,
	`updated` integer NOT NULL
);
--> statement-breakpoint
CREATE TABLE `presence` (
	`user` text PRIMARY KEY NOT NULL,
	`public_id` text NOT NULL,
	`alias` text NOT NULL,
	`lat` real NOT NULL,
	`lng` real NOT NULL,
	`updated` integer NOT NULL,
	`expires` integer NOT NULL
);
--> statement-breakpoint
CREATE INDEX `presence_area` ON `presence` (`lat`,`lng`,`expires`);--> statement-breakpoint
CREATE INDEX `presence_expiry` ON `presence` (`expires`);--> statement-breakpoint
CREATE TABLE `reports` (
	`id` text PRIMARY KEY NOT NULL,
	`owner` text NOT NULL,
	`kind` text NOT NULL,
	`lat` real NOT NULL,
	`lng` real NOT NULL,
	`note` text NOT NULL,
	`created` integer NOT NULL,
	`updated` integer NOT NULL,
	`expires` integer NOT NULL,
	`status` text DEFAULT 'active' NOT NULL
);
--> statement-breakpoint
CREATE INDEX `reports_area` ON `reports` (`lat`,`lng`,`expires`);--> statement-breakpoint
CREATE INDEX `reports_owner_created` ON `reports` (`owner`,`created`);--> statement-breakpoint
CREATE INDEX `reports_expiry` ON `reports` (`expires`);--> statement-breakpoint
CREATE TABLE `votes` (
	`report_id` text NOT NULL,
	`user` text NOT NULL,
	`value` text NOT NULL,
	`updated` integer NOT NULL,
	PRIMARY KEY(`report_id`, `user`)
);
