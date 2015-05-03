package com.talesdev.copsandcrims.dedicated;

import com.talesdev.copsandcrims.event.EntityDamageByWeaponEvent;
import com.talesdev.copsandcrims.weapon.Weapon;
import com.talesdev.copsandcrims.weapon.WeaponType;
import com.talesdev.core.TalesCore;
import com.talesdev.core.arena.GameState;
import com.talesdev.core.arena.event.ArenaCountdownEvent;
import com.talesdev.core.arena.event.GeneralArenaListener;
import com.talesdev.core.arena.event.PlayerJoinArenaEvent;
import com.talesdev.core.arena.event.PlayerLeaveArenaEvent;
import com.talesdev.core.arena.scoreboard.LobbyScoreboard;
import com.talesdev.core.entity.DamageData;
import com.talesdev.core.player.AutoRespawn;
import com.talesdev.core.player.CleanedPlayer;
import com.talesdev.core.player.CorePlayer;
import com.talesdev.core.player.message.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scoreboard.Team;

import java.util.Iterator;
import java.util.List;

/**
 * TDM Arena Listener
 *
 * @author MoKunz
 */
public class TDMArenaListener extends GeneralArenaListener<TDMGameArena> {

    public TDMArenaListener(TDMGameArena gameArena) {
        super(gameArena);
    }

    @EventHandler
    public void onArenaJoin(PlayerJoinArenaEvent event) {
        CleanedPlayer cp = new CleanedPlayer(event.getPlayer());
        cp.clean();
        event.setAfterRun(() -> {
            LobbyScoreboard lobby = getGameArena().getLobbyScoreboard();
            lobby.setMapName(ChatColor.GREEN + "NealTheFarmer");
            lobby.setTitle(ChatColor.RED + "CvC - TDM");
            lobby.setCountdown(-1);
            lobby.setMaxPlayers(getGameArena().getMaxPlayers());
            lobby.setPlayers(getGameArena().playing());
            getGameArena().initDisplay(event.getPlayer(), lobby);
            getGameArena().updateDisplay(lobby);
        });
    }

    @EventHandler
    public void onArenaCountdown(ArenaCountdownEvent event) {
        // countdown scoreboard update
        LobbyScoreboard lobby = getGameArena().getLobbyScoreboard();
        lobby.setCountdown(event.getCurrentCountdown());
        getGameArena().updateDisplay(lobby);
        // other stuff
        // title
        if (event.getCurrentCountdown() <= 10) {
            String color = calculateColor(event.getCurrentCountdown());
            Title title = new Title(color + event.getCurrentCountdown(), ChatColor.YELLOW + "seconds left until the game start!", 0, 30, 0);
            title.send(getGameArena().getPlayerSet());
        }
    }

    private String calculateColor(int time) {
        if (time <= 2) {
            return ChatColor.RED.toString();
        } else if (time <= 4) {
            return ChatColor.GOLD.toString();
        } else if (time <= 7) {
            return ChatColor.YELLOW.toString();
        } else {
            return ChatColor.GREEN.toString();
        }
    }

