package org.example.service

import org.example.db.Database
import java.sql.Connection
import java.sql.Types
import java.time.LocalDateTime

class ConsultaRegistroService {

    fun consultarERegistrar(codigo: String) {
        Database.connect().use { conn ->

            val ativo = buscarAtivo(conn, codigo)
            if (ativo != null) {
                println("\n✅ ATIVO ENCONTRADO")
                println("Patrimônio: ${ativo.patrimonio}")
                println("Usuário: ${ativo.usuario}")
                println("Tipo: ${ativo.tipo}")
                println("Modelo: ${ativo.modelo}")
                println("Hostname: ${ativo.hostname}")

                buscarMonitores(conn, ativo.id)

                registrarConferencia(
                    conn = conn,
                    codigo = codigo,
                    tipoItem = "ATIVO",
                    encontrado = 1,
                    ativoId = ativo.id,
                    monitorId = null,
                    usuario = ativo.usuario,
                    hostname = ativo.hostname
                )

                val jaExistia = registrarResumo(
                    conn = conn,
                    codigo = codigo,
                    tipoItem = "ATIVO",
                    ativoId = ativo.id,
                    monitorId = null,
                    usuario = ativo.usuario,
                    hostname = ativo.hostname
                )

                if (jaExistia) {
                    println("⚠️ Já lido anteriormente")
                }

                return
            }

            val monitor = buscarMonitor(conn, codigo)
            if (monitor != null) {
                println("\n🖥️ MONITOR ENCONTRADO")
                println("Patrimônio: ${monitor.patrimonio}")
                println("Modelo: ${monitor.modelo}")
                println("Service Tag: ${monitor.serviceTag}")
                println("Vinculado ao ativo: ${monitor.patrimonioAtivo}")
                println("Usuário: ${monitor.usuario}")
                println("Hostname: ${monitor.hostname}")

                registrarConferencia(
                    conn = conn,
                    codigo = codigo,
                    tipoItem = "MONITOR",
                    encontrado = 1,
                    ativoId = monitor.ativoId,
                    monitorId = monitor.id,
                    usuario = monitor.usuario,
                    hostname = monitor.hostname
                )

                val jaExistia = registrarResumo(
                    conn = conn,
                    codigo = codigo,
                    tipoItem = "MONITOR",
                    ativoId = monitor.ativoId,
                    monitorId = monitor.id,
                    usuario = monitor.usuario,
                    hostname = monitor.hostname
                )

                if (jaExistia) {
                    println("⚠️ Já lido anteriormente")
                }

                return
            }

            println("\n❌ NÃO ENCONTRADO NA BASE")

            registrarConferencia(
                conn = conn,
                codigo = codigo,
                tipoItem = "DESCONHECIDO",
                encontrado = 0,
                ativoId = null,
                monitorId = null,
                usuario = null,
                hostname = null
            )

            val jaExistia = registrarResumo(
                conn = conn,
                codigo = codigo,
                tipoItem = "DESCONHECIDO",
                ativoId = null,
                monitorId = null,
                usuario = null,
                hostname = null
            )

            if (jaExistia) {
                println("⚠️ Já lido anteriormente")
            }
        }
    }

