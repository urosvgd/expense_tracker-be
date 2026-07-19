package com.example.expensetracker.expense.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItemCategoryDetectorTest {

    private val detector = ItemCategoryDetector()

    @Test
    fun `toLatinDisplayName converts Serbian Cyrillic to Latin`() {
        val result = detector.toLatinDisplayName(
            "Млеко и хлеб Љубљана Његош Ђак"
        )

        assertEquals(
            "Mleko i hleb Ljubljana Njegoš Đak",
            result
        )
    }
    @Test
    fun `detectCode returns FOOD for chocolate with Serbian diacritics`() {
        assertEquals(
            "FOOD",
            detector.detectCode("Čokolada")
        )
    }

    @Test
    fun `toLatinDisplayName trims and collapses whitespace`() {
        val result = detector.toLatinDisplayName(
            "  Макси    Нови   Сад  "
        )

        assertEquals(
            "Maksi Novi Sad",
            result
        )
    }

    @Test
    fun `cleanMerchantName converts Cyrillic and normalizes whitespace`() {
        val result = detector.cleanMerchantName(
            "  МАКСИ    НОВИ САД  "
        )

        assertEquals(
            "MAKSI NOVI SAD",
            result
        )
    }

    @Test
    fun `cleanItemName removes receipt labels`() {
        val result = detector.cleanItemName(
            "Naziv Млеко Cena 180 Kol 2 Ukupno"
        )

        assertEquals(
            "Mleko 180 2",
            result
        )
    }

    @Test
    fun `cleanItemName removes abbreviated quantity label`() {
        val result = detector.cleanItemName(
            "Kol. 2 Хлеб"
        )

        assertEquals(
            "2 Hleb",
            result
        )
    }

    @Test
    fun `cleanItemName normalizes repeated spaces`() {
        val result = detector.cleanItemName(
            "Naziv    Јогурт     1L"
        )

        assertEquals(
            "Jogurt 1L",
            result
        )
    }

    @Test
    fun `detectCode returns FOOD for milk`() {
        assertEquals(
            "FOOD",
            detector.detectCode("Млеко 2.8%")
        )
    }

    @Test
    fun `detectCode returns FOOD for bread`() {
        assertEquals(
            "FOOD",
            detector.detectCode("Sveži hleb")
        )
    }

    @Test
    fun `detectCode returns FOOD for text without Serbian diacritics`() {
        assertEquals(
            "FOOD",
            detector.detectCode("Cokolada")
        )
    }

    @Test
    fun `detectCode prioritizes drinks when item also contains a food keyword`() {
        assertEquals(
            "DRINKS",
            detector.detectCode("Sok od pomorandže")
        )
    }

    @Test
    fun `detectCode returns DRINKS for coffee`() {
        assertEquals(
            "DRINKS",
            detector.detectCode("Jacobs kafa")
        )
    }

    @Test
    fun `detectCode returns HOUSEHOLD for detergent`() {
        assertEquals(
            "HOUSEHOLD",
            detector.detectCode("Deterdžent za sudove")
        )
    }

    @Test
    fun `detectCode returns PERSONAL_CARE for shampoo`() {
        assertEquals(
            "PERSONAL_CARE",
            detector.detectCode("Šampon za kosu")
        )
    }

    @Test
    fun `detectCode returns ELECTRONICS for USB cable`() {
        assertEquals(
            "ELECTRONICS",
            detector.detectCode("USB kabl")
        )
    }

    @Test
    fun `detectCode returns CLOTHING for shirt`() {
        assertEquals(
            "CLOTHING",
            detector.detectCode("Majica kratkih rukava")
        )
    }

    @Test
    fun `detectCode returns HEALTH for medicine`() {
        assertEquals(
            "HEALTH",
            detector.detectCode("Lek protiv bolova")
        )
    }

    @Test
    fun `detectCode is case insensitive`() {
        assertEquals(
            "DRINKS",
            detector.detectCode("GAZIRANA VODA")
        )
    }

    @Test
    fun `detectCode ignores Serbian diacritics while matching`() {
        assertEquals(
            "CLOTHING",
            detector.detectCode("Čarape")
        )
    }

    @Test
    fun `detectCode cleans receipt labels before matching`() {
        assertEquals(
            "FOOD",
            detector.detectCode(
                "Naziv Млеко Cena 180 Kol. 1 Ukupno 180"
            )
        )
    }

    @Test
    fun `detectCode returns OTHER for unknown item`() {
        assertEquals(
            "OTHER",
            detector.detectCode("Potpuno nepoznat proizvod")
        )
    }

    @Test
    fun `detectCode returns OTHER for blank item`() {
        assertEquals(
            "OTHER",
            detector.detectCode("   ")
        )
    }
}