    @EventHandler
    public void onArenaLeave(PlayerLeaveArenaEvent event) {
        // update player count
        if (getGameArena().getGameState().canJoin()) {
            LobbyScoreboard lobby = getGameArena().getLobbyScoreboard();
            lobby.setPlayers(getGameArena().playing());
            getGameArena().updateDisplay(lobby);
        }
    }


    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (getGameArena().containsPlayer(Bukkit.getPlayer(event.getEntity().getName()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void regenEvent(EntityRegainHealthEvent event) {
        if (event.getEntityType().equals(EntityType.PLAYER)) {
            Player player = ((Player) event.getEntity());
            if (getGameArena().containsPlayer(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void interactEvent(BlockFromToEvent event) {
        if (event.getBlock() != null && event.getToBlock() != null) {
            if (event.getBlock().getType().equals(Material.SOIL)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void soilChangePlayer(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.SOIL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void soilChangeEntity(EntityInteractEvent event) {
        if (event.getEntityType() != EntityType.PLAYER && event.getBlock().getType() == Material.SOIL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (getGameArena().containsPlayer(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            CorePlayer corePlayer = TalesCore.getPlugin().getCorePlayer((Player) event.getEntity());
            if (corePlayer.getPlayerDamage().isGod()) {
                event.setCancelled(true);
            }
        }
        if (event.getDamager() instanceof Player) {
            Player player = ((Player) event.getDamager());
            if (getGameArena().containsPlayer(player)) {
                if (!getGameArena().getGameState().equals(GameState.STARTED)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            CorePlayer corePlayer = TalesCore.getPlugin().getCorePlayer((Player) event.getEntity());
            if (corePlayer.getPlayerDamage().isGod()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTeamDamage(EntityDamageByWeaponEvent event) {
        if (getGameArena().getGameState().equals(GameState.END)) {
            event.setCancelled(true);
        }
        if (event.getEntity() instanceof Player) {
            CorePlayer corePlayer = TalesCore.getPlugin().getCorePlayer((Player) event.getEntity());
            if (corePlayer.getPlayerDamage().isGod()) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof Player) {
            Player player = ((Player) event.getEntity());
            // find team
            Team team = getGameArena().getTeam().getTeam(player);
            if (team.hasPlayer(event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

    private String getTeamColor(Player player) {
        Team team = getGameArena().getTeam().getTeam(player);
        if (team != null) {
            if (team.getName().equals("Terrorist")) {
                return ChatColor.RED.toString();
            } else {
                return ChatColor.BLUE.toString();
            }
        }
        return ChatColor.YELLOW.toString();
    }

    private String getDeathMessage(Player victim, Entity killer) {
        String killerName = "";
        String victimName = "";
        String weapon = "";
        String headShot = "";
        if (killer instanceof Player) {
            killerName = getTeamColor(((Player) killer)) + killer.getName();
        }

        CorePlayer corePlayer = TalesCore.getPlugin().getCorePlayer(victim);
        DamageData damageData = corePlayer.getPlayerDamage().getLastEntity();
        Weapon wp = damageData.getAttachment("Weapon", Weapon.class);
        if (wp != null) {
            weapon = WeaponType.getSymbol(wp);
        }
        boolean hs = damageData.getAttachment("HeadShot", Boolean.TYPE).booleanValue();
        if (hs) {
            headShot = "\u9270";
        }
        victimName = getTeamColor(victim) + victim.getName();
        return killerName + ChatColor.RESET + " " + weapon + headShot + " " + ChatColor.RESET + victimName;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (getGameArena().containsPlayer(event.getEntity())) {
            // spectator
            Player player = event.getEntity();
            event.getDrops().clear();
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
            getGameArena().getPlayerKD(player).addDeath();
            // add kill to killer
            CorePlayer corePlayer = TalesCore.getPlugin().getCorePlayer(player);
            DamageData last = corePlayer.getPlayerDamage().getLastEntity();
            String lastName = "";
            if (last.getDamager() instanceof Player) {
                Player damager = ((Player) last.getDamager());
                if (last.getAttachment("Weapon") != null && last.getDamageCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                    if (getGameArena().containsPlayer(damager)) {
                        getGameArena().getPlayerKD(damager).addKill();
                        lastName = damager.getName();
                        Team team = getGameArena().getTeam().getTeam(damager);
                        if (team != null) {
                            getGameArena().addKill(team.getName());
                        }
                    }
                }
            }
            // death message
            String msg = "";
            if (last.getDamager() instanceof Player) {
                msg = getDeathMessage(event.getEntity(), last.getDamager());
            }
            Title title = new Title(ChatColor.RED + ChatColor.BOLD.toString() + "YOU DIED!", msg, 20, 100, 20);
            title.send(player);
            event.setDeathMessage(msg);
            // assist
            List<DamageData> damageDataList = corePlayer.getPlayerDamage().getEntityDamage();
            for (Iterator<DamageData> dmgIterator = damageDataList.iterator(); dmgIterator.hasNext(); ) {
                DamageData data = dmgIterator.next();
                if (data != null) {
                    if (data.getDamager() != null) {
                        if (data.getDamager().getName().equals(lastName)) {
                            dmgIterator.remove();
                        }
                    }
                }
            }
            damageDataList.stream().filter(damageData -> damageData.getDamager() != null).filter(damageData -> last.getDamager() instanceof Player).forEach(damageData -> {
                Player damager = ((Player) last.getDamager());
                if (last.getAttachment("Weapon") != null && last.getDamageCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
                    if (getGameArena().containsPlayer(damager)) {
                        getGameArena().getPlayerKD(damager).addAssists();
                    }
                }
            });
            if (getGameArena().getGameState().equals(GameState.STARTED)) {
                getGameArena().checkStats();
            }
            // scoreboard
            getGameArena().getPlayerSet().forEach(getGameArena().getTdmScoreboard()::update);
            // force respawn
            AutoRespawn autoRespawn = new AutoRespawn(player);
            autoRespawn.perform();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (getGameArena().containsPlayer(event.getPlayer())) {
            LivingEntity killer = event.getPlayer().getKiller();
            if (killer != null) {
                Location location = killer.getEyeLocation().add(killer.getEyeLocation().getDirection().multiply(2));
                event.setRespawnLocation(location);
            }
            TDMRespawn respawn = new TDMRespawn(getGameArena(), event.getPlayer());
            respawn.start();
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player) {
            Player remover = ((Player) event.getRemover());
            if (getGameArena().containsPlayer(remover)) {
                event.setCancelled(true);
            }
        }
    }

}
