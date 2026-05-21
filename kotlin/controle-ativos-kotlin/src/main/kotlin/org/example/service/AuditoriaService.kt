package org.example.service

import org.example.db.Database
import java.sql.Connection
import java.sql.Statement
import java.time.LocalDateTime

class AuditoriaService {

    fun iniciarAuditoria() {
        Database.connect().use { conn ->

            println("\n=== MODO AUDITORIA ===")

            while (true) {
                println("\nLeia o patrimonio do ativo/monitor para iniciar auditoria")
                println("Digite 'sair' para voltar ao menu")

                print("> ")
                val codigoInicial = readln().trim()

                if (codigoInicial.lowercase() == "sair") {
                    break
                }

                val ativoDireto = buscarAtivo(conn, codigoInicial)
                val monitorInicial = buscarMonitorPorPatrimonio(conn, codigoInicial)

                val ativo = when {
                    ativoDireto != null -> ativoDireto
                    monitorInicial != null -> buscarAtivoPorId(conn, monitorInicial.ativoId)
                    else -> null
                }

                if (ativo == null) {
                    println("Ativo ou monitor nao encontrado.")
                    continue
                }

                executarAuditoria(
                    conn = conn,
                    ativo = ativo,
                    monitorInicial = monitorInicial
                )
            }
        }
    }

    private fun executarAuditoria(
        conn: Connection,
        ativo: AtivoAuditoriaDTO,
        monitorInicial: MonitorAuditoriaDTO?
    ) {
        println("\nAtivo encontrado")
        println("Patrimonio: ${ativo.patrimonio}")
        println("Usuario: ${ativo.usuario}")
        println("Hostname: ${ativo.hostname}")

        val auditoriaId = criarAuditoria(conn, ativo)

        val monitoresEsperados = buscarMonitores(conn, ativo.id)
        val esperadosPorPatrimonio = monitoresEsperados
            .filter { it.patrimonio != null }
            .associateBy { it.patrimonio!! }

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

        if (monitorInicial != null && monitorInicial.patrimonio != null) {
            val patrimonioInicial = monitorInicial.patrimonio

            if (patrimonioInicial in esperadosPorPatrimonio.keys) {
                lidos.add(patrimonioInicial)
                lidosCorretos.add(patrimonioInicial)

                salvarItemAuditoria(
                    conn = conn,
                    auditoriaId = auditoriaId,
                    codigoEsperado = monitorInicial.patrimonio,
                    modeloEsperado = monitorInicial.modelo,
                    codigoLido = monitorInicial.patrimonio,
                    modeloLido = monitorInicial.modelo,
                    tipo = "OK",
                    observacao = "Monitor inicial esperado confirmado"
                )

                println("\nMonitor inicial confirmado: $patrimonioInicial")
                mostrarProgresso(lidosCorretos.size, esperadosPorPatrimonio.size)
            }
        }

        println("\nLeia os monitores fisicos.")
        println("Digite 'fim' para encerrar esta auditoria.")

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

            val monitorLido = buscarMonitorPorPatrimonio(conn, leitura)

            lidos.add(leitura)

            if (leitura in esperadosPorPatrimonio.keys) {
                val esperado = esperadosPorPatrimonio[leitura]!!

                lidosCorretos.add(leitura)

                salvarItemAuditoria(
                    conn = conn,
                    auditoriaId = auditoriaId,
                    codigoEsperado = esperado.patrimonio,
                    modeloEsperado = esperado.modelo,
                    codigoLido = leitura,
                    modeloLido = monitorLido?.modelo ?: esperado.modelo,
                    tipo = "OK",
                    observacao = "Monitor esperado confirmado"
                )

                println("Monitor esperado confirmado: $leitura")
            } else {
                println("DIVERGENCIA: monitor nao esperado neste ativo: $leitura")

                print("Confirmar como divergencia? (s/n): ")
                val confirmar = readln().trim().lowercase()

                if (confirmar == "s" || confirmar == "sim") {
                    lidosExtras.add(leitura)

                    salvarItemAuditoria(
                        conn = conn,
                        auditoriaId = auditoriaId,
                        codigoEsperado = null,
                        modeloEsperado = null,
                        codigoLido = leitura,
                        modeloLido = monitorLido?.modelo,
                        tipo = "EXTRA",
                        observacao = "Monitor nao esperado neste ativo"
                    )

                    println("Divergencia confirmada.")
                } else {
                    lidos.remove(leitura)
                    println("Leitura descartada. Leia novamente se necessario.")
                    continue
                }
            }

            mostrarProgresso(lidosCorretos.size, esperadosPorPatrimonio.size)

            if (esperadosPorPatrimonio.isNotEmpty() && lidosCorretos.size == esperadosPorPatrimonio.size) {
                println("Todos os monitores esperados foram lidos.")
                println("Continue lendo para registrar extras ou digite 'fim' para finalizar.")
            }
        }

        val faltantes = monitoresEsperados.filter {
            it.patrimonio != null && it.patrimonio !in lidosCorretos
        }

