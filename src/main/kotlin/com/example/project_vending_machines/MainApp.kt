package com.example.project_vending_machines

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.image.ImageView
import javafx.scene.image.Image
import java.io.FileInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.project_vending_machines.dao.VendingMachineDAO
import com.example.project_vending_machines.entity.VendingMachine

class MainApp : Application() {

    private val vendingMachineDAO = VendingMachineDAO()
    private val machineData: ObservableList<VendingMachine> = FXCollections.observableArrayList()
    private val tableView = TableView<VendingMachine>()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ООО Торговые Автоматы - Личный кабинет"

        // Загрузка данных
        loadData()

        val root = BorderPane()
        root.top = createHeader()
        root.left = createNavigation()
        root.center = createMainContent()

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
        navigation.prefWidth = 200.0

        val navItems = listOf(
            "Главная" to true,
            "Монитор ТА" to false,
            "Детальные отчеты" to false,
            "Учет ТМЦ" to false,
            "Администрирование" to false
        )

        navItems.forEach { (item, isSelected) ->
            val button = Button(item)
            button.style = if (isSelected)
                "-fx-background-color: #3498db; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT;"
            else
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-pref-width: 180px; -fx-alignment: CENTER_LEFT;"
            button.setOnAction {
                // Обработка навигации
            }
            navigation.children.add(button)
        }

        return navigation
    }

    private fun createMainContent(): ScrollPane {
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

        return scrollPane
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

        // Пример данных
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

        // Пример данных
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

    private fun loadData() {
        try {
            val machines = vendingMachineDAO.getAll()
            machineData.setAll(machines)
        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Ошибка", "Не удалось загрузить данные: ${e.message}")
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