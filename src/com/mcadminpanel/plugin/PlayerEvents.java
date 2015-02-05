package com.mcadminpanel.plugin;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

@SuppressWarnings({ "unchecked", "deprecation" })
public class PlayerEvents
{
	protected static void heal(JSONObject r, List<String> arguments)
	{
		Player p = Bukkit.getPlayer(UUID.fromString(arguments.get(0))); //Change to UUID
		
		if (p != null && p.isOnline())
		{
			p.setHealth(p.getMaxHealth());
			p.sendMessage(ChatColor.GOLD + "[Web] " + ChatColor.AQUA + "You have been healed.");
			
			r.put("good", "Successfully healed " + p.getName());
		} else
			r.put("error", "The player is currently not online.");
	}
	
	protected static void feed(JSONObject r, List<String> arguments)
	{
		Player p = Bukkit.getPlayer(UUID.fromString(arguments.get(0))); //Change to UUID
		
		if (p != null && p.isOnline())
		{
			p.setFoodLevel(20);
			p.sendMessage(ChatColor.GOLD + "[Web] " + ChatColor.AQUA + "You have been fed.");
			
			r.put("good", "Successfully fed " + p.getName());
		} else
			r.put("error", "The player is currently not online.");
	}
	
	protected static void kill(JSONObject r, List<String> arguments)
	{
		Player p = Bukkit.getPlayer(UUID.fromString(arguments.get(0))); //Change to UUID
		
		if (p != null && p.isOnline())
		{
			p.setHealth(0);
			p.sendMessage(ChatColor.GOLD + "[Web] " + ChatColor.AQUA + "You have been killed.");
			
			r.put("good", "Successfully killed " + p.getName());
		} else
			r.put("error", "The player is currently not online.");
	}
	
	protected static void kick(JSONObject r, List<String> arguments)
	{
		Player p = Bukkit.getPlayer(UUID.fromString(arguments.get(0))); //Change to UUID
		
		if (p != null && p.isOnline())
		{
			if (arguments.get(2) != null)
			{
				p.kickPlayer(arguments.get(2));
				
				r.put("good", "Successfully kicked " + p.getName());
			} else
				r.put("error", "Please enter a message.");
		} else
			r.put("error", "The player is currently not online.");
	}
	
	protected static void ban(JSONObject r, List<String> arguments)
	{
		OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(arguments.get(0))); //Change to UUID
		
		op.setBanned(true);
		
		if (op.isOnline())
			if (arguments.get(2) != null)
				op.getPlayer().kickPlayer(arguments.get(2));
			else
				r.put("error", "Please enter a message.");
		
		r.put("good", "Successfully banned " + op.getName());
	}
}