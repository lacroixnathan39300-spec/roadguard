CREATE TABLE `presence_sessions` (
	`user` text PRIMARY KEY NOT NULL,
	`lease` text NOT NULL,
	`expires` integer NOT NULL
);
