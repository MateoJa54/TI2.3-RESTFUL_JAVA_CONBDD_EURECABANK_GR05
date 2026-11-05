-- =============================================================================
-- 02_datos.sql - EURECABANK / MySQL 8.4
-- Semilla de datos de prueba (datos ficticios heredados del dump de la guia).
--
-- CONTENIDO DE LA SEMILLA (verificado con consultas de conteo):
--   empleado ............ 14   (0001..0014; se excluye la fila 9999 'Internet')
--   usuario ............. 11   (0001..0011; se excluye la fila 9999 'Internet')
--   cliente ............. 20
--   sucursal ............. 7
--   moneda ............... 2   (01 = Soles, 02 = Dolares)
--   modulo .............. 6
--   tipomovimiento ...... 10
--   parametro ........... 2   (001 = ITF 0.08, 002 = 15 operaciones sin costo)
--   contador ............. 7
--   permiso ............ 42   (7 primeros empleados x 6 modulos)
--   asignado ........... 11
--   cargomantenimiento .. 2
--   costomovimiento ..... 2
--   interesmensual ...... 2
--   cuenta .............. 6   (5 ACTIVO + 1 CANCELADO '00300001')
--   movimiento ......... 74   (57 del dump de la guia + 17 de prueba)
--   logsession .......... 0   (tabla de auditoria: se llena en runtime)
--
-- Idempotente: todas las inserciones usan INSERT ... AS n ON DUPLICATE KEY UPDATE (sintaxis con alias de fila,
-- recomendada en MySQL 8.0.19+), asi que el script se puede correr dos veces
-- sin duplicar filas.
-- =============================================================================

USE eurekabank;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1) moneda
-- -----------------------------------------------------------------------------
INSERT INTO moneda (chr_monecodigo, vch_monedescripcion) VALUES
    ('01', 'Soles'),
    ('02', 'Dolares')
AS n ON DUPLICATE KEY UPDATE vch_monedescripcion = n.vch_monedescripcion;

-- -----------------------------------------------------------------------------
-- 2) empleado (14). La fila 9999 'Internet' del dump de la guia se descarta:
--    es un usuario de servicio sin credenciales reales.
-- -----------------------------------------------------------------------------
INSERT INTO empleado (chr_emplcodigo, vch_emplpaterno, vch_emplmaterno, vch_emplnombre, vch_emplciudad, vch_empldireccion) VALUES
    ('0001', 'Romero',    'Castillo',   'Carlos Alberto',  'Trujillo', 'Call1 1 Nro. 456'),
    ('0002', 'Castro',    'Vargas',     'Lidia',           'Lima',     'Federico Villarreal 456 - SMP'),
    ('0003', 'Reyes',     'Ortiz',      'Claudia',         'Lima',     'Av. Aviacion 3456 - San Borja'),
    ('0004', 'Ramos',     'Garibay',    'Angelica',        'Chiclayo', 'Calle Barcelona 345'),
    ('0005', 'Ruiz',      'Zabaleta',   'Claudia',         'Cusco',    'Calle Cruz Verde 364'),
    ('0006', 'Cruz',      'Tarazona',   'Ricardo',         'Arequipa', 'Calle La Gruta 304'),
    ('0007', 'Diaz',      'Flores',     'Edith',           'Lima',     'Av. Pardo 546'),
    ('0008', 'Sarmiento', 'Bellido',    'Claudia Rocio',   'Arequipa', 'Calle Alfonso Ugarte 1567'),
    ('0009', 'Pachas',    'Sifuentes',  'Luis Alberto',    'Trujillo', 'Francisco Pizarro 1263'),
    ('0010', 'Tello',     'Alarcon',    'Hugo Valentin',   'Cusco',    'Los Angeles 865'),
    ('0011', 'Carrasco',  'Vargas',     'Pedro Hugo',      'Chiclayo', 'Av. Balta 1265'),
    ('0012', 'Mendoza',   'Jara',       'Monica Valeria',  'Lima',     'Calle Las Toronjas 450'),
    ('0013', 'Espinoza',  'Melgar',     'Victor Eduardo', 'Huancayo', 'Av. San Martin 6734 Dpto. 508'),
    ('0014', 'Hidalgo',   'Sandoval',   'Milagros Leonor', 'Chiclayo', 'Av. Luis Gonzales 1230')
