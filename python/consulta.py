#CONSULTA SIMPLES. UTILIZAR O consulta_e_registra.py

# import sqlite3

# DB = "inventario.db"

# def consultar(codigo):
#     conn = sqlite3.connect(DB)
#     cursor = conn.cursor()

#     # Busca em ativos
#     cursor.execute("""
#         SELECT id, patrimonio, usuario, tipo, modelo, hostname
#         FROM ativos
#         WHERE patrimonio = ?
#     """, (codigo,))
#     ativo = cursor.fetchone()

#     if ativo:
#         ativo_id = ativo[0]

#         print("\n✅ ATIVO ENCONTRADO")
#         print("Categoria: Ativo principal")
#         print(f"Patrimônio: {ativo[1]}")
#         print(f"Usuário: {ativo[2]}")
#         print(f"Tipo: {ativo[3]}")
#         print(f"Modelo: {ativo[4]}")
#         print(f"Hostname: {ativo[5]}")

#         cursor.execute("""
#             SELECT modelo, patrimonio, service_tag, posicao
#             FROM monitores
#             WHERE ativo_id = ?
#             ORDER BY posicao
#         """, (ativo_id,))
#         monitores = cursor.fetchall()

#         if monitores:
#             print("\n🖥️ Monitores vinculados:")
#             for m in monitores:
#                 print(
#                     f"- Posição {m[3]} | Modelo: {m[0]} | "
#                     f"Patrimônio: {m[1]} | Service Tag: {m[2]}"
#                 )
#         else:
#             print("\n⚠️ Nenhum monitor vinculado")

#         conn.close()
#         return

#     # Busca em monitores
#     cursor.execute("""
#         SELECT m.id, m.modelo, m.service_tag, m.patrimonio, m.posicao,
#                a.patrimonio, a.usuario, a.tipo, a.modelo, a.hostname
#         FROM monitores m
#         JOIN ativos a ON a.id = m.ativo_id
#         WHERE m.patrimonio = ?
#     """, (codigo,))
#     monitor = cursor.fetchone()

#     if monitor:
#         print("\n✅ MONITOR ENCONTRADO")
#         print("Categoria: Monitor vinculado")
#         print(f"Patrimônio do monitor: {monitor[3]}")
#         print(f"Modelo: {monitor[1]}")
#         print(f"Service Tag: {monitor[2]}")
#         print(f"Posição: Monitor {monitor[4]}")

#         print("\n🔗 Vinculado ao ativo:")
#         print(f"Patrimônio do ativo: {monitor[5]}")
#         print(f"Usuário: {monitor[6]}")
#         print(f"Tipo: {monitor[7]}")
#         print(f"Modelo: {monitor[8]}")
#         print(f"Hostname: {monitor[9]}")

#         conn.close()
#         return

#     print("\n❌ NÃO ENCONTRADO NA BASE")
#     conn.close()


# while True:
#     codigo = input("\n📷 Leia o patrimônio (ou 'sair'): ").strip()

#     if codigo.lower() == "sair":
#         break

#     consultar(codigo)

import sqlite3

conn = sqlite3.connect("inventario.db")
cursor = conn.cursor()

print("\n--- RESUMO ---")
cursor.execute("SELECT codigo, tipo_item, total_leituras FROM resumo_inventario")
for row in cursor.fetchall():
    print(row)

print("\n--- HISTÓRICO ---")
cursor.execute("SELECT codigo_lido, data_hora FROM conferencias")
for row in cursor.fetchall():
    print(row)

conn.close()