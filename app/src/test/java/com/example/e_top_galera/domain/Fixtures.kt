package com.example.e_top_galera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail

/** Utilitários compartilhados pelos testes de domínio. */
object Fixtures {

    /** Lista com respostas "<prefixo>1".."<prefixo>10". */
    fun listaSimples(id: String, prefixo: String = id): ListaTop10 = ListaTop10(
        id = id,
        tema = "Tema $id",
        itens = (1..10).map { ItemLista(posicao = it, resposta = "$prefixo$it") },
    )

    val paises = ListaTop10(
        id = "paises-mais-populosos",
        tema = "Países mais populosos do mundo",
        itens = listOf(
            ItemLista(1, "Índia", listOf("India")),
            ItemLista(2, "China"),
            ItemLista(3, "Estados Unidos", listOf("EUA", "USA")),
            ItemLista(4, "Indonésia"),
            ItemLista(5, "Paquistão"),
            ItemLista(6, "Nigéria"),
            ItemLista(7, "Brasil"),
            ItemLista(8, "Bangladesh"),
            ItemLista(9, "Rússia"),
            ItemLista(10, "México"),
        ),
    )

    val temas: List<ListaTop10> = listOf(paises) + (1..20).map { listaSimples("lista$it") }

    /** Sorteio determinístico: sempre o primeiro disponível. */
    val primeiro: (List<ListaTop10>) -> ListaTop10 = { it.first() }

    fun <T> Resultado<T>.ok(): T {
        val v = valorOuNull()
        if (v == null) fail("Esperava Sucesso, veio ${erroOuNull()}")
        return v!!
    }

    fun <T> Resultado<T>.erro(): ErroRegra {
        val e = erroOuNull()
        assertNotNull("Esperava Falha, veio Sucesso: ${valorOuNull()}", e)
        return e!!
    }

    fun partida(
        quantidade: Int = 3,
        configuracao: ConfiguracaoJogo = ConfiguracaoJogo(),
    ): EstadoPartida = RegrasJogo.iniciarPartida(
        (0 until quantidade).map { "Jogador$it" }, configuracao,
    ).ok()

    fun comRodada(
        quantidade: Int = 3,
        configuracao: ConfiguracaoJogo = ConfiguracaoJogo(),
        tema: ListaTop10 = paises,
    ): EstadoPartida =
        RegrasJogo.iniciarRodada(partida(quantidade, configuracao), listOf(tema), primeiro).ok()

    /** Novo estado com os jogadores alterados (para preparar cenários). */
    fun EstadoPartida.comJogador(indice: Int, vidas: Int? = null, pontos: Int? = null): EstadoPartida {
        val j = jogadores[indice]
        val novos = jogadores.toMutableList()
        novos[indice] = j.copy(vidas = vidas ?: j.vidas, pontos = pontos ?: j.pontos)
        return copy(jogadores = novos)
    }

    fun EstadoPartida.palpita(texto: String) = RegrasJogo.palpitar(this, texto).ok()
    fun EstadoPartida.passa() = RegrasJogo.passar(this).ok()
    fun EstadoPartida.contesta(indice: Int) = RegrasJogo.contestar(this, indice).ok()
    fun EstadoPartida.novaRodada(temas: List<ListaTop10> = Fixtures.temas) =
        RegrasJogo.iniciarRodada(this, temas, primeiro).ok()

    fun assertVez(esperado: Int, estado: EstadoPartida) =
        assertEquals("jogador da vez", esperado, estado.indiceJogadorDaVez)
}
