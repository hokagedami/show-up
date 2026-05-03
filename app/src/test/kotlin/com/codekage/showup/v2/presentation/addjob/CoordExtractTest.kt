package com.codekage.showup.v2.presentation.addjob

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordExtractTest {

    @Test
    fun `extracts coords from bare lat,lng`() {
        val result = AddEditJobViewModel.extractCoordinates("54.5260, -1.5510")
        assertEquals(54.5260 to -1.5510, result)
    }

    @Test
    fun `extracts coords from Maps query URL`() {
        val url = "https://www.google.com/maps/search/?api=1&query=54.5260,-1.5510"
        val result = AddEditJobViewModel.extractCoordinates(url)
        assertEquals(54.5260 to -1.5510, result)
    }

    @Test
    fun `extracts coords from Maps at-sign format`() {
        val url = "https://www.google.com/maps/@54.5260,-1.5510,15z/data=!3m1"
        val result = AddEditJobViewModel.extractCoordinates(url)
        assertEquals(54.5260 to -1.5510, result)
    }

    @Test
    fun `extracts coords with label suffix`() {
        val text = "Bishopsgate House @ 54.5260,-1.5510 (DfE office)"
        val result = AddEditJobViewModel.extractCoordinates(text)
        assertEquals(54.5260 to -1.5510, result)
    }

    @Test
    fun `returns null for plain street address`() {
        assertNull(AddEditJobViewModel.extractCoordinates("Bishopsgate House, Feethams, Darlington DL1 5QE"))
    }

    @Test
    fun `rejects out-of-range numbers`() {
        // Looks like coords but lng is invalid
        assertNull(AddEditJobViewModel.extractCoordinates("99.5, 200.5"))
    }

    @Test
    fun `accepts negative coordinates in southern hemisphere`() {
        val result = AddEditJobViewModel.extractCoordinates("-33.8688, 151.2093")
        assertEquals(-33.8688 to 151.2093, result)
    }
}
