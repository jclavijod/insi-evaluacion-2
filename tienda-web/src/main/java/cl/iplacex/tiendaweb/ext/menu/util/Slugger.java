package cl.iplacex.tiendaweb.ext.menu.util;

import java.text.Normalizer;

public class Slugger {

	public static String toSlug(String text) {
		// 1. Convertir a minúsculas
		String slug = text.toLowerCase();

		// 2. Normalizar caracteres especiales (ej. "á" → "a", "ñ" → "n")
		slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
		slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

		// 3. Reemplazar espacios y caracteres no válidos por "-"
		slug = slug.replaceAll("[^a-z0-9\\s-]", ""); // Eliminar caracteres especiales
		slug = slug.replaceAll("\\s+", "-"); // Reemplazar espacios por "-"
		slug = slug.replaceAll("-{2,}", "-"); // Evitar múltiples guiones seguidos

		// 4. Eliminar guiones al inicio o final
		slug = slug.replaceAll("^-|-$", "");

		return slug;
	}
	
}
