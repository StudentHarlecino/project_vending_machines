package com.example.project_vending_machines

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.collections.ObservableList
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
import java.time.format.DateTimeFormatter
import com.example.project_vending_machines.dao.*
import com.example.project_vending_machines.entity.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.Priority
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.Node

class MainApp : Application() {

    private val vendingMachineDAO = VendingMachineDAO()
    private val productDAO = ProductDAO()
    private val saleDAO = SaleDAO()
    private val userDAO = UserDAO()
    private val maintenanceDAO = MaintenanceDAO()
    private val roleDAO = RoleDAO()

    private val mainContent = StackPane()
    private var currentPage = 0
    private val pageSize = 9
    private var currentTableData: ObservableList<Any> = FXCollections.observableArrayList()
    private var currentTableView = TableView<Any>()
    private var currentEntityClass: Class<*>? = null

    private val companyItems = listOf(
        "903823", "903825", "903826", "903827", "903822", "903820", "903821"
    )

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ООО Торговые Автоматы - Личный кабинет"

        val root = BorderPane()
        root.top = createHeader()
        root.left = createNavigation()
        root.center = mainContent

        // Показываем главную страницу по умолчанию
        showDashboard()

        val scene = Scene(root, 1400.0, 800.0)
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun createHeader(): HBox {
        val header = HBox()
        header.style = "-fx-background-color: #2c3e50; -fx-padding: 15px;"
        header.alignment = Pos.CENTER_LEFT

        val title = Label("ООО Торговые Автоматы")
        title.style = "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;"

        val userInfo = HBox(10.0)
        userInfo.alignment = Pos.CENTER_RIGHT

        val userName = Label("Автоматов А.А.")
        userName.style = "-fx-text-fill: white;"

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
        val navigation = VBox(10.0)
        navigation.style = "-fx-background-color: #34495e; -fx-padding: 20px;"
        navigation.prefWidth = 220.0

        // Заголовок навигации
        val navTitle = Label("Навигация")
        navTitle.style = "-fx-text-fill: #bdc3c7; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;"
        navigation.children.add(navTitle)

        // Основные пункты меню
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

        // Разделитель
        val separator = Separator()
        separator.style = "-fx-background-color: #4a5a6e; -fx-padding: 10 0 10 0;"
        navigation.children.add(separator)

        // Администрирование (раскрывающийся список)
        val adminButton = createNavButton("⚙️  Администрирование", true)

        // Выпадающий список для таблиц
        val adminMenu = VBox(5.0)
        adminMenu.style = "-fx-padding: 0 0 0 20;"
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
            "-fx-background-color: #3498db; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-padding: 8 10;"
        else
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-padding: 8 10;"
        button.onMouseEntered = EventHandler {
            if (!isSelected) {
                button.style = "-fx-background-color: #3d566e; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-padding: 8 10;"
            }
        }
        button.onMouseExited = EventHandler {
            if (!isSelected) {
                button.style = "-fx-background-color: transparent; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-padding: 8 10;"
            }
        }
        return button
    }

