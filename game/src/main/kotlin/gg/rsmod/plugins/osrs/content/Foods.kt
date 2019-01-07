package gg.rsmod.plugins.osrs.content

import gg.rsmod.game.fs.def.ItemDef
import gg.rsmod.game.model.INTERACTING_ITEM_SLOT
import gg.rsmod.game.model.TimerKey
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.plugin.PluginRepository
import gg.rsmod.game.plugin.ScanPlugins
import gg.rsmod.plugins.hasEquipped
import gg.rsmod.plugins.osrs.model.Equipment
import gg.rsmod.plugins.osrs.model.Skills
import gg.rsmod.plugins.osrs.content.combat.Combat
import gg.rsmod.plugins.playSound
import gg.rsmod.plugins.player

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Foods {

    private val FOOD_DELAY = TimerKey()
    private val COMBO_FOOD_DELAY = TimerKey()

    private const val EAT_FOOD_ANIM = 829
    private const val EAT_FOOD_ON_SLED_ANIM = 1469
    private const val EAT_FOOD_SOUND = 2393

    @JvmStatic
    @ScanPlugins
    fun register(r: PluginRepository) {
        Food.values().forEach { food ->
            r.bindItem(food.item, 1) {
                val p = it.player()

                if (!canEat(p, food)) {
                    return@bindItem
                }

                val inventorySlot = it.player().attr[INTERACTING_ITEM_SLOT]
                if (p.inventory.remove(id = food.item, fromIndex = inventorySlot).hasSucceeded()) {
                    eat(p, food)
                    if (food.replacement != -1) {
                        p.inventory.add(id = food.replacement, beginSlot = inventorySlot)
                    }
                }
            }
        }
    }

    fun canEat(p: Player, food: Food): Boolean = !p.timers.has(if (food.comboFood) COMBO_FOOD_DELAY else FOOD_DELAY)

    fun eat(p: Player, food: Food) {
        val delay = if (food.comboFood) COMBO_FOOD_DELAY else FOOD_DELAY
        val anim = if (p.hasEquipped(slot = Equipment.WEAPON, item = 1469)) EAT_FOOD_ON_SLED_ANIM else EAT_FOOD_ANIM

        val heal = when (food) {
            Food.ANGLERFISH -> {
                val c = when (p.getSkills().getMaxLevel(Skills.HITPOINTS)) {
                    in 25..49 -> 4
                    in 50..74 -> 6
                    in 75..92 -> 8
                    in 93..99 -> 13
                    else -> 2
                }
                Math.floor(p.getSkills().getMaxLevel(Skills.HITPOINTS) / 10.0).toInt() + c
            }
            else -> food.heal
        }

        val oldHp = p.getSkills().getCurrentLevel(Skills.HITPOINTS)
        val foodName = p.world.definitions.get(ItemDef::class.java, food.item).name

        p.animate(anim)
        p.playSound(EAT_FOOD_SOUND)
        if (heal > 0) {
            p.heal(heal, if (food.overheal) heal else 0)
        }

        p.timers[delay] = food.tickDelay
        p.timers[Combat.ATTACK_DELAY] = 5

        if (food == Food.KARAMBWAN) {
            // Eating Karambwans also blocks drinking potions.
            p.timers[Potions.POTION_DELAY] = 3
        }

        when (food) {
            else -> {
                p.message("You eat the ${foodName.toLowerCase()}.")
                if (p.getSkills().getCurrentLevel(Skills.HITPOINTS) > oldHp) {
                    p.message("It heals some health.")
                }
            }
        }
    }

    enum class Food(val item: Int, val heal: Int = 0, val overheal: Boolean = false,
                    val replacement: Int = -1, val tickDelay: Int = 3,
                    val comboFood: Boolean = false) {

        /**
         * Sea food.
         */
        SHRIMP(item = 315, heal = 3),
        SARDINE(item = 325, heal = 4),
        HERRING(item = 347, heal = 5),
        MACKEREL(item = 355, heal = 6),
        TROUT(item = 333, heal = 7),
        SALMON(item = 329, heal = 9),
        TUNA(item = 361, heal = 10),
        LOBSTER(item = 379, heal = 12),
        BASS(item = 365, heal = 13),
        SWORDFISH(item = 373, heal = 14),
        MONKFISH(item = 7946, heal = 16),
        KARAMBWAN(item = 3144, heal = 18, comboFood = true),
        SHARK(item = 385, heal = 20),
        MANTA_RAY(item = 391, heal = 21),
        DARK_CRAB(item = 11936, heal = 22),
        ANGLERFISH(item = 13441, overheal = true),

        /**
         * Meat.
         */
        CHICKEN(item = 2140, heal = 4),
        MEAT(item = 2142, heal = 4),

        /**
         * Pastries.
         */
        BREAD(item = 2309, heal = 5),
    }
}