import pandas as pd
import sqlite3
from datetime import datetime
import os

# CONFIG
ARQUIVO_EXCEL = "GESTÃO TI.xlsm"
ABA = "Inventário 08-2025"
DB = "inventario.db"

# Se quiser recriar o banco do zero a cada importação:
RECRIAR_BANCO = False

if RECRIAR_BANCO and os.path.exists(DB):
    os.remove(DB)

# Ler Excel
df = pd.read_excel(ARQUIVO_EXCEL, sheet_name=ABA)

# Conectar SQLite
conn = sqlite3.connect(DB)
cursor = conn.cursor()

# Melhorar integridade
cursor.execute("PRAGMA foreign_keys = ON;")

# Criar tabelas
cursor.execute("""
CREATE TABLE IF NOT EXISTS ativos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_interno TEXT,
    patrimonio TEXT,
    tipo TEXT,
    marca TEXT,
    modelo TEXT,
    service_tag TEXT,
    departamento TEXT,
    usuario TEXT,
    email_usuario TEXT,
    hostname TEXT,
    dominio TEXT,
    ip_cabo TEXT,
    mac_cabo TEXT,
    ip_wifi TEXT,
    mac_wifi TEXT,
    sistema_operacional TEXT,
    antivirus TEXT,
    processador TEXT,
    memoria TEXT,
    disco1 TEXT,
    disco2 TEXT,
    disco3 TEXT,
    nota_fiscal TEXT,
    data_compra TEXT,
    garantia_tempo TEXT,
    garantia_validade TEXT,
    garantia_status TEXT,
    observacoes TEXT,
    data_importacao TEXT
)
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS monitores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ativo_id INTEGER,
    modelo TEXT,
    service_tag TEXT,
    patrimonio TEXT,
    posicao INTEGER,
    FOREIGN KEY (ativo_id) REFERENCES ativos(id)
)
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS conferencias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_lido TEXT NOT NULL,
    tipo_item TEXT,
    encontrado_na_base INTEGER NOT NULL,
    ativo_id INTEGER,
    monitor_id INTEGER,
    usuario_relacionado TEXT,
    hostname_relacionado TEXT,
    data_hora TEXT NOT NULL,
    operador TEXT,
    dispositivo TEXT,
    observacao TEXT,
    FOREIGN KEY (ativo_id) REFERENCES ativos(id),
    FOREIGN KEY (monitor_id) REFERENCES monitores(id)
)
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS resumo_inventario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT UNIQUE,
    tipo_item TEXT,
    ativo_id INTEGER,
    monitor_id INTEGER,
    usuario TEXT,
    hostname TEXT,
    total_leituras INTEGER DEFAULT 1,
    primeira_leitura TEXT,
    ultima_leitura TEXT,
    status TEXT,
    FOREIGN KEY (ativo_id) REFERENCES ativos(id),
    FOREIGN KEY (monitor_id) REFERENCES monitores(id)
)
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS auditorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ativo_id INTEGER NOT NULL,
    codigo_ativo TEXT NOT NULL,
    usuario TEXT,
    hostname TEXT,
    data_hora_inicio TEXT NOT NULL,
    data_hora_fim TEXT,
    operador TEXT,
    dispositivo TEXT,
    status_final TEXT,
    observacao TEXT,
    FOREIGN KEY (ativo_id) REFERENCES ativos(id)
)
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS auditoria_itens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    auditoria_id INTEGER NOT NULL,
    tipo_registro TEXT NOT NULL,
    monitor_esperado_id INTEGER,
    patrimonio_esperado TEXT,
    patrimonio_lido TEXT,
    service_tag_lida TEXT,
    resultado TEXT NOT NULL,
    observacao TEXT,
    data_hora TEXT NOT NULL,
    FOREIGN KEY (auditoria_id) REFERENCES auditorias(id),
    FOREIGN KEY (monitor_esperado_id) REFERENCES monitores(id)
)
""")

