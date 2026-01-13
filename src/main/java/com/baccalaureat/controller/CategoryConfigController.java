package com.baccalaureat.controller;

import com.baccalaureat.model.Category;
import com.baccalaureat.service.CategoryService;
import com.baccalaureat.service.CategoryService.CategoryCreationResult;
import com.baccalaureat.service.CategoryService.CategoryUpdateResult;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.baccalaureat.util.DialogHelper;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for category management interface.
 * Allows users to add, edit, rename, and delete game categories.
 */
public class CategoryConfigController implements Initializable {
    
    @FXML private TableView<CategoryRow> categoryTable;
    @FXML private TableColumn<CategoryRow, String> displayNameColumn;
    @FXML private TableColumn<CategoryRow, String> iconColumn;
    @FXML private TableColumn<CategoryRow, Boolean> enabledColumn;
    @FXML private TableColumn<CategoryRow, Void> actionsColumn;
    
    @FXML private TextField displayNameField;
    @FXML private TextField iconField;
    @FXML private TextArea hintField;
    @FXML private Button addButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    
    private final CategoryService categoryService;
    private final ObservableList<CategoryRow> categoryData;
    
    public CategoryConfigController() {
        this.categoryService = new CategoryService();
        this.categoryData = FXCollections.observableArrayList();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupForm();
        loadCategories();
    }
    
    private void setupTable() {
        // Configure columns
        displayNameColumn.setCellValueFactory(new PropertyValueFactory<>("displayName"));
        iconColumn.setCellValueFactory(new PropertyValueFactory<>("icon"));
        
        // Enabled column with checkboxes
        enabledColumn.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        enabledColumn.setCellFactory(col -> new TableCell<CategoryRow, Boolean>() {
            private final CheckBox checkBox = new CheckBox();
            
            @Override
            protected void updateItem(Boolean enabled, boolean empty) {
                super.updateItem(enabled, empty);
                
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CategoryRow row = getTableRow().getItem();
                    checkBox.setSelected(enabled != null && enabled);
                    checkBox.setOnAction(e -> {
                        boolean newState = checkBox.isSelected();
                        if (newState != row.isEnabled()) {
                            toggleCategoryEnabled(row.getCategory(), newState);
                        }
                    });
                    setGraphic(checkBox);
                }
            }
        });
        
