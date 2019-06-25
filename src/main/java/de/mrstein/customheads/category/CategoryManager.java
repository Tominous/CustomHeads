package de.mrstein.customheads.category;

import de.mrstein.customheads.CustomHeads;
import de.mrstein.customheads.utils.ItemEditor;
import de.mrstein.customheads.utils.JsonFile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/*
 *  Project: CustomHeads in CategoryLoader
 *     by LikeWhat
 */

@Getter
public class CategoryManager {

    @Getter
    private static boolean loaded;
    private HashMap<String, SubCategory> subCategories = new HashMap<>();
    private HashMap<String, Category> categories = new HashMap<>();
    private HashMap<Category, File> sourceFiles = new HashMap<>();
    private File langRootDir;

    private String language;

    public CategoryManager(String language) {
        loaded = false;
        this.language = language;
        langRootDir = new File("plugins/CustomHeads/language/" + language + "/categories");

        loaded = false;
        int loaded = 0;
        int ignored = 0;
        int invalid = 0;
        if (!CustomHeads.hasReducedDebug())
            CustomHeads.getInstance().getServer().getConsoleSender().sendMessage(CustomHeads.chPrefix + "Loading " + CustomHeads.getCategoryLoaderConfig().get().getList("categories").size() + " Categories from " + language + "/categories");
        long timestamp = System.currentTimeMillis();
        CustomHeads.getCategoryLoaderConfig().reload();
        boolean ignoreInvalid = CustomHeads.getCategoryLoaderConfig().get().getBoolean("ignoreInvalid");

        File langRootDir = new File("plugins/CustomHeads/language/" + language + "/categories");
        if (langRootDir.listFiles() == null) {
            CustomHeads.getInstance().getServer().getConsoleSender().sendMessage(CustomHeads.chWarning + "No Categories found in language/" + language + "/categories");
            CategoryManager.loaded = true;
            return;
        }

        List<File> fileList = Arrays.stream(langRootDir.listFiles()).filter(file -> file.getName().endsWith(".json") && CustomHeads.getCategoryLoaderConfig().get().getList("categories").contains(file.getName().substring(0, file.getName().lastIndexOf(".")))).collect(Collectors.toList());

        for (File file : fileList) {
            JsonFile jsf = new JsonFile(file);
            ignored = CustomHeads.getCategoryLoaderConfig().get().getList("categories").size() - fileList.size();
            try {
                Category category = Category.getConverter().fromJson(jsf.getJson(), Category.class);
                if (category == null) {
                    if (ignoreInvalid) {
                        invalid++;
                        Bukkit.getServer().getConsoleSender().sendMessage(CustomHeads.chWarning + " Invalid Category in " + file.getName());
                    } else {
                        throw new NullPointerException("Invalid Category in " + file.getName());
                    }
                } else {
                    if (categories.containsKey(category.getId())) {
                        CustomHeads.getInstance().getServer().getConsoleSender().sendMessage(CustomHeads.chWarning + file.getName() + ": §cAn Category with ID " + category.getId() + " (" + categories.get(category.getId()).getName() + ") already exists.");
                        invalid++;
                        continue;
                    }
                    categories.put(category.getId(), category);
                    sourceFiles.put(category, file);
                    loaded++;
                    if (category.hasSubCategories()) {
                        for (SubCategory subCategory : category.getSubCategories()) {
                            if (subCategories.containsKey(subCategory.getId())) {
                                CustomHeads.getInstance().getServer().getConsoleSender().sendMessage(CustomHeads.chWarning + file.getName() + ": §cAn Subcategory with ID " + subCategory.getId() + " (" + subCategories.get(subCategory.getId()).getName() + ") already exists.");
                                continue;
                            }
                            subCategories.put(subCategory.getId(), subCategory);
                        }
                    }
                }
            } catch (Exception e) {
                if (ignoreInvalid) {
                    invalid++;
                } else {
                    CustomHeads.getInstance().getLogger().log(Level.WARNING, "Something went wrong while loading Category File " + file.getName(), e);
                    return;
                }
            }
        }

        if (!CustomHeads.hasReducedDebug())
            CustomHeads.getInstance().getServer().getConsoleSender().sendMessage(CustomHeads.chPrefix + "Successfully loaded " + loaded + " Categories and " + getAllHeads().size() + " Heads from " + language + "/categories in " + (System.currentTimeMillis() - timestamp) + "ms " + (ignoreInvalid ? "(" + (ignored + invalid) + " " + ((ignored + invalid) == 1 ? "Category was" : "Categories were") + " ignored - " + ignored + " not loaded or not found, " + (invalid > 0 ? "§c" : "") + invalid + " Invalid§7)" : ""));
        CategoryManager.loaded = true;
    }

