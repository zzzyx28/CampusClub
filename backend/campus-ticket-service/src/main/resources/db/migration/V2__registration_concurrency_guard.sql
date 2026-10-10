-- At most one active registration per user/activity, including under concurrent
-- requests. MySQL unique indexes allow multiple NULL values, so cancelled or
-- completed historical registrations remain valid.
ALTER TABLE `registration`
  ADD COLUMN `active_guard` TINYINT
    GENERATED ALWAYS AS (CASE WHEN `status` = 1 THEN 1 ELSE NULL END) STORED,
  ADD UNIQUE KEY `uk_registration_active` (`user_id`, `activity_id`, `active_guard`);
