import sqlite3
from datetime import datetime

DB = "inventario.db"
OPERADOR = "vinicius"
DISPOSITIVO = "ZEBRA01"

def registrar_resumo(cursor, codigo, tipo_item, ativo_id, monitor_id, usuario, hostname):
    agora = datetime.now().isoformat()

    cursor.execute("""
        SELECT id, total_leituras FROM resumo_inventario WHERE codigo = ?
    """, (codigo,))
    existente = cursor.fetchone()

    if existente:
        resumo_id, total = existente

        cursor.execute("""
            UPDATE resumo_inventario
            SET total_leituras = ?,
                ultima_leitura = ?
            WHERE id = ?
        """, (total + 1, agora, resumo_id))

        return True  # já existia

    else:
        cursor.execute("""
            INSERT INTO resumo_inventario (
                codigo, tipo_item, ativo_id, monitor_id,
                usuario, hostname,
                total_leituras, primeira_leitura, ultima_leitura, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            codigo, tipo_item, ativo_id, monitor_id,
            usuario, hostname,
            1, agora, agora, "OK"
        ))

        return False  # primeira vez


def registrar_conferencia(cursor, codigo, tipo_item, encontrado, ativo_id, monitor_id, usuario, hostname):
    cursor.execute("""
        INSERT INTO conferencias (
            codigo_lido, tipo_item, encontrado_na_base,
            ativo_id, monitor_id,
            usuario_relacionado, hostname_relacionado,
            data_hora, operador, dispositivo
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        codigo, tipo_item, encontrado,
        ativo_id, monitor_id,
        usuario, hostname,
        datetime.now().isoformat(),
        OPERADOR,
        DISPOSITIVO
    ))


def consultar_e_registrar(codigo):
    conn = sqlite3.connect(DB)
    cursor = conn.cursor()

    # 1 - Buscar ativo
    cursor.execute("""
        SELECT id, patrimonio, usuario, tipo, modelo, hostname
        FROM ativos
        WHERE patrimonio = ?
    """, (codigo,))
    ativo = cursor.fetchone()

    if ativo:
        ativo_id = ativo[0]

        print("\n✅ ATIVO ENCONTRADO")
        print(f"Patrimônio: {ativo[1]}")
        print(f"Usuário: {ativo[2]}")
        print(f"Tipo: {ativo[3]}")
        print(f"Modelo: {ativo[4]}")
        print(f"Hostname: {ativo[5]}")

        # Registrar histórico
        registrar_conferencia(
            cursor, codigo, "ATIVO", 1,
            ativo_id, None,
            ativo[2], ativo[5]
        )

        # Registrar resumo
        ja_existia = registrar_resumo(
            cursor, codigo, "ATIVO",
            ativo_id, None,
            ativo[2], ativo[5]
        )

        if ja_existia:
            print("⚠️ Já lido anteriormente")

        conn.commit()
        conn.close()
        return

    # 2 - Buscar monitor
    cursor.execute("""
        SELECT m.id, m.modelo, m.service_tag, m.patrimonio,
               a.id, a.usuario, a.hostname
        FROM monitores m
        JOIN ativos a ON a.id = m.ativo_id
        WHERE m.patrimonio = ?
    """, (codigo,))
    monitor = cursor.fetchone()

    if monitor:
        monitor_id = monitor[0]
        ativo_id = monitor[4]

        print("\n🖥️ MONITOR ENCONTRADO")
        print(f"Patrimônio: {monitor[3]}")
        print(f"Modelo: {monitor[1]}")
        print(f"Service Tag: {monitor[2]}")
        print(f"Usuário: {monitor[5]}")

        registrar_conferencia(
            cursor, codigo, "MONITOR", 1,
            ativo_id, monitor_id,
            monitor[5], monitor[6]
        )

        ja_existia = registrar_resumo(
            cursor, codigo, "MONITOR",
            ativo_id, monitor_id,
            monitor[5], monitor[6]
        )

        if ja_existia:
            print("⚠️ Já lido anteriormente")

        conn.commit()
        conn.close()
        return

    # 3 - Não encontrado
    print("\n❌ NÃO ENCONTRADO")

    registrar_conferencia(
        cursor, codigo, "DESCONHECIDO", 0,
        None, None,
        None, None
    )

    ja_existia = registrar_resumo(
        cursor, codigo, "DESCONHECIDO",
        None, None,
        None, None
    )

    if ja_existia:
        print("⚠️ Já lido anteriormente")

    conn.commit()
    conn.close()


# LOOP
while True:
    codigo = input("\n📷 Leia o patrimônio (ou 'sair'): ").strip()

    if codigo.lower() == "sair":
        break

    consultar_e_registrar(codigo)