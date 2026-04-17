package org.example.db

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private const val DB_PATH = "C:/Users/vinicius.paschoal/Documents/Codes/Controle de Ativos TI/controle-ativos-ti/python/inventario.db"
    private val JDBC_URL = "jdbc:sqlite:$DB_PATH"

    fun connect(): Connection {
        return DriverManager.getConnection(JDBC_URL)
    }
}