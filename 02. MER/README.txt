================================================================================
02. MER - MODELO ENTIDAD-RELACION DE EURECABANK
Proyecto: TI2.1 RESTFUL_JAVA_CONBDD_EURECABANK_GR03
================================================================================

1. QUE ES EL MER
--------------------------------------------------------------------------------
El Modelo Entidad-Relacion (MER) describe el modelo de datos del sistema
EURECABANK en tres niveles de abstraccion, cada uno en su propia carpeta y con
su propio proyecto de PowerDesigner:

  01. CONCEPTUAL  -> MC_EUREKABANK.cdm   (CDM) - el modelo conceptual
  02. LOGICO      -> ML_EUREKABANK.ldm   (LDM) - el modelo logico
  03. FISICO      -> MF_EUREKABANK.pdm   (PDM) - el modelo fisico

La fuente de verdad de los datos es el DDL de la base de datos
"03. BDD\docker\init\01_estructura.sql" (MySQL 8.4, esquema "eurekabank",
17 tablas) y el CONTRATO CONGELADO (01. UML\00. CONTRATO). Ninguna tabla,
columna, tipo o restriccion de este MER es inventada: el modelo fisico se
comprueba tabla por tabla y columna por columna contra ese DDL.

Los tres PNG se exportan a "05. DOCUMENTACION\img\" para incrustarlos en el
ERS, el ECUD y el manual:
  MC_EUREKABANK.png, ML_EUREKABANK.png, MF_EUREKABANK.png


2. QUE CONTIENE CADA MODELO
--------------------------------------------------------------------------------
A) 02. MER\01. CONCEPTUAL\MC_EUREKABANK.cdm  (proyecto: 01_MODELO_CONCEPTUAL.prj)
   Modelo conceptual (CDM). Contiene las 17 entidades del negocio con sus
   atributos de NEGOCIO (nombres significativos, no las claves tecnicas
   chr_*, vch_*, dec_*), un identificador natural por entidad (ID_<ENTIDAD>),
   18 relaciones con sus cardinalidades y la autorrelacion logica de la
   cuenta referida. Entidades: CLIENTE, EMPLEADO, USUARIO, SUCURSAL, MONEDA,
   CUENTA, MOVIMIENTO, TIPO_MOVIMIENTO, ASIGNADO, MODULO, PERMISO, PARAMETRO,
   COSTO_MANTENIMIENTO, COSTO_MOVIMIENTO, INTERES_MENSUAL, CONTADOR,
   LOG_SESION.

B) 02. MER\02. LOGICO\ML_EUREKABANK.ldm  (proyecto: 02_MODELO_LOGICO.prj)
   Modelo logico (LDM). 17 entidades con los nombres de la guia en mayusculas
   (ASIGNADO, CARGO_MANTENIMIENTO, CLIENTE, CONTADOR, COSTO_MOVIMIENTO, CUENTA,
   EMPLEADO, INTERES_MENSUAL, LOG_SESSION, MODULO, MONEDA, MOVIMIENTO,
   PARAMETRO, PERMISO, SUCURSAL, TIPO_MOVIMIENTO, USUARIO) y los 78 atributos
   con el nombre de columna EXACTO de la BDD. Tipos de dato logicos:
   string(n), integer, decimal(12,2), date, datetime. Incluye clave primaria
   (PK_<ENTIDAD>, compuesta donde el DDL la tiene), regla de nulabilidad
   (Mandatory) y 17 relaciones con cardinalidad, roles y el join que declara
   la clave foranea logica.

