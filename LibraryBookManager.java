// JavaFX Library Database App (All classes in one file)
// Group Members: Your Name (Main Logic and Integration)

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;

public class LibraryBookManager extends Application {
    private TableView<Book> tableView = new TableView<>();
    private TextField titleField = new TextField();
    private TextField yearField = new TextField();
    private ComboBox<Author> authorComboBox = new ComboBox<>();

    private DatabaseManager dbManager;

    @Override
    public void start(Stage primaryStage) {
        dbManager = new DatabaseManager();

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> cellData.getValue().titleProperty());

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(cellData -> cellData.getValue().authorNameProperty());

        TableColumn<Book, Integer> yearCol = new TableColumn<>("Year Published");
        yearCol.setCellValueFactory(cellData -> cellData.getValue().yearPublishedProperty().asObject());

        tableView.getColumns().addAll(titleCol, authorCol, yearCol);
        tableView.setItems(dbManager.getAllBooks());

        titleField.setPromptText("Title");
        yearField.setPromptText("Year (e.g. 1984)");
        authorComboBox.setItems(dbManager.getAllAuthors());

        Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            String title = titleField.getText();
            String year = yearField.getText();
            Author author = authorComboBox.getValue();
            if (title.isEmpty() || year.isEmpty() || author == null) {
                showAlert("All fields are required.");
                return;
            }
            try {
                dbManager.addBook(title, author.getAuthorID(), Integer.parseInt(year));
                refreshData();
            } catch (NumberFormatException ex) {
                showAlert("Year must be a valid 4-digit number.");
            }
        });

        Button updateButton = new Button("Update");
        updateButton.setOnAction(e -> {
            Book selected = tableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Select a book to update.");
                return;
            }
            String title = titleField.getText();
            String year = yearField.getText();
            Author author = authorComboBox.getValue();
            try {
                dbManager.updateBook(selected.getBookID(), title, author.getAuthorID(), Integer.parseInt(year));
                refreshData();
            } catch (Exception ex) {
                showAlert("Invalid input.");
            }
        });

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> {
            Book selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                dbManager.deleteBook(selected.getBookID());
                refreshData();
            }
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshData());

        HBox inputBox = new HBox(10, titleField, authorComboBox, yearField);
        HBox buttonBox = new HBox(10, addButton, updateButton, deleteButton, refreshButton);
        VBox root = new VBox(10, tableView, inputBox, buttonBox);
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 700, 400));
        primaryStage.setTitle("Library Database Manager");
        primaryStage.show();
    }

    private void refreshData() {
        tableView.setItems(dbManager.getAllBooks());
        authorComboBox.setItems(dbManager.getAllAuthors());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Book {
    private final IntegerProperty bookID;
    private final StringProperty title;
    private final StringProperty authorName;
    private final IntegerProperty yearPublished;

    public Book(int bookID, String title, String authorName, int yearPublished) {
        this.bookID = new SimpleIntegerProperty(bookID);
        this.title = new SimpleStringProperty(title);
        this.authorName = new SimpleStringProperty(authorName);
        this.yearPublished = new SimpleIntegerProperty(yearPublished);
    }

    public int getBookID() { return bookID.get(); }
    public String getTitle() { return title.get(); }
    public String getAuthorName() { return authorName.get(); }
    public int getYearPublished() { return yearPublished.get(); }

    public IntegerProperty bookIDProperty() { return bookID; }
    public StringProperty titleProperty() { return title; }
    public StringProperty authorNameProperty() { return authorName; }
    public IntegerProperty yearPublishedProperty() { return yearPublished; }
}

class Author {
    private final IntegerProperty authorID;
    private final StringProperty name;

    public Author(int authorID, String name) {
        this.authorID = new SimpleIntegerProperty(authorID);
        this.name = new SimpleStringProperty(name);
    }

    public int getAuthorID() { return authorID.get(); }
    public String getName() { return name.get(); }

    @Override
    public String toString() {
        return name.get();
    }
}

class DatabaseManager {
    private final String URL = "jdbc:mysql://localhost:3306/library";
    private final String USER = "scott";
    private final String PASSWORD = "tiger";
    private Connection conn;

    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ObservableList<Book> getAllBooks() {
        ObservableList<Book> books = FXCollections.observableArrayList();
        String query = "SELECT b.BookID, b.Title, a.Name, b.YearPublished FROM Books b JOIN Authors a ON b.AuthorID = a.AuthorID";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public ObservableList<Author> getAllAuthors() {
        ObservableList<Author> authors = FXCollections.observableArrayList();
        String query = "SELECT * FROM Authors";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                authors.add(new Author(rs.getInt("AuthorID"), rs.getString("Name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return authors;
    }

    public void addBook(String title, int authorID, int yearPublished) {
        String query = "INSERT INTO Books (Title, AuthorID, YearPublished) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setInt(2, authorID);
            stmt.setInt(3, yearPublished);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateBook(int bookID, String title, int authorID, int yearPublished) {
        String query = "UPDATE Books SET Title = ?, AuthorID = ?, YearPublished = ? WHERE BookID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, title);
            stmt.setInt(2, authorID);
            stmt.setInt(3, yearPublished);
            stmt.setInt(4, bookID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteBook(int bookID) {
        String query = "DELETE FROM Books WHERE BookID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookID);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