    private fun showDashboard() {
        val content = VBox(20.0)
        content.padding = Insets(20.0)
        content.style = "-fx-background-color: #ecf0f1;"

        // Заголовок
        val header = Text("Личный кабинет. Главная")
        header.font = Font.font("System", FontWeight.BOLD, 18.0)

        // Статистические карточки
        val statsGrid = GridPane()
        statsGrid.hgap = 20.0
        statsGrid.vgap = 20.0

        // Карточка эффективности
        val efficiencyCard = createStatsCard("Эффективность сети", "Работающих автомобилей - 100%", "#27ae60")
        statsGrid.add(efficiencyCard, 0, 0)

        // Карточка состояния
        val stateCard = createStatsCard("Состояние сети", "По умолчанию", "#3498db")
        statsGrid.add(stateCard, 1, 0)

        // Сводка
        val summaryCard = createSummaryCard()
        statsGrid.add(summaryCard, 0, 1, 2, 1)

        // Графики
        val chartsBox = HBox(20.0)
        chartsBox.children.addAll(createSalesChart(), createQuantityChart())

        // Новости
        val newsSection = createNewsSection()

        content.children.addAll(header, statsGrid, chartsBox, newsSection)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)
    }

    private fun createStatsCard(title: String, value: String, color: String): VBox {
        val card = VBox(10.0)
        card.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        card.prefWidth = 250.0

        val titleLabel = Label(title)
        titleLabel.style = "-fx-font-size: 14px; -fx-text-fill: #7f8c8d;"

        val valueLabel = Label(value)
        valueLabel.style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: $color;"

        card.children.addAll(titleLabel, valueLabel)
        return card
    }

    private fun createSummaryCard(): VBox {
        val card = VBox(10.0)
        card.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        card.prefWidth = 520.0

        val title = Label("Сводка")
        title.style = "-fx-font-size: 16px; -fx-font-weight: bold;"

        val grid = GridPane()
        grid.hgap = 30.0
        grid.vgap = 10.0

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
            labelNode.style = "-fx-text-fill: #7f8c8d;"

            val valueNode = Label(value)
            valueNode.style = "-fx-font-weight: bold;"

            val hbox = HBox(5.0)
            hbox.children.addAll(labelNode, valueNode)

            grid.add(hbox, col, row)
        }

        card.children.addAll(title, grid)
        return card
    }

    private fun createSalesChart(): VBox {
        val chartBox = VBox(10.0)
        chartBox.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        chartBox.prefWidth = 400.0

        val title = Label("Динамика продаж за последние 10 дней")
        title.style = "-fx-font-size: 14px; -fx-font-weight: bold;"

        val subtitle = Label("Данные по предыдущей с 01.03.2025 по 16.03.2025")
        subtitle.style = "-fx-font-size: 12px; -fx-text-fill: #7f8c8d;"

        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        yAxis.label = "По сумме"

        val chart = LineChart(xAxis, yAxis)
        chart.prefHeight = 250.0
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
        val chartBox = VBox(10.0)
        chartBox.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        chartBox.prefWidth = 400.0

        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        yAxis.label = "По количеству"

        val chart = LineChart(xAxis, yAxis)
        chart.prefHeight = 250.0
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
        val newsBox = VBox(10.0)
        newsBox.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"

        val title = Label("Новости")
        title.style = "-fx-font-size: 16px; -fx-font-weight: bold;"

        val newsItems = listOf(
            "20.01.25" to "Промышленник КИПик получил дизайнер от компании ...",
            "21.02.24" to "Восстановление пользователей от KIT landing / KIT Trip",
            "28.02.24" to "Стоянка ЛДС 35 кв.м ZX для KIT",
            "04.02.24" to "Регион новый CRM системный KIT Trip",
            "27.01.24" to "Новый модуль основных автоматов от KIT Landing",
            "20.01.24" to "Подключение сервисника PCI DSS 4.0.1"
        )

        newsItems.forEach { (date, text) ->
            val itemBox = HBox(10.0)
            itemBox.style = "-fx-padding: 5px;"

            val dateLabel = Label(date)
            dateLabel.style = "-fx-font-weight: bold; -fx-min-width: 70px;"

            val textLabel = Label(text)

            itemBox.children.addAll(dateLabel, textLabel)
            newsBox.children.add(itemBox)
        }

        val allNews = VBox(5.0)
        allNews.children.addAll(title, newsBox)

        return allNews
    }

    private fun showVendingMachines() {
        currentEntityClass = VendingMachine::class.java
        val data = vendingMachineDAO.getAll()
        setupTableView(data, VendingMachine::class.java)
    }

    private fun showProducts() {
        currentEntityClass = Product::class.java
        val data = productDAO.getAll()
        setupTableView(data, Product::class.java)
    }

    private fun showSales() {
        currentEntityClass = Sale::class.java
        val data = saleDAO.getAll()
        setupTableView(data, Sale::class.java)
    }

    private fun showUsers() {
        currentEntityClass = User::class.java
        val data = userDAO.getAll()
        setupTableView(data, User::class.java)
    }

    private fun showMaintenance() {
        currentEntityClass = Maintenance::class.java
        val data = maintenanceDAO.getAll()
        setupTableView(data, Maintenance::class.java)
    }

    private fun <T : Any> setupTableView(data: List<T>, entityClass: Class<T>) {
        currentPage = 0
        currentTableData = FXCollections.observableArrayList(data)
        currentTableView = createTableViewForEntity(entityClass)

        val content = VBox(20.0)
        content.padding = Insets(20.0)
        content.style = "-fx-background-color: #ecf0f1;"

        // Заголовок
        val header = Text("Управление: ${entityClass.simpleName}")
        header.font = Font.font("System", FontWeight.BOLD, 18.0)

        // Панель с компаниями слева (как на картинке)
        val mainHorizontalBox = HBox(20.0)

        // Левая панель с компаниями
        val companiesPanel = createCompaniesPanel()

        // Правая панель с таблицей
        val rightPanel = VBox(10.0)

        // Кнопки управления
        val buttonBar = createButtonBar(entityClass)

        // Таблица
        updateTableViewPage()

        // Пагинация
        val paginationBar = createPaginationBar(data.size)

        rightPanel.children.addAll(buttonBar, currentTableView, paginationBar)

        mainHorizontalBox.children.addAll(companiesPanel, rightPanel)
        HBox.setHgrow(rightPanel, Priority.ALWAYS)

        content.children.addAll(header, mainHorizontalBox)

        val scrollPane = ScrollPane(content)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"

        mainContent.children.clear()
        mainContent.children.add(scrollPane)
    }

    private fun createCompaniesPanel(): VBox {
        val panel = VBox(15.0)
        panel.style = "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 5px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"
        panel.prefWidth = 250.0

        val title = Label("Компании")
        title.style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;"

        val searchField = TextField()
        searchField.promptText = "Поиск..."
        searchField.style = "-fx-pref-width: 220px;"

        val companyList = VBox(5.0)

        // Добавляем элементы компаний как на картинке
        val companyGroups = listOf(
            "БД: «Магнит»" to listOf("База: Scano Crasallo - 400"),
            "ГП: «Магнит»" to listOf("Кол-во: 400"),
            "ДОСААБ" to listOf("База: Barcha BIM - 972"),
            "Зеяда «Тайфун»" to listOf("Кол-во: 1000"),
            "Милака «У Греции»" to listOf("Кол-во: 1000"),
            "Налоговая" to listOf("Кол-во: 1000"),
            "Рынок" to listOf("Кол-во: 1000"),
            "ТК «21 Вес»" to listOf("Кол-во: 1000"),
            "ТК «21 Вес» (опк)" to listOf("Кол-во: 1000")
        )

        companyGroups.forEach { (name, details) ->
            val groupBox = VBox(2.0)
            groupBox.style = "-fx-padding: 5px; -fx-border-color: #ecf0f1; -fx-border-width: 0 0 1 0;"

            val nameLabel = Label(name)
            nameLabel.style = "-fx-font-weight: bold; -fx-font-size: 13px;"

            groupBox.children.add(nameLabel)

            details.forEach { detail ->
                val detailLabel = Label(detail)
                detailLabel.style = "-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-padding: 0 0 0 10;"
                groupBox.children.add(detailLabel)
            }

            companyList.children.add(groupBox)
        }

        panel.children.addAll(title, searchField, companyList)

        val scrollPane = ScrollPane(panel)
        scrollPane.isFitToWidth = true
        scrollPane.style = "-fx-background-color: transparent;"
        scrollPane.prefHeight = 600.0

        return VBox(scrollPane)
    }

    private fun <T : Any> createButtonBar(entityClass: Class<T>): HBox {
        val buttonBar = HBox(10.0)
        buttonBar.alignment = Pos.CENTER_RIGHT
        buttonBar.style = "-fx-padding: 10 0;"

        val addButton = Button("➕ Добавить")
        addButton.style = "-fx-background-color: #27ae60; -fx-text-fill: white;"
        addButton.setOnAction { showAddDialog(entityClass) }

        val exportButton = Button("📤 Экспорт")
        exportButton.style = "-fx-background-color: #3498db; -fx-text-fill: white;"

        buttonBar.children.addAll(addButton, exportButton)

        val spacer = Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)
        buttonBar.children.add(0, spacer)

        return buttonBar
    }

    private fun createPaginationBar(totalItems: Int): HBox {
        val paginationBar = HBox(10.0)
        paginationBar.alignment = Pos.CENTER
        paginationBar.style = "-fx-padding: 10 0;"

        val prevButton = Button("◀ Предыдущая")
        prevButton.style = "-fx-background-color: #34495e; -fx-text-fill: white;"
        prevButton.setOnAction {
            if (currentPage > 0) {
                currentPage--
                updateTableViewPage()
            }
        }

        val nextButton = Button("Следующая ▶")
        nextButton.style = "-fx-background-color: #34495e; -fx-text-fill: white;"
        nextButton.setOnAction {
            if ((currentPage + 1) * pageSize < totalItems) {
                currentPage++
                updateTableViewPage()
            }
        }

        val pageInfo = Label("Страница ${currentPage + 1} из ${maxOf(1, (totalItems + pageSize - 1) / pageSize)}")
        pageInfo.style = "-fx-font-weight: bold;"

        paginationBar.children.addAll(prevButton, pageInfo, nextButton)

        return paginationBar
    }

    private fun updateTableViewPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, currentTableData.size)
        val pageData = currentTableData.subList(start, end)
        currentTableView.items = FXCollections.observableArrayList(pageData)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> createTableViewForEntity(entityClass: Class<T>): TableView<Any> {
        val tableView = TableView<Any>()
        tableView.prefHeight = 400.0
        tableView.style = "-fx-font-size: 12px;"

        tableView.columns.clear()

        when (entityClass) {
            VendingMachine::class.java -> {
                addColumn<VendingMachine>(tableView, "ID", "id", Long::class.java)
                addColumn<VendingMachine>(tableView, "Модель", "model", String::class.java)
                addColumn<VendingMachine>(tableView, "Серийный №", "serialNumber", String::class.java)
                addColumn<VendingMachine>(tableView, "Инвентарный №", "inventoryNumber", String::class.java)
                addColumn<VendingMachine>(tableView, "Статус", "status", String::class.java)
                addColumn<VendingMachine>(tableView, "Локация", "location", String::class.java)
                addColumn<VendingMachine>(tableView, "Доход", "totalIncome", java.math.BigDecimal::class.java)
                addColumn<VendingMachine>(tableView, "Дата произв.", "manufactureDate", LocalDate::class.java)
                addColumn<VendingMachine>(tableView, "Дата ввода", "commissioningDate", LocalDate::class.java)
                addColumnWithNode<VendingMachine>(tableView, "Действия") { item ->
                    createActionButtons(item)
                }
            }
            Product::class.java -> {
                addColumn<Product>(tableView, "ID", "id", Long::class.java)
                addColumn<Product>(tableView, "Название", "name", String::class.java)
                addColumn<Product>(tableView, "Цена", "price", java.math.BigDecimal::class.java)
                addColumn<Product>(tableView, "Количество", "quantityInStock", Integer::class.java)
                addColumn<Product>(tableView, "Мин. запас", "minimalStock", Integer::class.java)
                addColumn<Product>(tableView, "Описание", "description", String::class.java)
                addColumnWithNode<Product>(tableView, "Действия") { item ->
                    createActionButtons(item)
                }
            }
            Sale::class.java -> {
                addColumn<Sale>(tableView, "ID", "id", Long::class.java)
                addColumn<Sale>(tableView, "Устройство ID", "deviceId", Integer::class.java)
                addColumn<Sale>(tableView, "Продукт ID", "productId", Integer::class.java)
                addColumn<Sale>(tableView, "Кол-во", "quantitySold", Integer::class.java)
                addColumn<Sale>(tableView, "Сумма", "totalAmount", java.math.BigDecimal::class.java)
                addColumn<Sale>(tableView, "Дата/время", "saleDatetime", java.time.LocalDateTime::class.java)
                addColumn<Sale>(tableView, "Метод оплаты", "paymentMethod", String::class.java)
                addColumnWithNode<Sale>(tableView, "Действия") { item ->
                    createActionButtons(item)
                }
            }
            User::class.java -> {
                addColumn<User>(tableView, "ID", "id", Long::class.java)
                addColumn<User>(tableView, "Имя", "firstName", String::class.java)
                addColumn<User>(tableView, "Фамилия", "lastName", String::class.java)
                addColumn<User>(tableView, "Отчество", "patronymic", String::class.java)
                addColumn<User>(tableView, "Email", "email", String::class.java)
                addColumn<User>(tableView, "Телефон", "phone", String::class.java)
                addColumn<User>(tableView, "Роль ID", "roleId", Integer::class.java)
                addColumnWithNode<User>(tableView, "Действия") { item ->
                    createActionButtons(item)
                }
            }
            Maintenance::class.java -> {
                addColumn<Maintenance>(tableView, "ID", "id", Long::class.java)
                addColumn<Maintenance>(tableView, "Устройство ID", "deviceId", Integer::class.java)
                addColumn<Maintenance>(tableView, "Дата", "serviceDate", LocalDate::class.java)
                addColumn<Maintenance>(tableView, "Описание", "description", String::class.java)
                addColumn<Maintenance>(tableView, "Исполнитель ID", "performedBy", Integer::class.java)
                addColumnWithNode<Maintenance>(tableView, "Действия") { item ->
                    createActionButtons(item)
                }
            }
        }

        return tableView
    }

    private fun <T : Any> addColumn(
        tableView: TableView<Any>,
        title: String,
        propertyName: String,
        type: Class<*>
    ) {
        val column = TableColumn<Any, Any>(title)
        column.setCellValueFactory { cellData ->
            val item = cellData.value
            try {
                val property = item::class.java.getDeclaredField(propertyName).apply { isAccessible = true }
                SimpleObjectProperty(property.get(item))
            } catch (e: Exception) {
                SimpleObjectProperty(null)
            }
        }
        tableView.columns.add(column)
    }

    private fun <T : Any> addColumnWithNode(
        tableView: TableView<Any>,
        title: String,
        nodeFactory: (T) -> Node
    ) {
        val column = TableColumn<Any, Node>(title)
        column.setCellValueFactory { cellData ->
            val item = cellData.value as T
            SimpleObjectProperty(nodeFactory(item))
        }
        tableView.columns.add(column)
    }

    private fun createActionButtons(item: Any): HBox {
        val buttons = HBox(5.0)

        val editButton = Button("✏️")
        editButton.style = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11px;"
        editButton.setOnAction { showEditDialog(item) }

        val deleteButton = Button("🗑️")
        deleteButton.style = "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px;"
        deleteButton.setOnAction { showDeleteConfirmation(item) }

        buttons.children.addAll(editButton, deleteButton)
        return buttons
    }

    private fun showAddDialog(entityClass: Class<*>) {
        val dialog = Dialog<Any>()
        dialog.title = "Добавить запись"
        dialog.headerText = "Создание новой записи в таблице ${entityClass.simpleName}"

        val dialogPane = dialog.dialogPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0)

        val fields = mutableListOf<TextField>()
        var row = 0

        when (entityClass) {
            VendingMachine::class.java -> {
                addTextField(grid, "Модель:", row++, fields)
                addTextField(grid, "Серийный номер:", row++, fields)
                addTextField(grid, "Инвентарный номер:", row++, fields)
                addTextField(grid, "Локация:", row++, fields)
                addTextField(grid, "Статус:", row++, fields)
                addTextField(grid, "Производитель:", row++, fields)
            }
            Product::class.java -> {
                addTextField(grid, "Название:", row++, fields)
                addTextField(grid, "Цена:", row++, fields)
                addTextField(grid, "Количество:", row++, fields)
                addTextField(grid, "Мин. запас:", row++, fields)
                addTextField(grid, "Описание:", row++, fields)
            }
            User::class.java -> {
                addTextField(grid, "Имя:", row++, fields)
                addTextField(grid, "Фамилия:", row++, fields)
                addTextField(grid, "Email:", row++, fields)
                addTextField(grid, "Телефон:", row++, fields)
                addTextField(grid, "Роль ID:", row++, fields)
                addTextField(grid, "Пароль:", row++, fields)
            }
            else -> {
                addTextField(grid, "Поле 1:", row++, fields)
                addTextField(grid, "Поле 2:", row++, fields)
            }
        }

        dialogPane.content = grid

        dialog.setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                when (entityClass) {
                    VendingMachine::class.java -> {
                        VendingMachine().apply {
                            model = fields[0].text
                            serialNumber = fields[1].text
                            inventoryNumber = fields[2].text
                            location = fields[3].text
                            status = fields[4].text
                            manufacturer = fields[5].text
                        }.also { vendingMachineDAO.save(it) }
                    }
                    Product::class.java -> {
                        Product().apply {
                            name = fields[0].text
                            price = fields[1].text.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                            quantityInStock = fields[2].text.toIntOrNull() ?: 0
                            minimalStock = fields[3].text.toIntOrNull() ?: 0
                            description = fields[4].text
                        }.also { productDAO.save(it) }
                    }
                    User::class.java -> {
                        User().apply {
                            firstName = fields[0].text
                            lastName = fields[1].text
                            email = fields[2].text
                            phone = fields[3].text
                            roleId = fields[4].text.toIntOrNull() ?: 1
                            passwordHash = fields[5].text // В реальном приложении нужно хешировать
                        }.also { userDAO.save(it) }
                    }
                    else -> null
                }
            } else null
        }

        val result = dialog.showAndWait()
        result.ifPresent {
            refreshCurrentTable()
        }
    }

    private fun addTextField(grid: GridPane, label: String, row: Int, fields: MutableList<TextField>) {
        grid.add(Label(label), 0, row)
        val textField = TextField()
        grid.add(textField, 1, row)
        fields.add(textField)
    }

    private fun showEditDialog(item: Any) {
        val dialog = Dialog<Any>()
        dialog.title = "Редактировать запись"
        dialog.headerText = "Изменение записи ID: ${getIdValue(item)}"

        val dialogPane = dialog.dialogPane
        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(20.0)

        val fields = mutableListOf<TextField>()
        var row = 0

        when (item) {
            is VendingMachine -> {
                addTextFieldWithValue(grid, "Модель:", row++, item.model ?: "", fields)
                addTextFieldWithValue(grid, "Серийный номер:", row++, item.serialNumber ?: "", fields)
                addTextFieldWithValue(grid, "Инвентарный номер:", row++, item.inventoryNumber ?: "", fields)
                addTextFieldWithValue(grid, "Локация:", row++, item.location ?: "", fields)
                addTextFieldWithValue(grid, "Статус:", row++, item.status ?: "", fields)
                addTextFieldWithValue(grid, "Производитель:", row++, item.manufacturer ?: "", fields)
            }
            is Product -> {
                addTextFieldWithValue(grid, "Название:", row++, item.name, fields)
                addTextFieldWithValue(grid, "Цена:", row++, item.price.toString(), fields)
                addTextFieldWithValue(grid, "Количество:", row++, item.quantityInStock.toString(), fields)
                addTextFieldWithValue(grid, "Мин. запас:", row++, item.minimalStock.toString(), fields)
                addTextFieldWithValue(grid, "Описание:", row++, item.description ?: "", fields)
            }
            is User -> {
                addTextFieldWithValue(grid, "Имя:", row++, item.firstName, fields)
                addTextFieldWithValue(grid, "Фамилия:", row++, item.lastName, fields)
                addTextFieldWithValue(grid, "Email:", row++, item.email ?: "", fields)
                addTextFieldWithValue(grid, "Телефон:", row++, item.phone ?: "", fields)
                addTextFieldWithValue(grid, "Роль ID:", row++, item.roleId.toString(), fields)
            }
        }

        dialogPane.content = grid

        dialog.setResultConverter { buttonType ->
            if (buttonType == ButtonType.OK) {
                when (item) {
                    is VendingMachine -> {
                        item.apply {
                            model = fields[0].text
                            serialNumber = fields[1].text
                            inventoryNumber = fields[2].text
                            location = fields[3].text
                            status = fields[4].text
                            manufacturer = fields[5].text
                        }.also { vendingMachineDAO.update(it) }
                    }
                    is Product -> {
                        item.apply {
                            name = fields[0].text
                            price = fields[1].text.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                            quantityInStock = fields[2].text.toIntOrNull() ?: 0
                            minimalStock = fields[3].text.toIntOrNull() ?: 0
                            description = fields[4].text
                        }.also { productDAO.update(it) }
                    }
                    is User -> {
                        item.apply {
                            firstName = fields[0].text
                            lastName = fields[1].text
                            email = fields[2].text
                            phone = fields[3].text
                            roleId = fields[4].text.toIntOrNull() ?: 1
                        }.also { userDAO.update(it) }
                    }
                    else -> null
                }
            } else null
        }

        val result = dialog.showAndWait()
        result.ifPresent {
            refreshCurrentTable()
        }
    }

    private fun addTextFieldWithValue(grid: GridPane, label: String, row: Int, value: String, fields: MutableList<TextField>) {
        grid.add(Label(label), 0, row)
        val textField = TextField(value)
        grid.add(textField, 1, row)
        fields.add(textField)
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
        alert.contentText = "ID: ${getIdValue(item)}"

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
            VendingMachine::class.java -> showVendingMachines()
            Product::class.java -> showProducts()
            Sale::class.java -> showSales()
            User::class.java -> showUsers()
            Maintenance::class.java -> showMaintenance()
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