import sqlite3
from datetime import datetime

DB = "inventario.db"
OPERADOR = "vinicius"
DISPOSITIVO = "ZEBRA01"


def conectar():
    conn = sqlite3.connect(DB)
    conn.execute("PRAGMA foreign_keys = ON;")
    return conn


def buscar_ativo_por_patrimonio(cursor, codigo):
    cursor.execute("""
        SELECT id, patrimonio, usuario, tipo, modelo, hostname
        FROM ativos
        WHERE patrimonio = ?
    """, (codigo,))
    return cursor.fetchone()


def buscar_monitores_do_ativo(cursor, ativo_id):
    cursor.execute("""
        SELECT id, modelo, service_tag, patrimonio, posicao
        FROM monitores
        WHERE ativo_id = ?
        ORDER BY posicao
    """, (ativo_id,))
    return cursor.fetchall()


def buscar_monitor_por_patrimonio(cursor, codigo):
    cursor.execute("""
        SELECT m.id, m.modelo, m.service_tag, m.patrimonio, m.posicao,
               a.id, a.patrimonio, a.usuario, a.hostname
        FROM monitores m
        JOIN ativos a ON a.id = m.ativo_id
        WHERE m.patrimonio = ?
    """, (codigo,))
    return cursor.fetchone()


def criar_auditoria(cursor, ativo):
    agora = datetime.now().isoformat()

    cursor.execute("""
        INSERT INTO auditorias (
            ativo_id, codigo_ativo, usuario, hostname,
            data_hora_inicio, operador, dispositivo, status_final
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        ativo[0],         # ativo_id
        ativo[1],         # patrimonio
        ativo[2],         # usuario
        ativo[5],         # hostname
        agora,
        OPERADOR,
        DISPOSITIVO,
        "EM_ANDAMENTO"
    ))

    return cursor.lastrowid


def registrar_item_auditoria(
    cursor,
    auditoria_id,
    tipo_registro,
    monitor_esperado_id,
    patrimonio_esperado,
    patrimonio_lido,
    service_tag_lida,
    resultado,
    observacao=None
):
    cursor.execute("""
        INSERT INTO auditoria_itens (
            auditoria_id,
            tipo_registro,
            monitor_esperado_id,
            patrimonio_esperado,
            patrimonio_lido,
            service_tag_lida,
            resultado,
            observacao,
            data_hora
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        auditoria_id,
        tipo_registro,
        monitor_esperado_id,
        patrimonio_esperado,
        patrimonio_lido,
        service_tag_lida,
        resultado,
        observacao,
        datetime.now().isoformat()
    ))


def finalizar_auditoria(cursor, auditoria_id, status_final, observacao=None):
    cursor.execute("""
        UPDATE auditorias
        SET data_hora_fim = ?, status_final = ?, observacao = ?
        WHERE id = ?
    """, (
        datetime.now().isoformat(),
        status_final,
        observacao,
        auditoria_id
    ))


def mostrar_ativo(ativo):
    print("\n=== AUDITORIA INICIADA ===")
    print(f"Patrimônio: {ativo[1]}")
    print(f"Usuário: {ativo[2]}")
    print(f"Tipo: {ativo[3]}")
    print(f"Modelo: {ativo[4]}")
    print(f"Hostname: {ativo[5]}")


def mostrar_esperados(monitores):
    print("\nMonitores esperados:")
    if not monitores:
        print("- Nenhum monitor vinculado")
        return

    for m in monitores:
        patrimonio = m[3] if m[3] else "SEM PATRIMÔNIO"
        print(
            f"- Posição {m[4]} | Modelo: {m[1]} | "
            f"Patrimônio: {patrimonio} | Service Tag: {m[2]}"
        )


def obter_status_final(esperados, lidos_ok, divergencias_confirmadas):
    esperados_ids = {m[0] for m in esperados if m[0] is not None}

    if divergencias_confirmadas:
        return "DIVERGENTE"

    if esperados_ids and esperados_ids.issubset(lidos_ok):
        return "OK"

    if esperados_ids and not esperados_ids.issubset(lidos_ok):
        return "INCOMPLETA"

    return "OK"


def registrar_faltantes(cursor, auditoria_id, esperados, lidos_ok):
    for m in esperados:
        monitor_id = m[0]
        patrimonio_esperado = m[3]
        if monitor_id not in lidos_ok:
            registrar_item_auditoria(
                cursor=cursor,
                auditoria_id=auditoria_id,
                tipo_registro="MONITOR_ESPERADO",
                monitor_esperado_id=monitor_id,
                patrimonio_esperado=patrimonio_esperado,
                patrimonio_lido=None,
                service_tag_lida=None,
                resultado="NAO_LIDO",
                observacao="Monitor esperado não foi lido durante a auditoria"
            )


