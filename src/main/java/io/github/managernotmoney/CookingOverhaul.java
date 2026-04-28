package io.github.managernotmoney;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

public class CookingOverhaul extends JavaPlugin implements Listener {

    private NamespacedKey flourKey;
    private NamespacedKey doughKey;
    private NamespacedKey breadKey;
    private NamespacedKey doughRecipeKey;
    private NamespacedKey doughFurnaceKey;
    private NamespacedKey doughSmokerKey;

    private static final String FLOUR_NAME = "§fМука";
    private static final String DOUGH_NAME = "§fТесто";

    @Override
    public void onEnable() {
        getLogger().info("CookingOverhaul Загружен!");

        this.flourKey = new NamespacedKey(this, "custom_flour");
        this.doughKey = new NamespacedKey(this, "custom_dough");
        this.breadKey = NamespacedKey.minecraft("bread");
        this.doughRecipeKey = new NamespacedKey(this, "dough_from_flour");
        addDoughRecipe();
        this.doughFurnaceKey = new NamespacedKey(this, "dough_furnace");
        this.doughSmokerKey = new NamespacedKey(this, "dough_smoker");
        addDoughSmeltingRecipes();

        Bukkit.getPluginManager().registerEvents(this, this);
        removeVanillaBreadRecipe();
    }
    public ItemStack getFlour() {
        ItemStack flour = new ItemStack(Material.BONE_MEAL);
        ItemMeta meta = flour.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(FLOUR_NAME));
            meta.getPersistentDataContainer().set(flourKey, PersistentDataType.BOOLEAN, true);
            flour.setItemMeta(meta);
        }
        return flour;
    }
    public ItemStack getDough() {
        ItemStack dough = new ItemStack(Material.WHITE_DYE);
        ItemMeta meta = dough.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(DOUGH_NAME));
            meta.getPersistentDataContainer().set(doughKey, PersistentDataType.BOOLEAN, true);
            dough.setItemMeta(meta);
        }
        return dough;
    }
    private void removeVanillaBreadRecipe() {
        boolean removed = Bukkit.removeRecipe(breadKey);
        if (removed) {
            getLogger().info("Стандартный рецепт хлеба отключён.");
        } else {
            getLogger().warning("Не удалось найти рецепт хлеба.");
        }
    }
    private void addDoughRecipe() {
        ItemStack flourExample = getFlour();
        RecipeChoice flourChoice = new RecipeChoice.ExactChoice(flourExample);
        RecipeChoice waterChoice = new RecipeChoice.MaterialChoice(Material.WATER_BUCKET);

        ShapelessRecipe recipe = new ShapelessRecipe(doughRecipeKey, getDough());
        recipe.addIngredient(flourChoice);
        recipe.addIngredient(flourChoice);
        recipe.addIngredient(flourChoice);
        recipe.addIngredient(waterChoice);
        Bukkit.addRecipe(recipe);
    }
    private void restoreVanillaBreadRecipe() {
        if (Bukkit.getRecipe(breadKey) != null) {
            getLogger().info("Рецепт хлеба уже существует, восстановление не требуется.");
            return;
        }
        ShapedRecipe breadRecipe = new ShapedRecipe(breadKey, new ItemStack(Material.BREAD));
        breadRecipe.shape("WWW");
        breadRecipe.setIngredient('W', Material.WHEAT);
        Bukkit.addRecipe(breadRecipe);
        getLogger().info("Стандартный рецепт хлеба восстановлен.");
    }
    @EventHandler
    public void onBoneMealUse(BlockFertilizeEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isCustomItem(item, Material.BONE_MEAL, flourKey)) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onFlourCraft(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (mainHand != null && mainHand.getType() == Material.WHEAT
                    && offHand != null && offHand.getType() == Material.SHEARS) {
                int amount = mainHand.getAmount();
                ItemStack flour = getFlour();
                flour.setAmount(amount);
                player.getInventory().setItemInMainHand(flour);

                player.playSound(player.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.0f);

                ItemMeta shearsMeta = offHand.getItemMeta();
                if (shearsMeta instanceof Damageable damageable) {
                    damageable.setDamage(damageable.getDamage() + 1);
                    offHand.setItemMeta((ItemMeta) damageable);
                    if (damageable.getDamage() > offHand.getType().getMaxDurability()) {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onDyeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (isCustomItem(item, Material.WHITE_DYE, doughKey)) {
            event.setCancelled(true);
        }
    }
    private void addDoughSmeltingRecipes() {
        ItemStack doughExample = getDough();
        RecipeChoice doughChoice = new RecipeChoice.ExactChoice(doughExample);
        ItemStack result = new ItemStack(Material.BREAD);
        float exp = 0.35f;
        int furnaceTime = 300;
        int smokerTime = 150;

        FurnaceRecipe furnaceRecipe = new FurnaceRecipe(
                doughFurnaceKey, result, doughChoice, exp, furnaceTime);
        SmokingRecipe smokingRecipe = new SmokingRecipe(
                doughSmokerKey, result, doughChoice, exp, smokerTime);

        Bukkit.addRecipe(furnaceRecipe);
        Bukkit.addRecipe(smokingRecipe);
    }
    private boolean isCustomItem(ItemStack item, Material material, NamespacedKey key) {
        if (item == null || item.getType() != material) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
    @Override
    public void onDisable() {
        getLogger().info("CookingOverhaul Выгружен!");
        restoreVanillaBreadRecipe();
    }
}