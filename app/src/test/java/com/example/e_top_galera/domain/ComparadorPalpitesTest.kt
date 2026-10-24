package com.example.e_top_galera.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparadorPalpitesTest {

    private val india = ItemLista(1, "Índia", listOf("India", "Bharat"))
    private val eua = ItemLista(3, "Estados Unidos", listOf("EUA"))

    @Test
    fun normalizar_removeAcentosMaiusculasEEspacos() {
        assertEquals("sao paulo", ComparadorPalpites.normalizar("  São   PAULO \t"))
        assertEquals("acai coracao", ComparadorPalpites.normalizar("Açaí Coração"))
        assertEquals("", ComparadorPalpites.normalizar("   "))
    }

    @Test
    fun normalizar_cedilhaETilVirarLetraBase() {
        assertEquals("franca paquistao", ComparadorPalpites.normalizar("França Paquistão"))
    }

    @Test
    fun corresponde_ignoraMaiusculas() {
        assertTrue(ComparadorPalpites.corresponde("ÍNDIA", india))
        assertTrue(ComparadorPalpites.corresponde("índia", india))
    }

    @Test
    fun corresponde_ignoraAcentos() {
        assertTrue(ComparadorPalpites.corresponde("India", india))
        assertTrue(ComparadorPalpites.corresponde("indiá", india))
    }

    @Test
    fun corresponde_ignoraEspacosExtras() {
        assertTrue(ComparadorPalpites.corresponde("   estados    unidos  ", eua))
        assertTrue(ComparadorPalpites.corresponde("Estados\tUnidos", eua))
    }

    @Test
    fun corresponde_aceitaSinonimos() {
        assertTrue(ComparadorPalpites.corresponde("eua", eua))
        assertTrue(ComparadorPalpites.corresponde("BHARAT", india))
    }

    @Test
    fun corresponde_rejeitaTextoDiferenteOuParcial() {
        assertFalse(ComparadorPalpites.corresponde("Estados", eua))
        assertFalse(ComparadorPalpites.corresponde("Indiana", india))
        assertFalse(ComparadorPalpites.corresponde("EstadosUnidos", eua))
    }

    @Test
    fun corresponde_textoVazioNuncaCorresponde() {
        assertFalse(ComparadorPalpites.corresponde("", india))
        assertFalse(ComparadorPalpites.corresponde("   ", india))
        assertFalse(ComparadorPalpites.corresponde("", ItemLista(1, "", listOf(""))))
    }

    @Test
    fun encontrarItem_devolveItemCorretoOuNull() {
        val lista = Fixtures.paises
        assertEquals(3, ComparadorPalpites.encontrarItem("usa", lista)?.posicao)
        assertEquals(10, ComparadorPalpites.encontrarItem("mexico", lista)?.posicao)
        assertEquals(7, ComparadorPalpites.encontrarItem(" BRASIL ", lista)?.posicao)
        assertNull(ComparadorPalpites.encontrarItem("Argentina", lista))
    }
}
