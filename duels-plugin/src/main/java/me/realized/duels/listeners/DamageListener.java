package me.realized.duels.listeners;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.match.MatchEndEvent;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.util.EventUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Overrides damage cancellation by other plugins for players in a duel.
 */
public class DamageListener implements Listener {

    private final ArenaManagerImpl arenaManager;

    public DamageListener(final DuelsPlugin plugin) {
        this.arenaManager = plugin.getArenaManager();

        if (plugin.getConfiguration().isForceAllowCombat()) {
            plugin.doSyncAfter(() -> Bukkit.getPluginManager().registerEvents(this, plugin), 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final Player damager = EventUtil.getDamager(event);

        if (damager == null) {
            return;
        }

        final ArenaImpl arena = arenaManager.get(player);

        // Only activate when winner is undeclared
        if (arena == null || !arenaManager.isInMatch(damager) || arena.isEndGame()) {
            return;
        }

        KitImpl.Characteristic characteristic = arena.getMatch().getKit().getCharacteristics().stream().filter(
                c -> c == KitImpl.Characteristic.BOXING).findFirst().orElse(null);

        if(characteristic != null) {
            if(arena.getMatch().getHits(damager) >= 99) {
                PlayerDeathEvent customEvent = new PlayerDeathEvent(player,
                        new ArrayList<>(Arrays.asList(player.getInventory().getContents())), 0,
                        "Suck " + damager.getDisplayName() + " on boxing fight!");
                Bukkit.getPluginManager().callEvent(customEvent);
                event.setDamage(0);
                return;
            }

            event.setDamage(0);
        }

        arena.getMatch().addDamageToPlayer(damager, event.getFinalDamage());

        if(!event.isCancelled()) return;

        event.setCancelled(false);
    }

}
