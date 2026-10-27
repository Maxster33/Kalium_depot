package fr.kalium.contrats;

import fr.kalium.economy.KSEconomy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** Mise en forme : nombres, durées, dates, noms d'objets. */
final class Textes {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private Textes() {
    }

    /** 1234567 -> « 1 234 567 ». */
    static String nombre(long valeur) {
        String chiffres = String.valueOf(Math.abs(valeur));
        StringBuilder sortie = new StringBuilder(valeur < 0 ? "-" : "");
        for (int i = 0; i < chiffres.length(); i++) {
            if (i > 0 && (chiffres.length() - i) % 3 == 0) {
                sortie.append(' ');
            }
            sortie.append(chiffres.charAt(i));
        }
        return sortie.toString();
    }

    /** « 1 point », « 1 234 points ». */
    static String points(long valeur) {
        return nombre(valeur) + (Math.abs(valeur) > 1 ? " points" : " point");
    }

    /** « 36 j 14 h 21 min » (minutes arrondies au-dessus, jamais « 0 min » pour un reste positif). */
    static String duree(long ms) {
        long minutes = Math.max(0, (ms + Contrat.MINUTE - 1) / Contrat.MINUTE);
        long j = minutes / (24 * 60);
        long h = minutes / 60 % 24;
        long m = minutes % 60;
        StringBuilder s = new StringBuilder();
        if (j > 0) {
            s.append(j).append(" j ");
        }
        if (h > 0) {
            s.append(h).append(" h ");
        }
        if (m > 0 || s.length() == 0) {
            s.append(m).append(" min");
        }
        return s.toString().trim();
    }

    static String date(long ms) {
        return DATE.format(Instant.ofEpochMilli(ms).atZone(Contrat.PARIS));
    }

    static String periode(Contrat.Periode p) {
        return switch (p) {
            case JOUR -> "chaque jour";
            case SEMAINE -> "chaque semaine";
            case DEUX_SEMAINES -> "toutes les 2 semaines";
            case MOIS -> "chaque mois";
        };
    }

    /** « cette semaine », « ce jour »... : période en cours d'un contrat récurrent. */
    static String periodeEnCours(Contrat.Periode p) {
        return switch (p) {
            case JOUR -> "aujourd'hui";
            case SEMAINE -> "cette semaine";
            case DEUX_SEMAINES -> "ces 2 semaines";
            case MOIS -> "ce mois-ci";
        };
    }

    static Component nomObjet(ItemStack objet) {
        return KSEconomy.nomObjet(objet);
    }

    /** Nom d'un objet en texte simple (historique). */
    static String nomTexte(ItemStack objet) {
        Component nom = nomObjet(objet);
        if (nom instanceof TranslatableComponent) {
            return objet.getType().getKey().getKey().replace('_', ' ');
        }
        return PlainTextComponentSerializer.plainText().serialize(nom);
    }
}