AS n ON DUPLICATE KEY UPDATE
    vch_emplpaterno   = n.vch_emplpaterno,
    vch_emplmaterno   = n.vch_emplmaterno,
    vch_emplnombre    = n.vch_emplnombre,
    vch_emplciudad    = n.vch_emplciudad,
    vch_empldireccion = n.vch_empldireccion;

-- -----------------------------------------------------------------------------
-- 3) sucursal (7)
-- -----------------------------------------------------------------------------
INSERT INTO sucursal (chr_sucucodigo, vch_sucunombre, vch_sucuciudad, vch_sucudireccion, int_sucucontcuenta) VALUES
    ('001', 'Sipan',        'Chiclayo', 'Av. Balta 1456',                 2),
    ('002', 'Chan Chan',    'Trujillo', 'Jr. Independencia 456',           3),
    ('003', 'Los Olivos',   'Lima',     'Av. Central 1234',                0),
    ('004', 'Pardo',        'Lima',     'Av. Pardo 345 - Miraflores',      0),
    ('005', 'Misti',        'Arequipa', 'Bolivar 546',                     0),
    ('006', 'Machupicchu',  'Cusco',    'Calle El Sol 534',                0),
    ('007', 'Grau',         'Piura',    'Av. Grau 1528',                   0)
AS n ON DUPLICATE KEY UPDATE
    vch_sucunombre     = n.vch_sucunombre,
    vch_sucuciudad     = n.vch_sucuciudad,
    vch_sucudireccion  = n.vch_sucudireccion,
    int_sucucontcuenta = n.int_sucucontcuenta;

-- -----------------------------------------------------------------------------
-- 4) cliente (20)
-- -----------------------------------------------------------------------------
INSERT INTO cliente (chr_cliecodigo, vch_cliepaterno, vch_cliematerno, vch_clienombre, chr_cliedni, vch_clieciudad, vch_cliedireccion, vch_clietelefono, vch_clieemail) VALUES
    ('00001', 'CORONEL',     'CASTILLO',    'ERIC GUSTAVO',       '06914897', 'LIMA', 'LOS OLIVOS',     '996-664-457', 'gcoronelc@gmail.com'),
    ('00002', 'VALENCIA',    'MORALES',     'PEDRO HUGO',         '01576173', 'LIMA', 'MAGDALENA',       '924-7834',    'pvalencia@terra.com.pe'),
    ('00003', 'MARCELO',     'VILLALOBOS',  'RICARDO',            '10762367', 'LIMA', 'LINCE',           '993-62966',   'ricardomarcelo@hotmail.com'),
    ('00004', 'ROMERO',      'CASTILLO',    'CARLOS ALBERTO',     '06531983', 'LIMA', 'LOS OLIVOS',     '865-84762',    'c.romero@hotmail.com'),
    ('00005', 'ARANDA',      'LUNA',        'ALAN ALBERTO',       '10875611', 'LIMA', 'SAN ISIDRO',     '834-67125',   'a.aranda@hotmail.com'),
    ('00006', 'AYALA',       'PAZ',         'JORGE LUIS',         '10679245', 'LIMA', 'SAN BORJA',      '963-34769',   'j.ayala@yahoo.com'),
    ('00007', 'CHAVEZ',      'CANALES',     'EDGAR RAFAEL',       '10145693', 'LIMA', 'MIRAFLORES',     '999-96673',    'e.chavez@gmail.com'),
    ('00008', 'FLORES',      'CHAFLOQUE',   'ROSA LIZET',         '10773456', 'LIMA', 'LA MOLINA',      '966-87567',   'r.florez@hotmail.com'),
    ('00009', 'FLORES',      'CASTILLO',    'CRISTIAN RAFAEL',    '10346723', 'LIMA', 'LOS OLIVOS',     '978-43768',   'c.flores@hotmail.com'),
    ('00010', 'GONZALES',    'GARCIA',      'GABRIEL ALEJANDRO',  '10192376', 'LIMA', 'SAN MIGUEL',     '945-56782',   'g.gonzales@yahoo.es'),
    ('00011', 'LAY',         'VALLEJOS',    'JUAN CARLOS',        '10942287', 'LIMA', 'LINCE',           '956-12657',   'j.lay@peru.com'),
    ('00012', 'MONTALVO',    'SOTO',        'DEYSI LIDIA',        '10612376', 'LIMA', 'SURCO',           '965-67235',   'd.montalvo@hotmail.com'),
    ('00013', 'RICALDE',     'RAMIREZ',     'ROSARIO ESMERALDA',  '10761324', 'LIMA', 'MIRAFLORES',     '991-23546',   'r.ricalde@gmail.com'),
    ('00014', 'RODRIGUEZ',   'FLORES',      'ENRIQUE MANUEL',     '10773345', 'LIMA', 'LINCE',           '976-82838',   'e.rodriguez@gmail.com'),
    ('00015', 'ROJAS',       'OSCANOA',     'FELIX NINO',         '10238943', 'LIMA', 'LIMA',            '962-32158',   'f.rojas@yahoo.com'),
    ('00016', 'TEJADA',      'DEL AGUILA',  'TANIA LORENA',       '10446791', 'LIMA', 'PUEBLO LIBRE',    '966-23854',   't.tejada@hotmail.com'),
    ('00017', 'VALDEVIESO',  'LEYVA',       'LIDIA ROXANA',       '10452682', 'LIMA', 'SURCO',           '956-78951',   'r.valdivieso@terra.com.pe'),
    ('00018', 'VALENTIN',    'COTRINA',     'JUAN DIEGO',         '10398247', 'LIMA', 'LA MOLINA',      '921-12456',   'j.valentin@terra.com.pe'),
    ('00019', 'YAURICASA',   'BAUTISTA',    'YESABETH',           '10934584', 'LIMA', 'MAGDALENA',       '977-75777',   'y.yauricasa@terra.com.pe'),
    ('00020', 'ZEGARRA',     'GARCIA',      'FERNANDO MOISES',    '10772365', 'LIMA', 'SAN ISIDRO',     '936-45876',   'f.zegarra@hotmail.com')
