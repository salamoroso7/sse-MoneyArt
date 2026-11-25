-- Rebuild the isolated benchmark dataset. Run only against the Energibridge DB.
USE moneyart;

UPDATE utente SET id_seguito = NULL;

DELETE FROM segnalazione;
DELETE FROM notifica;
DELETE FROM partecipazione;
DELETE FROM rivendita;
DELETE FROM asta;
DELETE FROM opera;
DELETE FROM utente;
DELETE FROM timerservice.timer;

ALTER TABLE segnalazione AUTO_INCREMENT = 1;
ALTER TABLE notifica AUTO_INCREMENT = 1;
ALTER TABLE partecipazione AUTO_INCREMENT = 1;
ALTER TABLE rivendita AUTO_INCREMENT = 1;
ALTER TABLE asta AUTO_INCREMENT = 1;
ALTER TABLE opera AUTO_INCREMENT = 1;
ALTER TABLE utente AUTO_INCREMENT = 1;
ALTER TABLE timerservice.timer AUTO_INCREMENT = 1;

SET @benchmark_password = UNHEX(SHA2('benchmark-only-123', 256));
SET @profile_picture = LOAD_FILE('/benchmark-assets/profilePics/gattoProfilePic.jpg');
SET @auction_image_1 = LOAD_FILE('/benchmark-assets/bears-deluxe-3742.png');
SET @auction_image_2 = LOAD_FILE('/benchmark-assets/shibosis.jpg');
SET @resale_image = LOAD_FILE('/benchmark-assets/cupcat.jpg');

SET SESSION cte_max_recursion_depth = 6000;

INSERT INTO utente
  (id, id_seguito, email, pwd, username, nome, cognome, foto, saldo)
VALUES
  (1, NULL, 'benchmark-admin@example.test', @benchmark_password,
   'benchmark_admin', 'Benchmark', 'Admin', @profile_picture, 10000),
  (2, NULL, 'artist@example.test', @benchmark_password,
   'bench_artist', 'Benchmark', 'Artist', @profile_picture, 10000),
  (3, NULL, 'bidder-a@example.test', @benchmark_password,
   'bench_bidder_a', 'Benchmark', 'Bidder A', @profile_picture, 9000),
  (4, NULL, 'bidder-b@example.test', @benchmark_password,
   'bench_bidder_b', 'Benchmark', 'Bidder B', @profile_picture, 9500),
  (5, NULL, 'owner@example.test', @benchmark_password,
   'bench_owner', 'Benchmark', 'Owner', @profile_picture, 10000);

INSERT INTO utente
  (id, id_seguito, email, pwd, username, nome, cognome, foto, saldo)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
SELECT 5 + n, NULL,
       CONCAT('bench-user-', LPAD(n, 3, '0'), '@example.test'),
       @benchmark_password,
       CONCAT('bench_user_', LPAD(n, 3, '0')),
       'Benchmark', CONCAT('User ', LPAD(n, 3, '0')), NULL, 10000
FROM seq;

INSERT INTO opera
  (id, id_utente, id_artista, nome, descrizione, immagine, certificato, stato)
VALUES
  (1, 2, 2, 'Benchmark Auction 001', 'EnergiBridge benchmark fixture',
   @auction_image_1, NULL, 'ALL_ASTA'),
  (2, 2, 2, 'Benchmark Auction 002', 'EnergiBridge benchmark fixture',
   @auction_image_2, NULL, 'ALL_ASTA'),
  (3, 5, 2, 'Benchmark Resale 001', 'EnergiBridge benchmark fixture',
   @resale_image, NULL, 'IN_VENDITA');

INSERT INTO opera
  (id, id_utente, id_artista, nome, descrizione, immagine, certificato, stato)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 250
)
SELECT 3 + n,
       6 + MOD(n - 1, 50),
       6 + MOD(n - 1, 50),
       CONCAT('Benchmark Artwork ', LPAD(n, 4, '0')),
       'EnergiBridge generated benchmark fixture',
       CASE MOD(n, 3)
         WHEN 0 THEN @auction_image_1
         WHEN 1 THEN @auction_image_2
         ELSE @resale_image
       END,
       NULL, 'ALL_ASTA'
FROM seq;

INSERT INTO asta (id, id_opera, data_inizio, data_fine, stato)
VALUES
  (1, 1, '2026-01-01', '2099-12-31', 'IN_CORSO'),
  (2, 2, '2026-01-01', '2099-12-31', 'IN_CORSO');

INSERT INTO asta (id, id_opera, data_inizio, data_fine, stato)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 250
)
SELECT 2 + n, 3 + n, '2026-01-01', '2099-12-31', 'IN_CORSO'
FROM seq;

INSERT INTO rivendita (id, id_opera, prezzo, stato)
VALUES (1, 3, 250.00, 'IN_CORSO');

INSERT INTO partecipazione (id, id_utente, id_asta, offerta)
VALUES
  (1, 3, 1, 1000.00),
  (2, 4, 2, 500.00);

INSERT INTO partecipazione (id, id_utente, id_asta, offerta)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 250
),
bidder_slot(slot) AS (
  SELECT 0
  UNION ALL
  SELECT slot + 1 FROM bidder_slot WHERE slot < 9
)
SELECT 2 + ((seq.n - 1) * 10) + bidder_slot.slot + 1,
       6 + MOD(seq.n + bidder_slot.slot - 1, 50),
       2 + seq.n,
       100 + (bidder_slot.slot * 10) + MOD(seq.n - 1, 10)
FROM seq
CROSS JOIN bidder_slot;
