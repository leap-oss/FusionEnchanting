package org.leap.fusionenchanting.core;

public enum Equipment {
    SWORD, BOW, CROSSBOW, PICKAXE, TOOL, ARMOR, BOOTS, TRIDENT, MACE, FISHING_ROD, HELMET;

    public String label() {
        return switch (this) {
            case SWORD -> "Swords";
            case BOW -> "Bows";
            case CROSSBOW -> "Crossbow";
            case PICKAXE -> "Pickaxes";
            case TOOL -> "Mining tools";
            case ARMOR -> "Armor";
            case BOOTS -> "Boots";
            case TRIDENT -> "Tridents";
            case MACE -> "Mace";
            case FISHING_ROD -> "Fishing rods";
            case HELMET -> "Helmets";
        };
    }

    public boolean accepts(String material) {
        return switch (this) {
            case SWORD -> material.endsWith("_SWORD");
            case PICKAXE -> material.endsWith("_PICKAXE");
            case TOOL -> material.endsWith("_PICKAXE") || material.endsWith("_AXE")
                    || material.endsWith("_SHOVEL") || material.endsWith("_HOE");
            case ARMOR -> material.endsWith("_HELMET") || material.endsWith("_CHESTPLATE")
                    || material.endsWith("_LEGGINGS") || material.endsWith("_BOOTS");
            case BOOTS -> material.endsWith("_BOOTS");
            case HELMET -> material.endsWith("_HELMET");
            default -> name().equals(material);
        };
    }
}
