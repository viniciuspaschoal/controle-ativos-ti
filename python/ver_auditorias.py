import sqlite3

DB = "inventario.db"


def listar_auditorias(cursor):
    cursor.execute("""
        SELECT id, codigo_ativo, usuario, hostname,
               data_hora_inicio, data_hora_fim, status_final
        FROM auditorias
        ORDER BY id DESC
    """)
    return cursor.fetchall()


def mostrar_detalhe(cursor, auditoria_id):
    print(f"\n=== DETALHE AUDITORIA {auditoria_id} ===")

    cursor.execute("""
        SELECT codigo_ativo, usuario, hostname,
               data_hora_inicio, data_hora_fim, status_final
        FROM auditorias
        WHERE id = ?
    """, (auditoria_id,))

    cab = cursor.fetchone()

    if not cab:
        print("Auditoria não encontrada.")
        return

    print(f"Ativo: {cab[0]}")
    print(f"Usuário: {cab[1]}")
    print(f"Hostname: {cab[2]}")
    print(f"Início: {cab[3]}")
    print(f"Fim: {cab[4]}")
    print(f"Status: {cab[5]}")

    print("\n--- ITENS ---")

    cursor.execute("""
        SELECT tipo_registro,
               patrimonio_esperado,
               patrimonio_lido,
               resultado,
               observacao
        FROM auditoria_itens
        WHERE auditoria_id = ?
    """, (auditoria_id,))

    itens = cursor.fetchall()

    for item in itens:
        print(f"\nTipo: {item[0]}")
        print(f"Esperado: {item[1]}")
        print(f"Lido: {item[2]}")
        print(f"Resultado: {item[3]}")
        print(f"Obs: {item[4]}")


def main():
    conn = sqlite3.connect(DB)
    cursor = conn.cursor()

    while True:
        print("\n=== AUDITORIAS ===")

        auditorias = listar_auditorias(cursor)

        if not auditorias:
            print("Nenhuma auditoria encontrada.")
            break

        for a in auditorias:
            print(
                f"[{a[0]}] Ativo: {a[1]} | Usuário: {a[2]} | Status: {a[6]}"
            )

        escolha = input("\nDigite o ID da auditoria ou 'sair': ").strip()

        if escolha.lower() == "sair":
            break

        if not escolha.isdigit():
            print("Entrada inválida.")
            continue

        mostrar_detalhe(cursor, int(escolha))

    conn.close()


if __name__ == "__main__":
    main()