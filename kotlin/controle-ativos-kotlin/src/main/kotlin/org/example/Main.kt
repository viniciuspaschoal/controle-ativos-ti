package org.example

import org.example.db.Database
import org.example.service.AuditoriaService
import org.example.service.ConsultaRegistroService
import org.example.service.RelatorioService

fun main() {

    Database.inicializarBanco()

    val service = ConsultaRegistroService()
    val relatorioService = RelatorioService()

    println("=== SISTEMA DE INVENTARIO DE TI ===")

    while (true) {
        println("3 - Gerar relatorio")
        println("1 - Consulta e registra")
        println("2 - Auditoria")
        println("0 - Sair")

        print("Escolha: ")
        val opcao = readln()

        when (opcao) {
            "3" -> {
                relatorioService.gerarRelatorioAuditorias()
            }
            "2" -> {
                val auditoria = AuditoriaService()
                auditoria.iniciarAuditoria()
            }
            "1" -> {
                print("\nDigite o patrimonio: ")
                val codigo = readln().trim()
                service.consultarERegistrar(codigo)
            }
            "0" -> {
                println("Encerrando...")
                break
            }
            else -> println("Opcao invalida!")
        }
    }
}