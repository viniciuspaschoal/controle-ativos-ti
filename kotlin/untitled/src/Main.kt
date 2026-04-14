//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    println("=== SISTEMA DE INVENTÁRIO ===")

    while (true){
        println("\n1 - Consulta")
        println("2 - Auditoria")
        println("0 - Sair")

        print("Escolha: ")
        val opcao = readln()

        when (opcao) {
            "1" -> println("Modo Consulta")
            "2" -> println("Modo Auditoria")
            "0" -> break
            else -> println("Opção inválida")
        }
    }
}