AS n ON DUPLICATE KEY UPDATE
    vch_cliepaterno   = n.vch_cliepaterno,
    vch_cliematerno   = n.vch_cliematerno,
    vch_clienombre    = n.vch_clienombre,
    chr_cliedni       = n.chr_cliedni,
    vch_clieciudad    = n.vch_clieciudad,
    vch_cliedireccion = n.vch_cliedireccion,
    vch_clietelefono  = n.vch_clietelefono,
    vch_clieemail     = n.vch_clieemail;

-- -----------------------------------------------------------------------------
-- 5) modulo (6)
-- -----------------------------------------------------------------------------
INSERT INTO modulo (int_moducodigo, vch_modunombre, vch_moduestado) VALUES
    (1, 'Procesos',  'ACTIVO'),
    (2, 'Tablas',    'ACTIVO'),
    (3, 'Consultas', 'ACTIVO'),
    (4, 'Reportes',  'ACTIVO'),
    (5, 'Util',      'ACTIVO'),
    (6, 'Seguridad', 'ACTIVO')
AS n ON DUPLICATE KEY UPDATE
    vch_modunombre = n.vch_modunombre,
    vch_moduestado = n.vch_moduestado;

-- -----------------------------------------------------------------------------
-- 6) tipomovimiento (10). vch_tipoaccion define si el movimiento suma o resta
--    al saldo: es la base del calculo de dec_cuensaldo.
-- -----------------------------------------------------------------------------
INSERT INTO tipomovimiento (chr_tipocodigo, vch_tipodescripcion, vch_tipoaccion, vch_tipoestado) VALUES
    ('001', 'Apertura de Cuenta',   'INGRESO', 'ACTIVO'),
    ('002', 'Cancelar Cuenta',      'SALIDA',  'ACTIVO'),
    ('003', 'Deposito',             'INGRESO', 'ACTIVO'),
    ('004', 'Retiro',               'SALIDA',  'ACTIVO'),
    ('005', 'Interes',              'INGRESO', 'ACTIVO'),
    ('006', 'Mantenimiento',        'SALIDA',  'ACTIVO'),
    ('007', 'ITF',                  'SALIDA',  'ACTIVO'),
    ('008', 'Transferencia',        'INGRESO', 'ACTIVO'),
    ('009', 'Transferencia',        'SALIDA',  'ACTIVO'),
    ('010', 'Cargo por Movimiento', 'SALIDA',  'ACTIVO')
AS n ON DUPLICATE KEY UPDATE
    vch_tipodescripcion = n.vch_tipodescripcion,
    vch_tipoaccion     = n.vch_tipoaccion,
    vch_tipoestado     = n.vch_tipoestado;

