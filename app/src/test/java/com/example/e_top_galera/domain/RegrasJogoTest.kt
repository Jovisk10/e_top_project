package com.example.e_top_galera.domain

import com.example.e_top_galera.domain.Fixtures.assertVez
import com.example.e_top_galera.domain.Fixtures.comJogador
import com.example.e_top_galera.domain.Fixtures.comRodada
import com.example.e_top_galera.domain.Fixtures.contesta
import com.example.e_top_galera.domain.Fixtures.erro
import com.example.e_top_galera.domain.Fixtures.listaSimples
import com.example.e_top_galera.domain.Fixtures.novaRodada
import com.example.e_top_galera.domain.Fixtures.ok
import com.example.e_top_galera.domain.Fixtures.paises
import com.example.e_top_galera.domain.Fixtures.palpita
import com.example.e_top_galera.domain.Fixtures.partida
import com.example.e_top_galera.domain.Fixtures.passa
import com.example.e_top_galera.domain.Fixtures.primeiro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasJogoTest {

    // ---------------- Rodadas e temas ----------------

    @Test
    fun iniciarRodada_primeiraComecaPeloJogador0() {
        val e = comRodada()
        assertVez(0, e)
        val r = e.rodada!!
        assertEquals(1, r.numero)
        assertEquals(0, r.indiceIniciante)
        assertEquals(paises, r.tema)
        assertTrue(e.rodadaEmAndamento)
        assertEquals(setOf(paises.id), e.temasUsados)
    }

    @Test
    fun iniciarRodada_primeiraComecaPeloPrimeiroVivoSeJogador0EstiverEliminado() {
        val e = partida(3).comJogador(0, vidas = 0).novaRodada()
        assertVez(1, e)
        assertEquals(1, e.rodada!!.indiceIniciante)
    }

    @Test
    fun iniciarRodada_seguintesComecamPeloProximoDepoisDeQuemAbriu() {
        var e = partida(3).novaRodada()
        assertEquals(0, e.rodada!!.indiceIniciante)
        e = e.passa().passa().passa()
        assertTrue(e.rodada!!.encerrada)
        e = e.novaRodada()
        assertEquals(2, e.rodada!!.numero)
        assertEquals(1, e.rodada!!.indiceIniciante)
        assertVez(1, e)
        e = e.passa().passa().passa().novaRodada()
        assertVez(2, e)
        e = e.passa().passa().passa().novaRodada()
        assertVez(0, e) // volta ao início
    }

    @Test
    fun iniciarRodada_pulaIniciantEliminado() {
        // 3 jogadores; rodada 1 aberta por 0. Jogador 1 é eliminado nessa rodada.
        var e = partida(3).comJogador(1, vidas = 1).novaRodada()
        e = e.palpita("Argentina") // 0 palpita errado
        e = e.contesta(1).let { it } // 1 contesta palpite errado: 1 ganha ponto, 0 perde vida
        assertEquals(2, e.jogadores[0].vidas)
        // Agora rodada 2: aberta por 1
        e = e.novaRodada()
        assertVez(1, e)
        e = e.palpita("Brasil") // 1 palpita certo
        e = e.contesta(2).let { it } // 2 perde vida
        // Elimina o jogador 2 para testar o pulo do iniciante
        e = e.comJogador(2, vidas = 0)
        e = e.novaRodada()
        // Próximo depois de 1 é 2, eliminado -> 0
        assertVez(0, e)
    }

    @Test
    fun iniciarRodada_temasNaoSeRepetem() {
        val temas = listOf(listaSimples("a"), listaSimples("b"), listaSimples("c"))
        var e = partida(2)
        val vistos = mutableListOf<String>()
        repeat(3) {
            e = RegrasJogo.iniciarRodada(e, temas) { it.random() }.ok()
            vistos += e.rodada!!.tema.id
            e = e.passa().passa()
        }
        assertEquals(setOf("a", "b", "c"), vistos.toSet())
        assertEquals(3, vistos.size)
    }

    @Test
    fun iniciarRodada_sorteioRecebeApenasTemasIneditos() {
        val temas = listOf(listaSimples("a"), listaSimples("b"))
        var e = RegrasJogo.iniciarRodada(partida(2), temas, primeiro).ok().passa().passa()
        var recebidos: List<ListaTop10>? = null
        e = RegrasJogo.iniciarRodada(e, temas) { recebidos = it; it.first() }.ok()
        assertEquals(listOf("b"), recebidos!!.map { it.id })
        assertEquals("b", e.rodada!!.tema.id)
    }

    @Test
    fun iniciarRodada_temasComIdDuplicadoContamComoUm() {
        val a1 = listaSimples("a", "x")
        val a2 = listaSimples("a", "y")
        val e = RegrasJogo.iniciarRodada(partida(2), listOf(a1, a2), primeiro).ok().passa().passa()
        val fim = RegrasJogo.iniciarRodada(e, listOf(a1, a2), primeiro).ok()
        assertTrue(fim.fim is FimPartida.TemasEsgotados)
    }

    @Test
    fun iniciarRodada_semTemasIneditos_encerraComLideres() {
        var e = partida(3).novaRodada(listOf(paises)).passa().passa().passa()
        e = e.comJogador(0, pontos = 2).comJogador(1, pontos = 2, vidas = 1).comJogador(2, pontos = 1)
        val fim = RegrasJogo.iniciarRodada(e, listOf(paises), primeiro).ok()
        assertEquals(FimPartida.TemasEsgotados(listOf(0)), fim.fim)
        assertTrue(fim.encerrada)
        assertEquals(0, fim.indiceVencedor)
    }

    @Test
    fun temasEsgotados_empatePontosEVidas_semVencedorUnico() {
        var e = partida(3).novaRodada(listOf(paises)).passa().passa().passa()
        e = e.comJogador(0, pontos = 2).comJogador(1, pontos = 2).comJogador(2, pontos = 3, vidas = 0)
        val fim = RegrasJogo.iniciarRodada(e, listOf(paises), primeiro).ok()
        // jogador 2 tem mais pontos mas está eliminado: não é líder
        assertEquals(FimPartida.TemasEsgotados(listOf(0, 1)), fim.fim)
        assertNull(fim.indiceVencedor)
        assertNull(fim.vencedor)
    }

    @Test
    fun iniciarRodada_comRodadaEmAndamento_falha() {
        val e = comRodada()
        assertEquals(ErroRegra.RodadaEmAndamento, RegrasJogo.iniciarRodada(e, Fixtures.temas, primeiro).erro())
    }

    @Test
    fun acoesSemRodada_falham() {
        val e = partida(3)
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.palpitar(e, "Brasil").erro())
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.passar(e).erro())
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.contestar(e, 1).erro())
    }

    @Test
    fun acoesAposRodadaEncerrada_falhamAteNovaRodada() {
        val e = comRodada(3).passa().passa().passa()
        assertTrue(e.rodada!!.encerrada)
        assertFalse(e.rodadaEmAndamento)
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.palpitar(e, "Brasil").erro())
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.passar(e).erro())
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.contestar(e, 1).erro())
    }

    // ---------------- Palpitar ----------------

    @Test
    fun palpitar_avancaVezERegistraPendente() {
        val e = comRodada(3).palpita("  brasil ")
        assertVez(1, e)
        val p = e.rodada!!.palpitePendente!!
        assertEquals(0, p.indiceJogador)
        assertEquals(7, p.posicao)
        assertTrue(p.dentroDaLista)
        assertEquals("brasil", p.texto)
        assertEquals(1, e.rodada!!.palpites.size)
        assertTrue(e.rodada!!.posicoesConfirmadas.isEmpty()) // ainda não confirmado
    }

    @Test
    fun palpitar_foraDaLista_posicaoNula() {
        val p = comRodada().palpita("Argentina").rodada!!.palpitePendente!!
        assertNull(p.posicao)
        assertFalse(p.dentroDaLista)
    }

    @Test
    fun palpitar_vazio_falhaSemMudarVez() {
        val e = comRodada()
        assertEquals(ErroRegra.PalpiteVazio, RegrasJogo.palpitar(e, "").erro())
        assertEquals(ErroRegra.PalpiteVazio, RegrasJogo.palpitar(e, "   ").erro())
    }

    @Test
    fun palpitar_confirmaPendenteAnteriorQuandoNinguemContesta() {
        val e = comRodada(3).palpita("Brasil").palpita("China")
        assertEquals(setOf(7), e.rodada!!.posicoesConfirmadas)
        assertEquals(2, e.rodada!!.palpitePendente!!.posicao)
        assertEquals(listOf("Brasil"), e.rodada!!.respostasConfirmadas.map { it.resposta })
    }

    @Test
    fun palpitar_repetirRespostaConfirmada_falhaEVezNaoMuda() {
        val e = comRodada(3).palpita("Brasil").palpita("China") // Brasil confirmado; vez do 2
        val r = RegrasJogo.palpitar(e, "BRASIL")
        assertEquals(ErroRegra.RespostaRepetida(7), r.erro())
        assertVez(2, e)
    }

    @Test
    fun palpitar_repetirPalpitePendente_falha() {
        val e = comRodada(3).palpita("Brasil")
        assertEquals(ErroRegra.RespostaRepetida(7), RegrasJogo.palpitar(e, "brasil").erro())
    }

    @Test
    fun palpitar_repetirViaSinonimo_falha() {
        val e = comRodada(3).palpita("Estados Unidos")
        assertEquals(ErroRegra.RespostaRepetida(3), RegrasJogo.palpitar(e, "EUA").erro())
        val e2 = e.palpita("China") // EUA confirmado
        assertEquals(ErroRegra.RespostaRepetida(3), RegrasJogo.palpitar(e2, "usa").erro())
        assertEquals(ErroRegra.RespostaRepetida(1), RegrasJogo.palpitar(comRodada().palpita("India"), "Índia").erro())
    }

    @Test
    fun palpitar_repetirRespostaConfirmadaAposPasse_falha() {
        val e = comRodada(3).palpita("Brasil").passa()
        assertEquals(setOf(7), e.rodada!!.posicoesConfirmadas)
        assertEquals(ErroRegra.RespostaRepetida(7), RegrasJogo.palpitar(e, "Brasil").erro())
    }

    @Test
    fun palpitar_errosPodemSerRepetidos() {
        val e = comRodada(3).palpita("Argentina").palpita("argentina").palpita("ARGENTINA")
        assertEquals(3, e.rodada!!.palpites.size)
        assertTrue(e.rodada!!.posicoesConfirmadas.isEmpty())
    }

    @Test
    fun palpitar_estadoOriginalNaoEAlterado() {
        val e = comRodada(3)
        val copia = e.copy()
        RegrasJogo.palpitar(e, "Brasil").ok()
        assertEquals(copia, e)
    }

    @Test
    fun palpitar_pulaEliminadosNaVez() {
        val e = partida(4).comJogador(1, vidas = 0).comJogador(2, vidas = 0).novaRodada()
        assertVez(0, e)
        val e2 = e.palpita("Brasil")
        assertVez(3, e2)
        assertVez(0, e2.palpita("China"))
    }

    // ---------------- Passar ----------------

    @Test
    fun passar_avancaVezEFechaJanelaDeContestacao() {
        val e = comRodada(3).palpita("Argentina").passa()
        assertVez(2, e)
        assertNull(e.rodada!!.palpitePendente)
        assertEquals(ErroRegra.SemPalpiteParaContestar, RegrasJogo.contestar(e, 2).erro())
    }

    @Test
    fun passar_todosPassamSeguidos_encerraRodada() {
        var e = comRodada(3).passa().passa()
        assertFalse(e.rodada!!.encerrada)
        e = e.passa()
        assertEquals(DesfechoRodada.TodosPassaram, e.rodada!!.desfecho)
        assertTrue(e.rodada!!.encerrada)
        assertFalse(e.encerrada)
        // ninguém perde vida nem ganha ponto
        assertTrue(e.jogadores.all { it.vidas == 3 && it.pontos == 0 })
    }

    @Test
    fun passar_palpiteZeraContagem() {
        var e = comRodada(3).passa().passa().palpita("Brasil")
        assertEquals(0, e.rodada!!.passesConsecutivos)
        e = e.passa().passa()
        assertFalse(e.rodada!!.encerrada)
        e = e.passa()
        assertEquals(DesfechoRodada.TodosPassaram, e.rodada!!.desfecho)
        // Brasil foi confirmado ao passar
        assertTrue(7 in e.rodada!!.posicoesConfirmadas)
    }

    @Test
    fun passar_contaApenasJogadoresVivos() {
        val e = partida(5).comJogador(1, vidas = 0).comJogador(3, vidas = 0).novaRodada()
        var x = e.passa()
        assertVez(2, x)
        x = x.passa()
        assertVez(4, x)
        assertFalse(x.rodada!!.encerrada)
        x = x.passa()
        assertEquals(DesfechoRodada.TodosPassaram, x.rodada!!.desfecho)
    }

    @Test
    fun passar_doisJogadores() {
        val e = comRodada(2).passa()
        assertFalse(e.rodada!!.encerrada)
        assertTrue(e.passa().rodada!!.encerrada)
    }

    // ---------------- Contestar ----------------

    @Test
    fun contestar_palpiteErrado_autorPerdeVidaContestadorGanhaPonto() {
        val e = comRodada(3).palpita("Argentina").contesta(2)
        assertEquals(2, e.jogadores[0].vidas)
        assertEquals(0, e.jogadores[0].pontos)
        assertEquals(3, e.jogadores[2].vidas)
        assertEquals(1, e.jogadores[2].pontos)
        assertEquals(Jogador("Jogador1", 3, 0), e.jogadores[1])
        val d = e.rodada!!.desfecho as DesfechoRodada.Contestacao
        assertEquals(0, d.indiceAutor)
        assertEquals(2, d.indiceContestador)
        assertNull(d.posicaoRevelada)
        assertFalse(d.palpiteCorreto)
        assertEquals("Argentina", d.palpite.texto)
        assertTrue(e.rodada!!.encerrada)
        assertFalse(e.encerrada)
    }

    @Test
    fun contestar_palpiteCerto_contestadorPerdeVidaEPosicaoRevelada() {
        val e = comRodada(3).palpita("Brasil").contesta(1)
        assertEquals(3, e.jogadores[0].vidas)
        assertEquals(0, e.jogadores[0].pontos)
        assertEquals(2, e.jogadores[1].vidas)
        assertEquals(0, e.jogadores[1].pontos)
        val d = e.rodada!!.desfecho as DesfechoRodada.Contestacao
        assertEquals(7, d.posicaoRevelada)
        assertTrue(d.palpiteCorreto)
        assertTrue(e.rodada!!.encerrada)
    }

    @Test
    fun contestar_palpiteCertoViaSinonimo_revelaPosicao() {
        val e = comRodada(3).palpita("usa").contesta(2)
        assertEquals(3, (e.rodada!!.desfecho as DesfechoRodada.Contestacao).posicaoRevelada)
        assertEquals(2, e.jogadores[2].vidas)
    }

    @Test
    fun contestar_semPendente_falha() {
        assertEquals(ErroRegra.SemPalpiteParaContestar, RegrasJogo.contestar(comRodada(3), 1).erro())
    }

    @Test
    fun contestar_palpiteAnteriorAoPendente_naoPodeMais() {
        // 0 palpita errado, 1 palpita certo: o palpite de 0 não pode mais ser contestado.
        val e = comRodada(3).palpita("Argentina").palpita("Brasil")
        val d = e.contesta(0).rodada!!.desfecho as DesfechoRodada.Contestacao
        assertEquals(1, d.indiceAutor) // contesta o pendente (do jogador 1), não o de 0
        assertEquals(7, d.posicaoRevelada)
    }

    @Test
    fun contestar_jogadorInexistente_falha() {
        val e = comRodada(3).palpita("Argentina")
        assertEquals(ErroRegra.JogadorInexistente(3), RegrasJogo.contestar(e, 3).erro())
        assertEquals(ErroRegra.JogadorInexistente(-1), RegrasJogo.contestar(e, -1).erro())
    }

    @Test
    fun contestar_jogadorEliminado_falha() {
        val e = partida(3).comJogador(2, vidas = 0).novaRodada().palpita("Argentina")
        assertEquals(ErroRegra.JogadorEliminado(2), RegrasJogo.contestar(e, 2).erro())
    }

    @Test
    fun contestar_peloAutor_falha() {
        val e = comRodada(3).palpita("Argentina")
        assertEquals(ErroRegra.AutorNaoPodeContestar, RegrasJogo.contestar(e, 0).erro())
    }

    @Test
    fun contestar_jogadorDaVezPodeContestar() {
        val e = comRodada(3).palpita("Argentina")
        assertVez(1, e)
        assertEquals(1, e.contesta(1).jogadores[1].pontos)
    }

    @Test
    fun contestar_depoisDaRodadaEncerrada_falha() {
        val e = comRodada(3).palpita("Argentina").contesta(1)
        assertEquals(ErroRegra.SemRodadaEmAndamento, RegrasJogo.contestar(e, 2).erro())
    }

    @Test
    fun contestar_palpiteCertoAdicionaAConfirmadas() {
        val e = comRodada(3).palpita("Brasil").contesta(1)
        assertTrue(7 in e.rodada!!.posicoesConfirmadas)
    }

    @Test
    fun contestar_falhaNaoAlteraEstado() {
        val e = comRodada(3).palpita("Argentina")
        val r = RegrasJogo.contestar(e, 0)
        assertTrue(r is Resultado.Falha)
        assertEquals(3, e.jogadores[0].vidas)
    }

    // ---------------- Eliminação e vitória ----------------

    @Test
    fun jogadorSemVidas_saiDaRotacao() {
        var e = partida(3).comJogador(1, vidas = 1).novaRodada()
        e = e.palpita("Brasil").contesta(1) // 1 contesta certo e perde a última vida
        assertTrue(e.jogadores[1].eliminado)
        assertEquals(0, e.jogadores[1].vidas)
        assertFalse(e.encerrada)
        e = e.novaRodada() // iniciante: próximo vivo depois de 0 -> 2
        assertVez(2, e)
        e = e.palpita("China")
        assertVez(0, e)
        e = e.palpita("Índia")
        assertVez(2, e)
        assertEquals(listOf(0, 2), e.indicesVivos)
    }

    @Test
    fun vidasNaoFicamNegativas() {
        // Perder a última vida deixa exatamente 0 (nunca negativo).
        val e = partida(2).comJogador(0, vidas = 1).novaRodada().palpita("Argentina").contesta(1)
        assertEquals(0, e.jogadores[0].vidas)
    }

    @Test
    fun vitoriaPorPontos_ao4Pontos() {
        val e = comRodada(3).comJogador(2, pontos = 3).palpita("Argentina").contesta(2)
        assertEquals(4, e.jogadores[2].pontos)
        assertEquals(FimPartida.VitoriaPorPontos(2), e.fim)
        assertTrue(e.encerrada)
        assertEquals(2, e.indiceVencedor)
        assertEquals("Jogador2", e.vencedor!!.nome)
    }

    @Test
    fun com3Pontos_naoVence() {
        val e = comRodada(3).comJogador(2, pontos = 2).palpita("Argentina").contesta(2)
        assertEquals(3, e.jogadores[2].pontos)
        assertNull(e.fim)
    }

    @Test
    fun vitoriaPorUltimoSobrevivente_contestacaoCertaEliminaUltimoAdversario() {
        val e = comRodada(2).comJogador(1, vidas = 1).palpita("Brasil").contesta(1)
        assertEquals(FimPartida.UltimoSobrevivente(0), e.fim)
        assertEquals(0, e.indiceVencedor)
    }

    @Test
    fun vitoriaPorUltimoSobrevivente_contestacaoErradaEliminaAutor() {
        val e = comRodada(2).comJogador(0, vidas = 1).palpita("Argentina").contesta(1)
        assertEquals(FimPartida.UltimoSobrevivente(1), e.fim)
        assertEquals(1, e.jogadores[1].pontos)
    }

    @Test
    fun pontosTemPrioridadeQuandoAmbasCondicoesOcorrem() {
        // Contestador chega a 4 pontos e elimina o último adversário na mesma contestação.
        val e = comRodada(2).comJogador(0, vidas = 1).comJogador(1, pontos = 3)
            .palpita("Argentina").contesta(1)
        assertEquals(FimPartida.VitoriaPorPontos(1), e.fim)
        assertEquals(1, e.indiceVencedor)
    }

    @Test
    fun eliminacaoComTresJogadores_naoEncerraPartida() {
        val e = comRodada(3).comJogador(0, vidas = 1).palpita("Argentina").contesta(1)
        assertTrue(e.jogadores[0].eliminado)
        assertNull(e.fim)
    }

    @Test
    fun aposFim_todasAcoesFalhamComPartidaEncerrada() {
        val e = comRodada(2).comJogador(1, vidas = 1).palpita("Brasil").contesta(1)
        assertTrue(e.encerrada)
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.palpitar(e, "China").erro())
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.passar(e).erro())
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.contestar(e, 0).erro())
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.iniciarRodada(e, Fixtures.temas, primeiro).erro())
    }

    @Test
    fun aposTemasEsgotados_acoesFalhamComPartidaEncerrada() {
        val e = partida(2).novaRodada(listOf(paises)).passa().passa()
        val fim = RegrasJogo.iniciarRodada(e, listOf(paises), primeiro).ok()
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.iniciarRodada(fim, Fixtures.temas, primeiro).erro())
        assertEquals(ErroRegra.PartidaEncerrada, RegrasJogo.passar(fim).erro())
    }

    // ---------------- Configuração não padrão ----------------

    @Test
    fun configuracao_pontosParaVencerCustomizado() {
        val config = ConfiguracaoJogo(pontosParaVencer = 2)
        var e = comRodada(3, config).palpita("Argentina").contesta(1)
        assertNull(e.fim)
        e = e.novaRodada().palpita("Narnia") // rodada 2 aberta por 1
        e = e.contesta(0).let { it }
        // 0 ganhou 1 ponto, 1 tinha 1
        e = e.novaRodada() // aberta por 2
        e = e.palpita("Atlantida").contesta(1)
        assertEquals(2, e.jogadores[1].pontos)
        assertEquals(FimPartida.VitoriaPorPontos(1), e.fim)
    }

    @Test
    fun configuracao_vidasCustomizadas() {
        val config = ConfiguracaoJogo(vidasIniciais = 1)
        val e = comRodada(3, config).palpita("Argentina").contesta(1)
        assertTrue(e.jogadores[0].eliminado)
        assertNull(e.fim)
    }

    @Test
    fun configuracao_muitasVidas_contestacaoNaoElimina() {
        val config = ConfiguracaoJogo(vidasIniciais = 10, pontosParaVencer = 20)
        val e = comRodada(2, config).palpita("Argentina").contesta(1)
        assertEquals(9, e.jogadores[0].vidas)
        assertFalse(e.encerrada)
    }

    // ---------------- proximoVivo ----------------

    @Test
    fun proximoVivo_circularEPulaEliminados() {
        val js = listOf(Jogador("A", 1), Jogador("B", 0), Jogador("C", 2), Jogador("D", 0))
        assertEquals(2, RegrasJogo.proximoVivo(js, 0))
        assertEquals(0, RegrasJogo.proximoVivo(js, 2))
        assertEquals(2, RegrasJogo.proximoVivo(js, 1))
        assertEquals(0, RegrasJogo.proximoVivo(js, 3))
    }

    @Test
    fun proximoVivo_unicoVivoDevolveEleMesmo() {
        val js = listOf(Jogador("A", 1), Jogador("B", 0))
        assertEquals(0, RegrasJogo.proximoVivo(js, 0))
    }

    // ---------------- Partida completa ----------------

    @Test
    fun partidaCompleta_ponta_a_ponta_vitoriaPorPontos() {
        var e = RegrasJogo.iniciarPartida(listOf("Ana", "Bia", "Caio")).ok()
        val temas = Fixtures.temas

        // Rodada 1 (abre Ana=0): Ana acerta, Bia acerta, Caio erra; Ana contesta Caio.
        e = e.novaRodada(temas)
        assertEquals(paises.id, e.rodada!!.tema.id)
        e = e.palpita("Índia").palpita("China").palpita("Argentina")
        assertEquals(setOf(1, 2), e.rodada!!.posicoesConfirmadas)
        e = e.contesta(0)
        assertEquals(1, e.jogadores[0].pontos)
        assertEquals(2, e.jogadores[2].vidas)

        // Rodada 2 (abre Bia=1): todos passam.
        e = e.novaRodada(temas)
        assertVez(1, e)
        assertNotEquals(paises.id, e.rodada!!.tema.id)
        val tema2 = e.rodada!!.tema
        e = e.passa().passa().passa()
        assertEquals(DesfechoRodada.TodosPassaram, e.rodada!!.desfecho)

        // Rodada 3 (abre Caio=2): Caio acerta, Bia contesta e perde vida.
        e = e.novaRodada(temas)
        assertVez(2, e)
        val r3 = e.rodada!!.tema
        assertNotEquals(tema2.id, r3.id)
        e = e.palpita(r3.itens[4].resposta).contesta(1)
        assertEquals(2, e.jogadores[1].vidas)
        assertEquals(5, (e.rodada!!.desfecho as DesfechoRodada.Contestacao).posicaoRevelada)

        // Rodadas 4..6: Ana contesta palpites errados e chega a 4 pontos.
        repeat(3) { i ->
            e = e.novaRodada(temas)
            val autor = e.indiceJogadorDaVez
            if (autor == 0) e = e.passa()
            val autorReal = e.indiceJogadorDaVez
            e = e.palpita("resposta inexistente $i").contesta(0)
            assertEquals(autorReal, (e.rodada!!.desfecho as DesfechoRodada.Contestacao).indiceAutor)
        }
        assertEquals(4, e.jogadores[0].pontos)
        assertEquals(FimPartida.VitoriaPorPontos(0), e.fim)
        assertEquals("Ana", e.vencedor!!.nome)
        assertEquals(6, e.rodada!!.numero)
        assertEquals(6, e.temasUsados.size)
    }

    @Test
    fun partidaCompleta_ponta_a_ponta_ultimoSobrevivente() {
        var e = RegrasJogo.iniciarPartida(listOf("Ana", "Bia")).ok()
        // Bia contesta palpites certos de Ana até ser eliminada (3 vidas).
        repeat(3) {
            e = e.novaRodada()
            if (e.indiceJogadorDaVez == 1) e = e.passa()
            e = e.palpita(e.rodada!!.tema.itens[0].resposta).contesta(1)
        }
        assertEquals(0, e.jogadores[1].vidas)
        assertEquals(FimPartida.UltimoSobrevivente(0), e.fim)
        assertEquals("Ana", e.vencedor!!.nome)
        assertEquals(3, e.jogadores[0].vidas)
    }
}
