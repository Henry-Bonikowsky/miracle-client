-- Supabase Schema for Miracle Client
-- Run this in your Supabase SQL Editor: https://supabase.com/dashboard/project/xthsogqpmclnafmzpape/sql

-- ============================================
-- MOD RELEASES (for auto-updates)
-- ============================================

CREATE TABLE IF NOT EXISTS mod_releases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mod_id TEXT NOT NULL,              -- e.g., "miracle-client"
    version TEXT NOT NULL,             -- e.g., "1.0.1"
    minecraft_version TEXT NOT NULL,   -- e.g., "1.21.8" or "1.21.4"
    download_url TEXT NOT NULL,        -- URL to the .jar file (use Supabase Storage)
    changelog TEXT,                    -- Release notes
    checksum TEXT,                     -- SHA256 hash for verification (optional)
    mandatory BOOLEAN DEFAULT false,   -- Force update?
    created_at TIMESTAMPTZ DEFAULT now(),

    UNIQUE(mod_id, version, minecraft_version)
);

ALTER TABLE mod_releases ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow public read" ON mod_releases
    FOR SELECT USING (true);

CREATE INDEX IF NOT EXISTS idx_mod_releases_lookup
    ON mod_releases (mod_id, minecraft_version, created_at DESC);


-- ============================================
-- USERS (player profiles)
-- ============================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    minecraft_uuid TEXT UNIQUE NOT NULL,  -- Minecraft UUID (no dashes, lowercase)
    username TEXT NOT NULL,               -- Minecraft username
    is_online BOOLEAN DEFAULT false,
    current_server TEXT,                  -- Server IP/name they're playing on
    last_seen TIMESTAMPTZ DEFAULT now(),
    created_at TIMESTAMPTZ DEFAULT now()
);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Anyone can read user profiles (for friend search)
CREATE POLICY "Allow public read users" ON users
    FOR SELECT USING (true);

-- Users can only update their own profile (matched by minecraft_uuid passed in request)
CREATE POLICY "Users update own profile" ON users
    FOR UPDATE USING (true);

-- Anyone can insert (for first-time registration)
CREATE POLICY "Allow insert users" ON users
    FOR INSERT WITH CHECK (true);

CREATE INDEX IF NOT EXISTS idx_users_minecraft_uuid ON users (minecraft_uuid);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);


-- ============================================
-- FRIENDSHIPS
-- ============================================

CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending',  -- 'pending' or 'accepted'
    created_at TIMESTAMPTZ DEFAULT now(),
    accepted_at TIMESTAMPTZ,

    -- Prevent duplicate friendships
    UNIQUE(user_id, friend_id),
    -- Can't friend yourself
    CHECK (user_id != friend_id)
);

ALTER TABLE friendships ENABLE ROW LEVEL SECURITY;

-- Anyone can read friendships (needed for friend list queries)
CREATE POLICY "Allow public read friendships" ON friendships
    FOR SELECT USING (true);

-- Anyone can insert friendships (send friend requests)
CREATE POLICY "Allow insert friendships" ON friendships
    FOR INSERT WITH CHECK (true);

-- Anyone can update friendships (accept/decline)
CREATE POLICY "Allow update friendships" ON friendships
    FOR UPDATE USING (true);

-- Anyone can delete friendships (remove friend)
CREATE POLICY "Allow delete friendships" ON friendships
    FOR DELETE USING (true);

CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships (user_id, status);
CREATE INDEX IF NOT EXISTS idx_friendships_friend ON friendships (friend_id, status);


-- ============================================
-- HELPER FUNCTIONS
-- ============================================

-- Function to get or create a user by Minecraft UUID
CREATE OR REPLACE FUNCTION get_or_create_user(
    p_minecraft_uuid TEXT,
    p_username TEXT
) RETURNS UUID AS $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Try to find existing user
    SELECT id INTO v_user_id FROM users WHERE minecraft_uuid = p_minecraft_uuid;

    -- If not found, create new user
    IF v_user_id IS NULL THEN
        INSERT INTO users (minecraft_uuid, username)
        VALUES (p_minecraft_uuid, p_username)
        RETURNING id INTO v_user_id;
    ELSE
        -- Update username if it changed
        UPDATE users SET username = p_username, last_seen = now()
        WHERE id = v_user_id AND username != p_username;
    END IF;

    RETURN v_user_id;
END;
$$ LANGUAGE plpgsql;


-- ============================================
-- EXAMPLE USAGE
-- ============================================

-- To upload mod JAR files:
-- 1. Go to Storage in your Supabase dashboard
-- 2. Create a bucket called "mods"
-- 3. Make it public (or use signed URLs)
-- 4. Upload your .jar files there
-- 5. Use the public URL in the download_url field

-- Example: Insert a mod release
-- INSERT INTO mod_releases (mod_id, version, minecraft_version, download_url, changelog)
-- VALUES (
--     'miracle-client',
--     '1.0.1',
--     '1.21.8',
--     'https://xthsogqpmclnafmzpape.supabase.co/storage/v1/object/public/mods/miracle-client-1.0.1-1.21.8.jar',
--     'Bug fixes and performance improvements'
-- );
