package com.example.project_vending_machines

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
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
import javafx.scene.layout.Priority
import javafx.event.EventHandler
import java.math.BigDecimal
import javafx.stage.Modality
import javafx.stage.FileChooser
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

class MainApp : Application() {

    private val vendingMachineDAO = VendingMachineDAO()
    private val productDAO = ProductDAO()
    private val saleDAO = SaleDAO()
    private val userDAO = UserDAO()
    private val maintenanceDAO = MaintenanceDAO()
    private val roleDAO = RoleDAO()

    private val mainContent = StackPane()
    private var currentPage = 0
    private var pageSize = 10
    private var currentData: List<Any> = listOf()
    private var currentEntityClass: Class<*>? = null
    private val tableView = TableView<Any>()

    // Для фильтрации
    private lateinit var filteredData: FilteredList<Any>
    private lateinit var filterField: TextField
    private lateinit var pageSizeComboBox: ComboBox<Int>
    private lateinit var paginationLabel: Label
    private lateinit var recordsInfoLabel: Label
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button

    // Форматтеры дат
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

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
                    "date" -> {
                        // Пробуем распарсить в формате дд.мм.гггг
                        try {
                            LocalDate.parse(text, dateFormatter)
                        } catch (e: Exception) {
                            // Если не получилось, пробуем стандартный формат
                            LocalDate.parse(text)
                        }
                    }
                    "datetime" -> {
                        // Пробуем распарсить в формате дд.мм.гггг ЧЧ:мм
                        try {
                            LocalDateTime.parse(text, dateTimeFormatter)
                        } catch (e: Exception) {
                            // Если не получилось, пробуем стандартный формат
                            LocalDateTime.parse(text)
                        }
                    }
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
        currentPage = 0

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
            currentPage = 0
            updatePagination()
        }

        // Стилизация таблицы
        tableView.style = "-fx-font-size: 12px;"
        tableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY



        VBox.setVgrow(tableView, Priority.ALWAYS)

        // Контейнер для таблицы
        val tableContainer = VBox(tableView)
        VBox.setVgrow(tableContainer, Priority.ALWAYS)
        tableContainer.style = "-fx-background-color: white; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"

        // Пагинация
        val paginationBar = createPaginationBar()

        content.children.addAll(header, controlBar, tableContainer, paginationBar)
        VBox.setVgrow(tableContainer, Priority.ALWAYS)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.isFitToHeight = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)

        // Первоначальное обновление пагинации
        updatePagination()
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
                    (date as? LocalDate)?.format(dateFormatter) ?: "—"
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

                // Колонка для устройства с названием вместо ID
                val deviceColumn = TableColumn<Any, String>("Устройство").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val sale = cellData.value as Sale
                        val machine = vendingMachineDAO.getById(sale.deviceId.toLong())
                        SimpleStringProperty(machine?.name ?: "ID: ${sale.deviceId}")
                    }
                }
                tableView.columns.add(deviceColumn)

                // Колонка для продукта с названием вместо ID
                val productColumn = TableColumn<Any, String>("Продукт").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val sale = cellData.value as Sale
                        val product = productDAO.getById(sale.productId)
                        SimpleStringProperty(product?.name ?: "ID: ${sale.productId}")
                    }
                }
                tableView.columns.add(productColumn)

                createColumn("Кол-во", "quantitySold", 70.0)
                createColumn("Сумма", "totalAmount", 80.0) { amount -> "$amount ₽" }
                createColumn("Дата", "saleDatetime", 120.0) { date ->
                    (date as? LocalDateTime)?.format(dateTimeFormatter) ?: "—"
                }
                createColumn("Оплата", "paymentMethod", 80.0)
                addActionColumn()
            }

            User::class.java -> {
                createColumn("ID", "id", 50.0)

                val nameColumn = TableColumn<Any, String>("ФИО").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val user = cellData.value as User
                        SimpleStringProperty("${user.lastName} ${user.firstName} ${user.patronymic ?: ""}".trim())
                    }
                }
                tableView.columns.add(nameColumn)

                createColumn("Email", "email", 150.0)
                createColumn("Телефон", "phone", 100.0)

                // Колонка для роли с названием вместо ID
                val roleColumn = TableColumn<Any, String>("Роль").apply {
                    prefWidth = 100.0
                    setCellValueFactory { cellData ->
                        val user = cellData.value as User
                        val role = roleDAO.getById(user.roleId)
                        SimpleStringProperty(role?.name ?: "ID: ${user.roleId}")
                    }
                }
                tableView.columns.add(roleColumn)

                createColumn("Создан", "createdAt", 80.0) { date ->
                    (date as? LocalDateTime)?.format(dateTimeFormatter) ?: "—"
                }
                createColumn("Вход", "lastLogin", 80.0) { date ->
                    (date as? LocalDateTime)?.format(dateTimeFormatter) ?: "—"
                }
                addActionColumn()
            }

            Maintenance::class.java -> {
                createColumn("ID", "id", 50.0)

                // Колонка для устройства с названием вместо ID
                val deviceColumn = TableColumn<Any, String>("Устройство").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val maint = cellData.value as Maintenance
                        val machine = vendingMachineDAO.getById(maint.deviceId.toLong())
                        SimpleStringProperty(machine?.name ?: "ID: ${maint.deviceId}")
                    }
                }
                tableView.columns.add(deviceColumn)

                createColumn("Дата", "serviceDate", 90.0) { date ->
                    (date as? LocalDate)?.format(dateFormatter) ?: "—"
                }
                createColumn("Описание", "description", 200.0)

                // Колонка для исполнителя с именем вместо ID
                val performerColumn = TableColumn<Any, String>("Исполнитель").apply {
                    prefWidth = 150.0
                    setCellValueFactory { cellData ->
                        val maint = cellData.value as Maintenance
                        if (maint.performedBy != null) {
                            val user = userDAO.getById(maint.performedBy)
                            if (user != null) {
                                SimpleStringProperty("${user.lastName} ${user.firstName}")
                            } else {
                                SimpleStringProperty("ID: ${maint.performedBy}")
                            }
                        } else {
                            SimpleStringProperty("—")
                        }
                    }
                }
                tableView.columns.add(performerColumn)

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

        // Выпадающий список "Показать X записей"
        val showLabel = Label("Показать:")
        showLabel.style = "-fx-font-size: 12px;"

        pageSizeComboBox = ComboBox(FXCollections.observableArrayList(5, 10, 15, 25, 50, 100))
        pageSizeComboBox.value = pageSize
        pageSizeComboBox.style = "-fx-font-size: 12px; -fx-pref-width: 70px;"
        pageSizeComboBox.setOnAction {
            pageSize = pageSizeComboBox.value
            currentPage = 0
            updatePagination()
        }

        val recordsLabel = Label("записей")
        recordsLabel.style = "-fx-font-size: 12px;"

        // Информация о количестве записей
        recordsInfoLabel = Label("")
        recordsInfoLabel.style = "-fx-font-size: 12px; -fx-padding: 0 0 0 10; -fx-font-weight: bold;"

        // Поле фильтра (теперь только по названию)
        val filterLabel = Label("Фильтр по названию:")
        filterLabel.style = "-fx-font-size: 12px; -fx-padding: 0 0 0 20;"

        filterField = TextField()
        filterField.style = "-fx-font-size: 12px; -fx-pref-width: 200px;"
        filterField.promptText = "Введите название..."

        // Кнопки
        val addButton = Button("➕ Добавить")
        addButton.style = "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        addButton.setOnAction { showAddDialog(entityClass) }

        // Кнопка экспорта
        val exportButton = MenuButton("📤 Экспорт")
        exportButton.style = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 3; -fx-font-size: 12px;"

        val csvItem = MenuItem("CSV")
        csvItem.setOnAction { exportToCSV() }

        val htmlItem = MenuItem("HTML")
        htmlItem.setOnAction { exportToHTML() }

        exportButton.items.addAll(csvItem, htmlItem)

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        controlBar.children.addAll(
            showLabel, pageSizeComboBox, recordsLabel, recordsInfoLabel,
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
                item.name.lowercase().contains(lowerFilter)
            }
            is Product -> {
                item.name.lowercase().contains(lowerFilter)
            }
            is Sale -> {
                // Для продаж фильтруем по названию продукта через productId
                val product = productDAO.getById(item.productId)
                product?.name?.lowercase()?.contains(lowerFilter) == true
            }
            is User -> {
                "${item.firstName} ${item.lastName}".lowercase().contains(lowerFilter) ||
                        item.email?.lowercase()?.contains(lowerFilter) == true
            }
            is Maintenance -> {
                // Для обслуживания фильтруем по названию автомата через deviceId
                val machine = vendingMachineDAO.getById(item.deviceId.toLong())
                machine?.name?.lowercase()?.contains(lowerFilter) == true
            }
            else -> false
        }
    }

    private fun createPaginationBar(): HBox {
        val paginationBar = HBox(8.0)
        paginationBar.alignment = Pos.CENTER
        paginationBar.style = "-fx-padding: 10 0;"

        prevButton = Button("◀ Предыдущая")
        prevButton.style = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        prevButton.setOnAction {
            if (currentPage > 0) {
                currentPage--
                updatePagination()
            }
        }

        nextButton = Button("Следующая ▶")
        nextButton.style = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        nextButton.setOnAction {
            if ((currentPage + 1) * pageSize < filteredData.size) {
                currentPage++
                updatePagination()
            }
        }

        paginationLabel = Label()
        paginationLabel.style = "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 10;"

        paginationBar.children.addAll(prevButton, paginationLabel, nextButton)

        return paginationBar
    }

    private fun updatePagination() {
        if (!::filteredData.isInitialized) return

        val totalPages = maxOf(1, (filteredData.size + pageSize - 1) / pageSize)

        // Корректируем текущую страницу, если она выходит за пределы
        if (currentPage >= totalPages) {
            currentPage = maxOf(0, totalPages - 1)
        }

        paginationLabel.text = "Страница ${currentPage + 1} из $totalPages"

        // Обновляем информацию о количестве записей
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, filteredData.size)
        recordsInfoLabel.text = "Записи ${start + 1}-$end из ${filteredData.size}"

        // Управляем видимостью кнопок пагинации
        prevButton.isVisible = currentPage > 0
        nextButton.isVisible = currentPage < totalPages - 1

        // Обновляем отображаемые данные
        val pageItems = FXCollections.observableArrayList<Any>()
        for (i in start until end) {
            pageItems.add(filteredData[i])
        }
        tableView.items = pageItems

        // Принудительно обновляем таблицу
        tableView.refresh()
    }

    private fun exportToCSV() {
        val entityClass = currentEntityClass ?: return
        val data = filteredData

        val fileChooser = FileChooser()
        fileChooser.title = "Сохранить как CSV"
        fileChooser.initialFileName = "${getEntityName(entityClass)}_${LocalDate.now().format(dateFormatter)}.csv"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("CSV файлы", "*.csv"),
            FileChooser.ExtensionFilter("Все файлы", "*.*")
        )

        val file = fileChooser.showSaveDialog(mainContent.scene.window) ?: return

        try {
            // Формируем содержимое CSV
            val content = StringBuilder()

            // Заголовки
            val headers = tableView.columns.filter { it.text != "Действия" }.joinToString(";") { it.text }
            content.appendLine(headers)

            // Данные (экспортируем все данные, не только текущую страницу)
            for (item in filteredData) {
                val row = tableView.columns.filter { it.text != "Действия" }.joinToString(";") { column ->
                    val cellData = getCellData(item, column)
                    when (cellData) {
                        is LocalDate -> cellData.format(dateFormatter)
                        is LocalDateTime -> cellData.format(dateTimeFormatter)
                        is BigDecimal -> cellData.toString().replace(".", ",")
                        else -> cellData?.toString()?.replace(";", ",") ?: ""
                    }
                }
                content.appendLine(row)
            }

            // Записываем в файл в кодировке Windows-1251
            Files.write(Paths.get(file.toURI()), content.toString().toByteArray(Charset.forName("Windows-1251")))

            showAlert("Успех", "Данные успешно экспортированы в CSV\nФайл сохранен: ${file.absolutePath}")

        } catch (e: Exception) {
            showAlert("Ошибка", "Не удалось экспортировать данные: ${e.message}")
        }
    }

    private fun exportToHTML() {
        val entityClass = currentEntityClass ?: return
        val data = filteredData

        val fileChooser = FileChooser()
        fileChooser.title = "Сохранить как HTML"
        fileChooser.initialFileName = "${getEntityName(entityClass)}_${LocalDate.now().format(dateFormatter)}.html"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("HTML файлы", "*.html", "*.htm"),
            FileChooser.ExtensionFilter("Все файлы", "*.*")
        )

        val file = fileChooser.showSaveDialog(mainContent.scene.window) ?: return

        try {
            val content = StringBuilder()

            content.appendLine("<!DOCTYPE html>")
            content.appendLine("<html>")
            content.appendLine("<head>")
            content.appendLine("<meta charset='UTF-8'>")
            content.appendLine("<title>${getEntityName(entityClass)} - Отчет</title>")
            content.appendLine("<style>")
            content.appendLine("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }")
            content.appendLine("h1 { color: #2c3e50; text-align: center; }")
            content.appendLine(".date { color: #7f8c8d; text-align: right; margin-bottom: 20px; }")
            content.appendLine("table { border-collapse: collapse; width: 100%; background-color: white; box-shadow: 0 1px 3px rgba(0,0,0,0.2); }")
            content.appendLine("th { background-color: #3498db; color: white; padding: 12px; text-align: left; }")
            content.appendLine("td { padding: 8px 12px; border-bottom: 1px solid #ddd; }")
            content.appendLine("tr:nth-child(even) { background-color: #f2f2f2; }")
            content.appendLine("tr:hover { background-color: #e8f4f8; }")
            content.appendLine(".footer { margin-top: 20px; text-align: center; color: #7f8c8d; font-size: 12px; }")
            content.appendLine("</style>")
            content.appendLine("</head>")
            content.appendLine("<body>")

            content.appendLine("<h1>${getEntityName(entityClass)}</h1>")
            content.appendLine("<div class='date'>Отчет создан: ${LocalDateTime.now().format(dateTimeFormatter)}</div>")

            content.appendLine("<table>")
            content.appendLine("<thead><tr>")
            tableView.columns.filter { it.text != "Действия" }.forEach { column ->
                content.appendLine("<th>${column.text}</th>")
            }
            content.appendLine("</tr></thead>")

            content.appendLine("<tbody>")
            // Экспортируем все данные, не только текущую страницу
            for (item in filteredData) {
                content.appendLine("<tr>")
                tableView.columns.filter { it.text != "Действия" }.forEach { column ->
                    val cellData = getCellData(item, column)
                    val text = when (cellData) {
                        is LocalDate -> cellData.format(dateFormatter)
                        is LocalDateTime -> cellData.format(dateTimeFormatter)
                        else -> cellData?.toString() ?: ""
                    }
                    content.appendLine("<td>$text</td>")
                }
                content.appendLine("</tr>")
            }
            content.appendLine("</tbody>")
            content.appendLine("</table>")

            content.appendLine("<div class='footer'>Всего записей: ${filteredData.size}</div>")
            content.appendLine("</body>")
            content.appendLine("</html>")

            // Записываем в файл в UTF-8
            Files.write(Paths.get(file.toURI()), content.toString().toByteArray(Charset.forName("UTF-8")))

            showAlert("Успех", "Данные успешно экспортированы в HTML\nФайл сохранен: ${file.absolutePath}")

        } catch (e: Exception) {
            showAlert("Ошибка", "Не удалось экспортировать данные в HTML: ${e.message}")
        }
    }

    // Вспомогательная функция для получения данных из ячейки
    private fun getCellData(item: Any, column: TableColumn<Any, *>): Any? {
        return try {
            val propertyName = when (column.text) {
                "ID" -> "id"
                "Название" -> "name"
                "Модель" -> "model"
                "Компания" -> "company"
                "Модем" -> "modem"
                "Дата" -> when (item) {
                    is VendingMachine -> "installationDate"
                    is Sale -> "saleDatetime"
                    is Maintenance -> "serviceDate"
                    else -> null
                }
                "Цена" -> "price"
                "В наличии" -> "quantityInStock"
                "Мин. запас" -> "minimalStock"
                "Описание" -> "description"
                "Тренды" -> "salesTrends"
                "Устройство" -> when (item) {
                    is Sale -> "deviceId"
                    is Maintenance -> "deviceId"
                    else -> null
                }
                "Продукт" -> "productId"
                "Кол-во" -> "quantitySold"
                "Сумма" -> "totalAmount"
                "Оплата" -> "paymentMethod"
                "Email" -> "email"
                "Телефон" -> "phone"
                "Роль" -> "roleId"
                "Создан" -> "createdAt"
                "Вход" -> "lastLogin"
                "Исполнитель" -> "performedBy"
                else -> null
            }

            if (propertyName != null) {
                val field = item::class.java.getDeclaredField(propertyName)
                field.isAccessible = true
                field.get(item)
            } else if (column.text == "ФИО" && item is User) {
                "${item.firstName} ${item.lastName}"
            } else if (column.text == "Адрес/Место" && item is VendingMachine) {
                val address = item.address ?: ""
                val location = item.location ?: ""
                if (address.isNotEmpty() || location.isNotEmpty()) "$address / $location" else "—"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
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
            val field: Control,
            val required: Boolean,
            val type: String? = null
        )

        val fields = mutableListOf<FieldMeta>()
        var row = 0

        fun addTextField(
            labelText: String,
            required: Boolean = false,
            type: String? = null,
            example: String = "",
            editable: Boolean = true
        ): TextField {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val tf = TextField()
            tf.isEditable = editable
            tf.promptText = example
            grid.add(label, 0, row)
            grid.add(tf, 1, row++)
            fields.add(FieldMeta(tf, required, type))
            return tf
        }

        fun addDateField(
            labelText: String,
            required: Boolean = false,
            example: String = ""
        ): DatePicker {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val dp = DatePicker()
            dp.promptText = example
            dp.converter = object : javafx.util.StringConverter<LocalDate>() {
                private val formatter = dateFormatter
                override fun toString(date: LocalDate?): String = date?.format(formatter) ?: ""
                override fun fromString(string: String): LocalDate? =
                    try { LocalDate.parse(string, formatter) } catch (e: Exception) { null }
            }
            grid.add(label, 0, row)
            grid.add(dp, 1, row++)
            fields.add(FieldMeta(dp, required, "date"))
            return dp
        }

        fun addComboBox(
            labelText: String,
            items: List<Pair<Any, String>>,
            required: Boolean = false
        ): ComboBox<Pair<Any, String>> {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val cb = ComboBox(FXCollections.observableArrayList(items))
            cb.converter = object : javafx.util.StringConverter<Pair<Any, String>>() {
                override fun toString(item: Pair<Any, String>?): String = item?.second ?: ""
                override fun fromString(string: String): Pair<Any, String>? = null
            }
            cb.promptText = "Выберите..."
            cb.style = "-fx-font-size: 12px;"
            grid.add(label, 0, row)
            grid.add(cb, 1, row++)
            fields.add(FieldMeta(cb, required))
            return cb
        }

        when (entityClass) {
            VendingMachine::class.java -> {
                addTextField("Название*", true, null, "Кофейный автомат №1")
                addTextField("Модель*", true, null, "Saeco 400")
                addTextField("Компания*", true, null, "ООО ТА")
                addTextField("Модем*", true, null, "1824100025")
                addTextField("Адрес*", true, null, "ул. Ленина, 10")
                addTextField("Место*", true, null, "1 этаж")
                addDateField("Дата установки*", true, "дд.мм.гггг")
            }

            Product::class.java -> {
                addTextField("Название*", true, null, "Кофе Латте")
                addTextField("Цена*", true, "decimal", "150.00")
                addTextField("Количество*", true, "int", "50")
                addTextField("Мин. запас*", true, "int", "10")
                addTextField("Описание*", true, null, "Классический кофе")
                addTextField("Тренды*", true, null, "Высокий спрос")
            }

            Sale::class.java -> {
                val machines = vendingMachineDAO.getAll().map { it.id as Any to "${it.name} (${it.address})" }
                val products = productDAO.getAll().map { it.id as Any to "${it.name} - ${it.price} ₽" }

                addComboBox("Устройство*", machines, true)
                addComboBox("Продукт*", products, true)
                addTextField("Количество*", true, "int", "3")
                val amountField = addTextField("Сумма*", true, "decimal", "450.00", false) // Недоступно для редактирования
                addTextField("Дата*", true, "datetime", "дд.мм.гггг чч:мм")
                addTextField("Оплата*", true, null, "Наличные/Карта")

                // Автоматический расчет суммы при выборе продукта и количества
                val productCb = fields[1].field as ComboBox<Pair<Any, String>>
                val quantityField = fields[2].field as TextField

                val updateAmount = {
                    try {
                        val productId = (productCb.value?.first as? Number)?.toInt()
                        val quantity = quantityField.text.toIntOrNull() ?: 0
                        if (productId != null && quantity > 0) {
                            val product = productDAO.getById(productId)
                            if (product != null) {
                                val total = product.price.multiply(BigDecimal(quantity))
                                amountField.text = total.toString()
                            }
                        } else {
                            amountField.text = ""
                        }
                    } catch (e: Exception) {
                        // Игнорируем ошибки при расчете
                    }
                }

                productCb.valueProperty().addListener { _, _, _ -> updateAmount() }
                quantityField.textProperty().addListener { _, _, _ -> updateAmount() }
            }

            User::class.java -> {
                addTextField("Имя*", true)
                addTextField("Фамилия*", true)
                addTextField("Отчество*", true)
                addTextField("Email*", true)
                addTextField("Телефон*", true)

                val roles = roleDAO.getAll().map { it.id as Any to it.name }
                addComboBox("Роль*", roles, true)

                addTextField("Пароль*", true)
            }

            Maintenance::class.java -> {
                val machines = vendingMachineDAO.getAll().map { it.id as Any to "${it.name} (${it.address})" }
                val users = userDAO.getAll().map { it.id as Any to "${it.lastName} ${it.firstName}" }

                addComboBox("Устройство*", machines, true)
                addDateField("Дата*", true, "дд.мм.гггг")
                addTextField("Описание*", true, null, "Замена детали")
                addComboBox("Исполнитель*", users, true)
            }
        }

        val saveButton = Button("Сохранить")
        saveButton.isDisable = true

        val cancelButton = Button("Отмена")

        fun validateAll(): Boolean {
            return fields.all { meta ->
                when (val field = meta.field) {
                    is TextField -> {
                        if (!field.isEditable) true else validateField(field, meta.required, meta.type)
                    }
                    is ComboBox<*> -> {
                        if (meta.required && field.value == null) {
                            field.style = "-fx-border-color: #e74c3c; -fx-border-width: 2;"
                            false
                        } else {
                            field.style = ""
                            true
                        }
                    }
                    is DatePicker -> {
                        if (meta.required && field.value == null) {
                            field.style = "-fx-border-color: #e74c3c; -fx-border-width: 2;"
                            false
                        } else {
                            field.style = ""
                            true
                        }
                    }
                    else -> true
                }
            }
        }

        fields.forEach { meta ->
            when (val field = meta.field) {
                is TextField -> {
                    if (field.isEditable) {
                        field.textProperty().addListener { _, _, _ ->
                            saveButton.isDisable = !validateAll()
                        }
                    }
                }
                is ComboBox<*> -> {
                    field.valueProperty().addListener { _, _, _ ->
                        saveButton.isDisable = !validateAll()
                    }
                }
                is DatePicker -> {
                    field.valueProperty().addListener { _, _, _ ->
                        saveButton.isDisable = !validateAll()
                    }
                }
            }
        }

        saveButton.setOnAction {
            try {
                when (entityClass) {
                    VendingMachine::class.java -> {
                        vendingMachineDAO.save(
                            VendingMachine(
                                name = (fields[0].field as TextField).text,
                                model = (fields[1].field as TextField).text,
                                company = (fields[2].field as TextField).text,
                                modem = (fields[3].field as TextField).text,
                                address = (fields[4].field as TextField).text,
                                location = (fields[5].field as TextField).text,
                                installationDate = (fields[6].field as DatePicker).value
                            )
                        )
                    }

                    Product::class.java -> {
                        productDAO.save(
                            Product(
                                name = (fields[0].field as TextField).text,
                                price = (fields[1].field as TextField).text.toBigDecimal(),
                                quantityInStock = (fields[2].field as TextField).text.toInt(),
                                minimalStock = (fields[3].field as TextField).text.toInt(),
                                description = (fields[4].field as TextField).text,
                                salesTrends = (fields[5].field as TextField).text
                            )
                        )
                    }

                    Sale::class.java -> {
                        val deviceCb = fields[0].field as ComboBox<Pair<Any, String>>
                        val productCb = fields[1].field as ComboBox<Pair<Any, String>>

                        saleDAO.save(
                            Sale(
                                deviceId = (deviceCb.value.first as Number).toInt(),
                                productId = (productCb.value.first as Number).toInt(),
                                quantitySold = (fields[2].field as TextField).text.toInt(),
                                totalAmount = (fields[3].field as TextField).text.toBigDecimal(),
                                saleDatetime = LocalDateTime.parse((fields[4].field as TextField).text, dateTimeFormatter),
                                paymentMethod = (fields[5].field as TextField).text
                            )
                        )
                    }

                    User::class.java -> {
                        val roleCb = fields[5].field as ComboBox<Pair<Any, String>>

                        userDAO.save(
                            User(
                                firstName = (fields[0].field as TextField).text,
                                lastName = (fields[1].field as TextField).text,
                                patronymic = (fields[2].field as TextField).text,
                                email = (fields[3].field as TextField).text,
                                phone = (fields[4].field as TextField).text,
                                roleId = (roleCb.value.first as Number).toInt(),
                                passwordHash = (fields[6].field as TextField).text
                            )
                        )
                    }

                    Maintenance::class.java -> {
                        val deviceCb = fields[0].field as ComboBox<Pair<Any, String>>
                        val performerCb = fields[3].field as ComboBox<Pair<Any, String>>

                        maintenanceDAO.save(
                            Maintenance(
                                deviceId = (deviceCb.value.first as Number).toInt(),
                                serviceDate = (fields[1].field as DatePicker).value,
                                description = (fields[2].field as TextField).text,
                                performedBy = (performerCb.value.first as Number).toInt()
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
            val field: Control,
            val required: Boolean,
            val type: String? = null
        )

        val fields = mutableListOf<FieldMeta>()
        var row = 0

        fun addTextField(
            labelText: String,
            value: String,
            required: Boolean = false,
            type: String? = null,
            example: String = "",
            editable: Boolean = true
        ): TextField {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val tf = TextField(value)
            tf.isEditable = editable
            tf.promptText = example
            grid.add(label, 0, row)
            grid.add(tf, 1, row++)
            fields.add(FieldMeta(tf, required, type))
            return tf
        }

        fun addDateField(
            labelText: String,
            value: LocalDate?,
            required: Boolean = false,
            example: String = ""
        ): DatePicker {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val dp = DatePicker(value)
            dp.promptText = example
            dp.converter = object : javafx.util.StringConverter<LocalDate>() {
                private val formatter = dateFormatter
                override fun toString(date: LocalDate?): String = date?.format(formatter) ?: ""
                override fun fromString(string: String): LocalDate? =
                    try { LocalDate.parse(string, formatter) } catch (e: Exception) { null }
            }
            grid.add(label, 0, row)
            grid.add(dp, 1, row++)
            fields.add(FieldMeta(dp, required, "date"))
            return dp
        }

        fun addComboBox(
            labelText: String,
            items: List<Pair<Any, String>>,
            selectedId: Any?,
            required: Boolean = false
        ): ComboBox<Pair<Any, String>> {
            val label = Label(labelText)
            if (required) label.style = "-fx-font-weight: bold; -fx-text-fill: #c0392b;"
            val cb = ComboBox(FXCollections.observableArrayList(items))
            cb.converter = object : javafx.util.StringConverter<Pair<Any, String>>() {
                override fun toString(item: Pair<Any, String>?): String = item?.second ?: ""
                override fun fromString(string: String): Pair<Any, String>? = null
            }

            // Устанавливаем выбранное значение
            if (selectedId != null) {
                val selectedItem = items.find { it.first == selectedId }
                if (selectedItem != null) {
                    cb.value = selectedItem
                }
            }

            cb.promptText = "Выберите..."
            cb.style = "-fx-font-size: 12px;"
            grid.add(label, 0, row)
            grid.add(cb, 1, row++)
            fields.add(FieldMeta(cb, required))
            return cb
        }

        when (item) {
            is VendingMachine -> {
                addTextField("Название*", item.name, true)
                addTextField("Модель*", item.model ?: "", true, null, "Saeco 400")
                addTextField("Компания*", item.company ?: "", true)
                addTextField("Модем*", item.modem ?: "", true)
                addTextField("Адрес*", item.address ?: "", true)
                addTextField("Место*", item.location ?: "", true)
                addDateField("Дата установки*", item.installationDate, true, "дд.мм.гггг")
            }

            is Product -> {
                addTextField("Название*", item.name, true)
                addTextField("Цена*", item.price.toString(), true, "decimal", "150.00")
                addTextField("Количество*", item.quantityInStock.toString(), true, "int", "50")
                addTextField("Мин. запас*", item.minimalStock.toString(), true, "int", "10")
                addTextField("Описание*", item.description ?: "", true)
                addTextField("Тренды*", item.salesTrends ?: "", true)
            }

            is Sale -> {
                val machines = vendingMachineDAO.getAll().map { it.id as Any to "${it.name} (${it.address})" }
                val products = productDAO.getAll().map { it.id as Any to "${it.name} - ${it.price} ₽" }

                addComboBox("Устройство*", machines, item.deviceId, true)
                addComboBox("Продукт*", products, item.productId, true)
                addTextField("Количество*", item.quantitySold.toString(), true, "int")
                val amountField = addTextField("Сумма*", item.totalAmount.toString(), true, "decimal", editable = false)
                addTextField("Дата*", item.saleDatetime.format(dateTimeFormatter), true, "datetime", "дд.мм.гггг чч:мм")
                addTextField("Оплата*", item.paymentMethod ?: "", true)

                // Автоматический расчет суммы при изменении продукта или количества
                val productCb = fields[1].field as ComboBox<Pair<Any, String>>
                val quantityField = fields[2].field as TextField

                val updateAmount = {
                    try {
                        val productId = (productCb.value?.first as? Number)?.toInt()
                        val quantity = quantityField.text.toIntOrNull() ?: 0
                        if (productId != null && quantity > 0) {
                            val product = productDAO.getById(productId)
                            if (product != null) {
                                val total = product.price.multiply(BigDecimal(quantity))
                                amountField.text = total.toString()
                            }
                        }
                    } catch (e: Exception) {
                        // Игнорируем ошибки при расчете
                    }
                }

                productCb.valueProperty().addListener { _, _, _ -> updateAmount() }
                quantityField.textProperty().addListener { _, _, _ -> updateAmount() }
            }

            is User -> {
                addTextField("Имя*", item.firstName, true)
                addTextField("Фамилия*", item.lastName, true)
                addTextField("Отчество*", item.patronymic ?: "", true)
                addTextField("Email*", item.email ?: "", true)
                addTextField("Телефон*", item.phone ?: "", true)

                val roles = roleDAO.getAll().map { it.id as Any to it.name }
                addComboBox("Роль*", roles, item.roleId, true)

                // Поле пароля не показываем при редактировании
            }

            is Maintenance -> {
                val machines = vendingMachineDAO.getAll().map { it.id as Any to "${it.name} (${it.address})" }
                val users = userDAO.getAll().map { it.id as Any to "${it.lastName} ${it.firstName}" }

                addComboBox("Устройство*", machines, item.deviceId, true)
                addDateField("Дата*", item.serviceDate, true, "дд.мм.гггг")
                addTextField("Описание*", item.description ?: "", true)
                addComboBox("Исполнитель*", users, item.performedBy, true)
            }
        }

        val saveButton = Button("Сохранить")
        saveButton.isDisable = true

        val cancelButton = Button("Отмена")

        fun validateAll(): Boolean {
            return fields.all { meta ->
                when (val field = meta.field) {
                    is TextField -> {
                        if (!field.isEditable) true else validateField(field, meta.required, meta.type)
                    }
                    is ComboBox<*> -> {
                        if (meta.required && field.value == null) {
                            field.style = "-fx-border-color: #e74c3c; -fx-border-width: 2;"
                            false
                        } else {
                            field.style = ""
                            true
                        }
                    }
                    is DatePicker -> {
                        if (meta.required && field.value == null) {
                            field.style = "-fx-border-color: #e74c3c; -fx-border-width: 2;"
                            false
                        } else {
                            field.style = ""
                            true
                        }
                    }
                    else -> true
                }
            }
        }

        fields.forEach { meta ->
            when (val field = meta.field) {
                is TextField -> {
                    if (field.isEditable) {
                        field.textProperty().addListener { _, _, _ ->
                            saveButton.isDisable = !validateAll()
                        }
                    }
                }
                is ComboBox<*> -> {
                    field.valueProperty().addListener { _, _, _ ->
                        saveButton.isDisable = !validateAll()
                    }
                }
                is DatePicker -> {
                    field.valueProperty().addListener { _, _, _ ->
                        saveButton.isDisable = !validateAll()
                    }
                }
            }
        }

        saveButton.isDisable = !validateAll()

        saveButton.setOnAction {
            try {
                when (item) {
                    is VendingMachine -> {
                        item.name = (fields[0].field as TextField).text
                        item.model = (fields[1].field as TextField).text
                        item.company = (fields[2].field as TextField).text
                        item.modem = (fields[3].field as TextField).text
                        item.address = (fields[4].field as TextField).text
                        item.location = (fields[5].field as TextField).text
                        item.installationDate = (fields[6].field as DatePicker).value
                        vendingMachineDAO.update(item)
                    }

                    is Product -> {
                        item.name = (fields[0].field as TextField).text
                        item.price = (fields[1].field as TextField).text.toBigDecimal()
                        item.quantityInStock = (fields[2].field as TextField).text.toInt()
                        item.minimalStock = (fields[3].field as TextField).text.toInt()
                        item.description = (fields[4].field as TextField).text
                        item.salesTrends = (fields[5].field as TextField).text
                        productDAO.update(item)
                    }

                    is Sale -> {
                        val deviceCb = fields[0].field as ComboBox<Pair<Any, String>>
                        val productCb = fields[1].field as ComboBox<Pair<Any, String>>

                        item.deviceId = (deviceCb.value.first as Number).toInt()
                        item.productId = (productCb.value.first as Number).toInt()
                        item.quantitySold = (fields[2].field as TextField).text.toInt()
                        item.totalAmount = (fields[3].field as TextField).text.toBigDecimal()
                        item.saleDatetime = LocalDateTime.parse((fields[4].field as TextField).text, dateTimeFormatter)
                        item.paymentMethod = (fields[5].field as TextField).text
                        saleDAO.update(item)
                    }

                    is User -> {
                        val roleCb = fields[5].field as ComboBox<Pair<Any, String>>

                        item.firstName = (fields[0].field as TextField).text
                        item.lastName = (fields[1].field as TextField).text
                        item.patronymic = (fields[2].field as TextField).text
                        item.email = (fields[3].field as TextField).text
                        item.phone = (fields[4].field as TextField).text
                        item.roleId = (roleCb.value.first as Number).toInt()

                        userDAO.update(item)
                    }

                    is Maintenance -> {
                        val deviceCb = fields[0].field as ComboBox<Pair<Any, String>>
                        val performerCb = fields[3].field as ComboBox<Pair<Any, String>>

                        item.deviceId = (deviceCb.value.first as Number).toInt()
                        item.serviceDate = (fields[1].field as DatePicker).value
                        item.description = (fields[2].field as TextField).text
                        item.performedBy = (performerCb.value.first as Number).toInt()
                        maintenanceDAO.update(item)
                    }
                }

                stage.close()
                refreshCurrentTable()
            } catch (e: Exception) {
                showAlert("Ошибка", e.message ?: "Ошибка обновления")
                e.printStackTrace()
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
                currentPage = 0
                updatePagination()
            }
            Product::class.java -> {
                currentData = productDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                currentPage = 0
                updatePagination()
            }
            Sale::class.java -> {
                currentData = saleDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                currentPage = 0
                updatePagination()
            }
            User::class.java -> {
                currentData = userDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                currentPage = 0
                updatePagination()
            }
            Maintenance::class.java -> {
                currentData = maintenanceDAO.getAll().sortedBy { it.id }
                val observableData = FXCollections.observableArrayList(currentData)
                filteredData = FilteredList(observableData) { true }
                currentPage = 0
                updatePagination()
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