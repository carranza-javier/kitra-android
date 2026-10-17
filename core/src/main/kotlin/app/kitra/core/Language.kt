package app.kitra.core

/** Idiomas de destino ofrecidos en la configuración. */
enum class Language(val code: String, val englishName: String, val nativeName: String) {
    SPANISH("es", "Spanish", "Español"),
    ENGLISH("en", "English", "English"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    ITALIAN("it", "Italian", "Italiano"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    CATALAN("ca", "Catalan", "Català"),
    DUTCH("nl", "Dutch", "Nederlands"),
    POLISH("pl", "Polish", "Polski"),
    JAPANESE("ja", "Japanese", "日本語"),
    CHINESE_SIMPLIFIED("zh", "Simplified Chinese", "中文"),
    KOREAN("ko", "Korean", "한국어");

    companion object {
        val DEFAULT = SPANISH

        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }

        /** Idioma del sistema si está soportado; si no, español. */
        fun forSystemLocale(languageCode: String?): Language = fromCode(languageCode) ?: DEFAULT
    }
}