        // Actions column with edit/delete buttons
        actionsColumn.setCellFactory(col -> new TableCell<CategoryRow, Void>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final Label predefinedBadge = new Label("Predefined");
            private final HBox buttonsBox = new HBox(5);
            
            {
                // Set minimum button sizes to ensure visibility
                editButton.setMinWidth(120);
                editButton.setPrefWidth(120);
                deleteButton.setMinWidth(120);
                deleteButton.setPrefWidth(120);
                
                editButton.getStyleClass().add("button-primary");
                deleteButton.getStyleClass().add("button-danger");
                predefinedBadge.getStyleClass().add("badge-info");
                predefinedBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 3;");
                
                // Configure HBox for proper centering and sizing
                buttonsBox.setAlignment(Pos.CENTER);
                buttonsBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
                buttonsBox.setMinWidth(Region.USE_PREF_SIZE);
                
                editButton.setOnAction(e -> {
                    CategoryRow row = getTableRow().getItem();
                    if (row != null && !row.isPredefined()) {
                        editCategory(row.getCategory());
                    }
                });
                
                deleteButton.setOnAction(e -> {
                    CategoryRow row = getTableRow().getItem();
                    if (row != null && !row.isPredefined()) {
                        deleteCategory(row.getCategory());
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CategoryRow row = getTableRow().getItem();
                    buttonsBox.getChildren().clear();
                    
                    if (row.isPredefined()) {
                        // Predefined category - show badge only
                        buttonsBox.getChildren().add(predefinedBadge);
                    } else {
                        // Custom category - show edit and delete buttons
                        buttonsBox.getChildren().addAll(editButton, deleteButton);
                    }
                    
                    setGraphic(buttonsBox);
                }
            }
        });
        
        categoryTable.setItems(categoryData);
        categoryTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    private void setupForm() {
        // Bind button state to form validity - only display name is required
        addButton.disableProperty().bind(
            displayNameField.textProperty().isEmpty()
        );
        
        // Default icon if empty
        iconField.setPromptText("📝");
        hintField.setPromptText("A custom category");
    }
    
    /**
     * Generate internal category name from display name.
     * Example: "Pays du monde" -> "PAYS_DU_MONDE"
     */
    private String generateInternalName(String displayName) {
        return displayName.toUpperCase()
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");
    }
    
    private void loadCategories() {
        categoryData.clear();
        categoryService.getAllCategories().stream()
            .map(CategoryRow::new)
            .forEach(categoryData::add);
    }
    
    @FXML
    private void onAddCategory() {
        String displayName = displayNameField.getText().trim();
        String icon = iconField.getText().trim();
        String hint = hintField.getText().trim();
        
        // Validate required field
        if (displayName.isEmpty()) {
            showStatus("Error: Display name is required", true);
            return;
        }
        
        // Auto-generate internal name from display name
        String internalName = generateInternalName(displayName);
        
        // Use defaults if optional fields are empty
        if (icon.isEmpty()) icon = "📝";
        if (hint.isEmpty()) hint = "A custom category";
        
        CategoryCreationResult result = categoryService.createCategory(internalName, displayName, icon, hint);
        
        if (result.isSuccess()) {
            showStatus("Category created successfully!", false);
            clearForm();
            loadCategories();
        } else {
            showStatus("Erreur : " + result.getErrorMessage(), true);
        }
    }
    
    @FXML
    private void onCancel() {
        clearForm();
    }
    
    private void clearForm() {
        displayNameField.clear();
        iconField.clear();
        hintField.clear();
        hideStatus();
    }
    
    private void editCategory(Category category) {
        // Prevent editing predefined categories
        if (category.isPredefined()) {
            showStatus("Predefined categories cannot be modified", true);
            return;
        }
        
        // Create edit dialog
        Dialog<CategoryUpdateData> dialog = createEditDialog(category);
        Optional<CategoryUpdateData> result = dialog.showAndWait();
        
        result.ifPresent(data -> {
            // Only update user-visible fields - internal name is immutable
            CategoryUpdateResult updateResult = categoryService.updateCategory(
                category.getId(), data.displayName, data.icon, data.hint
            );
            
            if (updateResult.isSuccess()) {
                showStatus("Category modified successfully!", false);
                loadCategories();
            } else {
                showStatus("Erreur : " + updateResult.getErrorMessage(), true);
            }
        });
    }
    
    private Dialog<CategoryUpdateData> createEditDialog(Category category) {
        Dialog<CategoryUpdateData> dialog = new Dialog<>();
        dialog.setTitle("Edit category");
        dialog.setHeaderText("Edit " + category.getDisplayName());
        
        // Form controls - NO internal name field for users
        TextField displayNameField = new TextField(category.getDisplayName());
        TextField iconField = new TextField(category.getIcon());
        TextArea hintField = new TextArea(category.getHint());
        
        displayNameField.setPromptText("Display name");
        iconField.setPromptText("Icon");
        hintField.setPromptText("Description");
        hintField.setPrefRowCount(3);
        
        // Layout - only user-friendly fields
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Display name *:"), displayNameField,
            new Label("Icon :"), iconField,
            new Label("Description :"), hintField
        );
        content.setPadding(new Insets(20));
        
        dialog.getDialogPane().setContent(content);
        
        // Buttons
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Enable save button only when required fields are filled
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
            displayNameField.textProperty().isEmpty()
        );
        
        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return new CategoryUpdateData(
                    category.getName(), // Keep original internal name unchanged
                    displayNameField.getText().trim(),
                    iconField.getText().trim(),
                    hintField.getText().trim(),
                    false // nameChanged = false, internal name never changes via UI
                );
            }
            return null;
        });
        
        return dialog;
    }
    
    private void deleteCategory(Category category) {
        // Prevent deletion of predefined categories
        if (category.isPredefined()) {
            showStatus("Predefined categories cannot be deleted", true);
            return;
        }
        
        boolean confirmed = DialogHelper.showConfirmation(
            "Delete category",
            "Are you sure?",
            "This action will permanently delete the category \"" + category.getDisplayName() + "\"."
        );
        
        if (confirmed) {
            boolean success = categoryService.deleteCategory(category.getId());
            if (success) {
                showStatus("Catégorie supprimée avec succès !", false);
                loadCategories();
            } else {
                showStatus("Erreur lors de la suppression de la catégorie", true);
            }
        }
    }
    
    private void toggleCategoryEnabled(Category category, boolean enabled) {
        // Proper enable/disable logic:
        // - Enable: enableCategory() sets enabled=true
        // - Disable: disableCategory() sets enabled=false (NOT delete)
        // Both operations preserve the category in the database
        boolean success = enabled ? 
            categoryService.enableCategory(category.getId()) : 
            categoryService.disableCategory(category.getId());
            
        if (success) {
            showStatus(enabled ? "Catégorie activée" : "Catégorie désactivée", false);
            loadCategories();
        } else {
            showStatus("Erreur lors de la modification", true);
            loadCategories(); // Refresh to revert checkbox
        }
    }
    
    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-success", "status-error");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-success");
        statusLabel.setVisible(true);
    }
    
    private void hideStatus() {
        statusLabel.setVisible(false);
    }
    
    @FXML
    private void onClose() {
        Stage stage = (Stage) categoryTable.getScene().getWindow();
        stage.close();
    }
    
    // Data classes
    public static class CategoryRow {
        private final Category category;
        private final StringProperty name;
        private final StringProperty displayName;
        private final StringProperty icon;
        private final BooleanProperty enabled;
        private final BooleanProperty predefined;
        
        public CategoryRow(Category category) {
            this.category = category;
            this.name = new javafx.beans.property.SimpleStringProperty(category.getName());
            this.displayName = new javafx.beans.property.SimpleStringProperty(category.getDisplayName());
            this.icon = new javafx.beans.property.SimpleStringProperty(category.getIcon());
            this.enabled = new SimpleBooleanProperty(category.isEnabled());
            this.predefined = new SimpleBooleanProperty(category.isPredefined());
        }
        
        // Property getters for TableView
        public StringProperty nameProperty() { return name; }
        public StringProperty displayNameProperty() { return displayName; }
        public StringProperty iconProperty() { return icon; }
        public BooleanProperty enabledProperty() { return enabled; }
        public BooleanProperty predefinedProperty() { return predefined; }
        
        // Getters
        public String getName() { return name.get(); }
        public String getDisplayName() { return displayName.get(); }
        public String getIcon() { return icon.get(); }
        public boolean isEnabled() { return enabled.get(); }
        public boolean isPredefined() { return predefined.get(); }
        public Category getCategory() { return category; }
    }
    
    private static class CategoryUpdateData {
        final String name;
        final String displayName;
        final String icon;
        final String hint;
        final boolean nameChanged;
        
        CategoryUpdateData(String name, String displayName, String icon, String hint, boolean nameChanged) {
            this.name = name;
            this.displayName = displayName;
            this.icon = icon;
            this.hint = hint;
            this.nameChanged = nameChanged;
        }
    }
}