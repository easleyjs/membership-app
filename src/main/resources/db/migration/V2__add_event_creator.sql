ALTER TABLE public.event
    ADD COLUMN created_by uuid REFERENCES public.app_users (id);
