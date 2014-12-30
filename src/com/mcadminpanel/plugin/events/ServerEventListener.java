package com.mcadminpanel.plugin.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.json.simple.JSONObject;

import com.mcadminpanel.plugin.AdminPanel;
import com.mcadminpanel.plugin.Utils;

public class ServerEventListener implements Listener
{
	private AdminPanel ap;
	
	public ServerEventListener(AdminPanel ap)
	{
		this.ap = ap;
	}
	
	@EventHandler
	public void onServerListPingEvent(ServerListPingEvent event)
	{
		if (ap.isConnected())
		{
			Utils.hasTinyUrl(ap, event);
			
			if (ap.enableWhitelist())
			{
				JSONObject user = ap.getUserFromIp(event.getAddress().getHostAddress());
				
				if (user != null)
				{
					if (!(Boolean) user.get("whitelisted") && !(Boolean) user.get("blacklisted"))
					{
						event.setMotd(ChatColor.YELLOW + "Your application is being processed...");
					} else if (!(Boolean) user.get("whitelisted") && (Boolean) user.get("blacklisted"))
					{
						event.setMotd(ChatColor.RED + "Your application has been denied...");
					}
				} else
				{
					event.setMotd(ChatColor.GOLD + "Apply online: " + ChatColor.YELLOW + ap.getTinyUrl());
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event)
	{
		if (ap.isConnected())
			ap.getChats().add(new String[] { event.getPlayer().getDisplayName(), event.getMessage() });
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event)
	{
		if (ap.isConnected())
			Utils.hasTinyUrl(ap, event);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLoginEvent(PlayerLoginEvent event)
	{
		
		if (ap.isConnected() && ap.enableWhitelist())
		{
			Player player = event.getPlayer();
			
			if (!player.isOp() && !player.hasPermission("server.admin"))
			{
				JSONObject user = ap.getUserFromName(player.getName());
				
				if (user != null)
				{
					if (!((String) user.get("ipAddress")).equals(event.getAddress().getHostAddress()))
					{
						ap.sendMethod("setUserIp", player.getName(), event.getAddress().getHostAddress());
					}
					
					if (!(Boolean) user.get("whitelisted") && (Boolean) user.get("blacklisted"))
					{
						event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "Sorry but you are not whitelisted");
					}
				} else
				{
					event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "Sorry but you are not whitelisted\n\n" + ChatColor.GOLD + "Apply online: " + ChatColor.YELLOW + ap.getTinyUrl());
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoinEvent(PlayerJoinEvent event)
	{
		if (ap.isConnected())
		{
			final Player player = event.getPlayer();
			
			if (ap.enableWhitelist())
			{
				JSONObject user = ap.getUserFromName(player.getName());
				
				if (user != null)
				{
					JSONObject group = ((JSONObject) user.get("group"));
					
					if (!player.isOp() && !player.hasPermission("server.admin"))
					{
						if ((Boolean) group.get("ghost"))
						{
							ap.getGhostFactory().setGhost(player, true);
							
							player.getInventory().clear();
						} else
						{
							ap.getGhostFactory().setGhost(player, false);
						}
					} else if (player.isOp() || ap.groupHasPermission(group.get("permissions").toString(), "applications"))
					{
						Bukkit.getScheduler().scheduleSyncDelayedTask(ap, new Runnable() {
							public void run()
							{
								player.sendMessage(ChatColor.GOLD + "[Web] " + ChatColor.AQUA + "There are currently " + ChatColor.RED + ap.getAppCount() + ChatColor.AQUA + " awaiting applications");
							}
						}, 5);
					}
				} else
				{
					player.kickPlayer("Sorry but you are not whitelisted\n\n" + ChatColor.GOLD + "Apply online: " + ChatColor.YELLOW + ap.getTinyUrl());
				}
			} else if (ap.getGhostFactory().isGhost(player))
			{
				ap.getGhostFactory().setGhost(player, false);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInventoryClickEvent(InventoryClickEvent event)
	{
		if (ap.isConnected())
		{
			if (event.getWhoClicked() instanceof Player)
			{
				Player player = (Player) event.getWhoClicked();
				
				if (ap.getGhostFactory().isGhost(player))
				{
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteractEvent(PlayerInteractEvent event)
	{
		if (ap.isConnected())
		{
			Player player = event.getPlayer();
			
			if (ap.getGhostFactory().isGhost(player))
			{
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event)
	{
		if (ap.isConnected())
		{
			if (event.getDamager() instanceof Player)
			{
				Player player = (Player) event.getDamager();
				
				if (ap.getGhostFactory().isGhost(player))
				{
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPickupItemEvent(PlayerPickupItemEvent event)
	{
		if (ap.isConnected())
		{
			Player player = event.getPlayer();
			
			if (ap.getGhostFactory().isGhost(player))
			{
				event.setCancelled(true);
			}
		}
	}
}