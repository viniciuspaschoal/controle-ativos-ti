package org.example

import org.example.service.ConsultaService

fun main(){

    val consultaService = ConsultaService()

    println("=== SISTEMA DE INVENTÁRIO DE TI ===")

    while (true) {
        println("\n1 - Consulta")
        println("0 - Sair")

        print("Escolha: ")
        val opcao = readln()

        when (opcao) {
            "1" -> {
                print("\nDigite o patrimônio")
                val codigo = readln().trim()
                consultaService.consultarCodigo(codigo)
            }

            "0" -> {
                println("Encerrando...")
                break
            }
            else -> println("Opção inválida!")
        }
    }
}