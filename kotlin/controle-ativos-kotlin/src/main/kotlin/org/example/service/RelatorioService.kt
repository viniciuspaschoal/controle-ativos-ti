package org.example.service

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.example.db.Database
import java.io.FileOutputStream
import java.sql.Connection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RelatorioService {

    fun gerarRelatorioAuditorias() {
        Database.connect().use { conn ->

            val workbook = XSSFWorkbook()

            criarAbaAuditorias(workbook, conn)
            criarAbaItens(workbook, conn)
            criarAbaDivergencias(workbook, conn)
            criarAbaFaltantes(workbook, conn)

            val dataHora = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

            val nomeArquivo = "relatorio_auditorias_$dataHora.xlsx"

            FileOutputStream(nomeArquivo).use { output ->
                workbook.write(output)
            }

            workbook.close()

            println("\nRelatorio gerado com sucesso:")
            println(nomeArquivo)
        }
    }

    private fun criarAbaAuditorias(workbook: XSSFWorkbook, conn: Connection) {
        val sheet = workbook.createSheet("Auditorias")

        val headers = listOf(
            "ID",
            "Ativo ID",
            "Patrimonio",
            "Usuario",
            "Hostname",
            "Data Inicio",
            "Data Fim",
            "Status"
        )

        criarCabecalho(sheet, headers)

        val sql = """
            SELECT id, ativo_id, patrimonio, usuario, hostname, data_inicio, data_fim, status
            FROM auditorias
            ORDER BY id DESC
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                var rowIndex = 1

                while (rs.next()) {
                    val row = sheet.createRow(rowIndex++)

                    row.createCell(0).setCellValue(rs.getInt("id").toDouble())
                    row.createCell(1).setCellValue(rs.getInt("ativo_id").toDouble())
                    row.createCell(2).setCellValue(rs.getString("patrimonio") ?: "")
                    row.createCell(3).setCellValue(rs.getString("usuario") ?: "")
                    row.createCell(4).setCellValue(rs.getString("hostname") ?: "")
                    row.createCell(5).setCellValue(rs.getString("data_inicio") ?: "")
                    row.createCell(6).setCellValue(rs.getString("data_fim") ?: "")
                    row.createCell(7).setCellValue(rs.getString("status") ?: "")
                }
            }
        }

        ajustarColunas(sheet, headers.size)
    }

    private fun criarAbaItens(workbook: XSSFWorkbook, conn: Connection) {
        val sheet = workbook.createSheet("Itens")

        val headers = listOf(
            "ID",
            "Auditoria ID",
            "Codigo Lido",
            "Tipo",
            "Observacao",
            "Data Hora"
        )

        criarCabecalho(sheet, headers)

        val sql = """
            SELECT id, auditoria_id, codigo_lido, tipo, observacao, data_hora
            FROM auditoria_itens
            ORDER BY id DESC
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                var rowIndex = 1

                while (rs.next()) {
                    val row = sheet.createRow(rowIndex++)

                    row.createCell(0).setCellValue(rs.getInt("id").toDouble())
                    row.createCell(1).setCellValue(rs.getInt("auditoria_id").toDouble())
                    row.createCell(2).setCellValue(rs.getString("codigo_lido") ?: "")
                    row.createCell(3).setCellValue(rs.getString("tipo") ?: "")
                    row.createCell(4).setCellValue(rs.getString("observacao") ?: "")
                    row.createCell(5).setCellValue(rs.getString("data_hora") ?: "")
                }
            }
        }

        ajustarColunas(sheet, headers.size)
    }

    private fun criarAbaDivergencias(workbook: XSSFWorkbook, conn: Connection) {
        val sheet = workbook.createSheet("Divergencias")

        val headers = listOf(
            "Auditoria ID",
            "Patrimonio Ativo",
            "Usuario",
            "Hostname",
            "Codigo Lido",
            "Tipo",
            "Observacao",
            "Data Hora"
        )

        criarCabecalho(sheet, headers)

        val sql = """
            SELECT 
                a.id AS auditoria_id,
                a.patrimonio,
                a.usuario,
                a.hostname,
                i.codigo_lido,
                i.tipo,
                i.observacao,
                i.data_hora
            FROM auditoria_itens i
            JOIN auditorias a ON a.id = i.auditoria_id
            WHERE i.tipo = 'EXTRA'
            ORDER BY i.id DESC
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                var rowIndex = 1

                while (rs.next()) {
                    val row = sheet.createRow(rowIndex++)

                    row.createCell(0).setCellValue(rs.getInt("auditoria_id").toDouble())
                    row.createCell(1).setCellValue(rs.getString("patrimonio") ?: "")
                    row.createCell(2).setCellValue(rs.getString("usuario") ?: "")
                    row.createCell(3).setCellValue(rs.getString("hostname") ?: "")
                    row.createCell(4).setCellValue(rs.getString("codigo_lido") ?: "")
                    row.createCell(5).setCellValue(rs.getString("tipo") ?: "")
                    row.createCell(6).setCellValue(rs.getString("observacao") ?: "")
                    row.createCell(7).setCellValue(rs.getString("data_hora") ?: "")
                }
            }
        }

        ajustarColunas(sheet, headers.size)
    }

    private fun criarAbaFaltantes(workbook: XSSFWorkbook, conn: Connection) {
        val sheet = workbook.createSheet("Faltantes")

        val headers = listOf(
            "Auditoria ID",
            "Patrimonio Ativo",
            "Usuario",
            "Hostname",
            "Codigo Faltante",
            "Tipo",
            "Observacao",
            "Data Hora"
        )

        criarCabecalho(sheet, headers)

        val sql = """
            SELECT 
                a.id AS auditoria_id,
                a.patrimonio,
                a.usuario,
                a.hostname,
                i.codigo_lido,
                i.tipo,
                i.observacao,
                i.data_hora
            FROM auditoria_itens i
            JOIN auditorias a ON a.id = i.auditoria_id
            WHERE i.tipo = 'FALTANTE'
            ORDER BY i.id DESC
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.executeQuery().use { rs ->
                var rowIndex = 1

                while (rs.next()) {
                    val row = sheet.createRow(rowIndex++)

                    row.createCell(0).setCellValue(rs.getInt("auditoria_id").toDouble())
                    row.createCell(1).setCellValue(rs.getString("patrimonio") ?: "")
                    row.createCell(2).setCellValue(rs.getString("usuario") ?: "")
                    row.createCell(3).setCellValue(rs.getString("hostname") ?: "")
                    row.createCell(4).setCellValue(rs.getString("codigo_lido") ?: "")
                    row.createCell(5).setCellValue(rs.getString("tipo") ?: "")
                    row.createCell(6).setCellValue(rs.getString("observacao") ?: "")
                    row.createCell(7).setCellValue(rs.getString("data_hora") ?: "")
                }
            }
        }

        ajustarColunas(sheet, headers.size)
    }

    private fun criarCabecalho(sheet: org.apache.poi.ss.usermodel.Sheet, headers: List<String>) {
        val row = sheet.createRow(0)

        headers.forEachIndexed { index, titulo ->
            row.createCell(index).setCellValue(titulo)
        }
    }

    private fun ajustarColunas(sheet: org.apache.poi.ss.usermodel.Sheet, totalColunas: Int) {
        for (i in 0 until totalColunas) {
            sheet.autoSizeColumn(i)
        }
    }
}