package com.example.e_top_galera.domain

import com.example.e_top_galera.domain.Fixtures.erro
import com.example.e_top_galera.domain.Fixtures.ok
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguracaoEInicioTest {

    @Test
    fun configuracaoPadrao_segueRegrasDaVersao1() {
        val c = ConfiguracaoJogo()
        assertEquals(3, c.vidasIniciais)
        assertEquals(4, c.pontosParaVencer)
        assertEquals(2, c.minimoJogadores)
        assertEquals(10, c.maximoJogadores)
    }

    @Test(expected = IllegalArgumentException::class)
    fun configuracao_rejeitaVidasZero() { ConfiguracaoJogo(vidasIniciais = 0) }

    @Test(expected = IllegalArgumentException::class)
    fun configuracao_rejeitaPontosZero() { ConfiguracaoJogo(pontosParaVencer = 0) }

    @Test(expected = IllegalArgumentException::class)
    fun configuracao_rejeitaMinimoMenorQue2() { ConfiguracaoJogo(minimoJogadores = 1) }

    @Test(expected = IllegalArgumentException::class)
    fun configuracao_rejeitaMaximoMenorQueMinimo() {
        ConfiguracaoJogo(minimoJogadores = 5, maximoJogadores = 4)
    }

    @Test
    fun iniciarPartida_todosComecamCom3VidasE0Pontos() {
        val e = RegrasJogo.iniciarPartida(listOf("Ana", "Bia", "Caio")).ok()
        assertEquals(3, e.jogadores.size)
        e.jogadores.forEach {
            assertEquals(3, it.vidas)
            assertEquals(0, it.pontos)
            assertTrue(it.vivo)
        }
        assertEquals(listOf("Ana", "Bia", "Caio"), e.jogadores.map { it.nome })
        assertNull(e.rodada)
        assertFalse(e.encerrada)
        assertNull(e.indiceVencedor)
    }

    @Test
    fun iniciarPartida_minimoDe2() {
        RegrasJogo.iniciarPartida(listOf("A", "B")).ok()
        assertEquals(
            ErroRegra.QuantidadeJogadoresInvalida(1, 2, 10),
            RegrasJogo.iniciarPartida(listOf("A")).erro(),
        )
        assertEquals(
            ErroRegra.QuantidadeJogadoresInvalida(0, 2, 10),
            RegrasJogo.iniciarPartida(emptyList()).erro(),
        )
    }

    @Test
    fun iniciarPartida_maximoDe10() {
        RegrasJogo.iniciarPartida((1..10).map { "J$it" }).ok()
        assertEquals(
            ErroRegra.QuantidadeJogadoresInvalida(11, 2, 10),
            RegrasJogo.iniciarPartida((1..11).map { "J$it" }).erro(),
        )
    }

    @Test
    fun iniciarPartida_nomeVazioOuSoEspacos() {
        assertEquals(ErroRegra.NomeVazio, RegrasJogo.iniciarPartida(listOf("Ana", "")).erro())
        assertEquals(ErroRegra.NomeVazio, RegrasJogo.iniciarPartida(listOf("Ana", "   ")).erro())
    }

    @Test
    fun iniciarPartida_nomesRepetidosNormalizados() {
        val erro = RegrasJogo.iniciarPartida(listOf("joão", "Joao", "Bia")).erro()
        assertTrue(erro is ErroRegra.NomesRepetidos)
        assertEquals(1, (erro as ErroRegra.NomesRepetidos).nomes.size)

        assertTrue(RegrasJogo.iniciarPartida(listOf("Ana", " ANA  ")).erro() is ErroRegra.NomesRepetidos)
        assertTrue(RegrasJogo.iniciarPartida(listOf("Ana Paula", "ana   paula")).erro() is ErroRegra.NomesRepetidos)
    }

    @Test
    fun iniciarPartida_aparaENormalizaEspacosDosNomes() {
        val e = RegrasJogo.iniciarPartida(listOf("  Ana   Paula ", "Bia")).ok()
        assertEquals("Ana Paula", e.jogadores[0].nome)
    }

    @Test
    fun configuracaoNaoPadrao_vidasELimites() {
        val config = ConfiguracaoJogo(vidasIniciais = 5, minimoJogadores = 3, maximoJogadores = 4)
        assertEquals(
            ErroRegra.QuantidadeJogadoresInvalida(2, 3, 4),
            RegrasJogo.iniciarPartida(listOf("A", "B"), config).erro(),
        )
        assertEquals(
            ErroRegra.QuantidadeJogadoresInvalida(5, 3, 4),
            RegrasJogo.iniciarPartida(listOf("A", "B", "C", "D", "E"), config).erro(),
        )
        val e = RegrasJogo.iniciarPartida(listOf("A", "B", "C", "D"), config).ok()
        assertTrue(e.jogadores.all { it.vidas == 5 })
        assertEquals(config, e.configuracao)
    }
}