# Índices úteis para consulta
cursor.execute("""
CREATE INDEX IF NOT EXISTS idx_ativos_patrimonio
ON ativos(patrimonio)
""")

cursor.execute("""
CREATE INDEX IF NOT EXISTS idx_monitores_patrimonio
ON monitores(patrimonio)
""")

cursor.execute("""
CREATE INDEX IF NOT EXISTS idx_conferencias_codigo_lido
ON conferencias(codigo_lido)
""")

cursor.execute("""
CREATE INDEX IF NOT EXISTS idx_resumo_codigo
ON resumo_inventario(codigo)
""")

conn.commit()

# Função para tratar valores
def safe(val):
    if pd.isna(val):
        return None

    texto = str(val).strip()

    if texto == "":
        return None

    if texto in ["-", "N/A", "NA", "nan", "None"]:
        return None

    return texto

# Opcional:
# se quiser limpar dados importados sem apagar histórico de conferência,
# descomente estas linhas:
#
# cursor.execute("DELETE FROM monitores")
# cursor.execute("DELETE FROM ativos")
# conn.commit()

# Inserir dados
total_ativos = 0
total_monitores = 0

for _, row in df.iterrows():
    cursor.execute("""
    INSERT INTO ativos (
        codigo_interno, patrimonio, tipo, marca, modelo, service_tag,
        departamento, usuario, email_usuario, hostname, dominio,
        ip_cabo, mac_cabo, ip_wifi, mac_wifi,
        sistema_operacional, antivirus,
        processador, memoria, disco1, disco2, disco3,
        nota_fiscal, data_compra, garantia_tempo,
        garantia_validade, garantia_status,
        observacoes, data_importacao
    )
    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
    """, (
        safe(row['ID']),
        safe(row['Patrimonio']),
        safe(row['Tipo']),
        safe(row['Marca']),
        safe(row['Modelo']),
        safe(row['ServiceTag Pc']),
        safe(row['Departamento']),
        safe(row['Usuário']),
        safe(row['e-mail de Usuário']),
        safe(row['Nome de Host']),
        safe(row['Domínio']),
        safe(row['Endereço IP Cabo']),
        safe(row['MAC Adress Cabo']),
        safe(row['Endereço IP Wifi']),
        safe(row['MAC Adress Wifi']),
        safe(row['Sistema Operacional']),
        safe(row['Antivírus']),
        safe(row['Processador']),
        safe(row['Memória']),
        safe(row['Disco 1']),
        safe(row['Disco 2']),
        safe(row['Disco 3']),
        safe(row['Nota Fiscal']),
        safe(row['Data de Compra']),
        safe(row['Tempo de Garantia']),
        safe(row['Validade da Garantia']),
        safe(row['Status da Garantia']),
        safe(row['Observações Adicionais']),
        datetime.now().isoformat()
    ))

    ativo_id = cursor.lastrowid
    total_ativos += 1

    # Monitor 1
    if safe(row['Monitor 1']) or safe(row['Pat. Monitor 1']):
        cursor.execute("""
        INSERT INTO monitores (ativo_id, modelo, service_tag, patrimonio, posicao)
        VALUES (?,?,?,?,?)
        """, (
            ativo_id,
            safe(row['Monitor 1']),
            safe(row['ServiceTag1']),
            safe(row['Pat. Monitor 1']),
            1
        ))
        total_monitores += 1

    # Monitor 2
    if safe(row['Monitor 2']) or safe(row['Pat. Monitor 2']):
        cursor.execute("""
        INSERT INTO monitores (ativo_id, modelo, service_tag, patrimonio, posicao)
        VALUES (?,?,?,?,?)
        """, (
            ativo_id,
            safe(row['Monitor 2']),
            safe(row['ServiceTag2']),
            safe(row['Pat. Monitor 2']),
            2
        ))
        total_monitores += 1

conn.commit()
conn.close()

print("Importação concluída com sucesso!")
print(f"Ativos importados: {total_ativos}")
print(f"Monitores importados: {total_monitores}")
print(f"Banco gerado: {DB}")