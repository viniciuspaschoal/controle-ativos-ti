package org.example.service

import org.example.db.Database
import java.sql.Connection

class ConsultaService {

    fun consultarCodigo(codigo: String) {
        Database.connect().use { conn ->

            val sqlAtivo = """
                SELECT id, patrimonio, usuario, tipo, modelo, hostname
                FROM ativos
                WHERE patrimonio = ?
            """.trimIndent()

            conn.prepareStatement(sqlAtivo).use { stmt ->
                stmt.setString(1, codigo)

                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val ativoId = rs.getInt("id")
                        val patrimonio = rs.getString("patrimonio")
                        val usuario = rs.getString("usuario")
                        val tipo = rs.getString("tipo")
                        val modelo = rs.getString("modelo")
                        val hostname = rs.getString("hostname")

                        println("\n✅ ATIVO ENCONTRADO")
                        println("Patrimônio: $patrimonio")
                        println("Usuário: $usuario")
                        println("Tipo: $tipo")
                        println("Modelo: $modelo")
                        println("Hostname: $hostname")

                        buscarMonitores(conn, ativoId)
                        return
                    }
                }
            }

            val sqlMonitor = """
                SELECT m.id, m.modelo, m.service_tag, m.patrimonio,
                       a.patrimonio AS patrimonio_ativo,
                       a.usuario, a.hostname
                FROM monitores m
                JOIN ativos a ON a.id = m.ativo_id
                WHERE m.patrimonio = ?
            """.trimIndent()

            conn.prepareStatement(sqlMonitor).use { stmt ->
                stmt.setString(1, codigo)

                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val patrimonioMonitor = rs.getString("patrimonio")
                        val modelo = rs.getString("modelo")
                        val serviceTag = rs.getString("service_tag")
                        val patrimonioAtivo = rs.getString("patrimonio_ativo")
                        val usuario = rs.getString("usuario")
                        val hostname = rs.getString("hostname")

                        println("\n🖥️ MONITOR ENCONTRADO")
                        println("Patrimônio: $patrimonioMonitor")
                        println("Modelo: $modelo")
                        println("Service Tag: $serviceTag")
                        println("Vinculado ao ativo: $patrimonioAtivo")
                        println("Usuário: $usuario")
                        println("Hostname: $hostname")
                        return
                    }
                }
            }

            println("\n❌ NÃO ENCONTRADO NA BASE")
        }
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
                    val patrimonio = rs.getString("patrimonio") ?: "SEM PATRIMÔNIO"
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
}