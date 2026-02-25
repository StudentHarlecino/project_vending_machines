package com.example.project_vending_machines

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.project_vending_machines.dao.*
import com.example.project_vending_machines.entity.*
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.Priority
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import java.math.BigDecimal
import javafx.scene.control.cell.PropertyValueFactory
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.stage.Modality
import java.time.format.DateTimeParseException

class MainApp : Application() {

    private val vendingMachineDAO = VendingMachineDAO()
    private val productDAO = ProductDAO()
    private val saleDAO = SaleDAO()
    private val userDAO = UserDAO()
    private val maintenanceDAO = MaintenanceDAO()
    private val roleDAO = RoleDAO()

    private val mainContent = StackPane()
    private var currentData: List<Any> = listOf()
    private var currentEntityClass: Class<*>? = null
    private val tableView = TableView<Any>()

    // Для фильтрации
    private lateinit var filteredData: FilteredList<Any>
    private lateinit var filterField: TextField

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ООО Торговые Автоматы - Личный кабинет"

        val root = BorderPane()
        root.top = createHeader()
        root.left = createNavigation()
        root.center = mainContent

        showDashboard()

        val scene = Scene(root, 1400.0, 800.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun setError(field: TextField) {
        field.style = "-fx-border-color: #e74c3c; -fx-border-width: 2;"
    }

    private fun clearError(field: TextField) {
        field.style = ""
    }

    private fun validateField(
        field: TextField,
        required: Boolean,
        type: String?
    ): Boolean {

        val text = field.text.trim()

        if (required && text.isEmpty()) {
            setError(field)
            return false
        }

        if (text.isNotEmpty()) {
            try {
                when (type) {
                    "int" -> text.toInt()
                    "decimal" -> text.toBigDecimal()
                    "date" -> LocalDate.parse(text)
                    "datetime" -> LocalDateTime.parse(text)
                }
            } catch (e: Exception) {
                setError(field)
                return false
            }
        }

        clearError(field)
        return true
    }

    private fun createHeader(): HBox {
        val header = HBox()
        header.style = "-fx-background-color: #2c3e50; -fx-padding: 10px 15px;"
        header.alignment = Pos.CENTER_LEFT

        val title = Label("ООО Торговые Автоматы")
        title.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        header.children.addAll(title, spacer)

        return header
    }

    private fun createNavigation(): VBox {
        val navigation = VBox(5.0)
        navigation.style = "-fx-background-color: #34495e; -fx-padding: 15px;"
        navigation.prefWidth = 200.0

        val navTitle = Label("Навигация")
        navTitle.style = "-fx-text-fill: #bdc3c7; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 5 0;"
        navigation.children.add(navTitle)

        val mainNavItems = listOf(
            "Главная" to "🏠",
            "Монитор ТА" to "📊",
            "Детальные отчеты" to "📈",
            "Учет ТМЦ" to "📦"
        )

        mainNavItems.forEach { (item, icon) ->
            val button = createNavButton("$icon  $item", false)
            if (item == "Главная") {
                button.setOnAction { showDashboard() }
            }
            navigation.children.add(button)
        }

        val separator = Separator()
        separator.style = "-fx-background-color: #4a5a6e; -fx-padding: 5 0 5 0;"
        navigation.children.add(separator)

        val adminButton = createNavButton("⚙️  Администрирование", true)

        val adminMenu = VBox(3.0)
        adminMenu.style = "-fx-padding: 0 0 0 15;"
        adminMenu.isVisible = false

        val tableItems = listOf(
            "Торговые автоматы" to "🛒",
            "Продукты" to "🥤",
            "Продажи" to "💰",
            "Пользователи" to "👥",
            "Обслуживание" to "🔧"
        )

        tableItems.forEach { (item, icon) ->
            val tableButton = createNavButton("$icon  $item", false)
            when (item) {
                "Торговые автоматы" -> tableButton.setOnAction { showVendingMachines() }
                "Продукты" -> tableButton.setOnAction { showProducts() }
                "Продажи" -> tableButton.setOnAction { showSales() }
                "Пользователи" -> tableButton.setOnAction { showUsers() }
                "Обслуживание" -> tableButton.setOnAction { showMaintenance() }
            }
            adminMenu.children.add(tableButton)
        }

        adminButton.setOnAction {
            adminMenu.isVisible = !adminMenu.isVisible
        }

        navigation.children.addAll(adminButton, adminMenu)

        return navigation
    }

    private fun createNavButton(text: String, isSelected: Boolean): Button {
        val button = Button(text)
        button.style = if (isSelected)
            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-pref-width: 170px; -fx-alignment: CENTER_LEFT; -fx-font-size: 13px; -fx-padding: 6 8; -fx-background-radius: 0;"
        else
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-pref-width: 170px; -fx-alignment: CENTER_LEFT; -fx-font-size: 13px; -fx-padding: 6 8; -fx-background-radius: 0;"
        button.onMouseEntered = EventHandler {
            if (!isSelected) {
                button.style = "-fx-background-color: #3d566e; -fx-text-fill: white; -fx-pref-width: 170px; -fx-alignment: CENTER_LEFT; -fx-font-size: 13px; -fx-padding: 6 8; -fx-background-radius: 0;"
            }
        }
        button.onMouseExited = EventHandler {
            if (!isSelected) {
                button.style = "-fx-background-color: transparent; -fx-text-fill: white; -fx-pref-width: 170px; -fx-alignment: CENTER_LEFT; -fx-font-size: 13px; -fx-padding: 6 8; -fx-background-radius: 0;"
            }
        }
        return button
    }

    private fun showDashboard() {
        val content = VBox(15.0)
        content.padding = Insets(15.0)
        content.style = "-fx-background-color: #ecf0f1;"
        content.alignment = Pos.CENTER

        val welcomeText = Text("Добро пожаловать!")
        welcomeText.font = Font.font("System", FontWeight.BOLD, 24.0)
        welcomeText.style = "-fx-fill: #2c3e50;"

        content.children.add(welcomeText)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)
    }

