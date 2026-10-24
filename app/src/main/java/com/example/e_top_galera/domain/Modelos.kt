package com.example.e_top_galera.domain

/** Jogador da partida. Fica eliminado quando as vidas chegam a 0. */
data class Jogador(
    val nome: String,
    val vidas: Int,
    val pontos: Int = 0,
) {
    val eliminado: Boolean get() = vidas <= 0
    val vivo: Boolean get() = !eliminado
}

/** Item de uma lista de Top 10 (espelha o formato JSON de `assets/listas`). */
data class ItemLista(
    val posicao: Int,
    val resposta: String,
    val sinonimos: List<String> = emptyList(),
)

/** Lista de Top 10 usada como tema de uma rodada. */
data class ListaTop10(
    val id: String,
    val tema: String,
    val itens: List<ItemLista>,
    val categoria: String = "",
    val criterio: String = "",
    val fontes: List<String> = emptyList(),
    val atualizadoEm: String = "",
)