def auditar_ativo():
    conn = conectar()
    cursor = conn.cursor()

    while True:
        codigo_ativo = input(
            "\nLeia o patrimônio do ativo para iniciar auditoria (ou 'sair'): "
        ).strip()

        if codigo_ativo.lower() == "sair":
            conn.close()
            return

        ativo = buscar_ativo_por_patrimonio(cursor, codigo_ativo)

        if not ativo:
            print("❌ Esse código não é um ativo principal cadastrado.")
            continue

        auditoria_id = criar_auditoria(cursor, ativo)
        conn.commit()

        monitores_esperados = buscar_monitores_do_ativo(cursor, ativo[0])
        mostrar_ativo(ativo)
        mostrar_esperados(monitores_esperados)

        esperados_por_id = {m[0]: m for m in monitores_esperados}
        esperados_por_patrimonio = {
            m[3]: m for m in monitores_esperados if m[3] is not None
        }

        lidos_ok = set()
        lidos_nesta_auditoria = set()
        divergencias_confirmadas = False

        if monitores_esperados:
            print("\nDigite os patrimônios dos monitores.")
            print("Quando quiser encerrar este item, digite 'fim'.")
        else:
            print("\nEsse ativo não possui monitores cadastrados.")
            print("Digite 'fim' para encerrar ou leia algo para registrar divergência.")

        while True:
            codigo = input("\nMonitor lido (ou 'fim'): ").strip()

            if codigo.lower() == "fim":
                registrar_faltantes(cursor, auditoria_id, monitores_esperados, lidos_ok)
                status_final = obter_status_final(
                    monitores_esperados,
                    lidos_ok,
                    divergencias_confirmadas
                )
                finalizar_auditoria(cursor, auditoria_id, status_final)
                conn.commit()

                print(f"\n✅ Auditoria finalizada com status: {status_final}")
                break

            if codigo in lidos_nesta_auditoria:
                print("⚠️ Esse monitor já foi lido nesta auditoria.")
                registrar_item_auditoria(
                    cursor=cursor,
                    auditoria_id=auditoria_id,
                    tipo_registro="MONITOR_LIDO",
                    monitor_esperado_id=None,
                    patrimonio_esperado=None,
                    patrimonio_lido=codigo,
                    service_tag_lida=None,
                    resultado="REPETIDO",
                    observacao="Monitor já havia sido lido nesta auditoria"
                )
                conn.commit()
                continue

            monitor_lido = buscar_monitor_por_patrimonio(cursor, codigo)

            if monitor_lido:
                monitor_id = monitor_lido[0]
                service_tag_lida = monitor_lido[2]
                patrimonio_lido = monitor_lido[3]
                ativo_vinculado_id = monitor_lido[5]
                patrimonio_ativo_vinculado = monitor_lido[6]

                if ativo_vinculado_id == ativo[0]:
                    if monitor_id in lidos_ok:
                        print("⚠️ Esse monitor esperado já foi confirmado antes.")
                        registrar_item_auditoria(
                            cursor=cursor,
                            auditoria_id=auditoria_id,
                            tipo_registro="MONITOR_LIDO",
                            monitor_esperado_id=monitor_id,
                            patrimonio_esperado=patrimonio_lido,
                            patrimonio_lido=patrimonio_lido,
                            service_tag_lida=service_tag_lida,
                            resultado="REPETIDO",
                            observacao="Monitor esperado já confirmado anteriormente"
                        )
                    else:
                        print("✅ Monitor esperado confirmado.")
                        lidos_ok.add(monitor_id)
                        lidos_nesta_auditoria.add(codigo)

                        registrar_item_auditoria(
                            cursor=cursor,
                            auditoria_id=auditoria_id,
                            tipo_registro="MONITOR_ESPERADO",
                            monitor_esperado_id=monitor_id,
                            patrimonio_esperado=patrimonio_lido,
                            patrimonio_lido=patrimonio_lido,
                            service_tag_lida=service_tag_lida,
                            resultado="OK",
                            observacao="Monitor esperado lido corretamente"
                        )

                        if monitores_esperados and len(lidos_ok) == len(esperados_por_id):
                            print("ℹ️ Todos os monitores esperados já foram lidos.")
                            print("Digite 'fim' para encerrar ou continue lendo para validar extras/divergências.")

                    conn.commit()
                    continue

                print("❌ DIVERGÊNCIA: monitor pertence a outro ativo.")
                print(f"Vinculado ao ativo: {patrimonio_ativo_vinculado}")
                print("1 - Ler novamente")
                print("2 - Confirmar divergência")

                escolha = input("Escolha: ").strip()

                if escolha == "2":
                    divergencias_confirmadas = True
                    lidos_nesta_auditoria.add(codigo)

                    registrar_item_auditoria(
                        cursor=cursor,
                        auditoria_id=auditoria_id,
                        tipo_registro="MONITOR_EXTRA",
                        monitor_esperado_id=None,
                        patrimonio_esperado=None,
                        patrimonio_lido=patrimonio_lido,
                        service_tag_lida=service_tag_lida,
                        resultado="DIVERGENTE",
                        observacao=f"Monitor vinculado a outro ativo ({patrimonio_ativo_vinculado})"
                    )
                    conn.commit()
                else:
                    print("↩️ Ok, faça a leitura novamente.")

                continue

            print("❌ DIVERGÊNCIA: monitor não cadastrado.")
            print("1 - Ler novamente")
            print("2 - Confirmar divergência")

            escolha = input("Escolha: ").strip()

            if escolha == "2":
                divergencias_confirmadas = True
                lidos_nesta_auditoria.add(codigo)

                registrar_item_auditoria(
                    cursor=cursor,
                    auditoria_id=auditoria_id,
                    tipo_registro="MONITOR_EXTRA",
                    monitor_esperado_id=None,
                    patrimonio_esperado=None,
                    patrimonio_lido=codigo,
                    service_tag_lida=None,
                    resultado="NAO_CADASTRADO",
                    observacao="Código lido não existe na base de monitores"
                )
                conn.commit()
            else:
                print("↩️ Ok, faça a leitura novamente.")


def main():
    print("=== MODO AUDITORIA ===")
    auditar_ativo()


if __name__ == "__main__":
    main()