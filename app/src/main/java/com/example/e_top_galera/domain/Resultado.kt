package com.example.e_top_galera.domain

/** Resultado de uma ação do jogo: novo estado ou erro de regra (o estado anterior não muda). */
sealed interface Resultado<out T> {
    data class Sucesso<out T>(val valor: T) : Resultado<T>
    data class Falha(val erro: ErroRegra) : Resultado<Nothing>
}

/** Atalho: devolve o valor em caso de sucesso, ou `null` em caso de falha. */
fun <T> Resultado<T>.valorOuNull(): T? = (this as? Resultado.Sucesso)?.valor

/** Atalho: devolve o erro em caso de falha, ou `null` em caso de sucesso. */
fun <T> Resultado<T>.erroOuNull(): ErroRegra? = (this as? Resultado.Falha)?.erro

/** Violações das regras do jogo. */
sealed interface ErroRegra {
    data class QuantidadeJogadoresInvalida(val quantidade: Int, val minimo: Int, val maximo: Int) : ErroRegra
    data object NomeVazio : ErroRegra
    data class NomesRepetidos(val nomes: List<String>) : ErroRegra
    data object PartidaEncerrada : ErroRegra
    data object RodadaEmAndamento : ErroRegra
    data object SemRodadaEmAndamento : ErroRegra
    data object PalpiteVazio : ErroRegra
    /** O palpite corresponde a um item já confirmado (ou ao palpite pendente) da rodada. */
    data class RespostaRepetida(val posicao: Int) : ErroRegra
    data object SemPalpiteParaContestar : ErroRegra
    data class JogadorInexistente(val indice: Int) : ErroRegra
    data class JogadorEliminado(val indice: Int) : ErroRegra
    data object AutorNaoPodeContestar : ErroRegra
}
