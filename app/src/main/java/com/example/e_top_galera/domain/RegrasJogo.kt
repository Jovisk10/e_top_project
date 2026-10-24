package com.example.e_top_galera.domain

/**
 * Regras do jogo como funções puras: cada ação recebe um [EstadoPartida] e devolve um novo
 * estado em [Resultado.Sucesso], ou um [ErroRegra] em [Resultado.Falha] sem alterar nada.
 */
object RegrasJogo {

    /** Cria a partida, validando quantidade de jogadores e nomes (não vazios e únicos). */
    fun iniciarPartida(
        nomes: List<String>,
        configuracao: ConfiguracaoJogo = ConfiguracaoJogo(),
    ): Resultado<EstadoPartida> {
        val limpos = nomes.map { it.trim().replace(Regex("\\s+"), " ") }
        if (limpos.size !in configuracao.minimoJogadores..configuracao.maximoJogadores) {
            return falha(
                ErroRegra.QuantidadeJogadoresInvalida(
                    limpos.size, configuracao.minimoJogadores, configuracao.maximoJogadores,
                ),
            )
        }
        if (limpos.any { it.isEmpty() }) return falha(ErroRegra.NomeVazio)

        val repetidos = limpos.groupBy { ComparadorPalpites.normalizar(it) }
            .values.filter { it.size > 1 }.map { it.first() }
        if (repetidos.isNotEmpty()) return falha(ErroRegra.NomesRepetidos(repetidos))

        return sucesso(
            EstadoPartida(
                configuracao = configuracao,
                jogadores = limpos.map { Jogador(nome = it, vidas = configuracao.vidasIniciais) },
            ),
        )
    }

    /**
     * Inicia uma rodada com um tema ainda não usado na partida, escolhido por [sortear]
     * (injetável para testes). Se não houver temas inéditos, a partida termina com
     * [FimPartida.TemasEsgotados].
     */
    fun iniciarRodada(
        estado: EstadoPartida,
        temas: List<ListaTop10>,
        sortear: (List<ListaTop10>) -> ListaTop10 = { it.random() },
    ): Resultado<EstadoPartida> {
        if (estado.encerrada) return falha(ErroRegra.PartidaEncerrada)
        if (estado.rodadaEmAndamento) return falha(ErroRegra.RodadaEmAndamento)

        val disponiveis = temas.distinctBy { it.id }.filter { it.id !in estado.temasUsados }
        if (disponiveis.isEmpty()) {
            return sucesso(estado.copy(fim = FimPartida.TemasEsgotados(lideres(estado.jogadores))))
        }

        val tema = sortear(disponiveis)
        require(tema in disponiveis) { "O seletor deve devolver um tema da lista recebida" }

        val anterior = estado.rodada
        val iniciante = if (anterior == null) {
            primeiroVivoAPartirDe(estado.jogadores, 0)
        } else {
            proximoVivo(estado.jogadores, anterior.indiceIniciante)
        }

        return sucesso(
            estado.copy(
                indiceJogadorDaVez = iniciante,
                rodada = Rodada(
                    numero = (anterior?.numero ?: 0) + 1,
                    tema = tema,
                    indiceIniciante = iniciante,
                ),
                temasUsados = estado.temasUsados + tema.id,
            ),
        )
    }

    /**
     * O jogador da vez dá um palpite. Fecha a janela de contestação do palpite pendente
     * anterior (confirmando-o, se estiver na lista), registra o novo palpite como pendente,
     * zera os passes e avança para o próximo jogador vivo.
     */
    fun palpitar(estado: EstadoPartida, texto: String): Resultado<EstadoPartida> {
        val rodada = rodadaAtiva(estado) ?: return falhaRodada(estado)
        if (ComparadorPalpites.normalizar(texto).isEmpty()) return falha(ErroRegra.PalpiteVazio)

        val confirmadas = confirmarPendente(rodada)
        val item = ComparadorPalpites.encontrarItem(texto, rodada.tema)
        if (item != null && item.posicao in confirmadas) {
            return falha(ErroRegra.RespostaRepetida(item.posicao))
        }

        val palpite = Palpite(
            indiceJogador = estado.indiceJogadorDaVez,
            texto = texto.trim(),
            posicao = item?.posicao,
        )
        return sucesso(
            estado.copy(
                indiceJogadorDaVez = proximoVivo(estado.jogadores, estado.indiceJogadorDaVez),
                rodada = rodada.copy(
                    palpites = rodada.palpites + palpite,
                    palpitePendente = palpite,
                    posicoesConfirmadas = confirmadas,
                    passesConsecutivos = 0,
                ),
            ),
        )
    }