    private fun showVendingMachines() {
        currentEntityClass = VendingMachine::class.java
        currentData = vendingMachineDAO.getAll().sortedBy { it.id }
        setupTableView(VendingMachine::class.java)
    }

    private fun showProducts() {
        currentEntityClass = Product::class.java
        currentData = productDAO.getAll().sortedBy { it.id }
        setupTableView(Product::class.java)
    }

    private fun showSales() {
        currentEntityClass = Sale::class.java
        currentData = saleDAO.getAll().sortedBy { it.id }
        setupTableView(Sale::class.java)
    }

    private fun showUsers() {
        currentEntityClass = User::class.java
        currentData = userDAO.getAll().sortedBy { it.id }
        setupTableView(User::class.java)
    }

    private fun showMaintenance() {
        currentEntityClass = Maintenance::class.java
        currentData = maintenanceDAO.getAll().sortedBy { it.id }
        setupTableView(Maintenance::class.java)
    }

    private fun <T : Any> setupTableView(entityClass: Class<T>) {
        val content = VBox(15.0)
        content.padding = Insets(15.0)
        content.style = "-fx-background-color: #ecf0f1;"

        val header = Text("Управление: ${getEntityName(entityClass)}")
        header.font = Font.font("System", FontWeight.BOLD, 16.0)

        // Панель управления таблицей
        val controlBar = createControlBar(entityClass)

        // Настройка TableView
        setupTableColumns(entityClass)

        // Инициализация данных с фильтрацией
        val observableData = FXCollections.observableArrayList(currentData)
        filteredData = FilteredList(observableData) { true }

        // Настройка фильтра
        filterField.textProperty().addListener { _, _, newValue ->
            filteredData.setPredicate { item ->
                if (newValue.isNullOrBlank()) return@setPredicate true
                filterItem(item, newValue)
            }
            tableView.items = filteredData
        }

        // Стилизация таблицы
        tableView.style = "-fx-font-size: 12px;"
        tableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        VBox.setVgrow(tableView, Priority.ALWAYS)

        // Контейнер для таблицы
        val tableContainer = VBox(tableView)
        VBox.setVgrow(tableContainer, Priority.ALWAYS)
        tableContainer.style = "-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"

        content.children.addAll(header, controlBar, tableContainer)
        VBox.setVgrow(tableContainer, Priority.ALWAYS)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.isFitToHeight = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)

