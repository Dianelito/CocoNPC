package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.CocoNPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

public final class MobListener implements Listener {
    private final MobRegistry registry;

    public MobListener(CocoNPC plugin, MobRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDamage(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDeath(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        CustomMob mob = registry.getByDriver(damager.getUniqueId());
        if (mob != null) {
            mob.triggerAttack();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onAggro(event.getTarget());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction)) return;

        CustomMob mob = registry.getByInteraction(event.getRightClicked());
        if (mob != null) {
            mob.onInteraction(event.getPlayer(), event.getHand());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        CustomMob mob = registry.getByDriver(event.getEntity().getUniqueId());
        if (mob != null) {
            mob.onDriverRemoved(event.getCause());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        registry.handleEntitiesLoad(event.getChunk(), event.getEntities());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        registry.handleChunkUnload(event.getChunk());
    }
}