-- -----------------------------------------------------------------------------
-- 7) usuario (11). Los hashes SHA-256 son los literales del CONTRATO CONGELADO
--    (seccion 12). Los usuarios creyes, cvalencia y ediaz conservan el hash
--    SHA-1 heredado del dump de la guia (40 caracteres): no permiten
--    autenticarse porque el sistema exige SHA-256 (regla RB-13).
-- -----------------------------------------------------------------------------
INSERT INTO usuario (chr_emplcodigo, vch_emplusuario, vch_emplclave, vch_emplestado) VALUES
    ('0001', 'cromero',     '891337e9a11b6a3a19872e32f6318a2c24e5060f0d2652e716bf038fbd8c68d4', 'ACTIVO'),
    ('0002', 'lcastro',     'd4b83a0cf9194ca8c9d5cb3f714596cfeb6543df33b618f9b05ef174df87aea4', 'ACTIVO'),
    ('0003', 'creyes',      '32b3491336522e073489725b5daf298cd749007a',                      'ANULADO'),
    ('0004', 'aramos',      'f0bd8805123716f1d5237930643ae643b43c7f378ff8d387090eacfa73a128ee', 'ACTIVO'),
    ('0005', 'cvalencia',   'c8a50f632c3c4baf27fc05facb1883104e1d16ef',                       'ACTIVO'),
    ('0006', 'rcruz',       'c6a5a412bea19bfa4133c0a15a97c20ddd49dc519330e07d82bdd9db926cc0ac', 'ACTIVO'),
    ('0007', 'ediaz',       '3718e00ac45cec21633e2211af9b77cd0a193698',                       'ANULADO'),
    ('0008', 'csarmiento',  '4503389bee2ee5ed2daf0125d825d7e364215989cce0f15804e67ac0fbce9adc', 'ANULADO'),
    ('0009', 'lpachas',     '386796f4e78be3c0ee919f1d97805e891fd076a940ba7e09db5601d94797b088', 'ACTIVO'),
    ('0010', 'htello',      'c35eb4169bb3099e24bf0590f0b3a6f3bab1a1d0cee46b182e8fb639e51b71e0', 'ACTIVO'),
    ('0011', 'pcarrasco',   '558c296936404a839676a427bc1e63e94db38f5404d489cbe2bde9ab79a51c85', 'ACTIVO')
AS n ON DUPLICATE KEY UPDATE
    vch_emplusuario = n.vch_emplusuario,
    vch_emplclave   = n.vch_emplclave,
    vch_emplestado  = n.vch_emplestado;

-- -----------------------------------------------------------------------------
-- 8) parametro (2). 001 = ITF en porcentaje, 002 = operaciones sin costo.
-- -----------------------------------------------------------------------------
INSERT INTO parametro (chr_paracodigo, vch_paradescripcion, vch_paravalor, vch_paraestado) VALUES
    ('001', 'ITF - Impuesto a la Transacciones Financieras', '0.08', 'ACTIVO'),
    ('002', 'Numero de Operaciones Sin Costo',               '15',   'ACTIVO')
AS n ON DUPLICATE KEY UPDATE
    vch_paradescripcion = n.vch_paradescripcion,
    vch_paravalor      = n.vch_paravalor,
    vch_paraestado     = n.vch_paraestado;

-- -----------------------------------------------------------------------------
-- 9) contador (7). Secuencias de codigos primarios, con los valores congelados.
-- -----------------------------------------------------------------------------
INSERT INTO contador (vch_conttabla, int_contitem, int_contlongitud) VALUES
    ('Asignado',       11, 6),
    ('Cliente',        20, 5),
    ('Empleado',       14, 4),
    ('Moneda',          2, 2),
    ('Parametro',       2, 3),
    ('Sucursal',        7, 3),
    ('TipoMovimiento', 10, 3)
AS n ON DUPLICATE KEY UPDATE
    int_contitem     = n.int_contitem,
    int_contlongitud = n.int_contlongitud;

