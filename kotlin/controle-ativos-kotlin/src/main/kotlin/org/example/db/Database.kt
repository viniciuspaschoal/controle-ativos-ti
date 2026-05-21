package org.example.db

import java.sql.Connection
import java.sql.DriverManager

object Database {

    private const val DB_PATH =
        "C:/Users/vinicius.paschoal/Documents/Codes/Controle de Ativos TI/controle-ativos-ti/python/inventario.db"

    private val JDBC_URL = "jdbc:sqlite:$DB_PATH"

    fun connect(): Connection {
        val conn = DriverManager.getConnection(JDBC_URL)
        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA busy_timeout = 5000")
        }
        return conn
    }

    fun inicializarBanco() {

        connect().use { conn ->

            conn.createStatement().use { stmt ->

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS auditorias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        ativo_id INTEGER,
                        patrimonio TEXT,
                        usuario TEXT,
                        hostname TEXT,
                        data_inicio TEXT,
                        data_fim TEXT,
                        status TEXT,
                        FOREIGN KEY (ativo_id) REFERENCES ativos(id)
                    )
                """.trimIndent())

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS auditoria_itens (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        auditoria_id INTEGER,
                        codigo_lido TEXT,
                        tipo TEXT,
                        observacao TEXT,
                        data_hora TEXT,
                        FOREIGN KEY (auditoria_id) REFERENCES auditorias(id)
                    )
                """.trimIndent())
            }
        }
    }
}