package com.talesdev.copsandcrims.dedicated;

import com.talesdev.copsandcrims.CopsAndCrims;
import com.talesdev.core.arena.ArenaTimer;
import com.talesdev.core.arena.GameArena;
import com.talesdev.core.arena.TeamGameSpawn;
import com.talesdev.core.arena.phase.EndPhase;
import com.talesdev.core.arena.phase.LobbyPhase;
import com.talesdev.core.arena.scoreboard.LobbyScoreboard;
import com.talesdev.core.arena.team.DefaultTeamSelector;
import com.talesdev.core.arena.team.TeamSelector;
import com.talesdev.core.arena.util.WinMessage;
import com.talesdev.core.config.ConfigFile;
import com.talesdev.core.player.CleanedPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * TDM Game Arena
 *
 * @author MoKunz
 */
public class TDMGameArena extends GameArena {
    private TeamGameSpawn teamGameSpawn;
    private ArenaTimer timer;
    private LobbyScoreboard lobbyScoreboard;
    private TDMScoreboard tdmScoreboard;
    private Map<String, Integer> teamKills;
    private Set<KillDeath> killDeathSet;
    // HACK
    private boolean fistBlood;
    private String winner = ChatColor.GREEN + "Draw";
    public TDMGameArena() {
        super(CopsAndCrims.getPlugin(), new ConfigFile("plugins/CopsAndCrims/config.yml"), null, null);
        setMaxPlayers(8);
        TDMArenaWorld arenaWorld = new TDMArenaWorld(this);
        arenaWorld.setName("NealTheFarmer");
        setArenaWorld(arenaWorld);
        setGameArenaListener(new TDMArenaListener(this));
        teamGameSpawn = new TeamGameSpawn(this);
        setArenaSpawn(teamGameSpawn);
        timer = new ArenaTimer(this, 300, false);
        tdmScoreboard = new TDMScoreboard(this);
        lobbyScoreboard = new LobbyScoreboard();
    }

    @Override
    protected void init() {
        getLogger().info("Dispatching initial phase");
        getTeam().newTeam(getTeam().createTeam("Terrorist"));
        if (!getConfig().contains("spawn.Terrorist")) getConfig().set("spawn.Terrorist", new ArrayList<>());
        getTeam().newTeam(getTeam().createTeam("CounterTerrorist"));
        if (!getConfig().contains("spawn.CounterTerrorist"))
            getConfig().set("spawn.CounterTerrorist", new ArrayList<>());
        initKills();
        dispatchPhase(new LobbyPhase());
        getLogger().info("Completed!");
    }

    protected void initKills() {
        teamKills = new HashMap<>();
        teamKills.put("Terrorist", 0);
        teamKills.put("CounterTerrorist", 0);
        killDeathSet = new HashSet<>();
    }

    @Override
    public void startGame() {
        // select team
        TeamSelector selector = new DefaultTeamSelector();
        selector.select(getTeam());
        // dispatch team update to client
        getGlobalScoreboard().updateLocalTeam();
        // k/d object
        // scoreboard
        killDeathSet.clear();
        getPlayerSet().forEach((player) -> {
            createPlayerKD(player);
            initDisplay(player, tdmScoreboard);
            CleanedPlayer cp = new CleanedPlayer(player);
            cp.clean();
        });
        // spawn player
        teamGameSpawn.readFromConfig(getConfig());
        teamGameSpawn.spawn(this);
        // kit
        for (Player player : getPlayerSet()) {
            String name = getTeam().getTeam(player).getName();
            TDMKitItem item = new TDMKitItem(player, name == null ? "" : name);
            item.give();
        }
        // timer action
        systemMessage("Game has been started!");
        timer.setUpdate(() -> {
            getPlayerSet().forEach(tdmScoreboard::update);
            getGlobalScoreboard().updateLocalTeam();
            if (timer.getTime() == 30) {
                systemMessage("The game will be ended in 30 seconds");
            } else if (timer.getTime() <= 10) {
                systemMessage("The game will be ended in " + timer.getTime() + "  seconds");
            }
        });
        timer.onStop(() -> {
            dispatchPhase(new EndPhase(winner));
        });
        timer.start();
    }

    @Override
    public void stopGame() {
        // check stats
        checkStats();
        // broadcast winner
        WinMessage winMessage = new WinMessage(this, "CopsAndCrims - TDM", winner);
        winMessage.send();
        // stop everything
        timer.stop();
        getPlayerSet().forEach(player -> {
            CleanedPlayer cp = new CleanedPlayer(player);
            cp.clean();
        });
        // stop timer
        timer = new ArenaTimer(this, 300, false);
        // initKills
        initKills();
    }

    @Override
    public void destroy() {
        if (!getConfig().contains("arena-locked")) getConfig().set("arena-locked", true);
        super.destroy();
    }

    public TeamGameSpawn getTeamGameSpawn() {
        return teamGameSpawn;
    }

    public LobbyScoreboard getLobbyScoreboard() {
        return lobbyScoreboard;
    }

    public void addKill(String teamName) {
        teamKills.put(teamName, teamKills.get(teamName) + 1);
    }

    public int getKills(String teamName) {
        return teamKills.get(teamName);
    }

    public void checkStats() {
        getTeam().getTeamList().stream().filter(team -> getKills(team.getName()) > 50).forEach(team -> {
            winner = (team.getName().equals("Terrorist") ? ChatColor.RED : ChatColor.BLUE) + team.getName();
            stopGame();
        });
        if (getKills("Terrorist") > getKills("CounterTerrorist")) {
            winner = ChatColor.RED + "Terrorist";
        } else if (getKills("Terrorist") == getKills("CounterTerrorist")) {
            winner = ChatColor.GREEN + "Draw";
        } else {
            winner = ChatColor.BLUE + "CounterTerrorist";
        }
    }


    public KillDeath getPlayerKD(Player player) {
        for (KillDeath kd : killDeathSet) {
            if (kd.getPlayer().equals(player)) {
                return kd;
            }
        }
        return null;
    }

    public void createPlayerKD(Player player) {
        killDeathSet.add(new KillDeath(player));
    }

    public ArenaTimer getTimer() {
        return timer;
    }

    public TDMScoreboard getTdmScoreboard() {
        return tdmScoreboard;
    }

    public boolean isFistBlood() {
        return fistBlood;
    }

    public void setFistBlood(boolean fistBlood) {
        this.fistBlood = fistBlood;
    }
}