        // Устанавливаем данные в таблицу
        tableView.items = filteredData
    }

    private fun <T : Any> setupTableColumns(entityClass: Class<T>) {
        tableView.columns.clear()

        when (entityClass) {
            VendingMachine::class.java -> {
                createColumn("ID", "id", 50.0)
                createColumn("Название", "name", 150.0)
                createColumn("Модель", "model", 120.0)
                createColumn("Компания", "company", 120.0)
                createColumn("Модем", "modem", 100.0)

                val addressColumn = TableColumn<Any, String>("Адрес/Место").apply {
                    prefWidth = 200.0
                    setCellValueFactory { cellData ->
                        val vm = cellData.value as VendingMachine
                        val address = vm.address ?: ""
                        val location = vm.location ?: ""
                        SimpleStringProperty(if (address.isNotEmpty() || location.isNotEmpty())
                            "$address / $location" else "—")
                    }
                }
                tableView.columns.add(addressColumn)

                createColumn("Дата", "installationDate", 90.0) { date ->
                    (date as? LocalDate)?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "—"
                }

                addActionColumn()
            }
            Product::class.java -> {
                createColumn("ID", "id", 50.0)
                createColumn("Название", "name", 150.0)
                createColumn("Цена", "price", 80.0) { price ->
                    "$price ₽"
                }

                val stockColumn = TableColumn<Any, String>("В наличии").apply {
                    prefWidth = 80.0
                    setCellValueFactory { cellData ->
                        val product = cellData.value as Product
                        SimpleStringProperty(product.quantityInStock.toString())
                    }
                    setCellFactory { col ->
                        object : TableCell<Any, String>() {
                            override fun updateItem(item: String?, empty: Boolean) {
                                super.updateItem(item, empty)
                                if (empty || item == null) {
                                    text = null
                                    style = ""
                                } else {
                                    text = item
                                    val product = tableView.items[index] as? Product
                                    if (product != null && product.quantityInStock <= product.minimalStock) {
                                        style = "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                                    } else {
                                        style = ""
                                    }
                                }
                            }
                        }
                    }
                }
                tableView.columns.add(stockColumn)

                createColumn("Мин. запас", "minimalStock", 80.0)

                createColumn("Описание", "description", 150.0) { desc ->
                    val str = desc as? String
                    if (str != null) {
                        if (str.length > 20) str.substring(0, 20) + "..." else str
                    } else "—"
                }

                createColumn("Тренды", "salesTrends", 100.0) { trend ->
                    val str = trend as? String
                    if (str != null) {
                        if (str.length > 15) str.substring(0, 15) + "..." else str
                    } else "—"
                }

                addActionColumn()
            }
            Sale::class.java -> {
                createColumn("ID", "id", 50.0)
                createColumn("Устройство", "deviceId", 80.0)
                createColumn("Продукт", "productId", 80.0)
                createColumn("Кол-во", "quantitySold", 70.0)
                createColumn("Сумма", "totalAmount", 80.0) { amount -> "$amount ₽" }
                createColumn("Дата", "saleDatetime", 90.0) { date ->
                    (date as? LocalDateTime)?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "—"
                }
                createColumn("Оплата", "paymentMethod", 80.0) { method ->
                    val str = method as? String
                    if (str != null) {
                        if (str.length > 10) str.substring(0, 10) + "..." else str
                    } else "—"
                }
                addActionColumn()
            }
            User::class.java -> {
                createColumn("ID", "id", 50.0)

                val nameColumn = TableColumn<Any, String>("ФИО").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val user = cellData.value as User
                        SimpleStringProperty("${user.firstName} ${user.lastName}")
                    }
                }
                tableView.columns.add(nameColumn)

                createColumn("Email", "email", 150.0) { email ->
                    val str = email as? String
                    if (str != null) {
                        if (str.length > 15) str.substring(0, 15) + "..." else str
                    } else "—"
                }
                createColumn("Телефон", "phone", 100.0) { phone ->
                    val str = phone as? String
                    if (str != null) {
                        if (str.length > 12) str.substring(0, 12) + "..." else str
                    } else "—"
                }
                createColumn("Роль", "roleId", 60.0)
                createColumn("Создан", "createdAt", 80.0) { date ->
                    (date as? LocalDateTime)?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "—"
                }
                createColumn("Вход", "lastLogin", 80.0) { date ->
                    (date as? LocalDateTime)?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "—"
                }
                addActionColumn()
            }
            Maintenance::class.java -> {
                createColumn("ID", "id", 50.0)
                createColumn("Устройство", "deviceId", 80.0)
                createColumn("Дата", "serviceDate", 90.0) { date ->
                    (date as? LocalDate)?.format(DateTimeFormatter.ofPattern("dd.MM.yy")) ?: "—"
                }
                createColumn("Описание", "description", 200.0) { desc ->
                    val str = desc as? String
                    if (str != null) {
                        if (str.length > 25) str.substring(0, 25) + "..." else str
                    } else "—"
                }
                createColumn("Исполнитель", "performedBy", 80.0) { id -> id?.toString() ?: "—" }
                addActionColumn()
            }
        }
    }

    private fun createColumn(title: String, property: String, width: Double, formatter: ((Any?) -> String)? = null) {
        val column = TableColumn<Any, Any>(title).apply {
            prefWidth = width
            setCellValueFactory { cellData ->
                val value = try {
                    val field = cellData.value::class.java.getDeclaredField(property)
                    field.isAccessible = true
                    field.get(cellData.value)
                } catch (e: Exception) {
                    null
                }
                SimpleObjectProperty(value)
            }
            setCellFactory { col ->
                object : TableCell<Any, Any>() {
                    override fun updateItem(item: Any?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (empty || item == null) {
                            text = null
                        } else {
                            text = formatter?.invoke(item) ?: item.toString()
                        }
                    }
                }
            }
        }
        tableView.columns.add(column)
    }

    private fun addActionColumn() {
        val actionColumn = TableColumn<Any, Void>("Действия").apply {
            prefWidth = 130.0
            setCellFactory { col ->
                object : TableCell<Any, Void>() {
                    private val editButton = Button("✏️").apply {
                        style = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 6; -fx-background-radius: 3;"
                        prefWidth = 30.0
                    }
                    private val deleteButton = Button("🗑️").apply {
                        style = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 3 6; -fx-background-radius: 3;"
                        prefWidth = 30.0
                    }
                    private val buttonBox = HBox(5.0, editButton, deleteButton).apply {
                        alignment = Pos.CENTER
                    }

                    init {
                        editButton.setOnAction {
                            val item = tableView.items[index]
                            showEditDialog(item)
                        }
                        deleteButton.setOnAction {
                            val item = tableView.items[index]
                            showDeleteConfirmation(item)
                        }
                    }

                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        if (empty) {
                            graphic = null
                        } else {
                            graphic = buttonBox
                        }
                    }
                }
            }
        }
        tableView.columns.add(actionColumn)
    }

    private fun <T : Any> createControlBar(entityClass: Class<T>): HBox {
        val controlBar = HBox(10.0)
        controlBar.alignment = Pos.CENTER_LEFT
        controlBar.style = "-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-width: 1px; -fx-padding: 8px;"

        // Поле фильтра
        val filterLabel = Label("Фильтр:")
        filterLabel.style = "-fx-font-size: 12px;"

        filterField = TextField()
        filterField.style = "-fx-font-size: 12px; -fx-pref-width: 200px;"
        filterField.promptText = "Поиск..."

        // Кнопки
        val addButton = Button("➕ Добавить")
        addButton.style = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        addButton.setOnAction { showAddDialog(entityClass) }

        val exportButton = Button("📤 Экспорт")
        exportButton.style = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        exportButton.setOnAction { exportData() }

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        controlBar.children.addAll(
            filterLabel, filterField,
            spacer,
            addButton, exportButton
        )

        return controlBar
    }

    private fun filterItem(item: Any, filterText: String): Boolean {
        val lowerFilter = filterText.lowercase()

        return when (item) {
            is VendingMachine -> {
                item.name.lowercase().contains(lowerFilter) ||
                        (item.model?.lowercase()?.contains(lowerFilter) == true) ||
                        (item.location?.lowercase()?.contains(lowerFilter) == true) ||
                        (item.address?.lowercase()?.contains(lowerFilter) == true)
            }
            is Product -> {
                item.name.lowercase().contains(lowerFilter) ||
                        (item.description?.lowercase()?.contains(lowerFilter) == true)
            }
            is Sale -> {
                item.deviceId.toString().contains(lowerFilter) ||
                        item.productId.toString().contains(lowerFilter) ||
                        (item.paymentMethod?.lowercase()?.contains(lowerFilter) == true)
            }
            is User -> {
                item.firstName.lowercase().contains(lowerFilter) ||
                        item.lastName.lowercase().contains(lowerFilter) ||
                        (item.email?.lowercase()?.contains(lowerFilter) == true) ||
                        (item.phone?.contains(lowerFilter) == true)
            }
            is Maintenance -> {
                item.deviceId.toString().contains(lowerFilter) ||
                        (item.description?.lowercase()?.contains(lowerFilter) == true)
            }
            else -> false
        }
    }

    private fun exportData() {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = "Экспорт данных"
        alert.headerText = null
        alert.contentText = "Функция экспорта будет реализована позже"
        alert.showAndWait()
    }

    private fun showAddDialog(entityClass: Class<*>) {

        val stage = Stage()
        stage.title = "Добавить запись"
        stage.initModality(Modality.APPLICATION_MODAL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 8.0
        grid.padding = Insets(15.0)

        data class FieldMeta(
            val field: TextField,
            val required: Boolean,
            val type: String?
        )

        val fields = mutableListOf<FieldMeta>()
        var row = 0

        fun addField(
            labelText: String,
            required: Boolean = false,
            type: String? = null,
            example: String = ""
        ) {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold;"
            val tf = TextField()
            tf.promptText = example
            grid.add(label, 0, row)
            grid.add(tf, 1, row++)
            fields.add(FieldMeta(tf, required, type))
        }

        when (entityClass) {

            VendingMachine::class.java -> {
                addField("Название*", true, null, "Кофейный автомат №1")
                addField("Модель", false, null, "Saeco 400")
                addField("Компания", false, null, "ООО ТА")
                addField("Модем", false, null, "1824100025")
                addField("Адрес", false, null, "ул. Ленина, 10")
                addField("Место", false, null, "1 этаж")
                addField("Дата установки", false, "date", "2024-01-15")
            }

            Product::class.java -> {
                addField("Название*", true, null, "Кофе Латте")
                addField("Цена*", true, "decimal", "150.00")
                addField("Количество*", true, "int", "50")
                addField("Мин. запас*", true, "int", "10")
                addField("Описание", false, null, "Классический кофе")
                addField("Тренды", false, null, "Высокий спрос")
            }

            User::class.java -> {
                addField("Имя*", true)
                addField("Фамилия*", true)
                addField("Отчество")
                addField("Email")
                addField("Телефон")
                addField("Role ID*", true, "int", "1")
                addField("Пароль*", true)
            }

            Sale::class.java -> {
                addField("Device ID*", true, "int", "1")
                addField("Product ID*", true, "int", "2")
                addField("Количество*", true, "int", "3")
                addField("Сумма*", true, "decimal", "450.00")
                addField("Дата", false, "datetime", "2024-03-20T10:30:00")
                addField("Оплата")
            }

            Maintenance::class.java -> {
                addField("Device ID*", true, "int", "1")
                addField("Дата", false, "date", "2024-03-20")
                addField("Описание")
                addField("Исполнитель ID", false, "int", "3")
            }
        }

        val saveButton = Button("Сохранить")
        saveButton.isDisable = true

        val cancelButton = Button("Отмена")

        fun validateAll(): Boolean =
            fields.all { validateField(it.field, it.required, it.type) }

        fields.forEach {
            it.field.textProperty().addListener { _, _, _ ->
                saveButton.isDisable = !validateAll()
            }
        }

        saveButton.setOnAction {
            try {

                when (entityClass) {

                    VendingMachine::class.java -> {
                        vendingMachineDAO.save(
                            VendingMachine(
                                name = fields[0].field.text,
                                model = fields[1].field.text.ifBlank { null },
                                company = fields[2].field.text.ifBlank { null },
                                modem = fields[3].field.text.ifBlank { null },
                                address = fields[4].field.text.ifBlank { null },
                                location = fields[5].field.text.ifBlank { null },
                                installationDate = fields[6].field.text
                                    .takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                            )
                        )
                    }

                    Product::class.java -> {
                        productDAO.save(
                            Product(
                                name = fields[0].field.text,
                                price = fields[1].field.text.toBigDecimal(),
                                quantityInStock = fields[2].field.text.toInt(),
                                minimalStock = fields[3].field.text.toInt(),
                                description = fields[4].field.text.ifBlank { null },
                                salesTrends = fields[5].field.text.ifBlank { null }
                            )
                        )
                    }

                    User::class.java -> {
                        userDAO.save(
                            User(
                                firstName = fields[0].field.text,
                                lastName = fields[1].field.text,
                                patronymic = fields[2].field.text.ifBlank { null },
                                email = fields[3].field.text.ifBlank { null },
                                phone = fields[4].field.text.ifBlank { null },
                                roleId = fields[5].field.text.toInt(),
                                passwordHash = fields[6].field.text
                            )
                        )
                    }

                    Sale::class.java -> {
                        saleDAO.save(
                            Sale(
                                deviceId = fields[0].field.text.toInt(),
                                productId = fields[1].field.text.toInt(),
                                quantitySold = fields[2].field.text.toInt(),
                                totalAmount = fields[3].field.text.toBigDecimal(),
                                saleDatetime = fields[4].field.text
                                    .takeIf { it.isNotBlank() }
                                    ?.let { LocalDateTime.parse(it) }
                                    ?: LocalDateTime.now(),
                                paymentMethod = fields[5].field.text.ifBlank { null }
                            )
                        )
                    }

                    Maintenance::class.java -> {
                        maintenanceDAO.save(
                            Maintenance(
                                deviceId = fields[0].field.text.toInt(),
                                serviceDate = fields[1].field.text
                                    .takeIf { it.isNotBlank() }
                                    ?.let { LocalDate.parse(it) }
                                    ?: LocalDate.now(),
                                description = fields[2].field.text.ifBlank { null },
                                performedBy = fields[3].field.text.toIntOrNull()
                            )
                        )
                    }
                }

                stage.close()
                refreshCurrentTable()

            } catch (e: Exception) {
                showAlert("Ошибка", e.message ?: "Ошибка сохранения")
            }
        }

        cancelButton.setOnAction { stage.close() }

        val buttons = HBox(10.0, saveButton, cancelButton)
        buttons.alignment = Pos.CENTER_RIGHT
        buttons.padding = Insets(10.0)

        val root = VBox(10.0, grid, buttons)
        root.padding = Insets(10.0)

        stage.scene = Scene(root)
        stage.showAndWait()
    }

    private fun showEditDialog(item: Any) {

        val stage = Stage()
        stage.title = "Редактировать запись"
        stage.initModality(Modality.APPLICATION_MODAL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 8.0
        grid.padding = Insets(15.0)

        data class FieldMeta(
            val field: TextField,
            val required: Boolean,
            val type: String?
        )

        val fields = mutableListOf<FieldMeta>()
        var row = 0

        fun addField(
            labelText: String,
            value: String,
            required: Boolean = false,
            type: String? = null,
            example: String = ""
        ) {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold;"

            val tf = TextField(value)
            tf.promptText = example

            grid.add(label, 0, row)
            grid.add(tf, 1, row++)
            fields.add(FieldMeta(tf, required, type))
        }

        when (item) {

            is VendingMachine -> {
                addField("Название*", item.name, true)
                addField("Модель", item.model ?: "", false, null, "Saeco 400")
                addField("Компания", item.company ?: "")
                addField("Модем", item.modem ?: "")
                addField("Адрес", item.address ?: "")
                addField("Место", item.location ?: "")
                addField(
                    "Дата установки",
                    item.installationDate?.toString() ?: "",
                    false,
                    "date",
                    "2024-01-15"
                )
            }

            is Product -> {
                addField("Название*", item.name, true)
                addField("Цена*", item.price.toString(), true, "decimal", "150.00")
                addField("Количество*", item.quantityInStock.toString(), true, "int", "50")
                addField("Мин. запас*", item.minimalStock.toString(), true, "int", "10")
                addField("Описание", item.description ?: "")
                addField("Тренды", item.salesTrends ?: "")
            }

            is User -> {
                addField("Имя*", item.firstName, true)
                addField("Фамилия*", item.lastName, true)
                addField("Отчество", item.patronymic ?: "")
                addField("Email", item.email ?: "")
                addField("Телефон", item.phone ?: "")
                addField("Role ID*", item.roleId.toString(), true, "int", "1")
            }

            is Sale -> {
                addField("Device ID*", item.deviceId.toString(), true, "int")
                addField("Product ID*", item.productId.toString(), true, "int")
                addField("Количество*", item.quantitySold.toString(), true, "int")
                addField("Сумма*", item.totalAmount.toString(), true, "decimal")
                addField(
                    "Дата",
                    item.saleDatetime.toString(),
                    false,
                    "datetime",
                    "2024-03-20T10:30:00"
                )
                addField("Метод оплаты", item.paymentMethod ?: "")
            }

            is Maintenance -> {
                addField("Device ID*", item.deviceId.toString(), true, "int")
                addField(
                    "Дата",
                    item.serviceDate.toString(),
                    false,
                    "date",
                    "2024-03-20"
                )
                addField("Описание", item.description ?: "")
                addField(
                    "Исполнитель ID",
                    item.performedBy?.toString() ?: "",
                    false,
                    "int",
                    "3"
                )
            }
        }

        val saveButton = Button("Сохранить")
        saveButton.isDisable = true

        val cancelButton = Button("Отмена")

        fun validateAll(): Boolean =
            fields.all { validateField(it.field, it.required, it.type) }

        fields.forEach {
            it.field.textProperty().addListener { _, _, _ ->
                saveButton.isDisable = !validateAll()
            }
        }

        saveButton.isDisable = !validateAll()

        saveButton.setOnAction {
            try {

                when (item) {

                    is VendingMachine -> {
                        item.name = fields[0].field.text
                        item.model = fields[1].field.text.ifBlank { null }
                        item.company = fields[2].field.text.ifBlank { null }
                        item.modem = fields[3].field.text.ifBlank { null }
                        item.address = fields[4].field.text.ifBlank { null }
                        item.location = fields[5].field.text.ifBlank { null }
                        item.installationDate = fields[6].field.text
                            .takeIf { it.isNotBlank() }
                            ?.let { LocalDate.parse(it) }

                        vendingMachineDAO.update(item)
                    }

                    is Product -> {
                        item.name = fields[0].field.text
                        item.price = fields[1].field.text.toBigDecimal()
                        item.quantityInStock = fields[2].field.text.toInt()
                        item.minimalStock = fields[3].field.text.toInt()
                        item.description = fields[4].field.text.ifBlank { null }
                        item.salesTrends = fields[5].field.text.ifBlank { null }

                        productDAO.update(item)
                    }

                    is User -> {
                        item.firstName = fields[0].field.text
                        item.lastName = fields[1].field.text
                        item.patronymic = fields[2].field.text.ifBlank { null }
                        item.email = fields[3].field.text.ifBlank { null }
                        item.phone = fields[4].field.text.ifBlank { null }
                        item.roleId = fields[5].field.text.toInt()

                        userDAO.update(item)
                    }

                    is Sale -> {
                        item.deviceId = fields[0].field.text.toInt()
                        item.productId = fields[1].field.text.toInt()
                        item.quantitySold = fields[2].field.text.toInt()
                        item.totalAmount = fields[3].field.text.toBigDecimal()
                        item.saleDatetime = fields[4].field.text
                            .takeIf { it.isNotBlank() }
                            ?.let { LocalDateTime.parse(it) }
                            ?: item.saleDatetime
                        item.paymentMethod = fields[5].field.text.ifBlank { null }

                        saleDAO.update(item)
                    }

                    is Maintenance -> {
                        item.deviceId = fields[0].field.text.toInt()
                        item.serviceDate = fields[1].field.text
                            .takeIf { it.isNotBlank() }
                            ?.let { LocalDate.parse(it) }
                            ?: item.serviceDate
                        item.description = fields[2].field.text.ifBlank { null }
                        item.performedBy = fields[3].field.text.toIntOrNull()

                        maintenanceDAO.update(item)
                    }
                }

                stage.close()
                refreshCurrentTable()

            } catch (e: Exception) {
                showAlert("Ошибка", e.message ?: "Ошибка обновления")
            }
        }

        cancelButton.setOnAction { stage.close() }

        val buttons = HBox(10.0, saveButton, cancelButton)
        buttons.alignment = Pos.CENTER_RIGHT
        buttons.padding = Insets(10.0)

        val root = VBox(10.0, grid, buttons)
        root.padding = Insets(10.0)

        stage.scene = Scene(root)
        stage.showAndWait()
    }

    private fun getEntityName(entityClass: Class<*>): String {
        return when (entityClass) {
            VendingMachine::class.java -> "Торговые автоматы"
            Product::class.java -> "Продукты"
            Sale::class.java -> "Продажи"
            User::class.java -> "Пользователи"
            Maintenance::class.java -> "Обслуживание"
            else -> entityClass.simpleName
        }
    }

    private fun getEntityShortName(entityClass: Class<*>): String {
        return when (entityClass) {
            VendingMachine::class.java -> "Автомат"
            Product::class.java -> "Продукт"
            Sale::class.java -> "Продажа"
            User::class.java -> "Пользователь"
            Maintenance::class.java -> "Обслуживание"
            else -> entityClass.simpleName
        }
    }

    private fun getIdValue(item: Any): Any {
        return try {
            val idField = item::class.java.getDeclaredField("id").apply { isAccessible = true }
            idField.get(item) ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun showDeleteConfirmation(item: Any) {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Подтверждение удаления"
        alert.headerText = "Вы уверены, что хотите удалить эту запись?"
        alert.contentText = "${getEntityShortName(item::class.java)} (ID: ${getIdValue(item)})"

        alert.dialogPane.buttonTypes.clear()

        val deleteButton = ButtonType("Удалить запись", ButtonBar.ButtonData.OK_DONE)
        val cancelButton = ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)

        alert.dialogPane.buttonTypes.addAll(deleteButton, cancelButton)

        alert.dialogPane.lookupButton(deleteButton)?.let { button ->
            button.style = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 3;"
        }

        alert.dialogPane.lookupButton(cancelButton)?.let { button ->
            button.style = "-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 3;"
        }

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == deleteButton) {
            try {
                when (item) {
                    is VendingMachine -> vendingMachineDAO.delete(item.id)
                    is Product -> productDAO.delete(item.id)
                    is Sale -> saleDAO.delete(item.id)
                    is User -> userDAO.delete(item.id)
                    is Maintenance -> maintenanceDAO.delete(item.id)
                }
                refreshCurrentTable()
            } catch (e: Exception) {
                showAlert("Ошибка", "Не удалось удалить запись: ${e.message}")
            }
        }
    }

    private fun refreshCurrentTable() {
        when (currentEntityClass) {
            VendingMachine::class.java -> {
                currentData = vendingMachineDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                tableView.items = filteredData
            }
            Product::class.java -> {
                currentData = productDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                tableView.items = filteredData
            }
            Sale::class.java -> {
                currentData = saleDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                tableView.items = filteredData
            }
            User::class.java -> {
                currentData = userDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                tableView.items = filteredData
            }
            Maintenance::class.java -> {
                currentData = maintenanceDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                tableView.items = filteredData
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}

fun main(args: Array<String>) {
    Application.launch(MainApp::class.java, *args)
}