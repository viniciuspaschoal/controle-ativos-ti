package org.example

import org.example.service.ConsultaRegistroService

fun main() {
    val service = ConsultaRegistroService()

    println("=== SISTEMA DE INVENTARIO DE TI ===")

    while (true) {
        println("\n1 - Consulta e registra")
        println("0 - Sair")

        print("Escolha: ")
        val opcao = readln()

        when (opcao) {
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