    private fun buscarAtivo(conn: Connection, codigo: String): AtivoDTO? {
        val sql = """
            SELECT id, patrimonio, usuario, tipo, modelo, hostname
            FROM ativos
            WHERE patrimonio = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, codigo)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return AtivoDTO(
                        id = rs.getInt("id"),
                        patrimonio = rs.getString("patrimonio"),
                        usuario = rs.getString("usuario"),
                        tipo = rs.getString("tipo"),
                        modelo = rs.getString("modelo"),
                        hostname = rs.getString("hostname")
                    )
                }
            }
        }

        return null
    }

    private fun buscarMonitor(conn: Connection, codigo: String): MonitorDTO? {
        val sql = """
            SELECT m.id, m.modelo, m.service_tag, m.patrimonio,
                   a.id AS ativo_id,
                   a.patrimonio AS patrimonio_ativo,
                   a.usuario, a.hostname
            FROM monitores m
            JOIN ativos a ON a.id = m.ativo_id
            WHERE m.patrimonio = ?
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, codigo)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return MonitorDTO(
                        id = rs.getInt("id"),
                        modelo = rs.getString("modelo"),
                        serviceTag = rs.getString("service_tag"),
                        patrimonio = rs.getString("patrimonio"),
                        ativoId = rs.getInt("ativo_id"),
                        patrimonioAtivo = rs.getString("patrimonio_ativo"),
                        usuario = rs.getString("usuario"),
                        hostname = rs.getString("hostname")
                    )
                }
            }
        }

        return null
    }

    private fun buscarMonitores(conn: Connection, ativoId: Int) {
        val sql = """
            SELECT modelo, patrimonio, service_tag, posicao
            FROM monitores
            WHERE ativo_id = ?
            ORDER BY posicao
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, ativoId)

            stmt.executeQuery().use { rs ->
                var temMonitor = false

                while (rs.next()) {
                    if (!temMonitor) {
                        println("\n🖥️ Monitores vinculados:")
                        temMonitor = true
                    }

                    val modelo = rs.getString("modelo")
                    val patrimonio = rs.getString("patrimonio") ?: "SEM PATRIMONIO"
                    val serviceTag = rs.getString("service_tag")
                    val posicao = rs.getInt("posicao")

                    println("- Posição $posicao | Modelo: $modelo | Patrimônio: $patrimonio | Service Tag: $serviceTag")
                }

                if (!temMonitor) {
                    println("\n⚠️ Nenhum monitor vinculado")
                }
            }
        }
    }

    private fun registrarConferencia(
        conn: Connection,
        codigo: String,
        tipoItem: String,
        encontrado: Int,
        ativoId: Int?,
        monitorId: Int?,
        usuario: String?,
        hostname: String?
    ) {
        val sql = """
            INSERT INTO conferencias (
                codigo_lido, tipo_item, encontrado_na_base,
                ativo_id, monitor_id,
                usuario_relacionado, hostname_relacionado,
                data_hora, operador, dispositivo
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, codigo)
            stmt.setString(2, tipoItem)
            stmt.setInt(3, encontrado)

            if (ativoId != null) stmt.setInt(4, ativoId) else stmt.setNull(4, Types.INTEGER)
            if (monitorId != null) stmt.setInt(5, monitorId) else stmt.setNull(5, Types.INTEGER)

            stmt.setString(6, usuario)
            stmt.setString(7, hostname)
            stmt.setString(8, LocalDateTime.now().toString())
            stmt.setString(9, "vinicius")
            stmt.setString(10, "KOTLIN-CONSOLE")

            stmt.executeUpdate()
        }
    }

    private fun registrarResumo(
        conn: Connection,
        codigo: String,
        tipoItem: String,
        ativoId: Int?,
        monitorId: Int?,
        usuario: String?,
        hostname: String?
    ): Boolean {
        val agora = LocalDateTime.now().toString()

        val sqlBusca = """
            SELECT id, total_leituras
            FROM resumo_inventario
            WHERE codigo = ?
        """.trimIndent()

        conn.prepareStatement(sqlBusca).use { stmt ->
            stmt.setString(1, codigo)

            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val resumoId = rs.getInt("id")
                    val totalLeituras = rs.getInt("total_leituras")

                    val sqlUpdate = """
                        UPDATE resumo_inventario
                        SET total_leituras = ?,
                            ultima_leitura = ?
                        WHERE id = ?
                    """.trimIndent()

                    conn.prepareStatement(sqlUpdate).use { updateStmt ->
                        updateStmt.setInt(1, totalLeituras + 1)
                        updateStmt.setString(2, agora)
                        updateStmt.setInt(3, resumoId)
                        updateStmt.executeUpdate()
                    }

                    return true
                }
            }
        }

        val sqlInsert = """
            INSERT INTO resumo_inventario (
                codigo, tipo_item, ativo_id, monitor_id,
                usuario, hostname,
                total_leituras, primeira_leitura, ultima_leitura, status
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sqlInsert).use { stmt ->
            stmt.setString(1, codigo)
            stmt.setString(2, tipoItem)

            if (ativoId != null) stmt.setInt(3, ativoId) else stmt.setNull(3, Types.INTEGER)
            if (monitorId != null) stmt.setInt(4, monitorId) else stmt.setNull(4, Types.INTEGER)

            stmt.setString(5, usuario)
            stmt.setString(6, hostname)
            stmt.setInt(7, 1)
            stmt.setString(8, agora)
            stmt.setString(9, agora)
            stmt.setString(10, "OK")

            stmt.executeUpdate()
        }

        return false
    }
}

data class AtivoDTO(
    val id: Int,
    val patrimonio: String,
    val usuario: String?,
    val tipo: String?,
    val modelo: String?,
    val hostname: String?
)

data class MonitorDTO(
    val id: Int,
    val modelo: String?,
    val serviceTag: String?,
    val patrimonio: String?,
    val ativoId: Int,
    val patrimonioAtivo: String?,
    val usuario: String?,
    val hostname: String?
)