        faltantes.forEach {
            salvarItemAuditoria(
                conn = conn,
                auditoriaId = auditoriaId,
                codigoEsperado = it.patrimonio,
                modeloEsperado = it.modelo,
                codigoLido = null,
                modeloLido = null,
                tipo = "FALTANTE",
                observacao = "Monitor esperado nao foi encontrado na auditoria"
            )
        }

        println("\n=== RESULTADO DA AUDITORIA ===")
        println("Ativo: ${ativo.patrimonio}")
        println("Usuario: ${ativo.usuario}")
        println("Hostname: ${ativo.hostname}")

        println("\nMonitores OK:")
        if (lidosCorretos.isEmpty()) println("- Nenhum")
        else lidosCorretos.forEach { println("- $it") }

        println("\nMonitores faltantes:")
        if (faltantes.isEmpty()) println("- Nenhum")
        else faltantes.forEach { println("- ${it.patrimonio} (${it.modelo})") }

        println("\nMonitores extras/divergentes:")
        if (lidosExtras.isEmpty()) println("- Nenhum")
        else lidosExtras.forEach { println("- $it") }

        val statusFinal = if (faltantes.isEmpty() && lidosExtras.isEmpty()) {
            "OK"
        } else {
            "DIVERGENTE"
        }

        finalizarAuditoria(conn, auditoriaId, statusFinal)

        println("\nStatus final: $statusFinal")
        println("Auditoria salva no banco com ID: $auditoriaId")
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

    private fun buscarAtivoPorId(conn: Connection, ativoId: Int): AtivoAuditoriaDTO? {
        val sql = """
            SELECT id, patrimonio, usuario, hostname
            FROM ativos
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ativoId)

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

    private fun buscarMonitorPorPatrimonio(conn: Connection, patrimonio: String): MonitorAuditoriaDTO? {
        val sql = """
            SELECT id, ativo_id, patrimonio, modelo
            FROM monitores
            WHERE patrimonio = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, patrimonio)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return MonitorAuditoriaDTO(
                        id = rs.getInt("id"),
                        ativoId = rs.getInt("ativo_id"),
                        patrimonio = rs.getString("patrimonio"),
                        modelo = rs.getString("modelo")
                    )
                }
            }
        }

        return null
    }

    private fun buscarMonitores(conn: Connection, ativoId: Int): List<MonitorAuditoriaDTO> {
        val lista = mutableListOf<MonitorAuditoriaDTO>()

        val sql = """
            SELECT id, ativo_id, patrimonio, modelo
            FROM monitores
            WHERE ativo_id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ativoId)

            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    lista.add(
                        MonitorAuditoriaDTO(
                            id = rs.getInt("id"),
                            ativoId = rs.getInt("ativo_id"),
                            patrimonio = rs.getString("patrimonio"),
                            modelo = rs.getString("modelo")
                        )
                    )
                }
            }
        }

        return lista
    }

    private fun criarAuditoria(
        conn: Connection,
        ativo: AtivoAuditoriaDTO
    ): Int {
        val sql = """
            INSERT INTO auditorias (
                ativo_id,
                patrimonio,
                usuario,
                hostname,
                data_inicio,
                status
            )
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setInt(1, ativo.id)
            stmt.setString(2, ativo.patrimonio)
            stmt.setString(3, ativo.usuario)
            stmt.setString(4, ativo.hostname)
            stmt.setString(5, LocalDateTime.now().toString())
            stmt.setString(6, "EM_ANDAMENTO")

            stmt.executeUpdate()

            stmt.generatedKeys.use { rs ->
                if (rs.next()) {
                    return rs.getInt(1)
                }
            }
        }

        throw Exception("Erro ao criar auditoria")
    }

    private fun salvarItemAuditoria(
        conn: Connection,
        auditoriaId: Int,
        codigoEsperado: String?,
        modeloEsperado: String?,
        codigoLido: String?,
        modeloLido: String?,
        tipo: String,
        observacao: String
    ) {
        val sql = """
            INSERT INTO auditoria_itens (
                auditoria_id,
                codigo_esperado,
                modelo_esperado,
                codigo_lido,
                modelo_lido,
                tipo,
                observacao,
                data_hora
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, auditoriaId)
            stmt.setString(2, codigoEsperado)
            stmt.setString(3, modeloEsperado)
            stmt.setString(4, codigoLido)
            stmt.setString(5, modeloLido)
            stmt.setString(6, tipo)
            stmt.setString(7, observacao)
            stmt.setString(8, LocalDateTime.now().toString())

            stmt.executeUpdate()
        }
    }

    private fun finalizarAuditoria(
        conn: Connection,
        auditoriaId: Int,
        status: String
    ) {
        val sql = """
            UPDATE auditorias
            SET status = ?, data_fim = ?
            WHERE id = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, status)
            stmt.setString(2, LocalDateTime.now().toString())
            stmt.setInt(3, auditoriaId)

            stmt.executeUpdate()
        }
    }
}

data class AtivoAuditoriaDTO(
    val id: Int,
    val patrimonio: String,
    val usuario: String?,
    val hostname: String?
)

data class MonitorAuditoriaDTO(
    val id: Int,
    val ativoId: Int,
    val patrimonio: String?,
    val modelo: String?
)