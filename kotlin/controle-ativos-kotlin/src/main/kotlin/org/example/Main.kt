package org.example

import org.example.service.AuditoriaService
import org.example.service.ConsultaRegistroService

fun main() {
    val service = ConsultaRegistroService()

    println("=== SISTEMA DE INVENTARIO DE TI ===")

    while (true) {
        println("1 - Consulta e registra")
        println("2 - Auditoria")
        println("0 - Sair")

        print("Escolha: ")
        val opcao = readln()

        when (opcao) {
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