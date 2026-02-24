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

class MainApp : Application() {

    private val vendingMachineDAO = VendingMachineDAO()
    private val productDAO = ProductDAO()
    private val saleDAO = SaleDAO()
    private val userDAO = UserDAO()
    private val maintenanceDAO = MaintenanceDAO()
    private val roleDAO = RoleDAO()

    private val mainContent = StackPane()
    private var currentPage = 0
    private var pageSize = 15
    private var currentData: List<Any> = listOf()
    private var currentEntityClass: Class<*>? = null
    private val tableView = TableView<Any>()

    // Для фильтрации
    private lateinit var filteredData: FilteredList<Any>
    private lateinit var filterField: TextField
    private lateinit var pageSizeComboBox: ComboBox<Int>
    private lateinit var paginationLabel: Label

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

    private fun createHeader(): HBox {
        val header = HBox()
        header.style = "-fx-background-color: #2c3e50; -fx-padding: 10px 15px;"
        header.alignment = Pos.CENTER_LEFT

        val title = Label("ООО Торговые Автоматы")
        title.style = "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;"

        val userInfo = HBox(10.0)
        userInfo.alignment = Pos.CENTER_RIGHT

        val userName = Label("Автоматов А.А.")
        userName.style = "-fx-text-fill: white; -fx-font-size: 14px;"

        val userRole = Label("Администратор")
        userRole.style = "-fx-text-fill: #bdc3c7; -fx-font-size: 12px;"

        val userBox = VBox(2.0)
        userBox.children.addAll(userName, userRole)

        val profileMenu = MenuButton("", null,
            MenuItem("Мой профиль"),
            MenuItem("Мои сессии"),
            MenuItem("Выход")
        )
        profileMenu.style = "-fx-background-color: transparent; -fx-text-fill: white;"

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        userInfo.children.addAll(userBox, profileMenu)
        header.children.addAll(title, spacer, userInfo)

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

        val header = Text("Личный кабинет. Главная")
        header.font = Font.font("System", FontWeight.BOLD, 16.0)

        val statsGrid = GridPane()
        statsGrid.hgap = 15.0
        statsGrid.vgap = 15.0

        val efficiencyCard = createStatsCard("Эффективность сети", "Работающих автомобилей - 100%", "#27ae60")
        statsGrid.add(efficiencyCard, 0, 0)

        val stateCard = createStatsCard("Состояние сети", "По умолчанию", "#3498db")
        statsGrid.add(stateCard, 1, 0)

        val summaryCard = createSummaryCard()
        statsGrid.add(summaryCard, 0, 1, 2, 1)

        val chartsBox = HBox(15.0)
        chartsBox.children.addAll(createSalesChart(), createQuantityChart())

        val newsSection = createNewsSection()

        content.children.addAll(header, statsGrid, chartsBox, newsSection)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)
    }

    private fun createStatsCard(title: String, value: String, color: String): VBox {
        val card = VBox(5.0)
        card.style = "-fx-background-color: white; -fx-padding: 12px; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"
        card.prefWidth = 230.0

        val titleLabel = Label(title)
        titleLabel.style = "-fx-font-size: 13px; -fx-text-fill: #7f8c8d;"

        val valueLabel = Label(value)
        valueLabel.style = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: $color;"

        card.children.addAll(titleLabel, valueLabel)
        return card
    }

    private fun createSummaryCard(): VBox {
        val card = VBox(8.0)
        card.style = "-fx-background-color: white; -fx-padding: 12px; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"
        card.prefWidth = 475.0

        val title = Label("Сводка")
        title.style = "-fx-font-size: 14px; -fx-font-weight: bold;"

        val grid = GridPane()
        grid.hgap = 25.0
        grid.vgap = 5.0

        val items = listOf(
            "Денег в ТБ:" to "27599 п.",
            "Сданы в ТБ:" to "12109 п.",
            "Выручка, сегодня:" to "11950 п.",
            "Выручка, завтра:" to "11360 п.",
            "Инвестирование, сегодня:" to "8145 п.",
            "Инвестирование, завтра:" to "9860 п.",
            "Обслуживание ТБ, сегодня:" to "2 / 2"
        )

        items.forEachIndexed { index, (label, value) ->
            val row = index / 2
            val col = index % 2

            val labelNode = Label(label)
            labelNode.style = "-fx-text-fill: #7f8c8d; -fx-font-size: 12px;"

            val valueNode = Label(value)
            valueNode.style = "-fx-font-weight: bold; -fx-font-size: 12px;"

            val hbox = HBox(5.0)
            hbox.children.addAll(labelNode, valueNode)

            grid.add(hbox, col, row)
        }

        card.children.addAll(title, grid)
        return card
    }

    private fun createSalesChart(): VBox {
        val chartBox = VBox(8.0)
        chartBox.style = "-fx-background-color: white; -fx-padding: 12px; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"
        chartBox.prefWidth = 380.0

        val title = Label("Динамика продаж за последние 10 дней")
        title.style = "-fx-font-size: 13px; -fx-font-weight: bold;"

        val subtitle = Label("Данные по предыдущей с 01.03.2025 по 16.03.2025")
        subtitle.style = "-fx-font-size: 11px; -fx-text-fill: #7f8c8d;"

        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        yAxis.label = "По сумме"

        val chart = LineChart(xAxis, yAxis)
        chart.prefHeight = 200.0
        chart.isLegendVisible = false

        val series = XYChart.Series<String, Number>()
        series.name = "Продажи"

        val data = listOf(15000, 12000, 14000, 11000, 13000, 10000, 14000, 12000, 15000, 13000)
        data.forEachIndexed { index, value ->
            series.data.add(XYChart.Data((index + 1).toString(), value))
        }

        chart.data.add(series)

        chartBox.children.addAll(title, subtitle, chart)
        return chartBox
    }

    private fun createQuantityChart(): VBox {
        val chartBox = VBox(8.0)
        chartBox.style = "-fx-background-color: white; -fx-padding: 12px; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"
        chartBox.prefWidth = 380.0

        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        yAxis.label = "По количеству"

        val chart = LineChart(xAxis, yAxis)
        chart.prefHeight = 200.0
        chart.isLegendVisible = false

        val series = XYChart.Series<String, Number>()
        series.name = "Количество"

        val data = listOf(5000, 6000, 4500, 7000, 5500, 4800, 6200, 5800, 7100, 4900)
        data.forEachIndexed { index, value ->
            series.data.add(XYChart.Data((index + 1).toString(), value))
        }

        chart.data.add(series)

        chartBox.children.addAll(chart)
        return chartBox
    }

    private fun createNewsSection(): VBox {
        val newsBox = VBox(8.0)
        newsBox.style = "-fx-background-color: white; -fx-padding: 12px; -fx-border-color: #dcdcdc; -fx-border-width: 1px;"

        val title = Label("Новости")
        title.style = "-fx-font-size: 14px; -fx-font-weight: bold;"

        val newsItems = listOf(
            "20.01.25" to "Промышленник КИПик получил дизайнер от компании ...",
            "21.02.24" to "Восстановление пользователей от KIT landing / KIT Trip",
            "28.02.24" to "Стоянка ЛДС 35 кв.м ZX для KIT",
            "04.02.24" to "Регион новый CRM системный KIT Trip",
            "27.01.24" to "Новый модуль основных автоматов от KIT Landing",
            "20.01.24" to "Подключение сервисника PCI DSS 4.0.1"
        )

        newsItems.forEach { (date, text) ->
            val itemBox = HBox(8.0)
            itemBox.style = "-fx-padding: 3px;"

            val dateLabel = Label(date)
            dateLabel.style = "-fx-font-weight: bold; -fx-min-width: 65px; -fx-font-size: 12px;"

            val textLabel = Label(text)
            textLabel.style = "-fx-font-size: 12px;"

            itemBox.children.addAll(dateLabel, textLabel)
            newsBox.children.add(itemBox)
        }

        val allNews = VBox(5.0)
        allNews.children.addAll(title, newsBox)

        return allNews
    }

    private fun showVendingMachines() {
        currentEntityClass = VendingMachine::class.java
        currentData = vendingMachineDAO.getAll()
        setupTableView(VendingMachine::class.java)
    }

    private fun showProducts() {
        currentEntityClass = Product::class.java
        currentData = productDAO.getAll()
        setupTableView(Product::class.java)
    }

    private fun showSales() {
        currentEntityClass = Sale::class.java
        currentData = saleDAO.getAll()
        setupTableView(Sale::class.java)
    }

    private fun showUsers() {
        currentEntityClass = User::class.java
        currentData = userDAO.getAll()
        setupTableView(User::class.java)
    }

    private fun showMaintenance() {
        currentEntityClass = Maintenance::class.java
        currentData = maintenanceDAO.getAll()
        setupTableView(Maintenance::class.java)
    }

    private fun <T : Any> setupTableView(entityClass: Class<T>) {
        currentPage = 0

        val content = VBox(15.0)
        content.padding = Insets(15.0)
        content.style = "-fx-background-color: #ecf0f1;"

        val header = Text("Управление: ${getEntityName(entityClass)}")
        header.font = Font.font("System", FontWeight.BOLD, 16.0)

        // Панель управления таблицей как на картинке
        val controlBar = createControlBar(entityClass)

        // Настройка TableView
        setupTableColumns(entityClass)

        // Инициализация данных с фильтрацией
        val observableData = FXCollections.observableArrayList(currentData)
        filteredData = FilteredList(observableData) { true }
        tableView.items = filteredData

        // Настройка фильтра
        filterField.textProperty().addListener { _, _, newValue ->
            filteredData.setPredicate { item ->
                if (newValue.isNullOrBlank()) return@setPredicate true
                filterItem(item, newValue)
            }
            updatePaginationLabel()
        }

        // Стилизация таблицы
        tableView.style = "-fx-font-size: 12px;"
        tableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY

        // Контейнер для таблицы с фиксированной высотой
        val tableContainer = VBox(tableView)
        VBox.setVgrow(tableView, Priority.ALWAYS)

        // Пагинация
        val paginationBar = createPaginationBar()

        content.children.addAll(header, controlBar, tableContainer, paginationBar)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)
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

                // Исправленные колонки с описанием и трендами
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

        // Выпадающий список "Показать X записей"
        val showLabel = Label("Показать:")
        showLabel.style = "-fx-font-size: 12px;"

        pageSizeComboBox = ComboBox(FXCollections.observableArrayList(15, 25, 50, 100))
        pageSizeComboBox.value = pageSize
        pageSizeComboBox.style = "-fx-font-size: 12px; -fx-pref-width: 70px;"
        pageSizeComboBox.setOnAction {
            pageSize = pageSizeComboBox.value
            currentPage = 0
            updatePaginationLabel()
        }

        val recordsLabel = Label("записей")
        recordsLabel.style = "-fx-font-size: 12px;"

        // Поле фильтра
        val filterLabel = Label("Фильтр:")
        filterLabel.style = "-fx-font-size: 12px; -fx-padding: 0 0 0 20;"

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
            showLabel, pageSizeComboBox, recordsLabel,
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

    private fun createPaginationBar(): HBox {
        val paginationBar = HBox(8.0)
        paginationBar.alignment = Pos.CENTER
        paginationBar.style = "-fx-padding: 10 0;"

        val prevButton = Button("◀ Предыдущая")
        prevButton.style = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        prevButton.setOnAction {
            if (currentPage > 0) {
                currentPage--
                updatePaginationLabel()
            }
        }

        val nextButton = Button("Следующая ▶")
        nextButton.style = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 3; -fx-font-size: 12px;"
        nextButton.setOnAction {
            if ((currentPage + 1) * pageSize < filteredData.size) {
                currentPage++
                updatePaginationLabel()
            }
        }

        paginationLabel = Label()
        paginationLabel.style = "-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 0 10;"

        updatePaginationLabel()

        paginationBar.children.addAll(prevButton, paginationLabel, nextButton)

        return paginationBar
    }

    private fun updatePaginationLabel() {
        val totalPages = maxOf(1, (filteredData.size + pageSize - 1) / pageSize)
        paginationLabel.text = "Страница ${currentPage + 1} из $totalPages"

        // Обновляем отображаемые данные
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, filteredData.size)

        // Создаем подсписок для текущей страницы
        val pageItems = FXCollections.observableArrayList<Any>()
        for (i in start until end) {
            pageItems.add(filteredData[i])
        }
        tableView.items = pageItems
    }

    private fun showAddDialog(entityClass: Class<*>) {
        val dialog = Dialog<Any>()
        dialog.title = "Добавить запись"
        dialog.headerText = "Создание новой записи: ${getEntityShortName(entityClass)}"

        val dialogPane = dialog.dialogPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        dialogPane.style = "-fx-background-color: white;"

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 8.0
        grid.padding = Insets(15.0)

        val fields = mutableListOf<TextField>()
        var row = 0

        when (entityClass) {
            VendingMachine::class.java -> {
                addTextField(grid, "Название*:", row++, fields, true)
                addTextField(grid, "Модель:", row++, fields)
                addTextField(grid, "Компания:", row++, fields)
                addTextField(grid, "Модем:", row++, fields)
                addTextField(grid, "Адрес:", row++, fields)
                addTextField(grid, "Место:", row++, fields)
                addTextField(grid, "Дата установки (ГГГГ-ММ-ДД):", row++, fields)
            }
            Product::class.java -> {
                addTextField(grid, "Название*:", row++, fields, true)
                addTextField(grid, "Цена*:", row++, fields, true)
                addTextField(grid, "Количество*:", row++, fields, true)
                addTextField(grid, "Мин. запас*:", row++, fields, true)
                addTextField(grid, "Описание:", row++, fields)
                addTextField(grid, "Тренды продаж:", row++, fields)
            }
            User::class.java -> {
                addTextField(grid, "Имя*:", row++, fields, true)
                addTextField(grid, "Фамилия*:", row++, fields, true)
                addTextField(grid, "Отчество:", row++, fields)
                addTextField(grid, "Email:", row++, fields)
                addTextField(grid, "Телефон:", row++, fields)
                addTextField(grid, "Роль ID*:", row++, fields, true)
                addTextField(grid, "Пароль (hash)*:", row++, fields, true)
            }
            Sale::class.java -> {
                addTextField(grid, "Устройство ID*:", row++, fields, true)
                addTextField(grid, "Продукт ID*:", row++, fields, true)
                addTextField(grid, "Количество*:", row++, fields, true)
                addTextField(grid, "Сумма*:", row++, fields, true)
                addTextField(grid, "Дата и время (ГГГГ-ММ-ДДTЧЧ:ММ:СС):", row++, fields)
                addTextField(grid, "Метод оплаты:", row++, fields)
            }
            Maintenance::class.java -> {
                addTextField(grid, "Устройство ID*:", row++, fields, true)
                addTextField(grid, "Дата (ГГГГ-ММ-ДД):", row++, fields)
                addTextField(grid, "Описание:", row++, fields)
                addTextField(grid, "Исполнитель ID:", row++, fields)
            }
        }

        dialogPane.content = grid

        dialog.setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                try {
                    when (entityClass) {
                        VendingMachine::class.java -> {
                            if (fields[0].text.isBlank()) throw Exception("Название обязательно")
                            VendingMachine().apply {
                                name = fields[0].text
                                model = fields[1].text.ifEmpty { null }
                                company = fields[2].text.ifEmpty { null }
                                modem = fields[3].text.ifEmpty { null }
                                address = fields[4].text.ifEmpty { null }
                                location = fields[5].text.ifEmpty { null }
                                installationDate = try {
                                    if (fields[6].text.isNotBlank()) LocalDate.parse(fields[6].text) else null
                                } catch (e: Exception) { null }
                            }.also { vendingMachineDAO.save(it) }
                        }
                        Product::class.java -> {
                            if (fields[0].text.isBlank()) throw Exception("Название обязательно")
                            Product().apply {
                                name = fields[0].text
                                price = fields[1].text.toBigDecimalOrNull() ?: throw Exception("Некорректная цена")
                                quantityInStock = fields[2].text.toIntOrNull() ?: throw Exception("Некорректное количество")
                                minimalStock = fields[3].text.toIntOrNull() ?: throw Exception("Некорректный мин. запас")
                                description = fields[4].text.ifEmpty { null }
                                salesTrends = fields[5].text.ifEmpty { null }
                            }.also { productDAO.save(it) }
                        }
                        User::class.java -> {
                            if (fields[0].text.isBlank()) throw Exception("Имя обязательно")
                            if (fields[1].text.isBlank()) throw Exception("Фамилия обязательна")
                            User().apply {
                                firstName = fields[0].text
                                lastName = fields[1].text
                                patronymic = fields[2].text.ifEmpty { null }
                                email = fields[3].text.ifEmpty { null }
                                phone = fields[4].text.ifEmpty { null }
                                roleId = fields[5].text.toIntOrNull() ?: throw Exception("Некорректный ID роли")
                                passwordHash = fields[6].text.ifBlank { throw Exception("Пароль обязателен") }
                                createdAt = LocalDateTime.now()
                            }.also { userDAO.save(it) }
                        }
                        Sale::class.java -> {
                            Sale().apply {
                                deviceId = fields[0].text.toIntOrNull() ?: throw Exception("Некорректный ID устройства")
                                productId = fields[1].text.toIntOrNull() ?: throw Exception("Некорректный ID продукта")
                                quantitySold = fields[2].text.toIntOrNull() ?: throw Exception("Некорректное количество")
                                totalAmount = fields[3].text.toBigDecimalOrNull() ?: throw Exception("Некорректная сумма")
                                saleDatetime = try {
                                    if (fields[4].text.isNotBlank()) LocalDateTime.parse(fields[4].text) else LocalDateTime.now()
                                } catch (e: Exception) { LocalDateTime.now() }
                                paymentMethod = fields[5].text.ifEmpty { null }
                            }.also { saleDAO.save(it) }
                        }
                        Maintenance::class.java -> {
                            Maintenance().apply {
                                deviceId = fields[0].text.toIntOrNull() ?: throw Exception("Некорректный ID устройства")
                                serviceDate = try {
                                    if (fields[1].text.isNotBlank()) LocalDate.parse(fields[1].text) else LocalDate.now()
                                } catch (e: Exception) { LocalDate.now() }
                                description = fields[2].text.ifEmpty { null }
                                performedBy = fields[3].text.toIntOrNull()
                            }.also { maintenanceDAO.save(it) }
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    showAlert("Ошибка", "Не удалось сохранить: ${e.message}")
                    null
                }
            } else null
        }

        val result = dialog.showAndWait()
        result.ifPresent {
            refreshCurrentTable()
        }
    }

    private fun addTextField(grid: GridPane, label: String, row: Int, fields: MutableList<TextField>, required: Boolean = false) {
        val labelText = if (required) Label("$label") else Label(label)
        labelText.style = if (required) "-fx-font-weight: bold; -fx-font-size: 12px;" else "-fx-font-size: 12px;"
        grid.add(labelText, 0, row)
        val textField = TextField()
        textField.style = "-fx-font-size: 12px;"
        if (required) {
            textField.promptText = "Обязательное поле"
        }
        grid.add(textField, 1, row)
        fields.add(textField)
    }

    private fun showEditDialog(item: Any) {
        val dialog = Dialog<Any>()
        dialog.title = "Редактировать запись"
        dialog.headerText = "Изменение: ${getEntityShortName(item::class.java)} (ID: ${getIdValue(item)})"

        val dialogPane = dialog.dialogPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
        dialogPane.style = "-fx-background-color: white;"

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 8.0
        grid.padding = Insets(15.0)

        val fields = mutableListOf<TextField>()
        var row = 0

        when (item) {
            is VendingMachine -> {
                addTextFieldWithValue(grid, "Название*:", row++, item.name, fields, true)
                addTextFieldWithValue(grid, "Модель:", row++, item.model ?: "", fields)
                addTextFieldWithValue(grid, "Компания:", row++, item.company ?: "", fields)
                addTextFieldWithValue(grid, "Модем:", row++, item.modem ?: "", fields)
                addTextFieldWithValue(grid, "Адрес:", row++, item.address ?: "", fields)
                addTextFieldWithValue(grid, "Место:", row++, item.location ?: "", fields)
                addTextFieldWithValue(grid, "Дата установки (ГГГГ-ММ-ДД):", row++, item.installationDate?.toString() ?: "", fields)
            }
            is Product -> {
                addTextFieldWithValue(grid, "Название*:", row++, item.name, fields, true)
                addTextFieldWithValue(grid, "Цена*:", row++, item.price.toString(), fields, true)
                addTextFieldWithValue(grid, "Количество*:", row++, item.quantityInStock.toString(), fields, true)
                addTextFieldWithValue(grid, "Мин. запас*:", row++, item.minimalStock.toString(), fields, true)
                addTextFieldWithValue(grid, "Описание:", row++, item.description ?: "", fields)
                addTextFieldWithValue(grid, "Тренды продаж:", row++, item.salesTrends ?: "", fields)
            }
            is User -> {
                addTextFieldWithValue(grid, "Имя*:", row++, item.firstName, fields, true)
                addTextFieldWithValue(grid, "Фамилия*:", row++, item.lastName, fields, true)
                addTextFieldWithValue(grid, "Отчество:", row++, item.patronymic ?: "", fields)
                addTextFieldWithValue(grid, "Email:", row++, item.email ?: "", fields)
                addTextFieldWithValue(grid, "Телефон:", row++, item.phone ?: "", fields)
                addTextFieldWithValue(grid, "Роль ID*:", row++, item.roleId.toString(), fields, true)
                addTextFieldWithValue(grid, "Пароль (hash):", row++, item.passwordHash, fields)
            }
            is Sale -> {
                addTextFieldWithValue(grid, "Устройство ID*:", row++, item.deviceId.toString(), fields, true)
                addTextFieldWithValue(grid, "Продукт ID*:", row++, item.productId.toString(), fields, true)
                addTextFieldWithValue(grid, "Количество*:", row++, item.quantitySold.toString(), fields, true)
                addTextFieldWithValue(grid, "Сумма*:", row++, item.totalAmount.toString(), fields, true)
                addTextFieldWithValue(grid, "Дата и время (ГГГГ-ММ-ДДTЧЧ:ММ:СС):", row++, item.saleDatetime.toString(), fields)
                addTextFieldWithValue(grid, "Метод оплаты:", row++, item.paymentMethod ?: "", fields)
            }
            is Maintenance -> {
                addTextFieldWithValue(grid, "Устройство ID*:", row++, item.deviceId.toString(), fields, true)
                addTextFieldWithValue(grid, "Дата (ГГГГ-ММ-ДД):", row++, item.serviceDate.toString(), fields)
                addTextFieldWithValue(grid, "Описание:", row++, item.description ?: "", fields)
                addTextFieldWithValue(grid, "Исполнитель ID:", row++, item.performedBy?.toString() ?: "", fields)
            }
        }

        dialogPane.content = grid

        dialog.setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                try {
                    when (item) {
                        is VendingMachine -> {
                            if (fields[0].text.isBlank()) throw Exception("Название обязательно")
                            item.apply {
                                name = fields[0].text
                                model = fields[1].text.ifEmpty { null }
                                company = fields[2].text.ifEmpty { null }
                                modem = fields[3].text.ifEmpty { null }
                                address = fields[4].text.ifEmpty { null }
                                location = fields[5].text.ifEmpty { null }
                                installationDate = try {
                                    if (fields[6].text.isNotBlank()) LocalDate.parse(fields[6].text) else null
                                } catch (e: Exception) { null }
                            }.also { vendingMachineDAO.update(it) }
                        }
                        is Product -> {
                            if (fields[0].text.isBlank()) throw Exception("Название обязательно")
                            item.apply {
                                name = fields[0].text
                                price = fields[1].text.toBigDecimalOrNull() ?: throw Exception("Некорректная цена")
                                quantityInStock = fields[2].text.toIntOrNull() ?: throw Exception("Некорректное количество")
                                minimalStock = fields[3].text.toIntOrNull() ?: throw Exception("Некорректный мин. запас")
                                description = fields[4].text.ifEmpty { null }
                                salesTrends = fields[5].text.ifEmpty { null }
                            }.also { productDAO.update(it) }
                        }
                        is User -> {
                            if (fields[0].text.isBlank()) throw Exception("Имя обязательно")
                            if (fields[1].text.isBlank()) throw Exception("Фамилия обязательна")
                            item.apply {
                                firstName = fields[0].text
                                lastName = fields[1].text
                                patronymic = fields[2].text.ifEmpty { null }
                                email = fields[3].text.ifEmpty { null }
                                phone = fields[4].text.ifEmpty { null }
                                roleId = fields[5].text.toIntOrNull() ?: throw Exception("Некорректный ID роли")
                                if (fields[6].text.isNotBlank()) {
                                    passwordHash = fields[6].text
                                }
                            }.also { userDAO.update(it) }
                        }
                        is Sale -> {
                            item.apply {
                                deviceId = fields[0].text.toIntOrNull() ?: throw Exception("Некорректный ID устройства")
                                productId = fields[1].text.toIntOrNull() ?: throw Exception("Некорректный ID продукта")
                                quantitySold = fields[2].text.toIntOrNull() ?: throw Exception("Некорректное количество")
                                totalAmount = fields[3].text.toBigDecimalOrNull() ?: throw Exception("Некорректная сумма")
                                saleDatetime = try {
                                    if (fields[4].text.isNotBlank()) LocalDateTime.parse(fields[4].text) else LocalDateTime.now()
                                } catch (e: Exception) { LocalDateTime.now() }
                                paymentMethod = fields[5].text.ifEmpty { null }
                            }.also { saleDAO.update(it) }
                        }
                        is Maintenance -> {
                            item.apply {
                                deviceId = fields[0].text.toIntOrNull() ?: throw Exception("Некорректный ID устройства")
                                serviceDate = try {
                                    if (fields[1].text.isNotBlank()) LocalDate.parse(fields[1].text) else LocalDate.now()
                                } catch (e: Exception) { LocalDate.now() }
                                description = fields[2].text.ifEmpty { null }
                                performedBy = fields[3].text.toIntOrNull()
                            }.also { maintenanceDAO.update(it) }
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    showAlert("Ошибка", "Не удалось обновить: ${e.message}")
                    null
                }
            } else null
        }

        val result = dialog.showAndWait()
        result.ifPresent {
            refreshCurrentTable()
        }
    }

    private fun addTextFieldWithValue(grid: GridPane, label: String, row: Int, value: String, fields: MutableList<TextField>, required: Boolean = false) {
        val labelText = if (required) Label("$label") else Label(label)
        labelText.style = if (required) "-fx-font-weight: bold; -fx-font-size: 12px;" else "-fx-font-size: 12px;"
        grid.add(labelText, 0, row)
        val textField = TextField(value)
        textField.style = "-fx-font-size: 12px;"
        if (required) {
            textField.promptText = "Обязательное поле"
        }
        grid.add(textField, 1, row)
        fields.add(textField)
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

        val result = alert.showAndWait()
        if (result.isPresent && result.get() == ButtonType.OK) {
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
                currentData = vendingMachineDAO.getAll()
                filteredData = FilteredList(FXCollections.observableArrayList(currentData)) { true }
                showVendingMachines()
            }
            Product::class.java -> {
                currentData = productDAO.getAll()
                filteredData = FilteredList(FXCollections.observableArrayList(currentData)) { true }
                showProducts()
            }
            Sale::class.java -> {
                currentData = saleDAO.getAll()
                filteredData = FilteredList(FXCollections.observableArrayList(currentData)) { true }
                showSales()
            }
            User::class.java -> {
                currentData = userDAO.getAll()
                filteredData = FilteredList(FXCollections.observableArrayList(currentData)) { true }
                showUsers()
            }
            Maintenance::class.java -> {
                currentData = maintenanceDAO.getAll()
                filteredData = FilteredList(FXCollections.observableArrayList(currentData)) { true }
                showMaintenance()
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