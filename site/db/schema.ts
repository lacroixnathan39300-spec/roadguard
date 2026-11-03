import { sqliteTable, text, integer, real, index, primaryKey } from "drizzle-orm/sqlite-core";
export const reports = sqliteTable("reports", {
  id: text("id").primaryKey(), owner: text("owner").notNull(),
  kind: text("kind").notNull(), lat: real("lat").notNull(), lng: real("lng").notNull(),
  note: text("note").notNull(), created: integer("created").notNull(),
  updated: integer("updated").notNull(), expires: integer("expires").notNull(),
  status: text("status").notNull().default("active"),
}, t => [index("reports_area").on(t.lat,t.lng,t.expires),index("reports_owner_created").on(t.owner,t.created), index("reports_expiry").on(t.expires),index("reports_page").on(t.lat,t.lng,t.id)]);
export const events = sqliteTable("events", {
  revision: integer("revision").primaryKey({autoIncrement:true}), reportId: text("report_id").notNull(),
  lat: real("lat").notNull(), lng: real("lng").notNull(), payload: text("payload").notNull(), created: integer("created").notNull(),
},t=>[index("events_created").on(t.created)]);
export const votes = sqliteTable("votes", {
  reportId: text("report_id").notNull(), user: text("user").notNull(), value: text("value").notNull(), updated: integer("updated").notNull(),
},t=>[primaryKey({columns:[t.reportId,t.user]})]);
export const presence = sqliteTable("presence", {
  user: text("user").primaryKey(), publicId:text("public_id").notNull(), alias: text("alias").notNull(),
  lat: real("lat").notNull(), lng: real("lng").notNull(), updated: integer("updated").notNull(), expires: integer("expires").notNull(),
},t=>[index("presence_area").on(t.lat,t.lng,t.expires),index("presence_expiry").on(t.expires)]);
export const preferences = sqliteTable("preferences", {
  user: text("user").primaryKey(), value: text("value").notNull(), updated: integer("updated").notNull(),
});
export const devices=sqliteTable('devices',{
 id:text('id').primaryKey(), user:text('user'), tokenHash:text('token_hash').notNull(), name:text('name').notNull(),
 created:integer('created').notNull(), pendingUntil:integer('pending_until').notNull(), expires:integer('expires').notNull(),signalHash:text('signal_hash'),
},t=>[index('devices_token').on(t.tokenHash),index('devices_owner').on(t.user),index('devices_pending').on(t.pendingUntil)]);
export const deviceSignals=sqliteTable('device_signals',{
 signalHash:text('signal_hash').notNull(),userId:text('user_id').notNull(),firstSeen:integer('first_seen').notNull(),lastSeen:integer('last_seen').notNull(),
},t=>[primaryKey({columns:[t.signalHash,t.userId]}),index('device_signals_user').on(t.userId)]);
export const rateLimits=sqliteTable('rate_limits',{
 key:text('key').primaryKey(),hits:integer('hits').notNull(),until:integer('until').notNull(),
},t=>[index('rate_expiry').on(t.until)]);
export const geocodeCache=sqliteTable('geocode_cache',{
 query:text('query').primaryKey(),value:text('value').notNull(),expires:integer('expires').notNull(),
});
export const presenceSessions=sqliteTable('presence_sessions',{
 user:text('user').primaryKey(),lease:text('lease').notNull(),expires:integer('expires').notNull(),
});
export const phoneAccounts=sqliteTable('phone_accounts',{
 phoneHash:text('phone_hash').primaryKey(),userId:text('user_id').notNull().unique(),lastFour:text('last_four').notNull(),
 displayName:text('display_name').notNull().default(''),unlimited:integer('unlimited').notNull().default(0),admin:integer('admin').notNull().default(0),
 trialStarted:integer('trial_started').notNull(),created:integer('created').notNull(),updated:integer('updated').notNull(),
},t=>[index('phone_accounts_created').on(t.created)]);
export const nativeCredentials=sqliteTable('native_credentials',{
 phoneHash:text('phone_hash').primaryKey(),userId:text('user_id').notNull().unique(),
 salt:text('salt').notNull(),verifier:text('verifier').notNull(),
 created:integer('created').notNull(),updated:integer('updated').notNull(),
});
export const accessGrants=sqliteTable('access_grants',{
 phoneHash:text('phone_hash').primaryKey(),lastFour:text('last_four').notNull(),unlimited:integer('unlimited').notNull(),updated:integer('updated').notNull(),
});