-- -----------------------------------------------------------------------------
-- 10) permiso (42) = 7 primeros empleados (0001..0007) x 6 modulos.
-- -----------------------------------------------------------------------------
INSERT INTO permiso (chr_emplcodigo, int_moducodigo, vch_permestado) VALUES
    ('0001', 1, 'ACTIVO'), ('0001', 2, 'ACTIVO'), ('0001', 3, 'ACTIVO'),
    ('0001', 4, 'ACTIVO'), ('0001', 5, 'ACTIVO'), ('0001', 6, 'ACTIVO'),
    ('0002', 1, 'ACTIVO'), ('0002', 2, 'ACTIVO'), ('0002', 3, 'ACTIVO'),
    ('0002', 4, 'ACTIVO'), ('0002', 5, 'CANCELADO'), ('0002', 6, 'CANCELADO'),
    ('0003', 1, 'ACTIVO'), ('0003', 2, 'CANCELADO'), ('0003', 3, 'ACTIVO'),
    ('0003', 4, 'ACTIVO'), ('0003', 5, 'ACTIVO'), ('0003', 6, 'CANCELADO'),
    ('0004', 1, 'CANCELADO'), ('0004', 2, 'ACTIVO'), ('0004', 3, 'ACTIVO'),
    ('0004', 4, 'CANCELADO'), ('0004', 5, 'ACTIVO'), ('0004', 6, 'CANCELADO'),
    ('0005', 1, 'ACTIVO'), ('0005', 2, 'CANCELADO'), ('0005', 3, 'ACTIVO'),
    ('0005', 4, 'ACTIVO'), ('0005', 5, 'ACTIVO'), ('0005', 6, 'CANCELADO'),
    ('0006', 1, 'ACTIVO'), ('0006', 2, 'ACTIVO'), ('0006', 3, 'ACTIVO'),
    ('0006', 4, 'ACTIVO'), ('0006', 5, 'ACTIVO'), ('0006', 6, 'ACTIVO'),
    ('0007', 1, 'CANCELADO'), ('0007', 2, 'ACTIVO'), ('0007', 3, 'ACTIVO'),
    ('0007', 4, 'CANCELADO'), ('0007', 5, 'ACTIVO'), ('0007', 6, 'CANCELADO')
AS n ON DUPLICATE KEY UPDATE vch_permestado = n.vch_permestado;

-- -----------------------------------------------------------------------------
-- 11) asignado (11) - asignacion de empleado a sucursal.
-- -----------------------------------------------------------------------------
INSERT INTO asignado (chr_asigcodigo, chr_sucucodigo, chr_emplcodigo, dtt_asigfechaalta, dtt_asigfechabaja) VALUES
    ('000001', '001', '0004', '2007-11-15', NULL),
    ('000002', '002', '0001', '2007-11-20', NULL),
    ('000003', '003', '0002', '2007-11-28', NULL),
    ('000004', '004', '0003', '2007-12-12', '2008-03-25'),
    ('000005', '005', '0006', '2007-12-20', NULL),
    ('000006', '006', '0005', '2008-01-05', NULL),
    ('000007', '004', '0007', '2008-01-07', NULL),
    ('000008', '005', '0008', '2008-01-07', NULL),
    ('000009', '001', '0011', '2008-01-08', NULL),
    ('000010', '002', '0009', '2008-01-08', NULL),
    ('000011', '006', '0010', '2008-01-08', NULL)
AS n ON DUPLICATE KEY UPDATE
    chr_sucucodigo    = n.chr_sucucodigo,
    chr_emplcodigo    = n.chr_emplcodigo,
    dtt_asigfechaalta = n.dtt_asigfechaalta,
    dtt_asigfechabaja = n.dtt_asigfechabaja;

-- -----------------------------------------------------------------------------
-- 12) cargomantenimiento (2) - umbral y costo del cargo por mantenimiento.
-- -----------------------------------------------------------------------------
INSERT INTO cargomantenimiento (chr_monecodigo, dec_cargMontoMaximo, dec_cargImporte) VALUES
    ('01', 3500.00, 7.00),
    ('02', 1200.00, 2.50)
AS n ON DUPLICATE KEY UPDATE
    dec_cargMontoMaximo = n.dec_cargMontoMaximo,
    dec_cargImporte     = n.dec_cargImporte;

-- -----------------------------------------------------------------------------
-- 13) costomovimiento (2) - costo por movimiento a partir de la operacion 16.
-- -----------------------------------------------------------------------------
INSERT INTO costomovimiento (chr_monecodigo, dec_costimporte) VALUES
    ('01', 2.00),
    ('02', 0.60)
AS n ON DUPLICATE KEY UPDATE dec_costimporte = n.dec_costimporte;