C) 02. MER\03. FISICO\MF_EUREKABANK.pdm  (proyecto: 03_MODELO_FISICO.prj)
   Modelo fisico (PDM) con destino MySQL. 17 tablas, 78 columnas, tipos
   exactos de MySQL 8.4 (char(n), varchar(n), decimal(12,2), int, date,
   datetime), obligatoriedad, valores por defecto, AUTO_INCREMENT, clave
   primaria con su nombre, 16 claves foraneas con nombre de restriccion y sus
   reglas ON DELETE/ON UPDATE RESTRICT, 15 indices (con U_Usuario_vch_emplusuario
   unico) y los 8 CHECK de estado y de importe, mas los comentarios de tabla
   y de columna del DDL.

   Nota sobre el destino: PowerDesigner 16.5 solo trae el modulo MySQL 5.0
   (Resource Files\DBMS\mysql50.xdb). Los tipos usados (char, varchar,
   decimal, int, date, datetime) tienen la misma sintaxis y el mismo
   comportamiento en MySQL 8.4, por lo que el PDM es equivalente al DDL.

   Regla de normalizacion documentada: en MySQL, "DEFAULT NULL" sobre una
   columna nullable equivale a no declarar valor por defecto; el PDM no
   declara ese default y el validador lo trata como equivalente.


3. COMO SE ABRE CADA UNO
--------------------------------------------------------------------------------
Opcion A - Workspace (recomendado):
  1. Abrir PowerDesigner 16.5 (32 bits) desde
     "C:\Program Files (x86)\Sybase\PowerDesigner 16".
  2. File > Open Workspace y elegir "RESTFUL_JAVA_CONBDD_GR03.sws" en la raiz
     del proyecto. El workspace ya tiene registrados los tres .prj y los
     tres modelos con rutas relativas.

Opcion B - Proyecto suelto:
  File > Open y elegir cualquiera de estos archivos .prj:
     02. MER\01. CONCEPTUAL\01_MODELO_CONCEPTUAL.prj
     02. MER\02. LOGICO\02_MODELO_LOGICO.prj
     02. MER\03. FISICO\03_MODELO_FISICO.prj

Opcion C - Modelo suelto:
  File > Open y elegir el .cdm / .ldm / .pdm directamente.

Todos los .prj, el .sws y los modelos usan rutas RELATIVAS: el proyecto se
puede mover de carpeta sin romper las referencias.


4. COMO SE REGENERA
--------------------------------------------------------------------------------
Los tres modelos se construyeron en PowerDesigner 16.5 (32 bits) y quedaron
verificados contra el DDL de la BDD ("03. BDD\docker\init\01_estructura.sql"):

  - El PDM (fisico) tiene 17 tablas con los tipos exactos de MySQL 8.4 del
    DDL: PK, FK con nombre de restriccion, indices, CHECK de estado, valores
    por defecto y comentarios.
  - El LDM (logico) tiene las mismas 17 entidades con atributos de tipo logico,
    PK, FK y cardinalidades.
  - El CDM (conceptual) tiene las entidades de negocio y sus relaciones.

Si se modifica el DDL hay que actualizar el PDM a mano en PowerDesigner y
volver a comprobar que coincide con "03. BDD\docker\init\01_estructura.sql".

5. FICHEROS DE ESTA CARPETA
--------------------------------------------------------------------------------
  README.txt                       este fichero
  01. CONCEPTUAL\                 modelo conceptual (CDM) + su proyecto .prj
  02. LOGICO\                     modelo logico (LDM) + su proyecto .prj
  03. FISICO\                     modelo fisico (PDM) + su proyecto .prj

  Los PNG de los tres modelos estan en "05. DOCUMENTACION\img\" y se
  referencian desde el ERS, el ECUD y el manual tecnico.
5. FICHEROS DE ESTA CARPETA
--------------------------------------------------------------------------------
  README.txt                       este fichero
  01. CONCEPTUAL\                 modelo conceptual (CDM) + su proyecto .prj
  02. LOGICO\                     modelo logico (LDM) + su proyecto .prj
  03. FISICO\                     modelo fisico (PDM) + su proyecto .prj

Los tres modelos estan registrados en el workspace "RESTFUL_JAVA_CONBDD_GR03.sws"
con rutas relativas, y sus imagenes van incrustadas en el ERS, el ECUD y el
manual tecnico.
================================================================================
