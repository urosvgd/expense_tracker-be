package com.example.expensetracker.expense.service

import org.springframework.stereotype.Component

@Component
class ItemCategoryDetector {

    private val rules: Map<String, List<String>> = mapOf(
        "FOOD" to listOf(
            "banana",
            "nektarina",
            "breskva",
            "jabuka",
            "kruska",
            "lubenica",
            "dinja",
            "kupus",
            "persun",
            "tresnja",
            "krompir",
            "paradajz",
            "krastavac",
            "luk",
            "paprika",
            "salata",
            "limun",
            "pomorandza",
            "mandarina",
            "sljiva",
            "grozdje",
            "mleko",
            "ml kr",
            "kis ml",
            "kiselo mleko",
            "kravica",
            "jogurt",
            "sir",
            "pavlaka",
            "kajmak",
            "puter",
            "kefir",
            "namaz",
            "dukat",
            "imlek",
            "kantica",
            "jaja",
            "jaje",
            "sveza jaja",
            "hleb",
            "hljeb",
            "kifla",
            "pecivo",
            "burek",
            "tost",
            "lepinja",
            "baget",
            "zemicka",
            "piletina",
            "svinjetina",
            "junetina",
            "kobasica",
            "salama",
            "slanina",
            "sunka",
            "prsuta",
            "cokolada",
            "keks",
            "cips",
            "smoki",
            "bombone",
            "sladoled",
            "kreker",
            "krekeri",
            "jaffa",
            "stapici",
            "slani",
            "pardon",
            "tak",
            "semenke",
            "bananica",
            "ulje",
            "dijamant",
            "omegol",
            "brasno",
            "secer",
            "so",
            "pirinac",
            "testenina",
            "makarone",
            "pasulj",
            "zacin"
        ),

        "DRINKS" to listOf(
            "voda",
            "gazirana",
            "knjaz",
            "milos",
            "sok",
            "cola",
            "fanta",
            "guarana",
            "rosa",
            "prolom",
            "jazak",
            "minakva",
            "aqua",
            "pet 1.25l",
            "kafa",
            "jacobs",
            "nescafe",
            "nes",
            "espresso",
            "caj",
            "crema",
            "gold",
            "pivo",
            "vino",
            "rakija",
            "vodka",
            "viski"
        ),

        "HOUSEHOLD" to listOf(
            "kese",
            "kesa",
            "tregerice",
            "stampa",
            "stamp",
            "deterdzent",
            "omeksivac",
            "ubrus",
            "toalet",
            "sapun",
            "sundjer",
            "folija"
        ),

        "PERSONAL_CARE" to listOf(
            "sampon",
            "pasta za zube",
            "dezodorans",
            "gel za tusiranje",
            "cetkica",
            "brijac"
        ),

        "ELECTRONICS" to listOf(
            "punjac",
            "kabl",
            "usb",
            "slusalice",
            "baterija",
            "adapter",
            "telefon",
            "mis",
            "tastatura"
        ),

        "CLOTHING" to listOf(
            "majica",
            "pantalone",
            "farmerke",
            "jakna",
            "duks",
            "patike",
            "cipele",
            "carape",
            "ves"
        ),

        "HEALTH" to listOf(
            "lek",
            "tablete",
            "vitamin",
            "sirup",
            "flaster",
            "zavoj",
            "apoteka"
        )
    )

    /*
     * More specific categories are checked before FOOD.
     *
     * Example:
     * "Sok od pomorandže" contains both "sok" and "pomorandza".
     * It should be classified as DRINKS, not FOOD.
     */
    private val categoryPriority = listOf(
        "DRINKS",
        "PERSONAL_CARE",
        "HOUSEHOLD",
        "ELECTRONICS",
        "CLOTHING",
        "HEALTH",
        "FOOD"
    )
    fun detectCode(rawItemName: String): String {
        val normalizedName = normalizeForMatching(
            cleanItemName(rawItemName)
        )

        if (normalizedName.isBlank()) {
            return "OTHER"
        }

        return categoryPriority.firstOrNull { categoryCode ->
            rules[categoryCode]
                .orEmpty()
                .any { keyword ->
                    containsKeyword(
                        normalizedName = normalizedName,
                        keyword = normalizeForMatching(keyword)
                    )
                }
        } ?: "OTHER"
    }

    private fun containsKeyword(
        normalizedName: String,
        keyword: String
    ): Boolean {
        if (keyword.isBlank()) {
            return false
        }

        val pattern = Regex(
            pattern = """(^|[^\p{L}\p{N}])${Regex.escape(keyword)}($|[^\p{L}\p{N}])"""
        )

        return pattern.containsMatchIn(normalizedName)
    }
    fun cleanItemName(rawName: String): String {
        val latinName = toLatinDisplayName(rawName)

        return latinName
            .replace(
                Regex(
                    pattern = """(?i)(?<![\p{L}\p{N}])(?:Naziv|Cena|Kol\.?|Ukupno)(?![\p{L}\p{N}])"""
                ),
                " "
            )
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
    fun cleanMerchantName(rawName: String): String {
        return toLatinDisplayName(rawName)
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeForMatching(value: String): String {
        return toLatinDisplayName(value)
            .lowercase()
            .replace("š", "s")
            .replace("č", "c")
            .replace("ć", "c")
            .replace("ž", "z")
            .replace("đ", "dj")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun toLatinDisplayName(value: String): String {
        return value
            .replace("А", "A")
            .replace("Б", "B")
            .replace("В", "V")
            .replace("Г", "G")
            .replace("Д", "D")
            .replace("Ђ", "Đ")
            .replace("Е", "E")
            .replace("Ж", "Ž")
            .replace("З", "Z")
            .replace("И", "I")
            .replace("Ј", "J")
            .replace("К", "K")
            .replace("Л", "L")
            .replace("Љ", "Lj")
            .replace("М", "M")
            .replace("Н", "N")
            .replace("Њ", "Nj")
            .replace("О", "O")
            .replace("П", "P")
            .replace("Р", "R")
            .replace("С", "S")
            .replace("Т", "T")
            .replace("Ћ", "Ć")
            .replace("У", "U")
            .replace("Ф", "F")
            .replace("Х", "H")
            .replace("Ц", "C")
            .replace("Ч", "Č")
            .replace("Џ", "Dž")
            .replace("Ш", "Š")
            .replace("а", "a")
            .replace("б", "b")
            .replace("в", "v")
            .replace("г", "g")
            .replace("д", "d")
            .replace("ђ", "đ")
            .replace("е", "e")
            .replace("ж", "ž")
            .replace("з", "z")
            .replace("и", "i")
            .replace("ј", "j")
            .replace("к", "k")
            .replace("л", "l")
            .replace("љ", "lj")
            .replace("м", "m")
            .replace("н", "n")
            .replace("њ", "nj")
            .replace("о", "o")
            .replace("п", "p")
            .replace("р", "r")
            .replace("с", "s")
            .replace("т", "t")
            .replace("ћ", "ć")
            .replace("у", "u")
            .replace("ф", "f")
            .replace("х", "h")
            .replace("ц", "c")
            .replace("ч", "č")
            .replace("џ", "dž")
            .replace("ш", "š")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}