-- -----------------------------------------------------------------------------
-- 14) interesmensual (2) - porcentaje mensual de interes proyectado.
-- -----------------------------------------------------------------------------
INSERT INTO interesmensual (chr_monecodigo, dec_inteimporte) VALUES
    ('01', 0.70),
    ('02', 0.60)
AS n ON DUPLICATE KEY UPDATE dec_inteimporte = n.dec_inteimporte;

-- -----------------------------------------------------------------------------
-- 15) cuenta (6) - 5 ACTIVO + 1 CANCELADO.
--     dec_cuensaldo e int_cuencontmov se recalculan al final del script a partir
--     de los movimientos, para que nunca queden desalineados.
-- -----------------------------------------------------------------------------
INSERT INTO cuenta (chr_cuencodigo, chr_monecodigo, chr_sucucodigo, chr_emplcreacuenta, chr_cliecodigo, dec_cuensaldo, dtt_cuenfechacreacion, vch_cuenestado, int_cuencontmov, chr_cuenclave) VALUES
    ('00100001', '01', '001', '0004', '00005', 7404.00, '2008-01-06', 'ACTIVO',    24, '123456'),
    ('00100002', '02', '001', '0004', '00005', 5002.97, '2008-01-08', 'ACTIVO',    12, '123456'),
    ('00200001', '01', '002', '0001', '00008', 7000.00, '2008-01-05', 'ACTIVO',    20, '123456'),
    ('00200002', '01', '002', '0001', '00001', 6800.00, '2008-01-09', 'ACTIVO',     7, '123456'),
    ('00200003', '02', '002', '0001', '00007', 6000.00, '2008-01-11', 'ACTIVO',     8, '123456'),
    ('00300001', '01', '003', '0002', '00010',    0.00, '2008-01-07', 'CANCELADO',  3, '123456')
AS n ON DUPLICATE KEY UPDATE
    chr_monecodigo     = n.chr_monecodigo,
    chr_sucucodigo     = n.chr_sucucodigo,
    chr_emplcreacuenta = n.chr_emplcreacuenta,
    chr_cliecodigo     = n.chr_cliecodigo,
    dec_cuensaldo      = n.dec_cuensaldo,
    dtt_cuenfechacreacion = n.dtt_cuenfechacreacion,
    vch_cuenestado     = n.vch_cuenestado,
    int_cuencontmov    = n.int_cuencontmov,
    chr_cuenclave      = n.chr_cuenclave;

