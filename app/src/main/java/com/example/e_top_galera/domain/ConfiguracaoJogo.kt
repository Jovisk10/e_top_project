package com.example.e_top_galera.domain

/**
 * Constantes configuráveis da partida. Os valores padrão seguem as regras da versão 1.
 */
data class ConfiguracaoJogo(
    val vidasIniciais: Int = VIDAS_INICIAIS_PADRAO,
    val pontosParaVencer: Int = PONTOS_PARA_VENCER_PADRAO,
    val minimoJogadores: Int = MINIMO_JOGADORES_PADRAO,
    val maximoJogadores: Int = MAXIMO_JOGADORES_PADRAO,
) {
    init {
        require(vidasIniciais >= 1) { "vidasIniciais deve ser >= 1" }
        require(pontosParaVencer >= 1) { "pontosParaVencer deve ser >= 1" }
        require(minimoJogadores >= 2) { "minimoJogadores deve ser >= 2" }
        require(maximoJogadores >= minimoJogadores) { "maximoJogadores deve ser >= minimoJogadores" }
    }

    companion object {
        const val VIDAS_INICIAIS_PADRAO = 3
        const val PONTOS_PARA_VENCER_PADRAO = 4
        const val MINIMO_JOGADORES_PADRAO = 2
        const val MAXIMO_JOGADORES_PADRAO = 10
        const val ITENS_POR_LISTA = 10
    }
}
