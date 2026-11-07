package com.equipo2.vinilos.data.model

import java.text.Collator
import java.util.Locale

/** Orden alfabético en español sin importar tildes ni mayúsculas, así «Ángela» queda junto a «Ana». */
fun <T> byName(name: (T) -> String): Comparator<T> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }
    return compareBy(collator, name)
}
