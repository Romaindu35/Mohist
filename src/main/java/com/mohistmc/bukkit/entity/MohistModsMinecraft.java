package com.mohistmc.bukkit.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMinecart;

public class MohistModsMinecraft extends CraftMinecart {

    public MohistModsMinecraft(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "MohistModsMinecraft{" + getType() + '}';
    }
}
