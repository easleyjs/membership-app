ALTER TABLE public.app_users
    ADD COLUMN status varchar(255) NOT NULL DEFAULT 'ACTIVE',
    ALTER COLUMN password_hash DROP NOT NULL,
    ADD CONSTRAINT app_users_status_password_check CHECK (
        (status = 'ACTIVE' AND password_hash IS NOT NULL)
        OR (status = 'PENDING_INVITE' AND password_hash IS NULL)
    );

ALTER TABLE public.app_users ALTER COLUMN status DROP DEFAULT;

CREATE TABLE public.app_user_invites (
    token_hash varchar(64) PRIMARY KEY,
    app_user_id uuid NOT NULL REFERENCES public.app_users (id) ON DELETE CASCADE,
    email varchar(255) NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone
);

CREATE UNIQUE INDEX app_user_invites_one_unused_per_user
    ON public.app_user_invites (app_user_id) WHERE used_at IS NULL;
