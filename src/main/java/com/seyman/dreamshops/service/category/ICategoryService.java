package com.seyman.dreamshops.service.category;

import com.seyman.dreamshops.model.Category;

import java.util.List;

public interface ICategoryService {

    Category addCategory(Category category);
    Category updateCategory(Category category, Long id);
    Category getCategoryById(Long id);
    Category getCategoryByName(String name);
    void deleteCategoryById(Long id);
    List<Category> getAllCategories();
}
