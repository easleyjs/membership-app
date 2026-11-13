ALTER TABLE public.check_in
    ADD COLUMN admission_type varchar(255) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN membership_override_requested boolean NOT NULL DEFAULT false,
    ADD CONSTRAINT check_in_admission_type_check
        CHECK (admission_type IN ('STANDARD', 'MEMBERSHIP_OVERRIDE'));