    /**
     * O jogador da vez passa. Fecha a janela de contestação do palpite pendente. Se todos os
     * jogadores vivos passarem seguidos, a rodada termina com [DesfechoRodada.TodosPassaram].
     */
    fun passar(estado: EstadoPartida): Resultado<EstadoPartida> {
        val rodada = rodadaAtiva(estado) ?: return falhaRodada(estado)

        val passes = rodada.passesConsecutivos + 1
        val todosPassaram = passes >= estado.indicesVivos.size
        val novaRodada = rodada.copy(
            palpitePendente = null,
            posicoesConfirmadas = confirmarPendente(rodada),
            passesConsecutivos = passes,
            desfecho = if (todosPassaram) DesfechoRodada.TodosPassaram else null,
        )
        return sucesso(
            estado.copy(
                indiceJogadorDaVez = if (todosPassaram) {
                    estado.indiceJogadorDaVez
                } else {
                    proximoVivo(estado.jogadores, estado.indiceJogadorDaVez)
                },
                rodada = novaRodada,
            ),
        )
    }

    /**
     * Um jogador vivo, diferente do autor, contesta o palpite pendente.
     * Fora da lista: autor perde 1 vida e contestador ganha 1 ponto.
     * Dentro da lista: contestador perde 1 vida e a posição é revelada.
     * A rodada termina e a vitória é verificada.
     */
    fun contestar(estado: EstadoPartida, indiceContestador: Int): Resultado<EstadoPartida> {
        val rodada = rodadaAtiva(estado) ?: return falhaRodada(estado)
        if (indiceContestador !in estado.jogadores.indices) {
            return falha(ErroRegra.JogadorInexistente(indiceContestador))
        }
        if (estado.jogadores[indiceContestador].eliminado) {
            return falha(ErroRegra.JogadorEliminado(indiceContestador))
        }
        val palpite = rodada.palpitePendente ?: return falha(ErroRegra.SemPalpiteParaContestar)
        val autor = palpite.indiceJogador
        if (autor == indiceContestador) return falha(ErroRegra.AutorNaoPodeContestar)

        val jogadores = estado.jogadores.toMutableList()
        val posicao = palpite.posicao
        if (posicao == null) {
            jogadores[autor] = jogadores[autor].perderVida()
            jogadores[indiceContestador] = jogadores[indiceContestador].let { it.copy(pontos = it.pontos + 1) }
        } else {
            jogadores[indiceContestador] = jogadores[indiceContestador].perderVida()
        }

        val novaRodada = rodada.copy(
            palpitePendente = null,
            posicoesConfirmadas = if (posicao != null) rodada.posicoesConfirmadas + posicao else rodada.posicoesConfirmadas,
            desfecho = DesfechoRodada.Contestacao(
                indiceAutor = autor,
                indiceContestador = indiceContestador,
                palpite = palpite,
                posicaoRevelada = posicao,
            ),
        )
        return sucesso(
            estado.copy(
                jogadores = jogadores,
                rodada = novaRodada,
                fim = verificarFim(jogadores, estado.configuracao),
            ),
        )
    }

    /** Próximo jogador vivo em sentido horário a partir de [indice] (exclusivo). */
    fun proximoVivo(jogadores: List<Jogador>, indice: Int): Int {
        for (passo in 1..jogadores.size) {
            val candidato = (indice + passo) % jogadores.size
            if (jogadores[candidato].vivo) return candidato
        }
        return indice
    }

    // --- auxiliares internos ---

    private fun primeiroVivoAPartirDe(jogadores: List<Jogador>, indice: Int): Int =
        if (jogadores[indice].vivo) indice else proximoVivo(jogadores, indice)

    private fun confirmarPendente(rodada: Rodada): Set<Int> {
        val posicao = rodada.palpitePendente?.posicao ?: return rodada.posicoesConfirmadas
        return rodada.posicoesConfirmadas + posicao
    }

    private fun verificarFim(jogadores: List<Jogador>, configuracao: ConfiguracaoJogo): FimPartida? {
        val porPontos = jogadores.indexOfFirst { it.vivo && it.pontos >= configuracao.pontosParaVencer }
        if (porPontos >= 0) return FimPartida.VitoriaPorPontos(porPontos)
        val vivos = jogadores.indices.filter { jogadores[it].vivo }
        if (vivos.size == 1) return FimPartida.UltimoSobrevivente(vivos.single())
        return null
    }

    private fun lideres(jogadores: List<Jogador>): List<Int> {
        val vivos = jogadores.indices.filter { jogadores[it].vivo }
        val melhor = vivos.maxOfOrNull { jogadores[it].pontos } ?: return emptyList()
        val porPontos = vivos.filter { jogadores[it].pontos == melhor }
        val maisVidas = porPontos.maxOf { jogadores[it].vidas }
        return porPontos.filter { jogadores[it].vidas == maisVidas }
    }

    private fun rodadaAtiva(estado: EstadoPartida): Rodada? =
        if (estado.encerrada) null else estado.rodada?.takeIf { !it.encerrada }

    private fun falhaRodada(estado: EstadoPartida): Resultado.Falha =
        if (estado.encerrada) falha(ErroRegra.PartidaEncerrada) else falha(ErroRegra.SemRodadaEmAndamento)

    private fun Jogador.perderVida(): Jogador = copy(vidas = (vidas - 1).coerceAtLeast(0))

    private fun <T> sucesso(valor: T): Resultado<T> = Resultado.Sucesso(valor)

    private fun falha(erro: ErroRegra): Resultado.Falha = Resultado.Falha(erro)
}
