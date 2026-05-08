package org.example.service

import org.example.db.Database
import java.sql.Connection

class AuditoriaService {

    fun iniciarAuditoria() {
        Database.connect().use { conn ->

            println("\n=== MODO AUDITORIA ===")

            print("Leia o patrimonio do ativo: ")
            val codigo = readln().trim()

            val ativo = buscarAtivo(conn, codigo)

            if (ativo == null) {
                println("Ativo nao encontrado")
                return
            }

            println("\nAtivo encontrado")
            println("Patrimonio: ${ativo.patrimonio}")
            println("Usuario: ${ativo.usuario}")
            println("Hostname: ${ativo.hostname}")

            val monitoresEsperados = buscarMonitores(conn, ativo.id)
            val esperados = monitoresEsperados.mapNotNull { it.patrimonio }.toSet()

            println("\nMonitores esperados:")
            if (monitoresEsperados.isEmpty()) {
                println("- Nenhum monitor vinculado")
            } else {
                monitoresEsperados.forEach {
                    val patrimonio = it.patrimonio ?: "SEM PATRIMONIO"
                    println("- $patrimonio (${it.modelo})")
                }
            }

            val lidos = mutableSetOf<String>()
            val lidosCorretos = mutableSetOf<String>()
            val lidosExtras = mutableSetOf<String>()

            println("\nLeia os monitores fisicos.")
            println("Digite 'fim' para encerrar manualmente.")

            while (true) {
                print("> ")
                val leitura = readln().trim()

                if (leitura.lowercase() == "fim") {
                    break
                }

                if (leitura.isBlank()) {
                    println("Leitura vazia. Tente novamente.")
                    continue
                }

                if (leitura in lidos) {
                    println("Ja lido nesta auditoria: $leitura")
                    continue
                }

                lidos.add(leitura)

                if (leitura in esperados) {
                    lidosCorretos.add(leitura)
                    println("Monitor esperado confirmado: $leitura")
                } else {
                    println("DIVERGENCIA: monitor nao esperado neste ativo: $leitura")

                    print("Confirmar como divergencia? (s/n): ")
                    val confirmar = readln().trim().lowercase()

                    if (confirmar == "s" || confirmar == "sim") {
                        lidosExtras.add(leitura)
                        println("Divergencia confirmada.")
                    } else {
                        lidos.remove(leitura)
                        println("Leitura descartada. Leia novamente se necessario.")
                        continue
                    }
                }

                mostrarProgresso(lidosCorretos.size, esperados.size)

                if (esperados.isNotEmpty() && lidosCorretos.size == esperados.size) {
                    println("Todos os monitores esperados foram lidos.")
                    println("Continue lendo para registrar extras ou digite 'fim' para finalizar.")
                }
            }

            val faltantes = esperados.filter { it !in lidosCorretos }

            println("\n=== RESULTADO DA AUDITORIA ===")
            println("Ativo: ${ativo.patrimonio}")
            println("Usuario: ${ativo.usuario}")
            println("Hostname: ${ativo.hostname}")

            println("\nMonitores OK:")
            if (lidosCorretos.isEmpty()) {
                println("- Nenhum")
            } else {
                lidosCorretos.forEach { println("- $it") }
            }

            println("\nMonitores faltantes:")
            if (faltantes.isEmpty()) {
                println("- Nenhum")
            } else {
                faltantes.forEach { println("- $it") }
            }

            println("\nMonitores extras/divergentes:")
            if (lidosExtras.isEmpty()) {
                println("- Nenhum")
            } else {
                lidosExtras.forEach { println("- $it") }
            }

            val statusFinal = when {
                faltantes.isEmpty() && lidosExtras.isEmpty() -> "OK"
                else -> "DIVERGENTE"
            }

            println("\nStatus final: $statusFinal")
        }
    }

    private fun mostrarProgresso(confirmados: Int, total: Int) {
        println("Progresso: $confirmados/$total")
    }

    private fun buscarAtivo(conn: Connection, codigo: String): AtivoAuditoriaDTO? {
        val sql = """
            SELECT id, patrimonio, usuario, hostname
            FROM ativos
            WHERE patrimonio = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, codigo)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return AtivoAuditoriaDTO(
                        id = rs.getInt("id"),
                        patrimonio = rs.getString("patrimonio"),
                        usuario = rs.getString("usuario"),
                        hostname = rs.getString("hostname")
                    )
                }
            }
        }

        return null
    }

    private fun buscarMonitores(conn: Connection, ativoId: Int): List<MonitorAuditoriaDTO> {
        val lista = mutableListOf<MonitorAuditoriaDTO>()

        val sql = """
            SELECT patrimonio, modelo
            FROM monitores
            WHERE ativo_id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ativoId)

            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    lista.add(
                        MonitorAuditoriaDTO(
                            patrimonio = rs.getString("patrimonio"),
                            modelo = rs.getString("modelo")
                        )
                    )
                }
            }
        }

        return lista
    }
}

data class AtivoAuditoriaDTO(
    val id: Int,
    val patrimonio: String,
    val usuario: String?,
    val hostname: String?
)

data class MonitorAuditoriaDTO(
    val patrimonio: String?,
    val modelo: String?
)