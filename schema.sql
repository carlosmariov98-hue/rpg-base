CREATE DATABASE IF NOT EXISTS rpg_grupal_db CHARACTER SET utf8mb4;
USE rpg_grupal_db;

CREATE TABLE IF NOT EXISTS heroes (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(15) NOT NULL UNIQUE,
    tipo        VARCHAR(10) NOT NULL,
    vida_maxima INT         NOT NULL,
    nivel       INT         NOT NULL DEFAULT 1,
    CONSTRAINT chk_tipo  CHECK (tipo IN ('GUERRERO', 'MAGO')),
    CONSTRAINT chk_vida  CHECK (vida_maxima > 0),
    CONSTRAINT chk_nivel CHECK (nivel >= 1)
);

  SELECT * FROM rpg_grupal_db.heroes;