    public void updateCategory(Category category, String forLanguage) {
        File categoryFile;
        if (!(categoryFile = new File(CustomHeads.getInstance().getDataFolder() + "/language/" + forLanguage)).exists())
            throw new IllegalArgumentException("Language " + forLanguage + " does not exist");
        try (OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(new File(categoryFile + "/categories", category.getName() + ".json")), StandardCharsets.UTF_8)) {
            outputStream.write(category.toString());
            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int importSingle(File file) {
        JsonFile jsf = new JsonFile(file);
        try {
            Category category = Category.getConverter().fromJson(jsf.getJson(), Category.class);
            if (category == null) {
                return 3;
            } else {
                if (categories.containsKey(category.getId())) {
                    return 1;
                }
                categories.put(category.getId(), category);
                sourceFiles.put(category, file);
                if (category.hasSubCategories()) {
                    for (SubCategory subCategory : category.getSubCategories()) {
                        if (subCategories.containsKey(subCategory.getId())) {
                            return 2;
                        }
                        subCategories.put(subCategory.getId(), subCategory);
                    }
                }
                List<String> loadedCategories = CustomHeads.getCategoryLoaderConfig().get().isList("categories") ? CustomHeads.getCategoryLoaderConfig().get().getStringList("categories") : new ArrayList<>();
                loadedCategories.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
                CustomHeads.getCategoryLoaderConfig().get().set("categories", loadedCategories);
                CustomHeads.getCategoryLoaderConfig().save();
            }
        } catch (Exception e) {
            CustomHeads.getInstance().getLogger().log(Level.WARNING, "Something went wrong while loading Category File " + file.getName(), e);
            return 4;
        }
        return 0;
    }

    /**
     * Removes an <code>Category</code> from the Loader Config
     *
     * @param category Category to be removed
     * @return Returns if Category was removed
     */
    public boolean removeCategory(Category category) {
        if (categories.containsKey(category.getId())) {
            categories.remove(category.getId());
            List<String> loadedCategories = CustomHeads.getCategoryLoaderConfig().get().isList("categories") ? CustomHeads.getCategoryLoaderConfig().get().getStringList("categories") : new ArrayList<>();
            loadedCategories.remove(CustomHeads.getCategoryManager().getSourceFile(category).getName().substring(0, CustomHeads.getCategoryManager().getSourceFile(category).getName().lastIndexOf(".")));
            CustomHeads.getCategoryLoaderConfig().get().set("categories", loadedCategories);
            CustomHeads.getCategoryLoaderConfig().save();
            return true;
        }
        return false;
    }

    /**
     * Permanently removes an <code>Category</code>
     *
     * @param category Category to be removed
     * @return Returns if Category was removed
     */
    public boolean deleteCategory(Category category) {
        return categories.containsKey(category.getId()) && getSourceFile(category).delete();
    }

    /**
     * Permanently removes an <code>SubCategory</code>
     *
     * @param subCategory SubCategory to be removed
     * @return Returns if Category was removed
     */
    public boolean deleteSubCategory(SubCategory subCategory) {
        Category originCategory = subCategory.getOriginCategory();
        if (subCategories.containsKey(subCategory.getId()) && originCategory != null) {
            List<SubCategory> subCategories = subCategory.getOriginCategory().getSubCategories();
            subCategories.remove(subCategory);
            originCategory.setSubCategories(subCategories);
            categories.put(originCategory.getId(), originCategory);

            // Rewrite Category File
            File categoryFile = getSourceFile(originCategory);
            try (OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(categoryFile), StandardCharsets.UTF_8)) {
                outputStream.write(originCategory.toString());
                outputStream.flush();
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    public Category createCategory(String name) {
        return new Category(nextCategoryID(), name, name.toLowerCase(), 0, new ItemEditor(Material.SKULL_ITEM, (byte) 3).setTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19").setDisplayName("Category Icon").setLore("§7Replace this Item\n§7to set the Icon").getItem());
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    public SubCategory getSubCategory(String id) {
        return subCategories.get(id);
    }

    public List<Category> getCategoryList() {
        return new ArrayList<>(categories.values());
    }

    public List<SubCategory> getSubCategoryList() {
        return new ArrayList<>(subCategories.values());
    }

    public File getSourceFile(Category category) {
        return sourceFiles.get(category);
    }

    public List<BaseCategory> getAllCategories() {
        List<BaseCategory> categories = new ArrayList<>(getCategoryList());
        categories.addAll(getSubCategoryList());
        return categories;
    }

    public List<CustomHead> getAllHeads() {
        List<CustomHead> customHeads = new ArrayList<>();
        for (Category category : categories.values()) {
            if (category.hasSubCategories()) {
                for (SubCategory subCategory : category.getSubCategories()) {
                    customHeads.addAll(subCategory.getHeads());
                }
            } else {
                customHeads.addAll(category.getHeads());
            }
        }
        return customHeads;

        List<ItemStack> heads = new ArrayList<>();
        for (Category category : categories.values()) {
            if (category.hasSubCategories()) {
                for (SubCategory subCategory : category.getSubCategories()) {
                    for (ItemStack itemStack : subCategory.getHeads()) {
                        heads.add(CustomHeads.getTagEditor().addTags(itemStack, "category", subCategory.getOriginCategory().getId(), "wearable"));
                    }
                }
            } else {
                for (ItemStack itemStack : category.getHeads()) {
                    heads.add(CustomHeads.getTagEditor().addTags(itemStack, "category", category.getId(), "wearable"));
                }
            }
        }
        return heads;
    }

    private int nextCategoryID() {
        int id = 0;
        while (categories.containsKey(String.valueOf(id))) {
            id++;
        }
        return id;
    }

}
