package com.example.e_top_galera.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Comparação tolerante de palpites: ignora maiúsculas/minúsculas, acentos e espaços extras,
 * e aceita os `sinonimos` de cada item.
 */
object ComparadorPalpites {

    private val MARCAS_DIACRITICAS = Regex("\\p{Mn}+")
    private val ESPACOS = Regex("\\s+")

    /** Normaliza um texto: sem acentos, minúsculo, espaços colapsados e aparados. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(MARCAS_DIACRITICAS, "")
            .lowercase(Locale.ROOT)
            .replace(ESPACOS, " ")
            .trim()

    /** Indica se o palpite corresponde à resposta ou a algum sinônimo do item. */
    fun corresponde(palpite: String, item: ItemLista): Boolean {
        val alvo = normalizar(palpite)
        if (alvo.isEmpty()) return false
        return normalizar(item.resposta) == alvo || item.sinonimos.any { normalizar(it) == alvo }
    }

    /** Devolve o item da lista que corresponde ao palpite, ou `null` se estiver fora da lista. */
    fun encontrarItem(palpite: String, lista: ListaTop10): ItemLista? =
        lista.itens.firstOrNull { corresponde(palpite, it) }
}