-- -----------------------------------------------------------------------------
-- 16) movimiento (74)
--     - Movimientos 1..57: replica literal del dump de la guia.
--     - Movimientos adicionales: 17 pruebas coherentes con las reglas de negocio
--       (RB-05 a RB-07). Las transferencias van y vuelven entre cuentas de la
--       MISMA moneda, por lo que el saldo de cada cuenta no cambia y la cuenta
--       CANCELADA '00300001' no recibe movimientos nuevos (CTA-002).
--       El ITF de esas transferencias da 0.00 (importes menores a 6.25), asi que
--       no se registra movimiento tipo 007 (RB-08: solo si itf > 0).
-- -----------------------------------------------------------------------------
INSERT INTO movimiento (chr_cuencodigo, int_movinumero, dtt_movifecha, chr_emplcodigo, chr_tipocodigo, dec_moviimporte, chr_cuenreferencia) VALUES
    -- 00100001 : 20 movimientos del dump + 4 de prueba = 24
    ('00100001',  1, '2008-01-06', '0004', '001', 2800.00, NULL),
    ('00100001',  2, '2008-01-15', '0004', '003', 3200.00, NULL),
    ('00100001',  3, '2008-01-20', '0004', '004',  800.00, NULL),
    ('00100001',  4, '2008-02-14', '0004', '003', 2000.00, NULL),
    ('00100001',  5, '2008-02-25', '0004', '004',  500.00, NULL),
    ('00100001',  6, '2008-03-03', '0004', '004',  800.00, NULL),
    ('00100001',  7, '2008-03-15', '0004', '003', 1000.00, NULL),
    ('00100001',  8, '2024-12-07', '0001', '003',  100.00, NULL),
    ('00100001',  9, '2024-12-07', '0001', '003',  100.00, NULL),
    ('00100001', 10, '2024-12-07', '0001', '003',  100.00, NULL),
    ('00100001', 11, '2024-12-07', '0001', '003',  100.00, NULL),
    ('00100001', 12, '2024-12-07', '0001', '003',   10.00, NULL),
    ('00100001', 13, '2024-12-07', '0001', '003',   10.00, NULL),
    ('00100001', 14, '2024-12-07', '0001', '003',   10.00, NULL),
    ('00100001', 15, '2024-12-07', '0001', '003',   15.00, NULL),
    ('00100001', 16, '2024-12-09', '0001', '003',   15.00, NULL),
    ('00100001', 17, '2024-12-09', '0001', '003',    5.00, NULL),
    ('00100001', 18, '2024-12-09', '0001', '003',   20.00, NULL),
    ('00100001', 19, '2024-12-09', '0001', '003',    7.00, NULL),
    ('00100001', 20, '2024-12-09', '0001', '003',   12.00, NULL),
    ('00100001', 21, '2024-12-10', '0001', '009',    5.00, '00200002'),
    ('00100001', 22, '2024-12-11', '0001', '008',    5.00, '00200002'),
    ('00100001', 23, '2024-12-12', '0001', '008',    6.00, '00200001'),
    ('00100001', 24, '2024-12-13', '0001', '009',    6.00, '00200001'),
    -- 00100002 : 10 movimientos del dump + 2 de prueba = 12
    ('00100002',  1, '2008-01-08', '0004', '001', 1800.00, NULL),
    ('00100002',  2, '2008-01-25', '0004', '004', 1000.00, NULL),
    ('00100002',  3, '2008-02-13', '0004', '003', 2200.00, NULL),
    ('00100002',  4, '2008-03-08', '0004', '003', 1500.00, NULL),
    ('00100002',  5, '2024-12-07', '0001', '003',  150.00, NULL),
    ('00100002',  6, '2024-12-07', '0001', '003',  150.00, NULL),
    ('00100002',  7, '2024-12-09', '0001', '003',    6.00, NULL),
    ('00100002',  8, '2024-12-09', '0001', '003',   15.99, NULL),
    ('00100002',  9, '2024-12-09', '0001', '003',   90.99, NULL),
    ('00100002', 10, '2024-12-09', '0001', '003',   89.99, NULL),
    ('00100002', 11, '2024-12-10', '0001', '009',    4.00, '00200003'),
    ('00100002', 12, '2024-12-11', '0001', '008',    4.00, '00200003'),
    -- 00200001 : 15 movimientos del dump + 5 de prueba = 20
    ('00200001',  1, '2008-01-05', '0001', '001', 5000.00, NULL),
    ('00200001',  2, '2008-01-07', '0001', '003', 4000.00, NULL),
    ('00200001',  3, '2008-01-09', '0001', '004', 2000.00, NULL),
    ('00200001',  4, '2008-01-11', '0001', '003', 1000.00, NULL),
    ('00200001',  5, '2008-01-13', '0001', '003', 2000.00, NULL),
    ('00200001',  6, '2008-01-15', '0001', '004', 4000.00, NULL),
    ('00200001',  7, '2008-01-19', '0001', '003', 2000.00, NULL),
    ('00200001',  8, '2008-01-21', '0001', '004', 3000.00, NULL),
    ('00200001',  9, '2008-01-23', '0001', '003', 7000.00, NULL),
    ('00200001', 10, '2008-01-27', '0001', '004', 1000.00, NULL),
    ('00200001', 11, '2008-01-30', '0001', '004', 3000.00, NULL),
    ('00200001', 12, '2008-02-04', '0001', '003', 2000.00, NULL),
    ('00200001', 13, '2008-02-08', '0001', '004', 4000.00, NULL),
    ('00200001', 14, '2008-02-13', '0001', '003', 2000.00, NULL),
    ('00200001', 15, '2008-02-19', '0001', '004', 1000.00, NULL),
    ('00200001', 16, '2024-12-12', '0001', '009',    6.00, '00100001'),
    ('00200001', 17, '2024-12-13', '0001', '008',    6.00, '00100001'),
    ('00200001', 18, '2024-12-14', '0001', '003',   30.00, NULL),
    ('00200001', 19, '2024-12-15', '0001', '004',   20.00, NULL),
    ('00200001', 20, '2024-12-15', '0001', '004',   10.00, NULL),
    -- 00200002 : 3 movimientos del dump + 4 de prueba = 7
    ('00200002', 1, '2008-01-09', '0001', '001', 3800.00, NULL),
    ('00200002', 2, '2008-01-20', '0001', '003', 4200.00, NULL),
    ('00200002', 3, '2008-03-06', '0001', '004', 1200.00, NULL),
    ('00200002', 4, '2024-12-10', '0001', '008',    5.00, '00100001'),
    ('00200002', 5, '2024-12-11', '0001', '009',    5.00, '00100001'),
    ('00200002', 6, '2024-12-16', '0001', '003',  100.00, NULL),
    ('00200002', 7, '2024-12-17', '0001', '004',  100.00, NULL),
    -- 00200003 : 6 movimientos del dump + 2 de prueba = 8
    ('00200003', 1, '2008-01-11', '0001', '001', 2500.00, NULL),
    ('00200003', 2, '2008-01-17', '0001', '003', 1500.00, NULL),
    ('00200003', 3, '2008-01-20', '0001', '004',  500.00, NULL),
    ('00200003', 4, '2008-02-09', '0001', '004',  500.00, NULL),
    ('00200003', 5, '2008-02-25', '0001', '003', 3500.00, NULL),
    ('00200003', 6, '2008-03-11', '0001', '004',  500.00, NULL),
    ('00200003', 7, '2024-12-10', '0001', '008',    4.00, '00100002'),
    ('00200003', 8, '2024-12-11', '0001', '009',    4.00, '00100002'),
    -- 00300001 : 3 movimientos del dump (cuenta CANCELADA). No recibe movimientos
    --            nuevos: una cuenta que no esta ACTIVO rechaza la operacion
    --            con el codigo CTA-002.
    ('00300001', 1, '2008-01-07', '0002', '001', 5600.00, NULL),
    ('00300001', 2, '2008-01-18', '0002', '003', 1400.00, NULL),
    ('00300001', 3, '2008-01-25', '0002', '002', 7000.00, NULL)
