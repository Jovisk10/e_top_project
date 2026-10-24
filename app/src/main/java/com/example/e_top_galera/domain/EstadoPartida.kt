package com.example.e_top_galera.domain

/**
 * Palpite feito por um jogador. [posicao] é a posição do item correspondente na lista,
 * ou `null` se o palpite estiver fora da lista. A interface não deve exibir [posicao]
 * antes de uma contestação ou do fim da rodada.
 */
data class Palpite(
    val indiceJogador: Int,
    val texto: String,
    val posicao: Int?,
) {
    val dentroDaLista: Boolean get() = posicao != null
}

/** Como a rodada terminou. */
sealed interface DesfechoRodada {
    /** Houve contestação. [posicaoRevelada] é preenchida quando o palpite estava na lista. */
    data class Contestacao(
        val indiceAutor: Int,
        val indiceContestador: Int,
        val palpite: Palpite,
        val posicaoRevelada: Int?,
    ) : DesfechoRodada {
        val palpiteCorreto: Boolean get() = posicaoRevelada != null
    }

    /** Todos os jogadores vivos passaram seguidos. */
    data object TodosPassaram : DesfechoRodada
}

/**
 * Rodada atual.
 *
 * Uma resposta é considerada **confirmada** quando um palpite dentro da lista deixa de poder
 * ser contestado, ou seja, quando o jogador seguinte palpita ou passa sem que ninguém conteste.
 * Palpites que repetem uma resposta confirmada, ou o palpite ainda pendente, são rejeitados.
 */
data class Rodada(
    val numero: Int,
    val tema: ListaTop10,
    val indiceIniciante: Int,
    val palpites: List<Palpite> = emptyList(),
    val palpitePendente: Palpite? = null,
    val posicoesConfirmadas: Set<Int> = emptySet(),
    val passesConsecutivos: Int = 0,
    val desfecho: DesfechoRodada? = null,
) {
    val encerrada: Boolean get() = desfecho != null

    /** Itens da lista já confirmados, em ordem de posição. */
    val respostasConfirmadas: List<ItemLista>
        get() = tema.itens.filter { it.posicao in posicoesConfirmadas }.sortedBy { it.posicao }
}

/** Como a partida terminou. */
sealed interface FimPartida {
    data class VitoriaPorPontos(val indiceVencedor: Int) : FimPartida
    data class UltimoSobrevivente(val indiceVencedor: Int) : FimPartida

    /**
     * Não há mais temas inéditos. Vencem os líderes em pontos (desempate por vidas);
     * pode haver empate com mais de um índice.
     */
    data class TemasEsgotados(val indicesLideres: List<Int>) : FimPartida
}

/** Estado imutável da partida. Cada ação em [RegrasJogo] gera um novo estado. */
data class EstadoPartida(
    val configuracao: ConfiguracaoJogo,
    val jogadores: List<Jogador>,
    val indiceJogadorDaVez: Int = 0,
    val rodada: Rodada? = null,
    val temasUsados: Set<String> = emptySet(),
    val fim: FimPartida? = null,
) {
    val encerrada: Boolean get() = fim != null

    val jogadorDaVez: Jogador get() = jogadores[indiceJogadorDaVez]

    val rodadaEmAndamento: Boolean get() = rodada != null && !rodada.encerrada

    val indicesVivos: List<Int> get() = jogadores.indices.filter { jogadores[it].vivo }

    /** Índice do vencedor único, se houver. */
    val indiceVencedor: Int?
        get() = when (val f = fim) {
            is FimPartida.VitoriaPorPontos -> f.indiceVencedor
            is FimPartida.UltimoSobrevivente -> f.indiceVencedor
            is FimPartida.TemasEsgotados -> f.indicesLideres.singleOrNull()
            null -> null
        }

    val vencedor: Jogador? get() = indiceVencedor?.let { jogadores[it] }
}
