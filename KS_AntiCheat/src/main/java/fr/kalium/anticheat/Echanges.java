package fr.kalium.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 1.3.0 (LeKiwi06, 09/10/2026 : « dernières ventes et échanges » dans la fiche d'un joueur) : journal des /echange
 * aboutis (signal de KS_Economy 1.1.5, déjà suivi par Revente). KS_Economy ne garde aucune trace des échanges : le
 * journal commence donc à l'installation de cette version. plugins/KS_AntiCheat/echanges.log, une ligne par échange :
 * date, puis pour chacun des deux joueurs son identifiant, son pseudo et ce qu'il a donné, séparés par des tabulations.
 */
final class Echanges {

    private final KSAntiCheat plugin;
    private final File fichier;

    Echanges(KSAntiCheat plugin) {
        this.plugin = plugin;
        this.fichier = new File(plugin.getDataFolder(), "echanges.log");
    }

    void noter(Player a, Player b, List<ItemStack> objetsA, List<ItemStack> objetsB, long pointsA, long pointsB) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fichier, StandardCharsets.UTF_8, true))) {
            out.println(String.join("\t", String.valueOf(System.currentTimeMillis()), a.getUniqueId().toString(),
                    a.getName(), donne(objetsA, pointsA), b.getUniqueId().toString(), b.getName(), donne(objetsB, pointsB)));
        } catch (IOException e) {
            plugin.getLogger().warning("Journal des échanges : " + e.getMessage());
        }
    }

    /** « 3 x diamond, 500 points », ou « rien ». */
    private static String donne(List<ItemStack> objets, long points) {
        Map<String, Integer> n = new LinkedHashMap<>();
        for (ItemStack objet : objets) {
            if (objet != null && !objet.getType().isAir()) {
                n.merge(Inventaires.nom(objet).replace('\t', ' ').replace('\n', ' '), objet.getAmount(), Integer::sum);
            }
        }
        List<String> parties = new ArrayList<>();
        n.forEach((nom, quantite) -> parties.add(quantite + " x " + nom));
        if (points > 0) {
            parties.add(Infos.points(points));
        }
        return parties.isEmpty() ? "rien" : String.join(", ", parties);
    }

    /** Derniers échanges de ce joueur, le plus récent d'abord : « 08/10 21:14 - avec X : donne ... ; reçoit ... ». */
    List<String> derniers(UUID joueur, int nombre) {
        List<String> r = new ArrayList<>();
        if (!fichier.exists()) {
            return r;
        }
        List<String> lignes;
        try {
            lignes = Files.readAllLines(fichier.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return r;
        }
        String id = joueur.toString();
        for (int i = lignes.size() - 1; i >= 0 && r.size() < nombre; i--) {
            String[] c = lignes.get(i).split("\t");
            if (c.length < 7 || (!c[1].equals(id) && !c[4].equals(id))) {
                continue;
            }
            boolean premier = c[1].equals(id);
            String date;
            try {
                date = Alertes.DATE.format(Instant.ofEpochMilli(Long.parseLong(c[0])));
            } catch (NumberFormatException e) {
                continue;
            }
            r.add(date + " - avec " + (premier ? c[5] : c[2]) + " : donne " + (premier ? c[3] : c[6]) + " ; reçoit "
                    + (premier ? c[6] : c[3]));
        }
        return r;
    }
}