AS n ON DUPLICATE KEY UPDATE
    dtt_movifecha      = n.dtt_movifecha,
    chr_emplcodigo     = n.chr_emplcodigo,
    chr_tipocodigo     = n.chr_tipocodigo,
    dec_moviimporte    = n.dec_moviimporte,
    chr_cuenreferencia = n.chr_cuenreferencia;

-- -----------------------------------------------------------------------------
-- 17) logsession: tabla de auditoria de runtime. Se garantiza vacia.
-- -----------------------------------------------------------------------------
DELETE FROM logsession;

-- -----------------------------------------------------------------------------
-- 18) Recalculo de la cuenta a partir de los movimientos.
--     dec_cuensaldo   = SUMA(INGRESO) - SUMA(SALIDA)
--     int_cuencontmov = numero de movimientos de la cuenta
--     Asi el saldo y el contador jamas quedan desalineados del libro (RB-02/RB-04).
-- -----------------------------------------------------------------------------
UPDATE cuenta c
SET c.dec_cuensaldo = (
        SELECT COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO'
                                 THEN m.dec_moviimporte
                                 ELSE -m.dec_moviimporte END), 0)
        FROM movimiento m
        JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo
        WHERE m.chr_cuencodigo = c.chr_cuencodigo),
    c.int_cuencontmov = (
        SELECT COUNT(*) FROM movimiento m WHERE m.chr_cuencodigo = c.chr_cuencodigo);

-- -----------------------------------------------------------------------------
-- 19) Comprobacion final de la semilla (debe devolver 6 filas y ninguna
--     diferencia entre el saldo guardado y el recalculado).
-- -----------------------------------------------------------------------------
SELECT c.chr_cuencodigo,
       c.vch_cuenestado,
       c.dec_cuensaldo,
       c.int_cuencontmov,
       (SELECT COUNT(*) FROM movimiento m WHERE m.chr_cuencodigo = c.chr_cuencodigo) AS movimientos_reales,
       (SELECT COALESCE(SUM(CASE WHEN t.vch_tipoaccion = 'INGRESO'
                                 THEN m.dec_moviimporte
                                 ELSE -m.dec_moviimporte END), 0)
        FROM movimiento m
        JOIN tipomovimiento t ON t.chr_tipocodigo = m.chr_tipocodigo
        WHERE m.chr_cuencodigo = c.chr_cuencodigo) AS saldo_calculado
FROM cuenta c
ORDER BY c.chr_cuencodigo;
