ALTER TABLE rescue_cases
ADD CONSTRAINT chk_rescue_cases_status
CHECK (
    status IN (
        'ADMITTED',
        'UNDER_EVALUATION',
        'IN_REHABILITATION',
        'READY_FOR_RELEASE',
        'RELEASED',
        'CLOSED'
    )
);