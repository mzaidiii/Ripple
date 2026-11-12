CREATE TABLE repositories(
    id UUID PRIMARY KEY ,
    full_name VARCHAR(255) NOT NULL UNIQUE ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'INDEXING' , 'READY' , 'FAILED')),
    last_indexed_sha VARCHAR(64) ,
    last_error TEXT ,
    indexed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);